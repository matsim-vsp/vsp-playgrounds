package playground.ikaddoura.analysis.moneyGIS;
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

/**
 * 
 * @author ikaddoura
 *
 */
public class TollGISAnalyzerMain {
	
	private final String runDirectory = "/Users/ihab/Documents/workspace/runs-svn/cne/berlin-dz-1pct-simpleNetwork/output-FINAL/m_r_output_run4_bln_cne_DecongestionPID/";
	private final String scenarioCRS = TransformationFactory.DHDN_GK4;
	
	// private final String shapeFileZones = "/Users/ihab/Documents/workspace/shared-svn/studies/ihab/berlin/shapeFiles/berlin_grid_1500/berlin_grid_1500.shp";
	// private final String crs = TransformationFactory.DHDN_GK4;

	private final String shapeFileZones = "/Users/ihab/Documents/workspace/shared-svn/studies/ihab/berlin/shapeFiles/berlin_LOR_SHP_EPSG_3068/Planungsraum_EPSG_3068.shp";
	private final String zonesCRS = TransformationFactory.DHDN_SoldnerBerlin;
	
	private final String outputFileName = "tolls_CNA_userBenefits_bezirke.shp";
	
	private final String homeActivity = "home";
	private final int scalingFactor = 100;
	
	private static final Logger log = Logger.getLogger(TollGISAnalyzerMain.class);
	
	public static void main(String[] args) throws IOException {
		TollGISAnalyzerMain main = new TollGISAnalyzerMain();
		main.run();
	}
		
	public void run() throws IOException {
		
		log.info("Loading scenario...");
		Scenario scenario = loadScenario();
		log.info("Loading scenario... Done.");
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		
		MoneyExtCostHandler moneyHandler = new MoneyExtCostHandler();
		eventsManager.addHandler(moneyHandler);
		
		log.info("Reading events file...");
		CombinedPersonLinkMoneyEventsReader reader = new CombinedPersonLinkMoneyEventsReader(eventsManager);
		String eventsFile1 = runDirectory + "/ITERS/it." + scenario.getConfig().controler().getLastIteration() + "/" + scenario.getConfig().controler().getLastIteration() + ".events.xml.gz";
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
		TollGISAnalyzer gisAnalysis = new TollGISAnalyzer(shapeFileZones, scalingFactor, homeActivity, zonesCRS, scenarioCRS, outputFileName);
		gisAnalysis.analyzeZoneTollsUserBenefits(scenario, runDirectory, personId2userBenefits, moneyHandler.getPersonId2toll(), moneyHandler.getPersonId2congestionToll(), moneyHandler.getPersonId2noiseToll(), moneyHandler.getPersonId2airPollutionToll() );
		log.info("Analyzing zones... Done.");
	}
	
	private Scenario loadScenario() {
		Config config = ConfigUtils.loadConfig(runDirectory + "output_config.xml.gz");
		config.network().setInputFile(runDirectory + "output_network.xml.gz");
		config.plans().setInputFile(runDirectory + "output_plans.xml.gz");
		config.vehicles().setVehiclesFile(null);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}
		
}
