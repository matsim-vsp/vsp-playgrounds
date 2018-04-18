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
package floetteroed.utilities.searchrepeater;

import java.util.Random;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class SearchRepeater<D> {

	// -------------------- CONSTANTS --------------------

	private final int maxTrials;

	private final int maxFailures;

	private Double objectiveFunctionValue = null;

	private D decisionVariable = null;

	private int trials;

	private int failures;

	private boolean minimize = true;

	// -------------------- CONSTRUCTION --------------------

	public SearchRepeater(final int maxTrials, final int maxFailures) {
		this.maxTrials = maxTrials;
		this.maxFailures = maxFailures;
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public void setMinimize() {
		this.minimize = true;
	}

	public void setMaximize() {
		this.minimize = false;
	}

	public boolean getMinimize() {
		return this.minimize;
	}

	public boolean getMaximize() {
		return (!this.minimize);
	}

	// -------------------- IMPLEMENTATION --------------------

	private boolean isImprovement(final double newObjFctVal) {
		return ((this.getMinimize() && (newObjFctVal < this.objectiveFunctionValue))
				|| (this.getMaximize() && (newObjFctVal > this.objectiveFunctionValue)));
	}

	public void run(final SearchAlgorithm<? extends D> algo) {

		if (this.minimize) {
			this.objectiveFunctionValue = Double.POSITIVE_INFINITY;
		} else {
			this.objectiveFunctionValue = Double.NEGATIVE_INFINITY;
		}

		this.decisionVariable = null;
		this.trials = 0;
		this.failures = 0;

		while ((trials < this.maxTrials) && (failures < this.maxFailures)) {

			algo.run();
			this.trials++;

			final Double newObjFctVal = algo.getObjectiveFunctionValue();
			// if (newObjFctVal < this.objectiveFunctionValue) {
			if (this.isImprovement(newObjFctVal)) {
				this.objectiveFunctionValue = newObjFctVal;
				this.decisionVariable = algo.getDecisionVariable();
				this.failures = 0;
			} else {
				this.failures++;
			}
		}
	}

	public Double getObjectiveFunctionValue() {
		return this.objectiveFunctionValue;
	}

	public D getDecisionVariable() {
		return this.decisionVariable;
	}

	public int getTrials() {
		return this.trials;
	}

	public int getFailures() {
		return this.failures;
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		final int maxTrials = 1;
		final int maxFailures = 10;
		final SearchRepeater<Integer> repeater = new SearchRepeater<>(maxTrials, maxFailures);
		repeater.run(new SearchAlgorithm<Integer>() {
			private final Random rnd = new Random();
			private Double objVal = null;
			private Integer decVar = null;

			@Override
			public void run() {
				this.decVar = rnd.nextInt(1000);
				this.objVal = (double) this.decVar * this.decVar;
				System.out.println("decVar, objVal  =\t" + this.decVar + "\t" + this.objVal);
			}

			@Override
			public Double getObjectiveFunctionValue() {
				return this.objVal;
			}

			@Override
			public Integer getDecisionVariable() {
				return this.decVar;
			}
		});
		System.out.println("----------");
		System.out.println(
				"decVar, objVal  =\t" + repeater.getDecisionVariable() + "\t" + repeater.getObjectiveFunctionValue());
		System.out.println("total trials    =\t" + repeater.getTrials());
		System.out.println("recent failures =\t" + repeater.getFailures());
	}

}
