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
package playground.jbischoff.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.fleet.ElectricFleetWriter;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.fleet.ElectricVehicleSpecification;
import org.matsim.contrib.ev.fleet.ImmutableElectricVehicleSpecification;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.common.collect.ImmutableList;

/**
 * @author jbischoff
 *
 */

public class WriteTestPopulation {

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationFactory fac = scenario.getPopulation().getFactory();
		Random r = MatsimRandom.getLocalInstance();
		int n = 15;
		List<ElectricVehicleSpecification> eVehicles = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			Person p = fac.createPerson(Id.createPersonId(i));
			Plan plan = fac.createPlan();
			p.addPlan(plan);
			Coord hCoord = new Coord(290625.48113156157, 5885518.622230196);
			Coord wCoord = new Coord(468532.721161525, 5728751.474117422);

			Activity h1 = fac.createActivityFromCoord("h", hCoord);
			h1.setEndTime(7 * 3600 + r.nextInt(7200));
			plan.addActivity(h1);
			Leg l1 = fac.createLeg(TransportMode.car);
			plan.addLeg(l1);
			Activity w1 = fac.createActivityFromCoord("h", wCoord);
			;
			plan.addActivity(w1);

			scenario.getPopulation().addPerson(p);
			ElectricVehicleSpecification ev = ImmutableElectricVehicleSpecification.newBuilder()
					.id(Id.create(p.getId(), ElectricVehicle.class))
					.batteryCapacity(EvUnits.kWh_to_J(40))
					.initialSoc(EvUnits.kWh_to_J(40))
					.vehicleType("defaultVehicleType")
					.chargerTypes(ImmutableList.of("default"))
					.build();
			eVehicles.add(ev);
		}
		new PopulationWriter(scenario.getPopulation()).write("C:/Users/Joschka/Desktop/ev_population.xml");
		new ElectricFleetWriter(eVehicles.stream()).write("C:/Users/Joschka/Desktop/evehicles.xml");

	}

}
