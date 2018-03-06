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

package playground.ikaddoura.berlin;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

/**
* @author ikaddoura
*/

public class RunMATSimWithRaptor {

	private static final Logger log = Logger.getLogger(RunMATSimWithRaptor.class);

	private static String configFile;
	private static String outputDirectory;
	private static String runId;	
	
	private static double ascCar;
	private static double ascPt;
	private static double ascWalk;
	private static double ascBicycle;	
	
	public static void main(String[] args) {
		if (args.length > 0) {
			
			configFile = args[0];		
			log.info("configFile: "+ configFile);
			
			outputDirectory = args[1];
			log.info("outputDirectory: "+ outputDirectory);
			
			runId = args[2];
			log.info("runId: "+ runId);
			
			ascCar = Double.parseDouble(args[3]);
			log.info("ascCar: "+ ascCar);

			ascPt = Double.parseDouble(args[4]);
			log.info("ascPt: "+ ascPt);

			ascWalk = Double.parseDouble(args[5]);
			log.info("ascWalk: "+ ascWalk);

			ascBicycle = Double.parseDouble(args[6]);
			log.info("ascBicycle: "+ ascBicycle);

			
		} else {
			
			configFile = "/Users/ihab/Desktop/ils4a/ziemke/open_berlin_scenario/input/be_3_ik/config_be_300_mode-choice_test.xml";
			outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/open_berlin_scenario/be_300_test_7/";
			runId = "test-run";
			ascCar = -2.;
			ascPt = -1.;
			ascWalk = 0.;
			ascBicycle = -3.;
		}
		
		RunMATSimWithRaptor runner = new RunMATSimWithRaptor();
		runner.run(configFile, outputDirectory, runId);
	}

	public void run(String configFile, String outputDirectory, String runId) {
		
		Config config = ConfigUtils.loadConfig(configFile);
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.failIfDirectoryExists);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setRunId(runId);
		
		config.planCalcScore().getModes().get(TransportMode.car).setConstant(ascCar);
		config.planCalcScore().getModes().get(TransportMode.pt).setConstant(ascPt);
		config.planCalcScore().getModes().get(TransportMode.walk).setConstant(ascWalk);
		config.planCalcScore().getModes().get("bicycle").setConstant(ascBicycle);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new SwissRailRaptorModule());
			}
		});
				
		controler.run();
		
	}

}

