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

package playground.michalm.taxi.ev;

import org.matsim.vsp.ev.charging.ChargingStrategy;
import org.matsim.vsp.ev.data.Battery;
import org.matsim.vsp.ev.data.Charger;
import org.matsim.vsp.ev.data.ElectricVehicle;

/**
 * @author michalm
 */
public class ETaxiChargingStrategy implements ChargingStrategy {
	// fast charging up to 80% of the battery capacity
	private static final double MAX_RELATIVE_SOC = 0.8;

	private final double effectivePower;

	public ETaxiChargingStrategy(Charger charger, double chargingSpeedFactor) {
		this.effectivePower = chargingSpeedFactor * charger.getPower();
	}

	@Override
	public void chargeVehicle(ElectricVehicle ev, double chargePeriod) {
		ev.getBattery().charge(effectivePower * chargePeriod);
	}

	@Override
	public boolean isChargingCompleted(ElectricVehicle ev) {
		return calcRemainingEnergyToCharge(ev) <= 0;
	}

	@Override
	public double calcRemainingEnergyToCharge(ElectricVehicle ev) {
		Battery b = ev.getBattery();
		return MAX_RELATIVE_SOC * b.getCapacity() - b.getSoc();
	}

	@Override
	public double calcRemainingTimeToCharge(ElectricVehicle ev) {
		return calcRemainingEnergyToCharge(ev) / effectivePower;
	}

	double getEffectivePower() {
		return effectivePower;
	}
}
