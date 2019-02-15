/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.santiago.run;

import java.text.ParseException;
import java.util.Collections;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.DvrpVehicle;
import org.matsim.contrib.dvrp.data.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.data.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.data.VehicleGenerator;
import org.matsim.contrib.dvrp.data.file.FleetWriter;
import org.matsim.contrib.util.random.RandomUtils;
import org.matsim.contrib.util.random.UniformRandom;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author michalm
 */
public class AVGenerator {
	private static class AVCreator implements VehicleGenerator.VehicleCreator {
		private static final int PAX_PER_CAR = 2;

		// 1 : 3 is the proportion in density between bigger and smaller rectangle
		private static final double DENSITY_RELATION = 1. / 4;

		private static final Coord MIN_COORD_SMALLER_RECTANGLE = new Coord(342295.1142950431, 6291210.245397029);
		private static final Coord MAX_COORD_SMALLER_RECTANGLE = new Coord(351912.2889312578, 6301887.03896847);

		private static final Coord MIN_COORD_BIGGER_RECTANGLE = new Coord(335093.5800615201, 6282523.057405231);
		private static final Coord MAX_COORD_BIGGER_RECTANGLE = new Coord(359383.2200004731, 6306617.889938615);

		private final Network network;
		private int currentVehicleId = 0;

		public AVCreator(Scenario scenario) {
			network = NetworkUtils.createNetwork();
			new TransportModeNetworkFilter(scenario.getNetwork()).filter(network, Collections.singleton("car"));
		}

		@Override
		public DvrpVehicleSpecification createVehicleSpecification(double t0, double t1) {
			Id<DvrpVehicle> vehId = Id.create("taxi" + currentVehicleId++, DvrpVehicle.class);

			Coord coord = RandomUtils.getGlobalGenerator().nextDouble() < DENSITY_RELATION ?
					randomCoordInBiggerRectangle() :
					randomCoordInSmallerRectangle();

			Link link = NetworkUtils.getNearestLinkExactly(network, coord);
			return ImmutableDvrpVehicleSpecification.newBuilder()
					.id(vehId)
					.startLinkId(link.getId())
					.capacity(PAX_PER_CAR)
					.serviceBeginTime(Math.round(t0))
					.serviceEndTime(Math.round(t1))
					.build();
		}

		private Coord randomCoordInSmallerRectangle() {
			UniformRandom uniform = RandomUtils.getGlobalUniform();
			double x = uniform.nextDouble(MIN_COORD_SMALLER_RECTANGLE.getX(), MAX_COORD_SMALLER_RECTANGLE.getX());
			double y = uniform.nextDouble(MIN_COORD_SMALLER_RECTANGLE.getY(), MAX_COORD_SMALLER_RECTANGLE.getY());
			return new Coord(x, y);
		}

		private Coord randomCoordInBiggerRectangle() {
			UniformRandom uniform = RandomUtils.getGlobalUniform();
			for (; ; ) {
				double x = uniform.nextDouble(MIN_COORD_BIGGER_RECTANGLE.getX(), MAX_COORD_BIGGER_RECTANGLE.getX());
				double y = uniform.nextDouble(MIN_COORD_BIGGER_RECTANGLE.getY(), MAX_COORD_BIGGER_RECTANGLE.getY());
				if (x < MIN_COORD_SMALLER_RECTANGLE.getX()
						|| x > MAX_COORD_SMALLER_RECTANGLE.getX()
						|| y < MIN_COORD_SMALLER_RECTANGLE.getY()
						|| y > MAX_COORD_SMALLER_RECTANGLE.getY()) {
					return new Coord(x, y);
				}
			}
		}

	}

	public static void main(String[] args) throws ParseException {
		String dir = "D:\\matsim-eclipse\\runs-svn\\santiago_AT_10pc\\";
		String networkFile = dir + "network_merged_cl.xml.gz";
		String taxisFilePrefix = dir + "taxis_";

		double startTime = 0;
		double workTime = 30 * 3600;

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);

		for (int i = 18000; i <= 24000; i += 2000) {
			AVCreator avc = new AVCreator(scenario);
			VehicleGenerator vg = new VehicleGenerator(workTime, workTime, avc);
			vg.generateVehicles(new double[] { i, i }, startTime, 30 * 3600);
			new FleetWriter(vg.getVehicleSpecifications().stream()).write(taxisFilePrefix + i + ".xml");
		}
	}
}
