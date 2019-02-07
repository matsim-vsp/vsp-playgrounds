/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.savPricing.generateSAV;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.DvrpVehicle;
import org.matsim.contrib.dvrp.data.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.data.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.data.file.FleetWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author ikaddoura
 */
public class RandomTaxiVehicleCreator {
	private static final Logger log = Logger.getLogger(RandomTaxiVehicleCreator.class);

	private final String vehiclesFilePrefix;
	private final CoordinateTransformation ct;
	private final Map<Integer, Geometry> zoneId2geometry = new HashMap<Integer, Geometry>();

	private final Scenario scenario;
	private final Random random = MatsimRandom.getRandom();
	private final String consideredMode = "car";

	public static void main(String[] args) {

		String networkFile = "/Users/ihab/Documents/workspace/matsim-berlin/scenarios/berlin-v5.2-10pct/input/berlin-v5.0.network.xml.gz";
		String shapeFile = "/Users/ihab/Documents/workspace/matsim-berlin/scenarios/berlin-v5.2-10pct/input/shp-inner-city-area/inner-city-area.shp";

		String vehiclesFilePrefix = "/Users/ihab/Desktop/taxi-files/taxis-berlin_";
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.DHDN_GK4,
				TransformationFactory.DHDN_GK4);

		RandomTaxiVehicleCreator tvc = new RandomTaxiVehicleCreator(networkFile, shapeFile, vehiclesFilePrefix, ct);

		for (int i = 0; i <= 5000; i = i + 50) {
			System.out.println(i);
			tvc.run(i);
		}
	}

	public RandomTaxiVehicleCreator(String networkfile, String shapeFile, String vehiclesFilePrefix,
			CoordinateTransformation ct) {
		this.vehiclesFilePrefix = vehiclesFilePrefix;
		this.ct = ct;

		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkfile);

		Set<String> modes = new HashSet<>();
		modes.add(consideredMode);
		new MultimodalNetworkCleaner(scenario.getNetwork()).run(modes);

		// change nodes in a way that pt links are not taken into consideration

		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getId().toString().startsWith("pt_")) {
				link.getFromNode().setCoord(new Coord(4449458.462246102, 4708888.241959611));
				link.getToNode().setCoord(new Coord(4449458.462246102, 4708888.241959611));
			}
		}
		loadShapeFile(shapeFile);
	}

	private void run(int amount) {
		List<DvrpVehicleSpecification> vehicles = new ArrayList<>();

		for (int i = 0; i < amount; i++) {
			Link link = null;

			while (link == null) {
				Point p = getRandomPointInFeature(random, zoneId2geometry.get(random.nextInt(zoneId2geometry.size())));
				link = NetworkUtils.getNearestLinkExactly(((Network)scenario.getNetwork()),
						ct.transform(MGC.point2Coord(p)));
				if (isCoordInArea(link.getFromNode().getCoord()) && isCoordInArea(link.getToNode().getCoord())) {
					if (link.getAllowedModes().contains(consideredMode)) {
						// ok
					} else {
						link = null;
					}
					// ok, the link is within the shape file
				} else {
					link = null;
				}
			}

			if (i % 5000 == 0)
				log.info("#" + i);

			DvrpVehicleSpecification v = ImmutableDvrpVehicleSpecification.newBuilder()
					.id(Id.create("rt" + i, DvrpVehicle.class))
					.startLinkId(link.getId())
					.capacity(5)
					.serviceBeginTime(Math.round(1))
					.serviceEndTime(Math.round(30 * 3600))
					.build();
			vehicles.add(v);

		}
		new FleetWriter(vehicles.stream()).write(vehiclesFilePrefix + amount + ".xml.gz");
	}

	private static Point getRandomPointInFeature(Random rnd, Geometry g) {
		Point p = null;
		double x, y;
		do {
			x = g.getEnvelopeInternal().getMinX() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxX()
					- g.getEnvelopeInternal().getMinX());
			y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxY()
					- g.getEnvelopeInternal().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!g.contains(p));
		return p;
	}

	private void loadShapeFile(String shapeFile) {
		Collection<SimpleFeature> features;
		features = ShapeFileReader.getAllFeatures(shapeFile);
		int featureCounter = 0;
		for (SimpleFeature feature : features) {
			zoneId2geometry.put(featureCounter, (Geometry)feature.getDefaultGeometry());
			featureCounter++;
		}
		log.info("features: " + featureCounter);
	}

	private boolean isCoordInArea(Coord coord) {
		boolean coordInArea = false;
		for (Geometry geometry : zoneId2geometry.values()) {
			Point p = MGC.coord2Point(coord);

			if (p.within(geometry)) {
				coordInArea = true;
			}
		}
		return coordInArea;
	}
}
