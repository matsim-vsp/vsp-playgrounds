/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.pseudosimulation.searchacceleration;

import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.Utilities;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class DeltaUpdater {

	private final Utilities utilities;

	DeltaUpdater(final Utilities utilities) {
		this.utilities = utilities;
	}

	private boolean shouldReplan(final IndividualReplanningResult individualResult) {
		if (this.utilities.getUtilities(individualResult.personId).isConverged()) {
			// A converged individual should replan uniformly.
			return individualResult.wouldBeUniformReplanner;
		} else {
			// A non-converged individual should replan greedily.
			return individualResult.wouldBeGreedyReplanner;
		}
	}

	int behaviorGapChangeIfSwitchFromGreedyToUniform(final IndividualReplanningResult individualResult) {	
		final boolean desiredReplanning = this.shouldReplan(individualResult);
		final boolean replanningBeforeSwitch = individualResult.wouldBeGreedyReplanner;
		final boolean replanningAfterSwitch = individualResult.wouldBeUniformReplanner;
		int result = 0;
		// Remove effect of greedy replanning.
		if (replanningBeforeSwitch != desiredReplanning) {
			result--;
		}
		// Include effect of uniform replanning.
		if (replanningAfterSwitch != desiredReplanning) {
			result++;
		}
		return result;
	}

}
