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

package playground.agarwalamit.opdyts.patna.networkModesOnly;

import java.util.Arrays;
import java.util.List;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareEventHandler;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.opdyts.ObjectiveFunctionEvaluator.ObjectiveFunctionType;
import playground.agarwalamit.opdyts.OpdytsScenario;
import playground.agarwalamit.opdyts.analysis.OpdytsModalStatsControlerListener;
import playground.agarwalamit.utils.FileUtils;

/**
 * @author amit
 */

class PatnaNetworkModesPlansRelaxor {

	public static void main (String[] args) {

		String configFile = "";
		String outDir = "";

		if ( args.length >0 ){
			configFile = args[0];
			outDir = args[1];
		} else {
			configFile = FileUtils.RUNS_SVN+"/opdyts/patna/networkModes/relaxedPlans/inputs/config_networkModesOnly.xml";
			outDir = FileUtils.RUNS_SVN+"/opdyts/patna/networkModes/relaxedPlans/output_selectExpBeta/";
		}

		Config config= ConfigUtils.loadConfig(configFile);
		config.controler().setOutputDirectory(outDir);

		new PatnaNetworkModesPlansRelaxor().run(config);
	}

	public void run (Config config) {
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		List<String> modes2consider = Arrays.asList("car","bike","motorbike");

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.bind(ModalShareEventHandler.class);
				this.addControlerListenerBinding().to(ModalShareControlerListener.class);

				this.bind(ModalTripTravelTimeHandler.class);
				this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);

				this.addControlerListenerBinding().toInstance(
						new OpdytsModalStatsControlerListener(modes2consider, new PatnaNetworkModesOneBinDistanceDistribution(
						OpdytsScenario.PATNA_1Pct)));

				this.bind(ObjectiveFunctionType.class).toInstance(ObjectiveFunctionType.SUM_SQR_DIFF_NORMALIZED);
			}
		});

		controler.run();

		// delete unnecessary iterations folder here.
		int firstIt = controler.getConfig().controler().getFirstIteration();
		int lastIt = controler.getConfig().controler().getLastIteration();
		FileUtils.deleteIntermediateIterations(config.controler().getOutputDirectory(),firstIt,lastIt);
	}
}