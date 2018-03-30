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

package playground.michalm.taxi.ev;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.matsim.vsp.ev.charging.ChargingWithQueueingLogic;
import org.matsim.vsp.ev.data.Charger;
import org.matsim.vsp.ev.data.ElectricVehicle;

public class ETaxiChargingLogic extends ChargingWithQueueingLogic {
	public static ETaxiChargingLogic create(Charger charger, double chargingSpeedFactor) {
		return new ETaxiChargingLogic(charger, new ETaxiChargingStrategy(charger, chargingSpeedFactor));
	}

	private final Charger charger;
	private final ETaxiChargingStrategy chargingStrategy;
	private final Map<Id<Vehicle>, ElectricVehicle> assignedVehicles = new HashMap<>();

	public ETaxiChargingLogic(Charger charger, ETaxiChargingStrategy chargingStrategy) {
		super(charger, chargingStrategy);
		this.charger = charger;
		this.chargingStrategy = chargingStrategy;
	}

	// at this point ETaxiChargingTask should point to Charger
	public void addAssignedVehicle(ElectricVehicle ev) {
		assignedVehicles.put(ev.getId(), ev);
	}

	// on deleting ETaxiChargingTask or vehicle arrival (the veh becomes plugged or queued)
	public void removeAssignedVehicle(ElectricVehicle ev) {
		if (assignedVehicles.remove(ev.getId()) == null) {
			throw new IllegalArgumentException();
		}
	}

	// TODO using task timing from schedules will be more accurate in predicting charge demand

	// does not include further demand (AUX for queued vehs)
	public double estimateMaxWaitTimeOnArrival() {
		if (getPluggedVehicles().size() < charger.getPlugs()) {
			return 0;
		}

		double sum = sumEnergyToCharge(getPluggedVehicles()) + sumEnergyToCharge(getQueuedVehicles());
		return sum / chargingStrategy.getEffectivePower() / charger.getPlugs();
	}

	// does not include further demand (AUX for queued vehs; AUX+driving for dispatched vehs)
	public double estimateAssignedWorkload() {
		double total = sumEnergyToCharge(getPluggedVehicles()) //
				+ sumEnergyToCharge(getQueuedVehicles()) //
				+ sumEnergyToCharge(assignedVehicles.values());
		return total / chargingStrategy.getEffectivePower();
	}

	private double sumEnergyToCharge(Iterable<ElectricVehicle> evs) {
		double energyToCharge = 0;
		for (ElectricVehicle ev : evs) {
			energyToCharge += chargingStrategy.calcRemainingEnergyToCharge(ev);
		}
		return energyToCharge;
	}

	public double estimateChargeTime(ElectricVehicle ev) {
		return chargingStrategy.calcRemainingTimeToCharge(ev);
	}

	public int getAssignedCount() {
		return assignedVehicles.size();
	}
}
