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

package playground.santiago.avcongestion;

import org.matsim.contrib.av.flow.AvIncreasedCapacityModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.trafficmonitoring.TravelTimeUtils;
import org.matsim.contrib.taxi.run.RunTaxiScenario;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiControlerCreator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.name.Names;

public class RunSantiagoAVFlow {
	public static void run(String configFile, double flowEfficiencyFactor, String inputEvents) {
		Config config = ConfigUtils.loadConfig(configFile, new TaxiConfigGroup(), new DvrpConfigGroup());
		final Controler controler = TaxiControlerCreator.createControler(config, false);

		// to speed up computations
//		final TravelTime initialTT = TravelTimeUtils.createTravelTimesFromEvents(controler.getScenario(), inputEvents);
//		controler.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
//				bind(TravelTime.class).annotatedWith(Names.named(DvrpTravelTimeModule.DVRP_INITIAL))
//						.toInstance(initialTT);
//			}
//		});

		controler.addOverridingModule(new AvIncreasedCapacityModule(flowEfficiencyFactor));

		controler.run();
	}

	public static void main(String[] args) {
		String dir = "D:\\matsim-eclipse\\runs-svn\\santiago_AT_10pc\\";
		String config = dir + "taxi_only_config_2.0.xml";
		double flowEfficiencyFactor = 2.0;
		run(config, flowEfficiencyFactor, null);
	}
}
