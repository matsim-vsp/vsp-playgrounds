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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;

import playground.vsp.demandde.cemdap.output.ActivityTypes;

/**
* @author ikaddoura
*/

public class AdjustConfigActivityParameters {
	private final Logger log = Logger.getLogger(AdjustConfigActivityParameters.class);
	private String configInputFile = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/be_3/config/config_be_300_mode-time-route.xml";
	private String outputConfigFile = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/be_3/config/config_be_300_mode-time-route_adjusted.xml";

	public static void main(String[] args) {	
		AdjustConfigActivityParameters adjustParameters = new AdjustConfigActivityParameters();
		adjustParameters.run();	
	}

	private void run() {
		Config config = ConfigUtils.loadConfig(configInputFile);
		
		for (ActivityParams params : config.planCalcScore().getActivityParams()) {
			
			String activityType = params.getActivityType();

			if (activityType.contains("interaction") || activityType.contains("dummy")) {
				log.info("Skipping activity " + activityType + "...");
				
			} else {
				
				if (activityType.startsWith(ActivityTypes.HOME)) {
					// don't set any opening or closing time
					
				} else if (activityType.startsWith(ActivityTypes.WORK)) {
					params.setOpeningTime(6 * 3600.);
					params.setClosingTime(20 * 3600.);
					
				} else if (activityType.startsWith(ActivityTypes.EDUCATION)) {
					params.setOpeningTime(8 * 3600.);
					params.setClosingTime(16 * 3600.);
					
				} else if (activityType.startsWith(ActivityTypes.LEISURE)) {
					params.setOpeningTime(9 * 3600.);
					params.setClosingTime(27 * 3600.);
					
				} else if (activityType.startsWith(ActivityTypes.SHOPPING)) {
					params.setOpeningTime(8 * 3600.);
					params.setClosingTime(20 * 3600.);
					
				} else if (activityType.startsWith(ActivityTypes.OTHER)) {
					// don't set any opening or closing time
					
				} else {
					log.warn("Skipping activity: " + activityType + "...");
				}
			}
		}
		
		ConfigWriter configWriter = new ConfigWriter(config);
		configWriter.write(outputConfigFile);
	}

}

