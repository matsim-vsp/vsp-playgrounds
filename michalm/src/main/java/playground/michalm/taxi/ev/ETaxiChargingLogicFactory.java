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

import org.matsim.vsp.ev.charging.ChargingLogic;
import org.matsim.vsp.ev.charging.ChargingWithQueueingAndAssignmentLogic;
import org.matsim.vsp.ev.charging.FixedSpeedChargingStrategy;
import org.matsim.vsp.ev.data.Charger;

/**
 * @author michalm
 */
public class ETaxiChargingLogicFactory implements ChargingLogic.Factory {
	private final double chargingSpeedFactor = 1.; // full speed
	private final double maxRelativeSoc = 0.8;// up to 80% SoC

	@Override
	public ChargingLogic create(Charger charger) {
		return new ChargingWithQueueingAndAssignmentLogic(charger,
				new FixedSpeedChargingStrategy(charger.getPower() * chargingSpeedFactor, maxRelativeSoc));
	}
}
