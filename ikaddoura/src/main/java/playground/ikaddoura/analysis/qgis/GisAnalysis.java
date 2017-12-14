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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;

import playground.ikaddoura.analysis.linkDemand.LinkDemandEventHandler;

public class GisAnalysis {
	private final static Logger log = Logger.getLogger(GisAnalysis.class);
	
	private static String runDirectory1 = "../../runs-svn/incidents/berlin/output/output_2016-02-11_networkChangeEvents-false_withinDayReplanning-true_onlyReplanDirectlyAffectedAgents-false_replanInterval-1800/";
	private static String runDirectory2 = "../../runs-svn/incidents/berlin/output/output_2016-02-11_networkChangeEvents-true_withinDayReplanning-true_onlyReplanDirectlyAffectedAgents-false_replanInterval-1800/";

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
		exportNetwork2Shapefile(scenario1);
		exportNetwork2Shapefile(scenario2);
		
		// write qgis project file
		// TODO

	}

	private void exportNetwork2Shapefile(Scenario scenario) {
		
		PolylineFeatureFactory factory = new PolylineFeatureFactory.Builder()
		.setCrs(MGC.getCRS(crs))
		.setName("Link")
		.addAttribute("Id", String.class)
		.addAttribute("Length", Double.class)
		.addAttribute("capacity", Double.class)
		.addAttribute("lanes", Double.class)
		.addAttribute("Freespeed", Double.class)
		.addAttribute("Modes", String.class)
		.create();
		
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
						
		for (Link link : scenario.getNetwork().getLinks().values()){
			if (link.getAllowedModes().contains("car")) {
				SimpleFeature feature = factory.createPolyline(
						new Coordinate[]{
								new Coordinate(MGC.coord2Coordinate(ct.transform(link.getFromNode().getCoord()))),
								new Coordinate(MGC.coord2Coordinate(ct.transform(link.getToNode().getCoord())))
						}, new Object[] {link.getId(), link.getLength(), link.getCapacity(), link.getNumberOfLanes(), link.getFreespeed(), link.getAllowedModes()
						}, null
				);
				features.add(feature);
			}
		}
		
		log.info("Writing out network lines shapefile... ");
		ShapeFileWriter.writeGeometries(features, scenario.getConfig().controler().getOutputDirectory() + "network.shp");
		log.info("Writing out network lines shapefile... Done.");
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
		

