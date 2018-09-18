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

package playground.ikaddoura.optAV;

import org.apache.log4j.Logger;
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.run.RunBerlinDrtScenario;

/**
* @author ikaddoura
*/

public class RunBerlinDrtOptScenario {
	private static final Logger log = Logger.getLogger(RunBerlinDrtOptScenario.class);

	public static void main(String[] args) {
		
		String configFileName;
		String overridingConfigFileName;
		String berlinShapeFile;
		String drtServiceAreaShapeFile;
		String transitStopCoordinatesSFile;
		String transitStopCoordinatesRBFile;
		
		String runId;
		String outputDirectory;
		
		if (args.length > 0) {
			
			configFileName = args[0];
			overridingConfigFileName = args[1];
			berlinShapeFile = args[2];
			drtServiceAreaShapeFile = args[3];
			transitStopCoordinatesSFile = args[4];
			transitStopCoordinatesRBFile = args[5];
			runId = args[6];
			outputDirectory = args[7];
			
		} else {
			
			String baseDirectory = "/Users/ihab/Documents/workspace/matsim-berlin/";
			
			configFileName = baseDirectory + "scenarios/berlin-v5.2-1pct/input/berlin-drt-v5.2-1pct.config_2agents_optAV.xml"; // 2 test agents
//			configFileName = baseDirectory + "scenarios/berlin-v5.2-1pct/input/berlin-drt-v5.2-1pct.config.xml"; // berlin 1pct
			
			overridingConfigFileName = null;
			
			berlinShapeFile = baseDirectory + "scenarios/berlin-v5.2-10pct/input/berlin-shp/berlin.shp";
			drtServiceAreaShapeFile = baseDirectory + "scenarios/berlin-v5.2-10pct/input/berlin_bb_area/service-area.shp";

			transitStopCoordinatesSFile = baseDirectory + "scenarios/berlin-v5.2-10pct/input/berlin-v5.2.transit-stop-coordinates_S-zoneC.csv";
			transitStopCoordinatesRBFile = baseDirectory + "scenarios/berlin-v5.2-10pct/input/berlin-v5.2.transit-stop-coordinates_RB-zoneC.csv";
			
			runId = "drt-opt-1";
			outputDirectory = baseDirectory + "scenarios/berlin-v5.2-1pct/output-local-run_" + runId + "/";
		}
			
		log.info("run Id: " + runId);
		log.info("output directory: " + outputDirectory); 

		RunBerlinDrtScenario berlin = new RunBerlinDrtScenario(configFileName, overridingConfigFileName, berlinShapeFile, drtServiceAreaShapeFile, transitStopCoordinatesSFile, transitStopCoordinatesRBFile);
		
		ConfigGroup[] modulesToAdd = {new OptAVConfigGroup(), new DecongestionConfigGroup(), new NoiseConfigGroup()};
		Config config = berlin.prepareConfig(modulesToAdd);
		config.controler().setRunId(runId);
		config.controler().setOutputDirectory(outputDirectory);
				
		Controler controler = berlin.prepareControler();		
		controler.addOverridingModule(new DRTpricingModule(controler.getScenario()));		
		
		controler.run();
		
		log.info("Done.");
	}

}

