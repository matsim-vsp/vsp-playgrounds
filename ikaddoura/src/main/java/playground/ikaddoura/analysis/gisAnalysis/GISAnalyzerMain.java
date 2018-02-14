package playground.ikaddoura.analysis.gisAnalysis;
/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.personLinkMoneyEvents.CombinedPersonLinkMoneyEventsReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;

/**
 * 
 * @author ikaddoura
 *
 */
public class GISAnalyzerMain {
	
//	private final String runDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV_new/output/output_v0_SAVuserOpCostPricingF_SAVuserExtCostPricingF_SAVdriverExtCostPricingF_CCuserExtCostPricingF/";
//	private final String scenarioCRS = TransformationFactory.DHDN_GK4;
//	private final String runId = "run0";
	
//	private final String runDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV_new/output/output_v0_SAVuserOpCostPricingF_SAVuserExtCostPricingF_SAVdriverExtCostPricingF_CCuserExtCostPricingT/";
//	private final String scenarioCRS = TransformationFactory.DHDN_GK4;
//	private final String runId = "run1";
	
//	private final String runDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV_new/output/output_v5000_SAVuserOpCostPricingF_SAVuserExtCostPricingF_SAVdriverExtCostPricingF_CCuserExtCostPricingF/";
//	private final String scenarioCRS = TransformationFactory.DHDN_GK4;
//	private final String runId = "run2b";
	
//	private final String runDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV_new/output/output_v5000_SAVuserOpCostPricingT_SAVuserExtCostPricingF_SAVdriverExtCostPricingF_CCuserExtCostPricingF/";
//	private final String scenarioCRS = TransformationFactory.DHDN_GK4;
//	private final String runId = "run3b";
	
//	private final String runDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV_new/output/output_v5000_SAVuserOpCostPricingT_SAVuserExtCostPricingT_SAVdriverExtCostPricingT_CCuserExtCostPricingF/";
//	private final String scenarioCRS = TransformationFactory.DHDN_GK4;
//	private final String runId = "run4b";
	
//	private final String runDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV_new/output/output_v5000_SAVuserOpCostPricingT_SAVuserExtCostPricingT_SAVdriverExtCostPricingT_CCuserExtCostPricingT/";
//	private final String scenarioCRS = TransformationFactory.DHDN_GK4;
//	private final String runId = "run5b";
	
	private final String runDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV_new/output/output_v5000_SAVuserOpCostPricingT_SAVuserExtCostPricingF_SAVdriverExtCostPricingF_CCuserExtCostPricingT/";
	private final String scenarioCRS = TransformationFactory.DHDN_GK4;
	private final String runId = "run6b";
	
	// ####
	
	private final String shapeFileZones = "/Users/ihab/Documents/workspace/shared-svn/studies/ihab/berlin/shapeFiles/berlin_grid_2500/berlin_grid_2500.shp";
	private final String zonesCRS = TransformationFactory.DHDN_GK4;
	private final String outputFileName = "tolls_userBenefits_travelTime_modes_zones_2500.shp";

//	private final String shapeFileZones = "/Users/ihab/Documents/workspace/shared-svn/studies/ihab/berlin/shapeFiles/berlin_grid_1500/berlin_grid_1500.shp";
//	private final String zonesCRS = TransformationFactory.DHDN_GK4;
//	private final String outputFileName = "tolls_userBenefits_travelTime_modes_zones_1500.shp";

//	private final String shapeFileZones = "/Users/ihab/Documents/workspace/shared-svn/studies/ihab/berlin/shapeFiles/berlin_LOR_SHP_EPSG_3068/Planungsraum_EPSG_3068.shp";
//	private final String zonesCRS = TransformationFactory.DHDN_SoldnerBerlin;
//	private final String outputFileName = "tolls_userBenefits_travelTime_modes_zones_LOR.shp";

	private final String homeActivity = "home";
	private final int scalingFactor = 10;
	
	private static final Logger log = Logger.getLogger(GISAnalyzerMain.class);
	
	public static void main(String[] args) throws IOException {
		GISAnalyzerMain main = new GISAnalyzerMain();
		main.run();
	}
		
	public void run() throws IOException {
		
		log.info("Loading scenario...");
		Scenario scenario = loadScenario();
		log.info("Loading scenario... Done.");
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		
		MoneyExtCostHandler moneyHandler = new MoneyExtCostHandler();
		eventsManager.addHandler(moneyHandler);
		
		BasicPersonTripAnalysisHandler basicHandler = new BasicPersonTripAnalysisHandler();
		basicHandler.setScenario(scenario);
		eventsManager.addHandler(basicHandler);
		
		log.info("Reading events file...");
		CombinedPersonLinkMoneyEventsReader reader = new CombinedPersonLinkMoneyEventsReader(eventsManager);
		String eventsFile1 = runDirectory + "/ITERS/it." + scenario.getConfig().controler().getLastIteration() + "/" + runId + "." + scenario.getConfig().controler().getLastIteration() + ".events.xml.gz";
		reader.readFile(eventsFile1);
		log.info("Reading events file... Done.");
				
		Map<Id<Person>, Double> personId2userBenefits = new HashMap<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			double score = 0.0;
			if (person.getSelectedPlan().getScore() != null) {
				score = person.getSelectedPlan().getScore();
			} else {
				log.warn("null score: " + person.toString());
			}
			personId2userBenefits.put(person.getId(), score);
		}
		
		log.info("Analyzing zones...");
		GISAnalyzer gisAnalysis = new GISAnalyzer(scenario, shapeFileZones, scalingFactor, homeActivity, zonesCRS, scenarioCRS);
		gisAnalysis.analyzeZoneTollsUserBenefits(runDirectory, runId + "." + outputFileName, personId2userBenefits, moneyHandler.getPersonId2toll(), moneyHandler.getPersonId2congestionToll(), moneyHandler.getPersonId2noiseToll(), moneyHandler.getPersonId2airPollutionToll(), basicHandler );
		log.info("Analyzing zones... Done.");
	}
	
	private Scenario loadScenario() {
		Config config = ConfigUtils.loadConfig(runDirectory + runId + ".output_config.xml");
		config.network().setInputFile(runDirectory + runId + ".output_network.xml.gz");
		config.plans().setInputFile(runDirectory + runId + ".output_plans.xml.gz");
		config.plans().setInputPersonAttributeFile(null);
		config.vehicles().setVehiclesFile(null);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}
		
}
