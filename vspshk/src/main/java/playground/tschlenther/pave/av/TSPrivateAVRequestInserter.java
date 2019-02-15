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

/**
 * 
 */
package playground.tschlenther.pave.av;

import java.util.Collection;
import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.taxi.passenger.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.UnplannedRequestInserter;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

import playground.jbischoff.avparking.optimizer.PrivateAVScheduler;

/**
 * @author tschlenther
 *
 */
public class TSPrivateAVRequestInserter implements UnplannedRequestInserter {

	private final Fleet fleet;
	private final TaxiScheduler scheduler;
	private final MobsimTimer timer;
	private final TravelTime travelTime;
	private final LeastCostPathCalculator router;
	
	/**
	 * 
	 */
	public TSPrivateAVRequestInserter(Fleet fleet, TaxiScheduler scheduler, MobsimTimer timer, TravelTime travelTime,
			LeastCostPathCalculator router) {
		this.fleet = fleet;
		this.scheduler = scheduler;
		this.timer = timer;
		this.travelTime = travelTime;
		this.router = router;
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.taxi.optimizer.UnplannedRequestInserter#scheduleUnplannedRequests(java.util.Collection)
	 */
	@Override
	public void scheduleUnplannedRequests(Collection<TaxiRequest> unplannedRequests) {
		Iterator<TaxiRequest> reqIter = unplannedRequests.iterator();
		while (reqIter.hasNext()) {
			TaxiRequest req = reqIter.next();
			Id<DvrpVehicle> personalAV = Id.create(req.getPassengerId().toString() + "_av", DvrpVehicle.class);
			DvrpVehicle veh = fleet.getVehicles().get(personalAV);
			if (veh == null) {
				throw new RuntimeException("Vehicle " + personalAV.toString() + "does not exist.");
			}
			if (!isWaitStayOrEmptyDrive((TaxiTask)veh.getSchedule().getCurrentTask())) {
				throw new RuntimeException("Vehicle " + personalAV.toString() + "is not idle.");

			}
			if (((TaxiTask)veh.getSchedule().getCurrentTask()).getTaxiTaskType() == TaxiTaskType.EMPTY_DRIVE) {
				((PrivateAVScheduler)scheduler).stopCruisingVehicle(veh);
			}

			VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(Schedules.getLastLinkInSchedule(veh),
					req.getFromLink(), timer.getTimeOfDay(), router, travelTime);
			scheduler.scheduleRequest(veh, req, path);
			
			
			/*
			 * this is the part in PrivatAVTaxiRequestInserter we don't need 
			 */
//			Id<Link> parkLinkId = manager.getVehicleParkingLocation(Id.createVehicleId(personalAV));
//			if (parkLinkId != null) {
//				manager.unParkVehicleHere(Id.createVehicleId(personalAV), parkLinkId, path.getDepartureTime());
//			}
			
			reqIter.remove();

		}
	}

	private boolean isWaitStayOrEmptyDrive(TaxiTask task) {
		return task.getTaxiTaskType() == TaxiTaskType.STAY || task.getTaxiTaskType() == TaxiTaskType.EMPTY_DRIVE;
	}
}
