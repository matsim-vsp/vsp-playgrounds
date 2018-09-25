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

import floetteroed.utilities.TimeDiscretization;
import floetteroed.utilities.math.Vector;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.opdyts.MATSimState;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RoadpricingState extends MATSimState {

	private final TollLevels tollLevels;

	private final TimeDiscretization timeDiscretization;

	private final double occupancyScale;

	private final double tollScale;

	RoadpricingState(final Population population,
			final Vector vectorRepresentation, final TollLevels tollLevels,
			final TimeDiscretization timeDiscretization,
			final double occupancyScale, final double tollScale) {
		super(population, vectorRepresentation);
		this.tollLevels = tollLevels;
		this.timeDiscretization = timeDiscretization;
		this.occupancyScale = occupancyScale;
		this.tollScale = tollScale;
	}

	public Vector getTollLevelsAsVector() {
		return this.tollLevels.toVector(this.timeDiscretization);
	}

	@Override
	public Vector getReferenceToVectorRepresentation() {
		final Vector occupancies = super.getReferenceToVectorRepresentation()
				.copy();
		occupancies.mult(this.occupancyScale);
		final Vector tolls = this.getTollLevelsAsVector().copy();
		tolls.mult(this.tollScale);
		return Vector.concat(occupancies, tolls);
	}

}
