/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.optimizer;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.vsp.ev.data.ElectricFleet;
import org.matsim.vsp.ev.data.ElectricVehicle;

/**
 * @author michalm
 */
public class ETaxiVehicle implements Vehicle {
	public static ETaxiVehicle create(Vehicle vehicle, ElectricFleet evFleet) {
		// return new ETaxi(vehicle, evFleet.getElectricVehicles().get(Id.create(vehicle.getId(),
		// ElectricVehicle.class)));
		return new ETaxiVehicle(vehicle, evFleet.getElectricVehicles().get(vehicle.getId()));
	}

	private final Vehicle vehicle;
	private final ElectricVehicle electricVehicle;

	public ETaxiVehicle(Vehicle vehicle, ElectricVehicle electricVehicle) {
		this.vehicle = vehicle;
		this.electricVehicle = electricVehicle;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}

	public ElectricVehicle getElectricVehicle() {
		return electricVehicle;
	}

	@Override
	public Id<Vehicle> getId() {
		return vehicle.getId();
	}

	@Override
	public Link getStartLink() {
		return vehicle.getStartLink();
	}

	@Override
	public double getCapacity() {
		return vehicle.getCapacity();
	}

	@Override
	public double getServiceBeginTime() {
		return vehicle.getServiceBeginTime();
	}

	@Override
	public double getServiceEndTime() {
		return vehicle.getServiceEndTime();
	}

	@Override
	public Schedule getSchedule() {
		return vehicle.getSchedule();
	}

	@Override
	public void setStartLink(Link link) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void resetSchedule() {
		throw new UnsupportedOperationException();
	}
}
