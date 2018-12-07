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

package playground.ikaddoura.analysis.od;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.MainModeIdentifier;

/**
 * @author ihab
 */
public final class AvoevMainModeIdentifierImpl implements MainModeIdentifier {
	
	private static final Logger log = Logger.getLogger(AvoevMainModeIdentifierImpl.class);
	
	@Override
	public String identifyMainMode( final List<? extends PlanElement> tripElements) {
		
		String mode = null;
		
		// start with the case that there is only one leg
		if (tripElements.size() == 1) {
			mode = ((Leg) tripElements.get( 0 )).getMode();
		
		// in case there are more legs: look for something else than access or egress legs
		} else {
			for ( PlanElement pe : tripElements ) {
				if ( pe instanceof Leg ) {
					Leg leg = (Leg) pe ;
					if (leg.getMode().startsWith("access") || leg.getMode().startsWith("egress")) {
						// skip
					} else {
						mode = leg.getMode();
					}
				}
			}
		}
		
		// if the mode is still null...
		if (mode == null) {
			for ( PlanElement pe : tripElements ) {
				if ( pe instanceof Leg ) {
					Leg leg = (Leg) pe ;
					if (leg.getMode().startsWith("access") || leg.getMode().startsWith("egress") || leg.getMode().equals("transit_walk")) {
						mode = "walk";
						log.warn("Main mode for " + tripElements.toString() + " is considered as 'walk'.");
					} else {
						mode = "unknown";
						log.warn("Can't identify main mode for: " + tripElements.toString());
					}
				}
			}
		}
		
		if (mode.equals("transit_walk")) {
			mode = "walk";
		}
		
		return mode;		
	}
}
