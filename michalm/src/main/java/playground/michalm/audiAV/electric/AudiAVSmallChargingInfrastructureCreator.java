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

package playground.michalm.audiAV.electric;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.infrastructure.ChargerReader;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargerWriter;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecificationImpl;
import org.matsim.contrib.ev.infrastructure.ImmutableChargerSpecification;
import org.matsim.contrib.util.random.RandomUtils;
import org.matsim.contrib.util.random.UniformRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

public class AudiAVSmallChargingInfrastructureCreator {
	public static void main(String[] args) {
		String dir = "../../../shared-svn/projects/audi_av/scenario/";
		String netFile = dir + "networkc.xml.gz";
		String runDir = "../../../runs-svn/avsim_time_variant_network/";
		String chFilePrefix = runDir + "chargers/chargers_";
		String fractChFilePrefix = runDir + "chargers_small/chargers_";

		String[] scenarios = { "FOSSIL_FUEL_MINUS_20", "ZERO", "MINUS_20", "ONLY_DRIVE", "PLUS_20" };

		int[] counts = { 10560, 15086, 21120, 4800, 6600 };// these numbers do not include plugs...

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(netFile);

		double fraction = 0.001;
		UniformRandom uniform = RandomUtils.getGlobalUniform();
		for (int i = 0; i < scenarios.length; i++) {
			String s = scenarios[i];
			int c = counts[i];

			final ChargingInfrastructureSpecification chargingInfrastructure = new ChargingInfrastructureSpecificationImpl();
			new ChargerReader(chargingInfrastructure).readFile(chFilePrefix + c + "_" + s + ".xml");

			List<ChargerSpecification> fractChargers = new ArrayList<>();
			int totalPlugs = 0;
			for (ChargerSpecification ch : chargingInfrastructure.getChargerSpecifications().values()) {
				int plugs = (int)uniform.floorOrCeil(fraction * ch.getPlugCount());
				if (plugs > 0) {
					fractChargers.add(ImmutableChargerSpecification.newBuilder(ch).plugCount(plugs).build());
					totalPlugs += plugs;
				}
			}

			new ChargerWriter(fractChargers.stream()).write(fractChFilePrefix + totalPlugs + "_" + s + ".xml");
		}
	}
}
