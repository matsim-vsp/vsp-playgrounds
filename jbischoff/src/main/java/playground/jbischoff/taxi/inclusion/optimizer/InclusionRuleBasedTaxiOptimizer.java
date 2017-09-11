/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxi.inclusion.optimizer;

import java.util.Iterator;
import java.util.TreeSet;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Requests;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder;
import org.matsim.contrib.taxi.optimizer.rules.UnplannedRequestZonalRegistry;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.contrib.zone.SquareGridSystem;
import org.matsim.contrib.zone.ZonalSystem;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class InclusionRuleBasedTaxiOptimizer extends AbstractTaxiOptimizer {
	protected final BestDispatchFinder dispatchFinder;

	protected final InclusionIdleTaxiZonalRegistry idleTaxiRegistry;
	private final UnplannedRequestZonalRegistry unplannedRequestRegistry;

	private final InclusionRuleBasedTaxiOptimizerParams params;

	public InclusionRuleBasedTaxiOptimizer(TaxiConfigGroup taxiCfg, Fleet fleet, Network network, MobsimTimer timer,
			TravelTime travelTime, TravelDisutility travelDisutility, TaxiScheduler scheduler,
			InclusionRuleBasedTaxiOptimizerParams params) {
		this(taxiCfg, fleet, network, timer, travelTime, travelDisutility, scheduler, params,
				new SquareGridSystem(network, params.cellSize));
	}

	public InclusionRuleBasedTaxiOptimizer(TaxiConfigGroup taxiCfg, Fleet fleet, Network network, MobsimTimer timer,
			TravelTime travelTime, TravelDisutility travelDisutility, TaxiScheduler scheduler,
			InclusionRuleBasedTaxiOptimizerParams params, ZonalSystem zonalSystem) {
		super(taxiCfg, fleet, scheduler, params, new TreeSet<TaxiRequest>(Requests.ABSOLUTE_COMPARATOR), false, false);

		this.params = params;

		if (taxiCfg.isVehicleDiversion()) {
			throw new RuntimeException("Diversion is not supported by RuleBasedTaxiOptimizer");
		}

		dispatchFinder = new BestDispatchFinder(scheduler, network, timer, travelTime, travelDisutility);
		idleTaxiRegistry = new InclusionIdleTaxiZonalRegistry(zonalSystem, scheduler, params.INCLUSION_TAXI_PREFIX);
		unplannedRequestRegistry = new UnplannedRequestZonalRegistry(zonalSystem);
	}

	@Override
	protected void scheduleUnplannedRequests() {

		scheduleUnplannedRequestsImpl();// reduce T_W (regular NOS), for the time being, no overloads expected

	}

	// request-initiated scheduling
	private void scheduleUnplannedRequestsImpl() {
		int idleCount = idleTaxiRegistry.getVehicleCount();

		Iterator<TaxiRequest> reqIter = getUnplannedRequests().iterator();
		while (reqIter.hasNext() && idleCount > 0) {
			TaxiRequest req = reqIter.next();
			boolean barrierFreeRequest = req.getPassenger().getId().toString()
					.startsWith(params.INCLUSION_CUSTOMER_PREFIX) ? true : false;
			Iterable<Vehicle> selectedVehs = idleTaxiRegistry.findNearestVehicles(req.getFromLink().getFromNode(),
					barrierFreeRequest);

			if (barrierFreeRequest) {
				// Logger.getLogger(getClass()).info("barrier free request for : "+req.getPassenger().getId()+".
				// Assigned Vehicles: "+selectedVehs.toString());
			}

			BestDispatchFinder.Dispatch<TaxiRequest> best = dispatchFinder.findBestVehicleForRequest(req, selectedVehs);

			if (best != null) {
				getScheduler().scheduleRequest(best.vehicle, best.destination, best.path);

				reqIter.remove();
				unplannedRequestRegistry.removeRequest(req);
				idleCount--;
			}
		}
	}

	@Override
	public void requestSubmitted(Request request) {
		super.requestSubmitted(request);
		unplannedRequestRegistry.addRequest((TaxiRequest)request);
	}

	@Override
	public void nextTask(Vehicle vehicle) {
		super.nextTask(vehicle);

		Schedule schedule = vehicle.getSchedule();
		if (schedule.getStatus() == ScheduleStatus.COMPLETED) {
			TaxiStayTask lastTask = (TaxiStayTask)Schedules.getLastTask(schedule);
			if (lastTask.getBeginTime() < vehicle.getServiceEndTime()) {
				idleTaxiRegistry.removeVehicle(vehicle);
			}
		} else if (getScheduler().isIdle(vehicle)) {
			idleTaxiRegistry.addVehicle(vehicle);
		} else {
			if (schedule.getCurrentTask().getTaskIdx() != 0) {// not first task
				TaxiTask previousTask = (TaxiTask)Schedules.getPreviousTask(schedule);
				if (isWaitStay(previousTask)) {
					idleTaxiRegistry.removeVehicle(vehicle);
				}
			}
		}
	}

	@Override
	protected boolean doReoptimizeAfterNextTask(TaxiTask newCurrentTask) {
		return isWaitStay(newCurrentTask);
	}

	protected boolean isWaitStay(TaxiTask task) {
		return task.getTaxiTaskType() == TaxiTaskType.STAY;
	}
}
