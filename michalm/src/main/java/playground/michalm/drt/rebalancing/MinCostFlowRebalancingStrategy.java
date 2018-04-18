/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.michalm.drt.rebalancing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.google.inject.name.Named;
import com.vividsolutions.jts.geom.Geometry;

/**
 * @author michalm
 */
public class MinCostFlowRebalancingStrategy implements RebalancingStrategy {
	public interface RebalancingTargetCalculator {
		int estimate(String zone, double time);
	}

	private static final double MAX_REMAINING_TIME_TO_IDLENESS = 1800;// for soon-idle vehicles
	private static final double MIN_REMAINING_SERVICE_TIME = 3600;// for idle vehicles

	private final RebalancingTargetCalculator rebalancingTargetCalculator;
	private final DrtZonalSystem zonalSystem;
	private final Network network;
	private final Fleet fleet;

	@Inject
	public MinCostFlowRebalancingStrategy(RebalancingTargetCalculator rebalancingTargetCalculator,
			DrtZonalSystem zonalSystem, @Named(DvrpRoutingNetworkProvider.DVRP_ROUTING) Network network, Fleet fleet) {
		this.rebalancingTargetCalculator = rebalancingTargetCalculator;
		this.zonalSystem = zonalSystem;
		this.network = network;
		this.fleet = fleet;
	}

	@Override
	public List<Relocation> calcRelocations(Stream<? extends Vehicle> rebalancableVehicles, double time) {
		Map<String, List<Vehicle>> rebalancableVehiclesPerZone = groupRebalancableVehicles(rebalancableVehicles, time);
		if (rebalancableVehiclesPerZone.isEmpty()) {
			return Collections.emptyList();
		}
		Map<String, List<Vehicle>> soonIdleVehiclesPerZone = groupSoonIdleVehicles(time);

		List<Triple<String, String, Integer>> interZonalRelocations = solveTransportProblem(time,
				rebalancableVehiclesPerZone, soonIdleVehiclesPerZone);
		return calcRelocations(rebalancableVehiclesPerZone, interZonalRelocations);
	}

	private Map<String, List<Vehicle>> groupRebalancableVehicles(Stream<? extends Vehicle> rebalancableVehicles,
			double time) {
		Map<String, List<Vehicle>> rebalancableVehiclesPerZone = new HashMap<>();
		rebalancableVehicles.filter(v -> v.getServiceEndTime() > time + MIN_REMAINING_SERVICE_TIME).forEach(v -> {
			Link link = ((StayTask)v.getSchedule().getCurrentTask()).getLink();
			String zone = zonalSystem.getZoneForLinkId(link.getId());
			if (zone != null) {
				// zonePerVehicle.put(v.getId(), zone);
				rebalancableVehiclesPerZone.computeIfAbsent(zone, z -> new ArrayList<>()).add(v);
			}
		});
		return rebalancableVehiclesPerZone;
	}

	// also include vehicles being right now relocated or recharged
	private Map<String, List<Vehicle>> groupSoonIdleVehicles(double time) {
		Map<String, List<Vehicle>> soonIdleVehiclesPerZone = new HashMap<>();
		for (Vehicle v : fleet.getVehicles().values()) {
			Schedule s = v.getSchedule();
			StayTask stayTask = (StayTask)Schedules.getLastTask(s);
			if (stayTask.getStatus() == TaskStatus.PLANNED
					&& stayTask.getBeginTime() < time + MAX_REMAINING_TIME_TO_IDLENESS
					&& stayTask.getBeginTime() < time + MIN_REMAINING_SERVICE_TIME) {// XXX a separate constant???
				String zone = zonalSystem.getZoneForLinkId(stayTask.getLink().getId());
				if (zone != null) {
					soonIdleVehiclesPerZone.computeIfAbsent(zone, z -> new ArrayList<>()).add(v);
				}
			}
		}
		return soonIdleVehiclesPerZone;
	}

	private List<Triple<String, String, Integer>> solveTransportProblem(double time,
			Map<String, List<Vehicle>> rebalancableVehiclesPerZone,
			Map<String, List<Vehicle>> soonIdleVehiclesPerZone) {
		List<Pair<String, Integer>> producers = new ArrayList<>();
		List<Pair<String, Integer>> consumers = new ArrayList<>();

		for (String z : zonalSystem.getZones().keySet()) {
			int rebalancable = rebalancableVehiclesPerZone.getOrDefault(z, Collections.emptyList()).size();
			int soonIdle = soonIdleVehiclesPerZone.getOrDefault(z, Collections.emptyList()).size();
			int target = rebalancingTargetCalculator.estimate(z, time);

			int delta = Math.min(rebalancable + soonIdle - target, rebalancable);
			if (delta < 0) {
				consumers.add(Pair.of(z, -delta));
			} else if (delta > 0) {
				producers.add(Pair.of(z, delta));
			}
		}

		return new TransportProblem<String, String>(this::calcStraightLineDistance).solve(producers, consumers);
	}

	private int calcStraightLineDistance(String zone1, String zone2) {
		return (int)DistanceUtils.calculateDistance(zonalSystem.getZoneCentroid(zone1),
				zonalSystem.getZoneCentroid(zone2));
	}

	private List<Relocation> calcRelocations(Map<String, List<Vehicle>> rebalancableVehiclesPerZone,
			List<Triple<String, String, Integer>> interZonalRelocations) {
		List<Relocation> relocations = new ArrayList<>();
		for (Triple<String, String, Integer> r : interZonalRelocations) {
			List<Vehicle> rebalancableVehicles = rebalancableVehiclesPerZone.get(r.getLeft());

			String toZone = r.getMiddle();
			Geometry z = zonalSystem.getZone(toZone);
			Coord zoneCentroid = MGC.point2Coord(z.getCentroid());
			Link destinationLink = NetworkUtils.getNearestLink(network, zoneCentroid);

			int flow = r.getRight();
			for (int f = 0; f < flow; f++) {
				// TODO use BestDispatchFinder (needs to be moved from taxi to dvrp) instead
				Vehicle nearestVehicle = findNearestVehicle(rebalancableVehicles, destinationLink);
				relocations.add(new Relocation(nearestVehicle, destinationLink));
				rebalancableVehicles.remove(nearestVehicle);// TODO use map to have O(1) removal
			}
		}
		return relocations;
	}

	private Vehicle findNearestVehicle(List<Vehicle> rebalancableVehicles, Link destinationLink) {
		Coord toCoord = destinationLink.getFromNode().getCoord();
		return rebalancableVehicles.stream().min(Comparator.comparing(v -> DistanceUtils.calculateSquaredDistance(//
				Schedules.getLastLinkInSchedule(v).getToNode().getCoord(), toCoord))).get();
	}
}
