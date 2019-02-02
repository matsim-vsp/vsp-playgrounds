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

package playground.michalm.audiAV;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.data.FleetSpecificationImpl;
import org.matsim.contrib.dvrp.data.file.FleetReader;
import org.matsim.contrib.dvrp.data.file.FleetWriter;
import org.matsim.contrib.util.random.RandomUtils;
import org.matsim.contrib.util.random.UniformRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import com.google.common.collect.ImmutableList;

public class AudiAVSmallFleetCreator {
	public static void main(String[] args) {
		String dir = "../../../shared-svn/projects/audi_av/scenario/";
		String netFile = dir + "networkc.xml.gz";
		String vehFile = dir + "v100pct/taxi_vehicles_100000.xml.gz";
		String fractVehFilePrefix = dir + "v_small/taxi_vehicles_";

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(netFile);
		FleetSpecificationImpl fleet = new FleetSpecificationImpl();
		new FleetReader(fleet).readFile(vehFile);

		UniformRandom uniform = RandomUtils.getGlobalUniform();
		for (int i = 5; i <= 20; i++) {
			double fraction = (double)i / 10_000;
			ImmutableList<DvrpVehicleSpecification> fractVehs = fleet.getVehicleSpecifications().values().stream()
					.filter(v -> uniform.trueOrFalse(fraction)).collect(ImmutableList.toImmutableList());
			new FleetWriter(fractVehs.stream()).write(fractVehFilePrefix + fractVehs.size() + ".xml.gz");
		}
	}
}
