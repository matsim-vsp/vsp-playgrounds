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

import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerParams;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

/**
 * @author tschlenther
 *
 */
public class TSPrivateAVTaxiDispatcher extends DefaultTaxiOptimizer {

	/**
	 * @param taxiCfg
	 * @param fleet
	 * @param scheduler
	 * @param params
	 * @param requestInserter
	 * @param requestValidator
	 * @param eventsManager
	 */
	public TSPrivateAVTaxiDispatcher(TaxiConfigGroup taxiCfg, Fleet fleet, TaxiScheduler scheduler,
			DefaultTaxiOptimizerParams params, TSPrivateAVRequestInserter requestInserter,
			PassengerRequestValidator requestValidator, EventsManager eventsManager) {
		super(taxiCfg, fleet, scheduler, params, requestInserter, requestValidator, eventsManager);
		// TODO Auto-generated constructor stub
	}

	
	@Override
	protected void handleAimlessDriveTasks() {
		// TODO Auto-generated method stub
		//here we insert the freight plans ? 
		super.handleAimlessDriveTasks();
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		// TODO Auto-generated method stub
		//
		super.notifyMobsimBeforeSimStep(e);
	}
}
