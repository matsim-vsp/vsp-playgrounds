/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.jbischoff.avparking;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiOutputModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Binder;

import playground.jbischoff.avparking.optimizer.PrivateAVOptimizerProvider;

/**
 * @author jbischoff An example how to use parking search in MATSim.
 *         Technically, all you need as extra input is a facilities file
 *         containing "car interaction" locations.
 *
 *
 */

public class RunAvParking {

	public static void main(String[] args) {
		
		Config config = ConfigUtils.loadConfig("C:/Users/Joschka/Desktop/parkingsearch/config.xml", new DvrpConfigGroup(), new TaxiConfigGroup());
		//all further input files are set in the config.
		
		// set to false, if you don't require visualisation, then the example will run for 11 iterations, with OTFVis, only one iteration is performed. 
		boolean otfvis = false;
		if (otfvis) {
			config.controler().setLastIteration(0);
		} else {
			config.controler().setLastIteration(10);
		}
		new RunAvParking().run(config,otfvis);

	}

	/**
	 * @param config
	 * 			a standard MATSim config
	 * @param otfvis
	 *            turns otfvis visualisation on or off
	 */
	public void run(Config config, boolean otfvis) {
		config.qsim().setStartTime(0);
		config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);
		config.qsim().setSnapshotStyle(SnapshotStyle.withHoles);
		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		
		
		
		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}
		PrivateAVFleetGenerator fleet = new PrivateAVFleetGenerator(scenario);  
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(Fleet.class).toInstance(fleet);
				addControlerListenerBinding().toInstance(fleet);				
			}
		});
		controler.addOverridingModule(new ParkingTaxiModule(PrivateAVOptimizerProvider.class));
		controler.addOverridingModule(new TaxiOutputModule());
		controler.run();
	}

}
