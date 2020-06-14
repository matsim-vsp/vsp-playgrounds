/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.gleich.drt;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import playground.gleich.utilsFromOthers.jbischoff.JbUtils;

/**
 * @author  vsp-gleich
 *
 */
public class CreateTaxiVehicles {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		int numberofVehicles = 400;
		double operationStartTime = 0.; //t0
		double operationEndTime = 2*24*3600.;	//t1
		int seats = 1;
		String networkfile = "";
		String taxisFile = "data/input/Berlin100pct/drt/DrtVehicles.100pct.DRT_" + numberofVehicles + "_Cap" + seats + ".xml";
		List<DvrpVehicleSpecification> vehicles = new ArrayList<>();
		Random random = MatsimRandom.getLocalInstance();
		Geometry geometryStudyArea = JbUtils.readShapeFileAndExtractGeometry("AV_OPERATION_AREA_SHP", "AV_OPERATION_AREA_SHP_KEY").get("AV_OPERATION_AREA_SHP_ELEMENT");
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkfile);
		List<Id<Link>> linksInArea = new ArrayList<>();
			for(Link link: scenario.getNetwork().getLinks().values()){
				if(geometryStudyArea.contains(MGC.coord2Point(link.getFromNode().getCoord())) &&
						geometryStudyArea.contains(MGC.coord2Point(link.getToNode().getCoord()))){
					linksInArea.add(link.getId());
				}
			}
		for (int i = 0; i< numberofVehicles;i++){
			Link startLink;
			do {
			Id<Link> linkId = linksInArea.get(random.nextInt(linksInArea.size()));
			startLink =  scenario.getNetwork().getLinks().get(linkId);
			}
			while (!startLink.getAllowedModes().contains(TransportMode.car));
			//for multi-modal networks: Only links where cars can ride should be used.
			DvrpVehicleSpecification v = ImmutableDvrpVehicleSpecification.newBuilder()
					.id(Id.create("DRT" + i, DvrpVehicle.class))
					.startLinkId(startLink.getId())
					.capacity(seats)
					.serviceBeginTime(operationStartTime)
					.serviceEndTime(operationEndTime)
					.build();
		    vehicles.add(v);    
		}
		new FleetWriter(vehicles.stream()).write(taxisFile);
	}

}
