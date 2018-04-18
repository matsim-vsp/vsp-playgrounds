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

package playground.michalm.drt.run;

import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalModule;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.ZonalDemandAggregator;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import playground.michalm.drt.rebalancing.MinCostFlowRebalancingStrategy;

public class RunSharedTaxiMielec {
	public static void main(String[] args) {
		String configFile = "mielec_2014_02/mielec_drt_config.xml";
		RunSharedTaxiMielec.run(configFile, false, true);
	}

	public static void run(String configFile, boolean otfvis, boolean rebalancing) {
		Config config = ConfigUtils.loadConfig(configFile, new DvrpConfigGroup(), new DrtConfigGroup(),
				new OTFVisConfigGroup(), new TaxiFareConfigGroup());

		DrtConfigGroup drtCfg = DrtConfigGroup.get(config);
		// drtCfg.setMaxWaitTime(maxWaitTime);

		drtCfg.setRebalancingInterval(600);
		config.controler().setLastIteration(1);
		config.controler().setWriteEventsInterval(1);

		Controler controler = DrtControlerCreator.createControler(config, otfvis);

		if (rebalancing == true) {
			System.err.println("Rebalancing Online");

			DrtZonalSystem zones = new DrtZonalSystem(controler.getScenario().getNetwork(), 1000);

			controler.addOverridingModule(new DrtZonalModule());
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bind(DrtZonalSystem.class).toInstance(zones);
					bind(RebalancingStrategy.class).to(MinCostFlowRebalancingStrategy.class).asEagerSingleton();
					bind(ZonalDemandAggregator.class).asEagerSingleton();
				}
			});
		}

		controler.run();
	}
}
