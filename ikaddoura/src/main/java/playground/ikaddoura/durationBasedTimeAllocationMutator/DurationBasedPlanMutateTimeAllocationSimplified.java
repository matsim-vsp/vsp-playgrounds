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

package playground.ikaddoura.durationBasedTimeAllocationMutator;

import java.util.Random;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
import org.matsim.core.utils.misc.Time;

/**
 * Mutates the duration of activities randomly within a specified range.
 * Similar to {@Link PlanMutateTimeAllocationSimplified} except that for activities with a maximum duration the mutation range is set to the minimum of maximum duration and mutation range set in the config.
 * In case an activity has an end time and no maximum duration, the end time is shifted using the mutation range set in the config.
 * 
 * @author ikaddoura
 */
public final class DurationBasedPlanMutateTimeAllocationSimplified implements PlanAlgorithm {

	private final double mutationRange;
	private final Random random;
	private final boolean affectingDuration;
	
	/**
	 * Initializes an instance mutating all non-stage activities in a plan
	 * @param mutationRange
	 * @param affectingDuration
	 * @param random
	 */
	public DurationBasedPlanMutateTimeAllocationSimplified(final double mutationRange, boolean affectingDuration, final Random random) {
		this.mutationRange = mutationRange;
		this.affectingDuration = affectingDuration;
		this.random = random;
	}

	@Override
	public void run(final Plan plan) {
		for ( Activity act : TripStructureUtils.getActivities( plan, StageActivityHandling.ExcludeStageActivities ) ) {
			
			if (!Time.isUndefinedTime(act.getEndTime())) {
				act.setEndTime(mutateTime(act.getEndTime(), this.mutationRange));
			}
			if ( affectingDuration ) {
				if (!Time.isUndefinedTime(act.getMaximumDuration())) {
					
					double mutationRangeToBeUsed;
					if (act.getMaximumDuration() > this.mutationRange) {
						mutationRangeToBeUsed = this.mutationRange;
					} else {
						mutationRangeToBeUsed = act.getMaximumDuration();
					}
					
					act.setMaximumDuration(mutateTime(act.getMaximumDuration(), mutationRangeToBeUsed));
				}
			}
		}
	}

	private double mutateTime(final double time, final double mutationRange) {
		double t = time;
		t = t + (int)((this.random.nextDouble() * 2.0 - 1.0) * mutationRange);

		if (t < 0) {
			t = 0;
		}
		
		return t;
	}

}
