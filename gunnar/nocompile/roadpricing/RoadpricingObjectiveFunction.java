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

import com.google.inject.Inject;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.roadpricing.CalcPaidToll;

/**
 * Returns the negative sum of the scores of the selected plans of all agents,
 * excluding toll.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RoadpricingObjectiveFunction implements ObjectiveFunction {

	@Inject
	private CalcPaidToll calcPaidToll;

	private final double tollEffectivity;

	// private final Coord tollZoneCenter = CoordUtils
	// .createCoord(674000, 6581000);
	// private final double radius = 6000;

	public RoadpricingObjectiveFunction(final double tollEffectivity) {
		this.tollEffectivity = tollEffectivity;
	}

	private boolean isNearTollZone(final Plan plan) {
		return true;
		// for (int i = 0; i < plan.getPlanElements().size(); i += 2) {
		// final Activity act = (Activity) plan.getPlanElements().get(i);
		// if (CoordUtils.calcDistance(act.getCoord(), this.tollZoneCenter) <
		// this.radius) {
		// return true;
		// }
		// }
		// return false;
	}

	@Override
	public double value(final SimulatorState state) {
		final RoadpricingState roadpricingState = (RoadpricingState) state;
		double result = -this.tollEffectivity * this.calcPaidToll.getAllAgentsToll();
		for (Id<Person> personId : roadpricingState.getPersonIdView()) {
			final Plan selectedPlan = roadpricingState
					.getSelectedPlan(personId);
			if (isNearTollZone(selectedPlan)) {
				result -= selectedPlan.getScore();
			}
		}
		result /= roadpricingState.getPersonIdView().size();
		return result;
	}
}
