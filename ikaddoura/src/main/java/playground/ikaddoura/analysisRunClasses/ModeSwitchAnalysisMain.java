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

package playground.ikaddoura.analysisRunClasses;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.analysis.detailedPersonTripAnalysis.IKEventsReader;
import org.matsim.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;
import org.matsim.analysis.modeSwitchAnalysis.PersonTripScenarioComparison;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * ikaddoura
 * 
 */
public class ModeSwitchAnalysisMain {
	private final static Logger log = Logger.getLogger(ModeSwitchAnalysisMain.class);
	
    public static void main(String[] args) throws IOException {
        ModeSwitchAnalysisMain modeSwitcherAnalyser = new ModeSwitchAnalysisMain();
        modeSwitcherAnalyser.analyze();
    }

    public void analyze() throws IOException {
    	
		final String subpopulation = null;
    	
        // 0: base case
    	String directory0 = "/Users/ihab/Desktop/ils3a/kaddoura/snz-berlin/output/output_2019-06-03_snz-bc-1/";
    	String runId0 = "snz-bc-1";
    	
		// 1: policy case
    	String directory1 = "/Users/ihab/Desktop/ils3a/kaddoura/snz-berlin/output/output_2019-06-11_snz-drt-3/";
    	String runId1 = "snz-drt-3";
    	
    	// ################
    	
		String dir0lastIterationFile = directory0 + runId0 + ".output_events.xml.gz";
	    String dir0networkFile = directory0 + runId0 + ".output_network.xml.gz";
	    String dir0populationFile = directory0 + runId0 + ".output_plans.xml.gz";
	    
	    String dir1lastIterationFile = directory1 + runId0 + ".output_events.xml.gz";
	    String dir1networkFile = directory1 + runId1 + ".output_network.xml.gz";
	    String dir1populationFile = directory1 + runId1 + ".output_plans.xml.gz";	  
		
		String analysisOutputFolder = "modeSwitchAnalysis/";
		File f = new File(directory1 + analysisOutputFolder);
		f.mkdirs();
		
		BasicPersonTripAnalysisHandler basicHandler0;
		BasicPersonTripAnalysisHandler basicHandler1;
		
		Scenario scenario0;
		{
			log.info("Loading scenario0 and reading events...");

			Config config = ConfigUtils.createConfig();	
			config.network().setInputCRS("EPSG:25832");
			config.global().setCoordinateSystem("EPSG:25832");
			config.plans().setInputFile(dir0populationFile);
			config.network().setInputFile(dir0networkFile);
			
			scenario0 = ScenarioUtils.loadScenario(config);
			
			final String[] helpLegModes = {TransportMode.transit_walk, TransportMode.non_network_walk, TransportMode.access_walk, TransportMode.egress_walk};
			final String stageActivitySubString = "interaction";
	        
	        basicHandler0 = new BasicPersonTripAnalysisHandler(helpLegModes, stageActivitySubString);
			basicHandler0.setScenario(scenario0);
			
			EventsManager events = EventsUtils.createEventsManager();
			events.addHandler(basicHandler0);
			
			IKEventsReader reader = new IKEventsReader(events);
			reader.readFile(dir0lastIterationFile);
			log.info("Loading scenario0 and reading events... Done.");
		}
		
		Scenario scenario1;
		{
			log.info("Loading scenario1 and reading events...");
			Config config = ConfigUtils.createConfig();	
			config.plans().setInputFile(dir1populationFile);
			config.network().setInputFile(dir1networkFile);
			
			scenario1 = ScenarioUtils.loadScenario(config);
	        
			final String[] helpLegModes = {TransportMode.transit_walk, TransportMode.access_walk, TransportMode.egress_walk};
			final String stageActivitySubString = "interaction";
			
	        basicHandler1 = new BasicPersonTripAnalysisHandler(helpLegModes, stageActivitySubString);
			basicHandler1.setScenario(scenario1);
			
			EventsManager events = EventsUtils.createEventsManager();
			events.addHandler(basicHandler1);
			
			IKEventsReader reader = new IKEventsReader(events);
			reader.readFile(dir1lastIterationFile);
			
			log.info("Loading scenario1 and reading events... Done.");
		}
		
		List<String> modes = new ArrayList<>();
		modes.add(TransportMode.car);
		modes.add(TransportMode.taxi);
		PersonTripScenarioComparison modeSwitchAnalysis = new PersonTripScenarioComparison("home", analysisOutputFolder, scenario1, basicHandler1, scenario0, basicHandler0, modes, null);
		modeSwitchAnalysis.analyzeByMode();
    }
}
