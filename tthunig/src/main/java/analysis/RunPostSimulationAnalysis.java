/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author tthunig
 */
public class RunPostSimulationAnalysis {

//	private static String runDirectory = "../../runs-svn/cottbus/mt-its19/2019-02-5-18-6-55_1000it_LaemmerFix_14re3re7re_minG5_cap07_fixSensor_fixReset/";
//	private static String runDirectory = "../../runs-svn/cottbus/mt-its19/2019-02-5-17-22-3_1000it_Sylvia_noFixedCycle_maxExt1.5_cap07_fixSensor/";
	private static String runDirectory = "../../runs-svn/cottbus/mt-its19/2018-11-24-20-9-17_1000it_MS_cap07/";
	private static String outputDirectory = runDirectory + "ITERS/it.1000/analysis/";
	private static String runId = "1000";
	
	private static PrintStream writingStream;

	public static void main(String[] args) {
		
		Config config = ConfigUtils.createConfig();
		config.controler().setRunId(runId);
		config.network().setInputFile(runDirectory + runId + ".output_network.xml.gz");
		config.controler().setOutputDirectory(runDirectory);
		config.global().setCoordinateSystem("EPSG:25833");
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Network fullNetwork = scenario.getNetwork();
		String subnetworkFeature = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/shape_files/signal_systems/bounding_box.shp";
		TtSubnetworkAnalyzer subNetAnalyzer = new TtSubnetworkAnalyzer(subnetworkFeature, fullNetwork);

		createWritingStream();
		
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(subNetAnalyzer);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		
		reader.readFile(runDirectory + "ITERS/it.0/" + runId + ".0.events.xml.gz");
		writeItAnalysis(subNetAnalyzer, 0);
		
		subNetAnalyzer.reset(1000);
		reader.readFile(runDirectory + "ITERS/it.1000/" + runId + ".1000.events.xml.gz");
		writeItAnalysis(subNetAnalyzer, 1000);
				
		writingStream.close();
	}
	
	private static void createWritingStream() {
		// create writing stream
		try {
			writingStream = new PrintStream(new File(outputDirectory + "subNetworkPostAnalysis.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		// write header
		String header = "it \ttotal tt inner city[s] \ttotal delay inner city[s] \ttotal dist inner city[m] \tnumber of trips inner city \trel number of trips inner city";
		writingStream.println(header);
	}

	private static void writeItAnalysis(TtSubnetworkAnalyzer subNetAnalyzer, int iteration) {
		StringBuffer line = new StringBuffer();
		line.append(iteration);
		line.append("\t" + subNetAnalyzer.getTotalTtSubnetwork());
		line.append("\t" + subNetAnalyzer.getTotalDelaySubnetwork());
		line.append("\t" + subNetAnalyzer.getTotalDistanceSubnetwork());
		line.append("\t" + subNetAnalyzer.getNumberOfTripsInSubnetwork());
		line.append("\t" + subNetAnalyzer.getRelativeNumberOfTripsInSubnetwork());
		writingStream.println(line.toString());
	}

}
