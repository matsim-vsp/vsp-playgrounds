/* *********************************************************************** *
 * project: org.matsim.*
 * LeastPointedPlanPruningConflictSolver.java
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
package playground.thibautd.socnetsim.replanning.selectors.coalitionselector;

import java.util.Collection;
import java.util.Map;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.replanning.selectors.coalitionselector.CoalitionSelector.ConflictSolver;

/**
 * @author thibautd
 */
public class LeastPointedPlanPruningConflictSolver implements ConflictSolver {

	@Override
	public void attemptToSolveConflicts(
			final Map<JointPlan, Collection<PlanRecord>> recordsPerJointPlan) {
		int minPoint = Integer.MAX_VALUE;
		Map.Entry<JointPlan , Collection<PlanRecord>> toMark = null;
		for ( Map.Entry<JointPlan , Collection<PlanRecord>> entry : recordsPerJointPlan.entrySet() ) {
			final JointPlan jointPlan = entry.getKey();
			final Collection<PlanRecord> records = entry.getValue();

			int nPoint = 0;
			for ( PlanRecord r : records ) {
				if ( jointPlan.getIndividualPlan( r.getPlan().getPerson().getId() ) == r.getPlan() ) nPoint++;
			}

			if ( nPoint > 1 && nPoint < minPoint ) {
				minPoint = nPoint;
				toMark = entry;
			}
		}

		if ( toMark == null ) {
			throw new RuntimeException( "could not find least pointed joint plan in "+recordsPerJointPlan );
		}

		for ( PlanRecord r : toMark.getValue() ) r.setInfeasible();
		recordsPerJointPlan.remove( toMark.getKey() );
	}
}

