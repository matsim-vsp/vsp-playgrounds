/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.DvrpVehicle;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder;
import org.matsim.contrib.taxi.optimizer.UnplannedRequestInserter;
import org.matsim.contrib.taxi.optimizer.rules.IdleTaxiZonalRegistry;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.rules.UnplannedRequestZonalRegistry;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author michalm
 */
public class InclusionRuleBasedRequestInserter implements UnplannedRequestInserter {
	public static final String INCLUSION_TAXI_PREFIX = "hc_";
	public static final String INCLUSION_CUSTOMER_PREFIX = "hc_";

	private final TaxiScheduler scheduler;
	private final BestDispatchFinder dispatchFinder;

	private final IdleTaxiZonalRegistry idleTaxiRegistry;
	private final UnplannedRequestZonalRegistry unplannedRequestRegistry;

	public InclusionRuleBasedRequestInserter(TaxiScheduler scheduler, MobsimTimer timer, Network network,
			TravelTime travelTime, TravelDisutility travelDisutility, RuleBasedTaxiOptimizerParams params,
			IdleTaxiZonalRegistry idleTaxiRegistry, UnplannedRequestZonalRegistry unplannedRequestRegistry) {
		this(scheduler, new BestDispatchFinder(scheduler, network, timer, travelTime, travelDisutility), params,
				idleTaxiRegistry, unplannedRequestRegistry);
	}

	public InclusionRuleBasedRequestInserter(TaxiScheduler scheduler, BestDispatchFinder dispatchFinder,
			RuleBasedTaxiOptimizerParams params, IdleTaxiZonalRegistry idleTaxiRegistry,
			UnplannedRequestZonalRegistry unplannedRequestRegistry) {
		this.scheduler = scheduler;
		this.dispatchFinder = dispatchFinder;
		this.idleTaxiRegistry = idleTaxiRegistry;
		this.unplannedRequestRegistry = unplannedRequestRegistry;
	}

	@Override
	public void scheduleUnplannedRequests(Collection<TaxiRequest> unplannedRequests) {
		// reduce T_W (regular NOS), for the time being, no overloads expected
		// request-initiated scheduling
		int idleCount = idleTaxiRegistry.getVehicleCount();

		Iterator<TaxiRequest> reqIter = unplannedRequests.iterator();
		while (reqIter.hasNext() && idleCount > 0) {
			TaxiRequest req = reqIter.next();
			boolean barrierFreeRequest = req.getPassengerId().toString().startsWith(INCLUSION_CUSTOMER_PREFIX)
					? true : false;

			Stream<DvrpVehicle> selectedVehs = idleTaxiRegistry.findNearestVehicles(req.getFromLink().getFromNode(),
					Integer.MAX_VALUE, barrierFreeRequest ? this::isBarrierFree : null);

			if (barrierFreeRequest) {
				// Logger.getLogger(getClass()).info("barrier free request for : "+req.getPassengerId()+".
				// Assigned Vehicles: "+selectedVehs.toString());
			}

			BestDispatchFinder.Dispatch<TaxiRequest> best = dispatchFinder.findBestVehicleForRequest(req, selectedVehs);

			if (best != null) {
				scheduler.scheduleRequest(best.vehicle, best.destination, best.path);

				reqIter.remove();
				unplannedRequestRegistry.removeRequest(req);
				idleCount--;
			}
		}
	}

	private boolean isBarrierFree(DvrpVehicle vehicle) {
		return vehicle.getId().toString().startsWith(INCLUSION_TAXI_PREFIX);
	}
}
