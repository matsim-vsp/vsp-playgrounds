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
package org.matsim.contrib.greedo;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public interface GreedoProgressListener {

	public default void callToNotifyStartup_greedo(StartupEvent event) {
	}

	public default void callToReset_greedo(int iteration) {
	}

	public default void callToNotifyIterationEnds_greedo(IterationEndsEvent event) {
	}

	public default void setWeightOfHypotheticalReplanning(double weight) {
	}

	public default void extractedLastPhysicalPopulationState(int iteration) {
	}

	public default void observedLastPSimIterationWithinABlock(int iteration) {
	}

	public default void madeReplanningDecisions(int iteration) {
	}

}
