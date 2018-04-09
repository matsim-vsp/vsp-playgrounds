/* *********************************************************************** *
 * project: org.matsim.*
 * PlanMutateTimeAllocation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package playground.ikaddoura.timeAllocationMutatorIK;

import java.util.Random;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.Time;

/**
 * Mutates the duration of activities randomly within a specified range.
 * Similar to {@Link PlanMutateTimeAllocationSimplified} except that mutation ranges are taken from activity attributes:
 * 		- If there is an activity duration attribute: The mutation range is set to the minimum of the duration in the activity attribute and the mutation range provided in the config.
 * 		- If there is no activity duration attribute: The mutation range is set to the mutation range in the config.
 * 
 * @author ikaddoura
 */
public final class PlanMutateTimeAllocationSimplifiedIK implements PlanAlgorithm {

	private final StageActivityTypes blackList;
	private final double mutationRange;
	private final Random random;
	private final boolean affectingDuration;
	
	private final String durationActivityAttribute = "cemdapStopDuration_s";

	/**
	 * Initializes an instance mutating all non-stage activities in a plan
	 * @param mutationRange
	 * @param affectingDuration
	 * @param random
	 */
	public PlanMutateTimeAllocationSimplifiedIK(final StageActivityTypes blackList, final double mutationRange, boolean affectingDuration, final Random random) {
		this.blackList = blackList;
		this.mutationRange = mutationRange;
		this.affectingDuration = affectingDuration;
		this.random = random;
	}

	@Override
	public void run(final Plan plan) {
		for ( Activity act : TripStructureUtils.getActivities( plan , blackList ) ) {
			
			if (act.getAttributes().getAttribute(this.durationActivityAttribute) == null) {				
				if (act.getEndTime() != Time.UNDEFINED_TIME) {
					act.setEndTime(mutateTime(act.getEndTime()));
				}
				if ( affectingDuration ) {
					if (act.getMaximumDuration() != Time.UNDEFINED_TIME) {
						act.setMaximumDuration(mutateTime(act.getMaximumDuration()));
					}
				}
				
			} else {
				if (act.getEndTime() != Time.UNDEFINED_TIME) {
					act.setEndTime(mutateTime(act.getEndTime(), (int) act.getAttributes().getAttribute("cemdapStopDuration_s")));
				}
				if ( affectingDuration ) {
					if (act.getMaximumDuration() != Time.UNDEFINED_TIME) {
						act.setMaximumDuration(mutateTime(act.getMaximumDuration(), (int) act.getAttributes().getAttribute("cemdapStopDuration_s")));
					}
				}
			}
		}
	}

	private double mutateTime(final double time) {
		double t = time;
		t = t + (int)((this.random.nextDouble() * 2.0 - 1.0) * this.mutationRange);

		if (t < 0) {
			t = 0;
		}
		
		return t;
	}
	
	private double mutateTime(final double time, final int duration) {
		
		double mutationRangeToBeUsed;
		if (duration > this.mutationRange) {
			mutationRangeToBeUsed = this.mutationRange;
		} else {
			mutationRangeToBeUsed = duration;
		}
				
		double t = time;
		t = t + (int)((this.random.nextDouble() * 2.0 - 1.0) * mutationRangeToBeUsed);

		if (t < 0) {
			t = 0;
		}
		
		return t;
	}

}
