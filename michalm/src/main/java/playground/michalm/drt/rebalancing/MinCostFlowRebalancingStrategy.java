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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.ZonalDemandAggregator;
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
	private static final double MAX_REMAINING_TIME_TO_IDLENESS = 1800;// for soon-idle vehicles
	private static final double MIN_REMAINING_SERVICE_TIME = 3600;// for idle vehicles

	private ZonalDemandAggregator demandAggregator;
	private DrtZonalSystem zonalSystem;
	private Network network;
	private Fleet fleet;

	private Map<String, List<Vehicle>> rebalancableVehiclesPerZone;
	private Map<String, List<Vehicle>> soonIdleVehiclesPerZone;

	@Inject
	public MinCostFlowRebalancingStrategy(ZonalDemandAggregator demandAggregator, DrtZonalSystem zonalSystem,
			@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING) Network network, Fleet fleet) {
		this.demandAggregator = demandAggregator;
		this.zonalSystem = zonalSystem;
		this.network = network;
		this.fleet = fleet;
	}

	@Override
	public List<Relocation> calcRelocations(Stream<? extends Vehicle> rebalancableVehicles, double time) {
		Map<Id<Vehicle>, Vehicle> rebalancableVehiclesMap = rebalancableVehicles
				.filter(v -> v.getServiceEndTime() > time + MIN_REMAINING_SERVICE_TIME)
				.collect(Collectors.toMap(v -> v.getId(), v -> v));
		if (rebalancableVehiclesMap.isEmpty()) {
			return Collections.emptyList();
		}

		groupRebalancableVehicles(rebalancableVehiclesMap.values());
		groupSoonIdleVehicles(time);

		List<Triple<String, String, Integer>> interZonalRelocations = solveTransportProblem(time, this::estimateTarget);

		return calcRelocations(interZonalRelocations);
	}

	private void groupRebalancableVehicles(Collection<Vehicle> rebalancableVehicles) {
		rebalancableVehiclesPerZone = new HashMap<>();
		for (Vehicle v : rebalancableVehicles) {
			Link link = ((StayTask)v.getSchedule().getCurrentTask()).getLink();
			String zone = zonalSystem.getZoneForLinkId(link.getId());
			if (zone != null) {
				// zonePerVehicle.put(v.getId(), zone);
				rebalancableVehiclesPerZone.computeIfAbsent(zone, z -> new ArrayList<>()).add(v);
			}
		}
	}

	// also include vehicles being right now relocated or recharged
	private void groupSoonIdleVehicles(double time) {
		soonIdleVehiclesPerZone = new HashMap<>();
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
	}

	private List<Triple<String, String, Integer>> solveTransportProblem(double time, IntUnaryOperator targetEstimator) {
		// XXX this "time+60" means probably "in the next time bin"
		Map<String, MutableInt> expectedDemandMap = demandAggregator.getExpectedDemandForTimeBin(time + 60);
		List<Pair<String, Integer>> producers = new ArrayList<>();
		List<Pair<String, Integer>> consumers = new ArrayList<>();

		for (String z : zonalSystem.getZones().keySet()) {
			int rebalancable = rebalancableVehiclesPerZone.getOrDefault(z, Collections.emptyList()).size();
			int soonIdle = soonIdleVehiclesPerZone.getOrDefault(z, Collections.emptyList()).size();

			MutableInt expectedDemand = expectedDemandMap.get(z);
			int target = expectedDemand == null ? 0 : targetEstimator.applyAsInt(expectedDemand.intValue());

			int delta = Math.min(rebalancable + soonIdle - target, rebalancable);
			if (delta < 0) {
				consumers.add(Pair.of(z, -delta));
			} else if (delta > 0) {
				producers.add(Pair.of(z, delta));
			}
		}

		return new TransportProblem<String, String>(this::calcStraightLineDistance).solve(producers, consumers);
	}

	// FIXME targets should be calculated more intelligently
	private int estimateTarget(int expectedDemand) {
		if (expectedDemand == 0) {
			return 0; // for larger zones we may assume that target is at least 1 ??????
		}
		double alpha = 0.5;
		double beta = 0.5;
		return (int)Math.round(alpha * expectedDemand + beta);
	}

	private int calcStraightLineDistance(String zone1, String zone2) {
		return (int)DistanceUtils.calculateDistance(zonalSystem.getZoneCentroid(zone1),
				zonalSystem.getZoneCentroid(zone2));
	}

	private List<Relocation> calcRelocations(List<Triple<String, String, Integer>> interZonalRelocations) {
		List<Relocation> relocations = new ArrayList<>();
		for (Triple<String, String, Integer> r : interZonalRelocations) {
			List<Vehicle> rebalancableVehicles = rebalancableVehiclesPerZone.get(r.getLeft());

			String toZone = r.getMiddle();
			Geometry z = zonalSystem.getZone(toZone);
			Coord zoneCentroid = MGC.point2Coord(z.getCentroid());
			Link destinationLink = NetworkUtils.getNearestLink(network, zoneCentroid);

			int flow = r.getRight();
			for (int f = 0; f < flow; f++) {
				Vehicle nearestVehicle = findNearestVehicle(rebalancableVehicles, destinationLink);
				relocations.add(new Relocation(nearestVehicle, destinationLink));
				rebalancableVehicles.remove(nearestVehicle);// TODO use map to have O(1) removal
			}
		}
		return relocations;
	}

	private Vehicle findNearestVehicle(List<Vehicle> rebalancableVehicles, Link destinationLink) {
		double closestDistance = Double.MAX_VALUE;
		Vehicle closestVeh = null;
		for (Vehicle v : rebalancableVehicles) {
			Link vl = Schedules.getLastLinkInSchedule(v);
			if (vl != null) {
				double distance = DistanceUtils.calculateDistance(vl.getCoord(),
						destinationLink.getFromNode().getCoord());
				if (distance < closestDistance) {
					closestDistance = distance;
					closestVeh = v;
				}
			}
		}
		return closestVeh;
	}
}
