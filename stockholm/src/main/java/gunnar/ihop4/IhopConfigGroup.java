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
package gunnar.ihop4;

import java.util.function.Function;

import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.composite.CompositeDecisionVariable;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.composite.OneAtATimeRandomizer;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.composite.RandomCombinationRandomizer;
import org.matsim.core.config.ReflectiveConfigGroup;

import floetteroed.opdyts.DecisionVariableRandomizer;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class IhopConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "ihop";

	public IhopConfigGroup() {
		super(GROUP_NAME);
	}

	// -------------------- tollZoneCountsFolder --------------------

	private String tollZoneCountsFolder = null;

	@StringSetter("tollZoneCountsFolder")
	public void setTollZoneCountsFolder(final String tollZoneCountsFolder) {
		this.tollZoneCountsFolder = tollZoneCountsFolder;
	}

	@StringGetter("tollZoneCountsFolder")
	public String getTollZoneCountsFolder() {
		return this.tollZoneCountsFolder;
	}

	// -------------------- simulatedPopulationShare --------------------

	private Double simulatedPopulationShare = null;

	@StringSetter("simulatedPopulationShare")
	public void setSimulatedPopulationShare(final Double simulatedPopulationShare) {
		this.simulatedPopulationShare = simulatedPopulationShare;
	}

	@StringGetter("simulatedPopulationShare")
	public Double getSimulatedPopulationShare() {
		return this.simulatedPopulationShare;
	}

	// -------------------- countResidualMagnitude --------------------

	public static enum CountResidualMagnitudeType {
		absolute, square
	};

	private CountResidualMagnitudeType countResidualMagnitude = null;

	@StringGetter("countResidualMagnitude")
	public CountResidualMagnitudeType getCountResidualMagnitude() {
		return this.countResidualMagnitude;
	}

	@StringSetter("countResidualMagnitude")
	public void setCountResidualMagnitude(final CountResidualMagnitudeType countResidualMagnitude) {
		this.countResidualMagnitude = countResidualMagnitude;
	}

	public Function<Double, Double> newCountResidualMagnitudeFunction() {
		if (CountResidualMagnitudeType.absolute.equals(this.countResidualMagnitude)) {
			return (Double res) -> Math.abs(res);
		} else if (CountResidualMagnitudeType.square.equals(this.countResidualMagnitude)) {
			return (Double res) -> res * res;
		} else {
			throw new RuntimeException("Unknown countResidualMagnitude: " + this.countResidualMagnitude);
		}
	}

	// -------------------- DECISION VARIABLE RANDOMIZATION --------------------

	// TODO one could move this into OpdytsIntegration?

	// randomizer

	public static enum DecisionVariableRandomizerType {
		coordByCoord, randomRecombination
	};

	private DecisionVariableRandomizerType decisionVariableRandomizer = null;

	@StringGetter("decisionVariableRandomizer")
	public DecisionVariableRandomizerType getDecisionVariableRandomizer() {
		return this.decisionVariableRandomizer;
	}

	@StringSetter("decisionVariableRandomizer")
	public void setRandomizer(final DecisionVariableRandomizerType randomizer) {
		this.decisionVariableRandomizer = randomizer;
	}

	// numberOfVariations

	private Integer numberOfDecisionVariableVariations = null;

	@StringGetter("numberOfDecisionVariableVariations")
	public Integer getNumberOfDecisionVariableVariations() {
		return this.numberOfDecisionVariableVariations;
	}

	@StringSetter("numberOfDecisionVariableVariations")
	public void setNumberOfDecisionVariableVariations(final Integer numberOfVariations) {
		this.numberOfDecisionVariableVariations = numberOfVariations;
	}

	// innovationProba

	private Double decisionVariableInnovationProba = null;

	@StringGetter("decisionVariableInnovationProba")
	public Double getDecisionVariableInnovationProba() {
		return this.decisionVariableInnovationProba;
	}

	@StringSetter("decisionVariableInnovationProba")
	public void setDecisionVariableInnovationProba(final Double decisionVariableInnovationProba) {
		this.decisionVariableInnovationProba = decisionVariableInnovationProba;
	}

	// convenience factory method

	public DecisionVariableRandomizer<CompositeDecisionVariable> newDecisionVariableRandomizer() {
		if (DecisionVariableRandomizerType.coordByCoord.equals(this.decisionVariableRandomizer)) {
			return new OneAtATimeRandomizer();
		} else if (DecisionVariableRandomizerType.randomRecombination.equals(this.decisionVariableRandomizer)) {
			return new RandomCombinationRandomizer(this.numberOfDecisionVariableVariations,
					this.decisionVariableInnovationProba);
		} else {
			throw new RuntimeException("Unknown decision variable randomizer: " + this.decisionVariableRandomizer);
		}
	}

}
