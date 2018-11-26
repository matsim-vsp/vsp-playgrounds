/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

package playground.jbischoff.sharedTaxiBerlin.saturdaynight;

import org.matsim.contrib.drt.run.Drt;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import playground.jbischoff.sharedTaxiBerlin.saturdaynight.ZonalSystem.OptimizationCriterion;
import playground.jbischoff.utils.JbUtils;

/**
 * @author jbischoff
 */
public class RunSharedTaxiWithDynamicZones {
	public static void main(String[] args) {

		String folder = "../../../shared-svn/projects/sustainability-w-michal-and-dlr/data/scenarios/drt_saturdaynight/";
		Config config = ConfigUtils.loadConfig(folder + "config0.1.xml", new DrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());
		config.plans().setInputFile("population_night_bln_dummy_0.1.xml");
		config.controler().setOutputDirectory("D:/runs-svn/sharedTaxi/trb_zones/fare_10");
		config.controler().setLastIteration(250);
		config.controler().setWriteEventsInterval(10);
		ZonalSystem zones = new ZonalSystem(
				JbUtils.readShapeFileAndExtractGeometry(folder + "shp/berlin_grid_1500.shp", "ID"),
				OptimizationCriterion.Fare);

		DrtConfigGroup drt = (DrtConfigGroup)config.getModules().get(DrtConfigGroup.GROUP_NAME);
		drt.setEstimatedBeelineDistanceFactor(1.5);
		drt.setVehiclesFile("vehicles_50.xml");
		drt.setNumberOfThreads(7);
		drt.setMaxTravelTimeAlpha(1.5);
		drt.setMaxTravelTimeBeta(420);
		drt.setMaxWaitTime(420);
		drt.setIdleVehiclesReturnToDepots(false);

		Controler controler = DrtControlerCreator.createControler(config, false);
		ZonalBasedRequestValidator validator = new ZonalBasedRequestValidator(controler.getScenario().getNetwork(),
				zones);

		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				addControlerListenerBinding().to(TaxiZoneManager.class).asEagerSingleton();
				bind(ZonalOccupancyAggregator.class).asEagerSingleton();
				bind(SharedTaxiFareCalculator.class).asEagerSingleton();
				bind(PassengerRequestValidator.class).annotatedWith(Drt.class).toInstance(validator);
				bind(ZonalBasedRequestValidator.class).toInstance(validator);
				bind(ZonalSystem.class).toInstance(zones);

			}
		});
		controler.run();
	}
}
