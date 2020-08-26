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

import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class RunSharedTaxiMielec {
	public static void main(String[] args) {
		String configFile = "mielec_2014_02/mielec_drt_config.xml";
		RunSharedTaxiMielec.run(configFile, false);
	}

	public static void run(String configFile, boolean otfvis) {
		Config config = ConfigUtils.loadConfig(configFile, new DvrpConfigGroup(), new MultiModeDrtConfigGroup(),
				new OTFVisConfigGroup());

		DrtConfigGroup drtCfg = DrtConfigGroup.getSingleModeDrtConfig(config);
		// drtCfg.setMaxWaitTime(maxWaitTime);

		RebalancingParams rebalancingParams = drtCfg.getRebalancingParams().get();
		rebalancingParams.setInterval(600);
		rebalancingParams.setCellSize(500);

		config.controler().setLastIteration(1);
		config.controler().setWriteEventsInterval(1);
		config.controler().setOutputDirectory("d:/temp/mielec-rebalancing/zone_0500_target_a_50_b_00");

		Controler controler = DrtControlerCreator.createControler(config, otfvis);

		controler.run();
	}
}
