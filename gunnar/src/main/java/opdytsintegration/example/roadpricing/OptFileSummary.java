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
package opdytsintegration.example.roadpricing;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class OptFileSummary {

	// -------------------- MEMBERS --------------------

	private List<Integer> initialTransitionCounts = new ArrayList<>();

	private List<Double> initialEquilibriumGapWeights = new ArrayList<>();

	private List<Double> initialUniformityGapWeights = new ArrayList<>();

	private List<Double> finalObjectiveFunctionValues = new ArrayList<>();

	private List<Integer> addedTransitionCounts = new ArrayList<>();

	// -------------------- CONSTRUCTION --------------------

	OptFileSummary() {
	}

	void add(final Integer initialTransitionCount,
			final Double initialEquilibriumGapWeight,
			final Double initialUniformityGapWeight,
			final Double finalObjectiveFunctionValue,
			final Integer addedTransitionCount) {
		this.initialTransitionCounts.add(initialTransitionCount);
		this.initialEquilibriumGapWeights.add(initialEquilibriumGapWeight);
		this.initialUniformityGapWeights.add(initialUniformityGapWeight);
		this.finalObjectiveFunctionValues.add(finalObjectiveFunctionValue);
		this.addedTransitionCounts.add(addedTransitionCount);
	}

	// -------------------- CONTENT ACCESS --------------------

	public int getStageCnt() {
		return this.initialTransitionCounts.size();
	}

	public List<Integer> getInitialTransitionCounts() {
		return this.initialTransitionCounts;
	}

	public List<Double> getInitialEquilbriumGapWeights() {
		return this.initialEquilibriumGapWeights;
	}

	public List<Double> getInitialUniformityGapWeights() {
		return this.initialUniformityGapWeights;
	}

	public List<Double> getFinalObjectiveFunctionValues() {
		return this.finalObjectiveFunctionValues;
	}

	public List<Integer> getAddedTransitionCounts() {
		return this.addedTransitionCounts;
	}
}
