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
package org.matsim.contrib.greedo.logging;

import java.util.List;

import org.matsim.contrib.greedo.ReplannerIdentifier;
import org.matsim.contrib.greedo.WireGreedoIntoMATSimListener;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class LogDataWrapper {

	private final WireGreedoIntoMATSimListener accelerator;

	private final ReplannerIdentifier identifier;

	private final Integer driversInPseudoSim;

	public LogDataWrapper(final WireGreedoIntoMATSimListener accelerator, final ReplannerIdentifier identifier,
			final Integer driversInPseudoSim) {
		this.accelerator = accelerator;
		this.identifier = identifier;
		this.driversInPseudoSim = driversInPseudoSim;
	}

	public Integer getDriversInPhysicalSim() {
		return this.accelerator.getDriversInPhysicalSim();
	}

	public Integer getDriversInPseudoSim() {
		return this.driversInPseudoSim;
	}

	public Double getEffectiveReplanninRate() {
		return this.accelerator.getEffectiveReplanningRate();
	}

	public Double getMeanReplanningRate() {
		return this.identifier.getMeanReplanningRate();
	}

	public Double getSumOfWeightedCountDifferences2() {
		return this.identifier.getSumOfWeightedCountDifferences2();
	}

	public Double getSumOfUnweightedCountDifferences2() {
		return this.identifier.getSumOfUnweightedCountDifferences2();
	}

	public Double getLastRealizedUtilitySum() {
		return this.accelerator.getLastRealizedUtilitySum();
	}
	
	public Double getLastExpectedUtilityChangeSumAccelerated() {
		return this.accelerator.getLastExpectedUtilityChangeSumAccelerated();
	}

	public Double getLastExpectedUtilityChangeSumUniform() {
		return this.accelerator.getLastExpectedUtilityChangeSumUniform();
	}

	public Double getLastRealizedUtilityChangeSum() {
		return this.accelerator.getLastRealizedUtilityChangeSum();
	}
	
	public List<Integer> getSortedAgesView() {
		return this.accelerator.getSortedAgesView();
	}
	
	public Double getAverageAge() {
		return this.accelerator.getAveragAge();
	}
	
	public Double getAverageWeight() {
		return this.accelerator.getAverageWeight();
	}

	public Double getSumOfUnweightedUtilityChanges() {
		return this.identifier.getSumOfUnweightedUtilityChanges();
	}
	
	public Double getSumOfWeightedUtilityChanges() {
		return this.identifier.getSumOfWeightedUtilityChanges();
	}
	
}
