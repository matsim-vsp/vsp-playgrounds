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

import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.vsp.ev.data.Charger;
import org.matsim.vsp.ev.data.ElectricVehicle;
import org.matsim.vsp.ev.data.ElectricVehicleImpl;
import org.matsim.vsp.ev.data.EvData;
import org.matsim.vsp.ev.discharging.OhdeSlaskiAuxEnergyConsumption;
import org.matsim.vsp.ev.discharging.OhdeSlaskiAuxEnergyConsumption.TemperatureProvider;
import org.matsim.vsp.ev.discharging.OhdeSlaskiDriveEnergyConsumption;

import playground.michalm.taxi.data.EvrpVehicle;
import playground.michalm.taxi.data.EvrpVehicle.Ev;

public class ETaxiUtils {
	public static void initEvData(Fleet fleet, EvData evData) {
		TemperatureProvider tempProvider = () -> 20;// aux power about 1 kW at 20oC
		double chargingSpeedFactor = 1.; // full speed

		for (Charger c : evData.getChargers().values()) {
			ChargingWithQueueingAndAssignmentLogic chargingLogic = new ChargingWithQueueingAndAssignmentLogic(c,
					new ETaxiChargingStrategy(c, chargingSpeedFactor));
			c.setLogic(chargingLogic);
		}

		for (Vehicle v : fleet.getVehicles().values()) {
			ElectricVehicleImpl ev = (ElectricVehicleImpl)((EvrpVehicle)v).getElectricVehicle();
			ev.setDriveEnergyConsumption(new OhdeSlaskiDriveEnergyConsumption());
			ev.setAuxEnergyConsumption(new OhdeSlaskiAuxEnergyConsumption(ev, tempProvider, ETaxiUtils::isTurnedOn));
			evData.addElectricVehicle(ev.getId(), ev);
		}
	}

	private static boolean isTurnedOn(ElectricVehicle ev) {
		return ((Ev)ev).getDvrpVehicle().getSchedule().getStatus() == ScheduleStatus.STARTED;
	}
}
