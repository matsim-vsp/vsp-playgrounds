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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import playground.ikaddoura.analysis.IKAnalysisRun;
import playground.ikaddoura.analysis.modalSplitUserType.AgentAnalysisFilter;

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
	private static double ascRide;	
	
	private static double marginalUtilityOfDistanceBicycle = Double.POSITIVE_INFINITY;
	private static double marginalUtilityOfDistanceWalk = Double.POSITIVE_INFINITY;

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
			
			ascRide = Double.parseDouble(args[7]);
			log.info("ascRide: "+ ascRide);
			
			if (args.length > 8) {
				marginalUtilityOfDistanceBicycle = Double.parseDouble(args[8]);
				log.info("marginalUtilityOfDistanceBicycle: "+ marginalUtilityOfDistanceBicycle);
				
				marginalUtilityOfDistanceWalk = Double.parseDouble(args[9]);
				log.info("marginalUtilityOfDistanceWalk: "+ marginalUtilityOfDistanceWalk);
			}

			
		} else {
			
			configFile = "/Users/ihab/Desktop/ils4a/ziemke/open_berlin_scenario/input/be_3_ik/config_be_300_mode-choice_test.xml";
			outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/open_berlin_scenario/be_300_test_7/";
			runId = "test-run";
			ascCar = -1.;
			ascPt = -1.;
			ascWalk = 0.;
			ascBicycle = -1.;
			ascRide = -1.;
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
		config.planCalcScore().getModes().get(TransportMode.ride).setConstant(ascRide);

		if (marginalUtilityOfDistanceWalk < Double.POSITIVE_INFINITY) config.planCalcScore().getModes().get(TransportMode.walk).setMarginalUtilityOfDistance(marginalUtilityOfDistanceWalk);
		if (marginalUtilityOfDistanceBicycle < Double.POSITIVE_INFINITY) config.planCalcScore().getModes().get("bicycle").setMarginalUtilityOfDistance(marginalUtilityOfDistanceBicycle);
	
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new SwissRailRaptorModule());
			}
		});
				
		controler.run();
		
		log.info("Running analysis...");
				
		final String scenarioCRS = TransformationFactory.DHDN_GK4;	
		final String shapeFileZones = null;
		final String zonesCRS = null;
		final String homeActivity = "home";
		final int scalingFactor = 10;
		
		List<AgentAnalysisFilter> filters = new ArrayList<>();

		AgentAnalysisFilter filter1 = new AgentAnalysisFilter(scenario);
		filter1.setSubpopulation("person");
		filter1.setPersonAttribute("berlin");
		filter1.setPersonAttributeName("home-activity-zone");
		filter1.preProcess(scenario);
		filters.add(filter1);
		
		AgentAnalysisFilter filter2 = new AgentAnalysisFilter(scenario);
		filter2.preProcess(scenario);
		filters.add(filter2);
		
		AgentAnalysisFilter filter3 = new AgentAnalysisFilter(scenario);
		filter3.setSubpopulation("person");
		filter3.setPersonAttribute("brandenburg");
		filter3.setPersonAttributeName("home-activity-zone");
		filter3.preProcess(scenario);
		filter3.preProcess(scenario);
		filters.add(filter3);

		IKAnalysisRun analysis = new IKAnalysisRun(
				scenario,
				null,
				scenarioCRS,
				shapeFileZones,
				zonesCRS,
				homeActivity,
				scalingFactor,
				filters,
				null);
		analysis.run();
	
		log.info("Done.");
		
	}

}

