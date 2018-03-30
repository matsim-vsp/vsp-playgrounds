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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.vsp.ev.charging.ChargingEstimations;
import org.matsim.vsp.ev.charging.ChargingWithQueueingLogic;
import org.matsim.vsp.ev.data.Charger;
import org.matsim.vsp.ev.data.ElectricVehicle;

import com.google.common.collect.Streams;

public class ETaxiChargingLogic extends ChargingWithQueueingLogic {
	public static ETaxiChargingLogic create(Charger charger, double chargingSpeedFactor) {
		return new ETaxiChargingLogic(charger, new ETaxiChargingStrategy(charger, chargingSpeedFactor));
	}

	private final Map<Id<ElectricVehicle>, ElectricVehicle> assignedVehicles = new LinkedHashMap<>();

	public ETaxiChargingLogic(Charger charger, ETaxiChargingStrategy chargingStrategy) {
		super(charger, chargingStrategy);
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

	// does not include AUX+driving for assigned vehs
	public double estimateAssignedWorkload() {
		return ChargingEstimations.estimateTotalTimeToCharge(getChargingStrategy(), Streams.concat(
				getPluggedVehicles().stream(), getQueuedVehicles().stream(), assignedVehicles.values().stream()));
	}

	private final Collection<ElectricVehicle> unmodifiableAssignedVehicles = Collections
			.unmodifiableCollection(assignedVehicles.values());

	public Collection<ElectricVehicle> getAssignedVehicles() {
		return unmodifiableAssignedVehicles;
	}
}
