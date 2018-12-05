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
package org.matsim.contrib.opdyts.buildingblocks.decisionvariables.capacityscaling;

import java.util.function.Consumer;

import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.scalar.ScalarDecisionVariable;
import org.matsim.core.config.Config;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SimulatedDemandShare implements ScalarDecisionVariable<SimulatedDemandShare> {

	// -------------------- CONSTANTS --------------------

	private final Config config;

	// TODO This could be the basis for an AbstractScalarDecisionVariable.
	private final Consumer<Double> optionalConsumer;

	// -------------------- MEMBERS --------------------

	private double value;

	// -------------------- CONSTRUCTION --------------------

	public SimulatedDemandShare(final Config config, final double factor, final Consumer<Double> optionalConsumer) {
		this.config = config;
		this.value = factor;
		this.optionalConsumer = optionalConsumer;
	}

	// --------------- IMPLEMENTATION OF ScalarDecisionVariable ---------------

	@Override
	public void implementInSimulation() {
		this.config.qsim().setFlowCapFactor(this.value);
		this.config.qsim().setStorageCapFactor(this.value);
		if (this.optionalConsumer != null) {
			this.optionalConsumer.accept(this.value);
		}
	}

	@Override
	public void setValue(final double val) {
		this.value = val;
	}

	@Override
	public double getValue() {
		return this.value;
	}

	@Override
	public SimulatedDemandShare newDeepCopy() {
		return new SimulatedDemandShare(this.config, this.value, this.optionalConsumer);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + this.value + ")";
	}
}
