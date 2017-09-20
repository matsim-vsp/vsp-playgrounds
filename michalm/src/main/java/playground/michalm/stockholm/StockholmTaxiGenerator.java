/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.stockholm;

import java.text.ParseException;

import org.apache.commons.math3.util.MathArrays;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.VehicleGenerator;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class StockholmTaxiGenerator {
	private static double[] getTaxisPerHour() {
		double[] taxisPerHour = { 664, 463, 304, 324, 606, 860, 1035, 1178, 1235, 1254, 1254, 1249, 1243, 1249, 1256,
				1256, 1237, 1195, 1102, 1002, 923, 883, 852, 795 };
		return taxisPerHour;
	}

	private static double[] getTaxis2to3(double[] taxis0to23) {
		double[] taxis2to3 = new double[26];
		System.arraycopy(taxis0to23, 2, taxis2to3, 0, 22);// 2-23 => 0-21
		System.arraycopy(taxis0to23, 0, taxis2to3, 22, 4);// 0-5 ==> 22-25
		return taxis2to3;
	}

	public static void main(String[] args) throws ParseException {
		String dir = "d:/temp/Stockholm/";
		String networkFile = dir + "network.xml";

		// avg values for each hour h, so they correspond to half past h (h:30,
		// for each hour h)
		double[] taxis0to23 = getTaxisPerHour();
		String taxisFile = dir + "taxis2to3_avg_weekday";

		// we start at 4:30 with vehicles, and at 5:00 with requests
		double startTime = StockholmServedRequests.ZERO_HOUR * 3600 - 1800;

		double minWorkTime = 4.0 * 3600;
		double maxWorkTime = 12.0 * 3600;

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);

//		for (int i = 2; i <= 10; i += 2) {
			StockholmTaxiCreator btc = new StockholmTaxiCreator(scenario);
			VehicleGenerator vg = new VehicleGenerator(minWorkTime, maxWorkTime, btc);
			vg.generateVehicles(MathArrays.scale(1, getTaxis2to3(taxis0to23)), startTime, 3600);
			new VehicleWriter(vg.getVehicles()).write(taxisFile + ".xml");
//		}
	}
}
