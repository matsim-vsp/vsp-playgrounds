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

import gunnar.ihop4.sampersutilities.SampersParameterUtils.Purpose;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class SampersTourUtilityFunction {

	// -------------------- CONSTANTS --------------------

	private static final double eps = 1e-8;

	private final SampersUtilityParameters utlParams;

	// -------------------- CONSTRUCTION --------------------

	SampersTourUtilityFunction(final SampersUtilityParameters utlParams) {
		this.utlParams = utlParams;
	}

	// -------------------- INTERNALS --------------------

	private double earlyArrivalTimeLoss_min(final SampersTour tour) {
		return Math.max(0.0,
				SampersParameterUtils.getActivityOpens_min(tour) - SampersParameterUtils.getActivityStart_min(tour));
	}

	private double lateDepartureTimeLoss_min(final SampersTour tour) {
		return Math.max(0.0,
				SampersParameterUtils.getActivityEnd_min(tour) - SampersParameterUtils.getActivityCloses_min(tour));
	}

	// -------------------- IMPLEMENTATION --------------------

	double getUtility(final SampersTour tour, final Person person) {

		final Purpose purpose = tour.getPurpose();
		final double income_SEK = SampersParameterUtils.getIncome_SEK_yr(person);
		
		double result = this.utlParams.getScheduleDelayCostEarly_1_min(purpose, income_SEK)
				* this.earlyArrivalTimeLoss_min(tour)
				+ this.utlParams.getScheduleDelayCostLate_1_min(purpose, income_SEK)
						* this.lateDepartureTimeLoss_min(tour);

		final double travelTime_min = tour.getTravelTime_min();
		if (travelTime_min > eps) {
			result += this.utlParams.getLinTimeCoeff_1_min(purpose, income_SEK) * travelTime_min;
		}

		final int stuck = tour.getStuck();
		if (stuck > 0) {
			result += this.utlParams.getStuckScore(tour, income_SEK) * stuck;
		}

		final double cost_SEK = -tour.getMoney_SEK();
		if (cost_SEK < 0) {
			throw new RuntimeException("tour cost = " + cost_SEK + " SEK");
		}
		if (cost_SEK > eps) {
			result += this.utlParams.getLinMoneyCoeff_1_SEK(purpose, income_SEK) * cost_SEK
					+ this.utlParams.getLnMoneyCoeff_lnArgInSEK(purpose, income_SEK) * Math.log(cost_SEK);
		}

		return result;
	}
}
