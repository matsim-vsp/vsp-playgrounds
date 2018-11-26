/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * SwitchingMobsimProvider.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.contrib.pseudosimulation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.pseudosimulation.transit.TransitEmulator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSimProvider;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class SwitchingMobsimProvider implements Provider<Mobsim> {

	@Inject
	private TravelTime travelTime;

	@Inject
	private TransitEmulator transitEmulator;

	@Inject
	private Scenario scenario;

	@Inject
	private EventsManager eventsManager;

	@Inject
	private MobSimSwitcher mobSimSwitcher;
	
	@Inject
	private QSimProvider qsimProvider;

	@Override
	public Mobsim get() {
		if (this.mobSimSwitcher.isQSimIteration()) {
			return this.qsimProvider.get();
		} else {
			return new PSim(this.scenario, this.eventsManager, 
					this.mobSimSwitcher.getPlansForPSim(),
					this.travelTime,
					this.transitEmulator);
		}
	}	
}
