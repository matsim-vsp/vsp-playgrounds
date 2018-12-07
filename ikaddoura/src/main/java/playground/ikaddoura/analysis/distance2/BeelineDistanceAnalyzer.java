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

package playground.ikaddoura.analysis.distance2;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.analysis.modules.legModeDistanceDistribution.LegModeDistanceDistribution;

/**
* @author ikaddoura
*/

public class BeelineDistanceAnalyzer {

	private static String outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV/output/output_v0_SAVuserOpCostPricingF_SAVuserExtCostPricingF_SAVdriverExtCostPricingF_CCuserExtCostPricingT/";
	private static String plansFile = outputDirectory + "run1.output_plans.xml.gz";

	public static void main(String[] args) {
		LegModeDistanceDistribution lmdd = new LegModeDistanceDistribution();
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(plansFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		lmdd.init(scenario);
		lmdd.postProcessData();
		new File(outputDirectory + "/LegModeDistanceDistribution").mkdir(); 
		lmdd.writeResults(outputDirectory + "/LegModeDistanceDistribution/");
	}

}

