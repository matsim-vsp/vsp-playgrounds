/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import java.util.List;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.data.VehicleGenerator.VehicleCreator;
import org.matsim.contrib.util.random.*;

import com.beust.jcommander.internal.Lists;

public class StockholmTaxiCreator implements VehicleCreator {
	private static final int PAXPERCAR = 4;
	private final UniformRandom uniform = RandomUtils.getGlobalUniform();

	private final List<Link> links;

	private int currentVehicleId = 0;

	@SuppressWarnings("unchecked")
	public StockholmTaxiCreator(Scenario scenario) {
		links = (List<Link>) Lists.newArrayList(scenario.getNetwork().getLinks().values());
	}

	@Override
	public Vehicle createVehicle(double t0, double t1) {
		Id<Vehicle> vehId = Id.create("taxi" + currentVehicleId++, Vehicle.class);
		int idx = uniform.nextInt(0, links.size() - 1);
		Link link = links.get(idx);
		return new VehicleImpl(vehId, link, PAXPERCAR, Math.round(t0), Math.round(t1));
	}
}