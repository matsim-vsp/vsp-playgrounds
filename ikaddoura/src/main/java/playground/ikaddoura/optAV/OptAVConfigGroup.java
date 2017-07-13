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

package playground.ikaddoura.optAV;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * 
 * @author ikaddoura
 */

public class OptAVConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "optAV" ;
	
	public OptAVConfigGroup() {
		super(GROUP_NAME);
	}
	
	private boolean accountForCongestion = false;
	private boolean accountForNoise = false;
	
	private OptAVApproach optAVApproach = OptAVApproach.NoPricing;
			
	public enum OptAVApproach {
		NoPricing, PrivateAndExternalCost, ExternalCost
	}
	
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

	@StringGetter( "optAVApproach" )
	public OptAVApproach getOptAVApproach() {
		return optAVApproach;
	}

	@StringSetter( "optAVApproach" )
	public void setOptAVApproach(OptAVApproach optAVApproach) {
		this.optAVApproach = optAVApproach;
	}

}

