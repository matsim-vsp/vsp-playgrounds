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

package playground.ikaddoura.analysis.moneyfare;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.core.utils.misc.Time;

/**
 * @author ikaddoura
 *
 */
public class MoneyFareHandler implements  PersonMoneyEventHandler {
	private static final Logger log = Logger.getLogger(MoneyFareHandler.class);

	private double totalAmounts = 0.;
	private double totalAmountsOnlyPositiveValues = 0.;
	private double totalAmountsOnlyNegativeValues = 0.;
	
	private int counter = 0;
	
	@Override
	public void handleEvent(PersonMoneyEvent event) {
		
		totalAmounts += event.getAmount();	
		if (event.getAmount() > 0.) totalAmountsOnlyPositiveValues += event.getAmount();
		if (event.getAmount() < 0.) totalAmountsOnlyNegativeValues += event.getAmount();
		
		counter++;
		
		if (counter%100 == 0.) {
			log.info("money event #" + counter + " --> time: " + Time.writeTime(event.getTime()));
			log.info(event.toString());
			log.info("total amount - all amounts: " + getTotalAmounts());
			log.info("total amount - only positive amounts: " + getTotalAmountsOnlyPositiveValues());
			log.info("total amount - only negative amounts: " + getTotalAmountsOnlyNegativeValues());
		}
	}

	public double getTotalAmounts() {
		return totalAmounts;
	}

	public double getTotalAmountsOnlyPositiveValues() {
		return totalAmountsOnlyPositiveValues;
	}

	public double getTotalAmountsOnlyNegativeValues() {
		return totalAmountsOnlyNegativeValues;
	}
	
}
