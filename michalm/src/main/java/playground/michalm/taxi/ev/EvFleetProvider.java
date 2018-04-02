/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.ev;

import java.util.function.DoubleSupplier;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.vsp.ev.data.ElectricVehicle;
import org.matsim.vsp.ev.data.ElectricVehicleImpl;
import org.matsim.vsp.ev.data.EvFleet;
import org.matsim.vsp.ev.data.EvFleetImpl;
import org.matsim.vsp.ev.discharging.OhdeSlaskiAuxEnergyConsumption;
import org.matsim.vsp.ev.discharging.OhdeSlaskiDriveEnergyConsumption;

import com.google.inject.Provider;

import playground.michalm.taxi.data.EvrpVehicle;
import playground.michalm.taxi.data.EvrpVehicle.Ev;

public class EvFleetProvider implements Provider<EvFleet> {
	@Inject
	private Fleet fleet;

	private final DoubleSupplier temperatureProvider;
	private final Predicate<ElectricVehicle> isTurnedOnPredicate;

	public EvFleetProvider(DoubleSupplier temperatureProvider, Predicate<ElectricVehicle> isTurnedOnPredicate) {
		this.temperatureProvider = temperatureProvider;
		this.isTurnedOnPredicate = isTurnedOnPredicate;
	}

	@Override
	public EvFleet get() {
		EvFleetImpl evFleet = new EvFleetImpl();
		for (Vehicle v : fleet.getVehicles().values()) {
			ElectricVehicleImpl ev = (ElectricVehicleImpl)((EvrpVehicle)v).getElectricVehicle();
			ev.setDriveEnergyConsumption(new OhdeSlaskiDriveEnergyConsumption());
			ev.setAuxEnergyConsumption(
					new OhdeSlaskiAuxEnergyConsumption(ev, temperatureProvider, isTurnedOnPredicate));
			evFleet.addElectricVehicle(ev.getId(), ev);
		}
		return evFleet;
	}

	public static boolean isTurnedOn(ElectricVehicle ev) {
		return ((Ev)ev).getDvrpVehicle().getSchedule().getStatus() == ScheduleStatus.STARTED;
	}
}
