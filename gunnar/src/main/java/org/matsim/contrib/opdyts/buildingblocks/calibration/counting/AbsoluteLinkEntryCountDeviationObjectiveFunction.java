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
package org.matsim.contrib.opdyts.buildingblocks.calibration.counting;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.opdyts.buildingblocks.calibration.plotting.CountTrajectorySummarizer;
import org.matsim.contrib.opdyts.buildingblocks.calibration.plotting.TrajectoryPlotDataSource;
import org.matsim.contrib.opdyts.microstate.MATSimState;
import org.matsim.contrib.opdyts.objectivefunction.MATSimObjectiveFunction;

import floetteroed.utilities.TimeDiscretization;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AbsoluteLinkEntryCountDeviationObjectiveFunction
		implements MATSimObjectiveFunction<MATSimState>, TrajectoryPlotDataSource {

	private final double[] realData;

	private final LinkEntryCounter simulationCounter;

	private final double simulatedPopulationShare;

	public AbsoluteLinkEntryCountDeviationObjectiveFunction(final double[] realData,
			final LinkEntryCounter simulationCounter, final double simulatedPopulationShare) {
		this.realData = realData;
		this.simulationCounter = simulationCounter;
		this.simulatedPopulationShare = simulatedPopulationShare;
	}

	public CountMeasurementSpecification getSpecification() {
		return this.simulationCounter.getSpecification();
	}

	@Override
	public double value(final MATSimState state) {
		final int[] simData = this.simulationCounter.getDataOfLastCompletedIteration();
		double result = 0;
		for (int i = 0; i < this.realData.length; i++) {
			result += Math.abs(this.realData[i] - simData[i] / this.simulatedPopulationShare);
		}
		return result;
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		result.append("LINKS: " + this.getSpecification().getLinks() + "\n");
		result.append("TIMES: " + this.getSpecification().getTimeDiscretization() + "\n");
		result.append("real: ");
		for (double val : this.realData) {
			result.append("\t" + val);
		}
		result.append("\n");
		result.append("simu: ");
		for (int val : this.simulationCounter.getDataOfLastCompletedIteration()) {
			result.append("\t" + val / this.simulatedPopulationShare);
		}
		result.append("\n");
		return result.toString();
	}

	// --------------- IMPLEMENTATION OF TrajectoryPlotDataSource ---------------

	@Override
	public String getDescription() {
		final Set<Id<Link>> linkIds = this.simulationCounter.getSpecification().getLinks();
		return "Traffic counts on link" + (linkIds.size() > 1 ? "s " : " ") + linkIds;
	}

	@Override
	public TimeDiscretization getTimeDiscretization() {
		return this.simulationCounter.getSpecification().getTimeDiscretization();
	}

	@Override
	public double[] getSimulatedData() {
		final int[] source = this.simulationCounter.getDataOfLastCompletedIteration();
		if (source == null) {
			return null;
		} else {
			final double[] result = new double[source.length];
			for (int i = 0; i < result.length; i++) {
				result[i] = source[i] / this.simulatedPopulationShare;
			}
			return result;
		}
	}

	@Override
	public double[] getRealData() {
		return this.realData;
	}
	
	@Override
	public String getDataType() {
		return CountTrajectorySummarizer.DATA_TYPE;
	}



}
