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
public class LogFileSummary {

	// -------------------- MEMBERS --------------------

	private List<Integer> totalTransitionCounts = new ArrayList<>();

	private List<Double> bestObjectiveFunctionValues = new ArrayList<>();

	private List<Double> equilibriumGapWeights = new ArrayList<>();

	private List<Double> uniformityGapWeights = new ArrayList<>();

	// -------------------- CONSTRUCTION --------------------

	LogFileSummary() {
	}

	void add(final int totalTransitionCount,
			final double bestObjectiveFunctionValue,
			final double equilibriumGapWeight, final double uniformityGapWeight) {
		this.totalTransitionCounts.add(totalTransitionCount);
		this.bestObjectiveFunctionValues.add(bestObjectiveFunctionValue);
		this.equilibriumGapWeights.add(equilibriumGapWeight);
		this.uniformityGapWeights.add(uniformityGapWeight);
	}

	// -------------------- CONTENT ACCESS --------------------

	public int getStageCnt() {
		return this.totalTransitionCounts.size();
	}
	
	public List<Integer> getTotalTransitionCounts() {
		return this.totalTransitionCounts;
	}
	
	public List<Double> getBestObjectiveFunctionValues() {
		return this.bestObjectiveFunctionValues;
	}
	
	public List<Double> getEquilbriumGapWeights() {
		return this.equilibriumGapWeights;
	}
	
	public List<Double> getUniformityGapWeights() {
		return this.uniformityGapWeights;
	}
	
}

