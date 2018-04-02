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

package playground.michalm.audiAV.electric;

import java.util.function.DoubleSupplier;

import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.vsp.ev.data.ElectricVehicle;
import org.matsim.vsp.ev.data.ElectricVehicleImpl;
import org.matsim.vsp.ev.data.EvFleetImpl;
import org.matsim.vsp.ev.discharging.OhdeSlaskiAuxEnergyConsumption;
import org.matsim.vsp.ev.discharging.OhdeSlaskiDriveEnergyConsumption;

import playground.michalm.taxi.data.EvrpVehicle;
import playground.michalm.taxi.data.EvrpVehicle.Ev;

public class EAVUtils {
	public static void initEvData(Fleet fleet, EvFleetImpl evFleet) {
		DoubleSupplier tempProvider = () -> 20;// aux power about 1 kW at 20oC
		for (Vehicle v : fleet.getVehicles().values()) {
			ElectricVehicleImpl ev = (ElectricVehicleImpl)((EvrpVehicle)v).getElectricVehicle();
			ev.setDriveEnergyConsumption(new OhdeSlaskiDriveEnergyConsumption());
			ev.setAuxEnergyConsumption(
					new OhdeSlaskiAuxEnergyConsumption(ev, tempProvider, EAVUtils::isServingCustomer));
			evFleet.addElectricVehicle(ev.getId(), ev);
		}
	}

	private static boolean isServingCustomer(ElectricVehicle ev) {
		Schedule schedule = ((Ev)ev).getDvrpVehicle().getSchedule();
		if (schedule.getStatus() != ScheduleStatus.STARTED) {
			return false;
		}

		switch (((TaxiTask)schedule.getCurrentTask()).getTaxiTaskType()) {
			case PICKUP:
			case OCCUPIED_DRIVE:
			case DROPOFF:
				return true;

			// TODO driving empty to cusomer is not handled yet (will be?)

			default:
				return false;
		}
	}
}
