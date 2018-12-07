/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.agentSpecificActivityScheduling;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.analysis.pngSequence2Video.MATSimVideoUtils;

/**
* @author ikaddoura
*/

public class ASASRunExample {
	private static final Logger log = Logger.getLogger(ASASRunExample.class);

	private static String configFile;
	private static String outputDirectory;
	private static String runId;

	public static void main(String[] args) throws IOException {
		
		if (args.length > 0) {
			configFile = args[0];		
			log.info("config file: "+ configFile);
			
			outputDirectory = args[1];
			log.info("output directory: "+ outputDirectory);
			
			runId = args[2];
			log.info("run Id: "+ runId);
			
		} else {
			configFile = "/Users/ihab/Documents/workspace/runs-svn/berlin-dz-time/input/input_0.1sample/config_route_time_test.xml";
			outputDirectory = "";
		}

		Config config = ConfigUtils.loadConfig(configFile, new AgentSpecificActivitySchedulingConfigGroup());
		if (!outputDirectory.isEmpty()) config.controler().setOutputDirectory(outputDirectory);
		config.controler().setRunId(runId);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		controler.addOverridingModule(new AgentSpecificActivitySchedulingModule(scenario));
		
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
		MATSimVideoUtils.createLegHistogramVideo(controler.getConfig().controler().getOutputDirectory());
	}

}

