/* *********************************************************************** *
* project: org.matsim.*
* firstControler
* *
* *********************************************************************** *
* *
* copyright : (C) 2007 by the members listed in the COPYING, *
* LICENSE and WARRANTY file. *
* email : info at matsim dot org *
* *
* *********************************************************************** *
* *
* This program is free software; you can redistribute it and/or modify *
* it under the terms of the GNU General Public License as published by *
* the Free Software Foundation; either version 2 of the License, or *
* (at your option) any later version. *
* See also COPYING, LICENSE and WARRANTY file *
* *
* *********************************************************************** */ 

package playground.ikaddoura.analysis.qgis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.ikaddoura.analysis.linkDemand.LinkDemandEventHandler;
import playground.ikaddoura.analysis.shapes.Network2Shape;

public class GisAnalysis {
	private final static Logger log = Logger.getLogger(GisAnalysis.class);
	
	private static String runDirectory1 = "/Users/ihab/Desktop/ils4/kaddoura/incidents/output/output_run1a_baseCase/";
	private static String runDirectory2 = "/Users/ihab/Desktop/ils4/kaddoura/incidents/output/output_run2a_2016-02-11/";

	private final String crs = TransformationFactory.DHDN_GK4;
	private final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(crs, crs);

	
	private final String outputDirectory1;
	private final String outputDirectory2;

	public static void main(String[] args) {
		GisAnalysis analysis = new GisAnalysis(runDirectory1, runDirectory2);
		analysis.run();
	}
	
	public GisAnalysis(String runDirectory1, String runDirectory2) {
		this.outputDirectory1 = runDirectory1;
		this.outputDirectory2 = runDirectory2;
	}

	public void run() {
		
		// write traffic volumes csv file into run directories
		Scenario scenario1 = writeTrafficVolumeCSVFile(outputDirectory1);
		Scenario scenario2 = writeTrafficVolumeCSVFile(outputDirectory2);
		
		// write network to shapefile
		Network2Shape.exportNetwork2Shp1(scenario1, crs, ct);
		Network2Shape.exportNetwork2Shp1(scenario2, crs, ct);
				
		// write qgis project file
		// TODO

	}

	private Scenario writeTrafficVolumeCSVFile(String outputDirectory) {
		
		if (!outputDirectory.endsWith("/")) {
			outputDirectory = outputDirectory + "/";
		}
	
		Config config = ConfigUtils.loadConfig(outputDirectory + "output_config.xml");
		config.plans().setInputFile(null);
		config.plans().setInputPersonAttributeFile(null);
		config.network().setChangeEventsInputFile(null);
		config.vehicles().setVehiclesFile(null);
		config.network().setInputFile("output_network.xml.gz");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
				
		LinkDemandEventHandler handler = new LinkDemandEventHandler(scenario.getNetwork());
		events.addHandler(handler);
		
		String eventsFile = outputDirectory + "ITERS/it." + config.controler().getLastIteration() + "/" + config.controler().getLastIteration() + ".events.xml.gz";
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		String analysis_output_file = outputDirectory + "ITERS/it." + config.controler().getLastIteration() + "/link_dailyDemand.csv";
		handler.printResults(analysis_output_file);
		
		return scenario;
	}
			 
}
		

