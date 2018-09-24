/*
 * *********************************************************************** *
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
 * *********************************************************************** *
 */

package playground.michalm.multimodedvrp;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.optimizer.insertion.DefaultUnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.insertion.ParallelPathDataProvider;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiControlerCreator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author Michal Maciejewski (michalm)
 */
public class RunMultiModeDvrp {
	static final String CONFIG_FILE = "multi_mode_dvrp/multi_mode_dvrp_config.xml";

	public static void run(String configFile, boolean otfvis) {
		Config config = ConfigUtils.loadConfig(configFile, new DrtConfigGroup(), new TaxiConfigGroup(),
				new DvrpConfigGroup(), new OTFVisConfigGroup());
		Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory(config);
		ScenarioUtils.loadScenario(scenario);
		config.controler()
				.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		Controler controler = new Controler(scenario);
		DrtControlerCreator.addDrtWithoutDvrpModuleToControler(controler);
		TaxiControlerCreator.addTaxiWithoutDvrpModuleToControler(controler);

		String taxiMode = TaxiConfigGroup.get(controler.getConfig()).getMode();
		DvrpModeQSimModule taxiModeQSimModule = new DvrpModeQSimModule.Builder(taxiMode).addListener(
				TaxiOptimizer.class).build();

		String drtMode = DrtConfigGroup.get(controler.getConfig()).getMode();
		DvrpModeQSimModule drtModeQSimModule = new DvrpModeQSimModule.Builder(drtMode).addListener(DrtOptimizer.class)
				.addListener(DefaultUnplannedRequestInserter.class)
				.addListener(ParallelPathDataProvider.class)
				.build();

		controler.addOverridingModule(new DvrpModule(taxiModeQSimModule, drtModeQSimModule));
		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}

		controler.run();
	}

	public static void main(String[] args) {
		run(CONFIG_FILE, false); // switch to 'true' to turn on visualisation
	}

}
