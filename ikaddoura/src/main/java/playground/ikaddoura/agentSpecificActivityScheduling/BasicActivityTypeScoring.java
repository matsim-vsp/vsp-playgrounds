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

package playground.ikaddoura.agentSpecificActivityScheduling;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.functions.ActivityTypeOpeningIntervalCalculator;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.ScoringParameters;

/**
*
* The default {@link CharyparNagelActivityScoring} except that the opening and closing times are considered for groups of activities. Here,
* activities such as "home_3600" or "work_7200" are simply split and only the first part, e.g. "work", "home", etc., is used to look up the opening and closing times.
* 
* @author ikaddoura
*/

public class BasicActivityTypeScoring implements org.matsim.core.scoring.SumScoringFunction.ActivityScoring {
	
	private final CharyparNagelActivityScoring delegate;
	private final String delimiter = "_";
			
	public BasicActivityTypeScoring(ScoringParameters parameters, Person person) {
		this.delegate = new CharyparNagelActivityScoring(parameters, new ActivityTypeOpeningIntervalCalculator(parameters));
	}

	@Override
	public void finish() {
		this.delegate.finish();
	}

	@Override
	public double getScore() {
		return this.delegate.getScore();
	}

	@Override
	public void handleFirstActivity(Activity act) {
		
		if (act.getType().contains(delimiter)) {
			act.setType(split(act.getType()));
		}
				
		this.delegate.handleFirstActivity(act);
	}

	@Override
	public void handleActivity(Activity act) {
		
		if (act.getType().contains(delimiter)) {
			act.setType(split(act.getType()));
		}
		
		this.delegate.handleActivity(act);
	}

	@Override
	public void handleLastActivity(Activity act) {
		
		if (act.getType().contains(delimiter)) {
			act.setType(split(act.getType()));
		}
	
		this.delegate.handleLastActivity(act);
	}
	
	private String split(String activityType) {
		String[] parts = activityType.split(delimiter);
		return parts[0];		
	}

}

