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
package playground.jbischoff.sharedTaxiBerlin.saturdaynight;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class CreateSupply {

	
	public static void main(String[] args) {
		
		List<Id<Link>> startLinks = new ArrayList<>();
		startLinks.add(Id.createLinkId("735967926_262467975")); //Alex
		startLinks.add(Id.createLinkId("26762962_26704067")); //Zoo
		startLinks.add(Id.createLinkId("25664099_26869274")); //Potsdamer Platz
		startLinks.add(Id.createLinkId("26823011_26823012")); //Nollendorfplatz
		startLinks.add(Id.createLinkId("38457825_38457826")); //Schlesisches Tor
		startLinks.add(Id.createLinkId("26858261_26859119")); //Hauptbahnhof
		startLinks.add(Id.createLinkId("29785114_29785111")); //Warschauer Str.
		startLinks.add(Id.createLinkId("25662552_21487180")); //Brandenburger Tor
		startLinks.add(Id.createLinkId("21385802_437210309")); //Schönhauser Allee
		startLinks.add(Id.createLinkId("29270295_12614600")); //Landsberger Allee / Danziger
		String folder = "../../../shared-svn/projects/sustainability-w-michal-and-dlr/data/scenarios/drt_saturdaynight/";

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(folder+"network_shortIds_v1.xml.gz");
		
		int noOfVehiclesPerRank = 4;
		int capacity = 3;
		double serviceBeginTime = 17*3600;
		double serviceEndTime = 31*3600;

		List<DvrpVehicleSpecification> vehicles = new ArrayList<>();
		int z = 0;
		for (Id<Link> l : startLinks){
		for (int i = 0; i<noOfVehiclesPerRank;i++){
			Id<DvrpVehicle> id = Id.create("taxi_" + z, DvrpVehicle.class);
			DvrpVehicleSpecification v = ImmutableDvrpVehicleSpecification.newBuilder()
					.id(Id.create(id, DvrpVehicle.class))
					.startLinkId(network.getLinks().get(l).getId())
					.capacity(capacity)
					.serviceBeginTime(serviceBeginTime)
					.serviceEndTime(serviceEndTime)
					.build();
			vehicles.add(v);
			z++;
		}
		}
		new FleetWriter(vehicles.stream()).write(folder + "/vehicles_" + (z++) + ".xml");
			
		
		
	}
}
