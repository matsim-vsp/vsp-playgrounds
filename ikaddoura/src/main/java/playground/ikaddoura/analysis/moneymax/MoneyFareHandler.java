/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.ikaddoura.analysis.moneymax;

import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;

/**
 * @author ikaddoura
 *
 */
public class MoneyFareHandler implements  PersonMoneyEventHandler {

	private double maxToll = 0.;
		
	@Override
	public void handleEvent(PersonMoneyEvent event) {
		
		double toll = event.getAmount() * (-1);
		if (toll > maxToll) maxToll = toll;
	}

	public double getMaxToll() {
		return maxToll;
	}
	
}
