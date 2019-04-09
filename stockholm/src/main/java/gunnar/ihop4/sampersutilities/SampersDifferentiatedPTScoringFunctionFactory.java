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
package gunnar.ihop4.sampersutilities;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

import com.google.inject.Inject;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SampersDifferentiatedPTScoringFunctionFactory implements ScoringFunctionFactory {

	private final SampersTourUtilityFunction utilityFunction;

	@Inject
	public SampersDifferentiatedPTScoringFunctionFactory() {
		this.utilityFunction = new SampersTourUtilityFunction(new SampersUtilityParameters());
	}

	@Override
	public ScoringFunction createNewScoringFunction(final Person person) {
		return new SampersDifferentiatedPTScoringFunction(person, this.utilityFunction);
	}

}
