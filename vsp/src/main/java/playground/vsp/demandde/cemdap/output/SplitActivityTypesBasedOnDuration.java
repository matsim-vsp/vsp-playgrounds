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

package playground.vsp.demandde.cemdap.output;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.TypicalDurationScoreComputation;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
* @author ikaddoura
*/

public class SplitActivityTypesBasedOnDuration {
	private final Logger log = Logger.getLogger(SplitActivityTypesBasedOnDuration.class);

	private Scenario scenario;
	
	public SplitActivityTypesBasedOnDuration(String inputPopulationFile) {
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(inputPopulationFile);
		this.scenario = ScenarioUtils.loadScenario(config);
	}

	public static void main(String[] args) {
		
		final String inputPopulationFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_3/population/pop_300/plans_10pct_corine-landcover.xml.gz";
		
		final String outputPopulationFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_3/population/pop_300/plans_10pct_corine-landcover_test.xml.gz";
		final String outputConfigFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_3/population/pop_300/config_activity-parameters_test.xml";
		
		final double timeBinSize_s = 600.;
		final String[] activities = {ActivityTypes.HOME, ActivityTypes.WORK, ActivityTypes.EDUCATION, ActivityTypes.LEISURE, ActivityTypes.SHOPPING, ActivityTypes.OTHER}; 
		
		SplitActivityTypesBasedOnDuration splitAct = new SplitActivityTypesBasedOnDuration(inputPopulationFile);
		splitAct.run(outputPopulationFile, outputConfigFile, timeBinSize_s, activities);	
	}

	private void run(String outputPopulationFile, String outputConfigFile, double timeBinSize_s, String[] activities) {
		
		CemdapPopulationTools tools = new CemdapPopulationTools();
		tools.setActivityTypesAccordingToDurationAndMergeOvernightActivities(scenario.getPopulation(), timeBinSize_s);
		
		PopulationWriter writer = new PopulationWriter(scenario.getPopulation());
		writer.write(outputPopulationFile);
		
		// config
				
		List<ActivityParams> initialActivityParams = new ArrayList<>();
		
		log.info("Initial activity parameters: ");
		for (String activity : activities) {
			ActivityParams params = new ActivityParams(activity);
			initialActivityParams.add(params);
			log.info(" -> " + params.getActivityType());
		}
		
		log.info("Adding duration-specific activity types to config...");
		
		List<ActivityParams> newActivityParams = new ArrayList<>();
		
		for (ActivityParams actParams : initialActivityParams) {
			String activityType = actParams.getActivityType();
			
			if (activityType.contains("interaction")) {
				log.info("Skipping activity " + activityType + "...");
				
			} else {
				
				log.info("Splitting activity " + activityType + " in duration-specific activities.");

//				double maximumDuration = 27 * 3600.;
				double maximumDuration = tools.getMaxEndTime();
				
				for (double n = timeBinSize_s; n <= maximumDuration ; n = n + timeBinSize_s) {
					ActivityParams params = new ActivityParams(activityType + "_" + n);
					params.setTypicalDuration(n);
					params.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
					newActivityParams.add(params);
				}
			}
		}
		
		for (ActivityParams actParams : newActivityParams) {
			scenario.getConfig().planCalcScore().addActivityParams(actParams);
		}
		
		log.info("New activity parameters: ");
		for (ActivityParams actParams : scenario.getConfig().planCalcScore().getActivityParams()) {
			initialActivityParams.add(actParams);
			log.info(" -> " + actParams.getActivityType());
		}
		
		log.info("Adding duration-specific activity types to config... Done.");
		
		ConfigWriter configWriter = new ConfigWriter(scenario.getConfig());
		configWriter.write(outputConfigFile);
	}

}

