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

/**
 * 
 */
package playground.ikaddoura.berlin;

import java.time.LocalDate;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.gtfs.RunGTFS2MATSim;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.vehicles.VehicleWriterV1;

/**
 * @author  jbischoff
 * This is an example script that utilizes GTFS2MATSim and creates a pseudo network and vehicles using MATSim standard API functionality.
 */

public class RunGTFS2MATSimExampleRVR {

	
	public static void main(String[] args) {
	
		//this was tested for the latest VBB GTFS, available at 
		// http://www.vbb.de/de/article/fahrplan/webservices/datensaetze/1186967.html
		
		//input data
		String gtfsZipFile = "/Users/ihab/Documents/workspace/shared-svn/projects/nemo_mercator/data/pt/google_transit_vrr_2018_05_11.zip"; 
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25832");
		LocalDate date = LocalDate.parse("2018-05-15");

		//output files 
		String scheduleFile = "/Users/ihab/Documents/workspace/shared-svn/projects/nemo_mercator/data/pt/transitSchedule_GTFS.xml.gz";
		String networkFile = "/Users/ihab/Documents/workspace/shared-svn/projects/nemo_mercator/data/pt/tertiaryNemo_10112017_EPSG_25832_filteredcleaned_network_with-pt.xml.gz";
		String transitVehiclesFile ="/Users/ihab/Documents/workspace/shared-svn/projects/nemo_mercator/data/pt/transitVehicles_GTFS.xml.gz";
		
		//Convert GTFS
		RunGTFS2MATSim.convertGtfs(gtfsZipFile, scheduleFile, date, ct, false);
		
		//Parse the schedule again
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readFile(scheduleFile);
		
		//if neccessary, parse in an existing network file here:
		new MatsimNetworkReader(scenario.getNetwork()).readFile("/Users/ihab/Documents/workspace/shared-svn/projects/nemo_mercator/data/matsim_input/tertiaryNemo_10112017_EPSG_25832_filteredcleaned_network.xml.gz");
		
		//Create a network around the schedule
		new CreatePseudoNetwork(scenario.getTransitSchedule(),scenario.getNetwork(),"pt_").createNetwork();
		
		//Create simple transit vehicles
		new CreateVehiclesForSchedule(scenario.getTransitSchedule(), scenario.getTransitVehicles()).run();
		
		//Write out network, vehicles and schedule
		new NetworkWriter(scenario.getNetwork()).write(networkFile);
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(scheduleFile);
		new VehicleWriterV1(scenario.getTransitVehicles()).writeFile(transitVehiclesFile);
	}
}
