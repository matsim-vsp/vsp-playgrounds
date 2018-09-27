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

package playground.ikaddoura.taxiPricing;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * 
 * @author ikaddoura
 */

public class TaxiPricingConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "taxiPricing" ;
	
	public TaxiPricingConfigGroup() {
		super(GROUP_NAME);
	}
		
	private boolean accountForCongestion = false;
	private boolean accountForNoise = false;
	
	private boolean chargeTollsFromSAVDriver = true;
	private boolean chargeSAVTollsFromPassengers = true;
	private boolean chargeTollsFromCarUsers = true;
		
	@StringGetter( "accountForCongestion" )
	public boolean isAccountForCongestion() {
		return accountForCongestion;
	}

	@StringSetter( "accountForCongestion" )
	public void setAccountForCongestion(boolean accountForCongestion) {
		this.accountForCongestion = accountForCongestion;
	}

	@StringGetter( "accountForNoise" )
	public boolean isAccountForNoise() {
		return accountForNoise;
	}
	
	@StringSetter( "accountForNoise" )
	public void setAccountForNoise(boolean accountForNoise) {
		this.accountForNoise = accountForNoise;
	}

	@StringGetter( "chargeSAVTollsFromPassengers" )
	public boolean isChargeSAVTollsFromPassengers() {
		return chargeSAVTollsFromPassengers;
	}

	@StringSetter( "chargeSAVTollsFromPassengers" )
	public void setChargeSAVTollsFromPassengers(boolean chargeSAVTollsFromPassengers) {
		this.chargeSAVTollsFromPassengers = chargeSAVTollsFromPassengers;
	}

	@StringGetter( "chargeTollsFromCarUsers" )
	public boolean isChargeTollsFromCarUsers() {
		return chargeTollsFromCarUsers;
	}

	@StringSetter( "chargeTollsFromCarUsers" )
	public void setChargeTollsFromCarUsers(boolean chargeTollsFromCarUsers) {
		this.chargeTollsFromCarUsers = chargeTollsFromCarUsers;
	}

	@StringGetter( "chargeTollsFromSAVDriver" )
	public boolean isChargeTollsFromSAVDriver() {
		return chargeTollsFromSAVDriver;
	}

	@StringSetter( "chargeTollsFromSAVDriver" )
	public void setChargeTollsFromSAVDriver(boolean chargeTollsFromSAVDriver) {
		this.chargeTollsFromSAVDriver = chargeTollsFromSAVDriver;
	}

}

