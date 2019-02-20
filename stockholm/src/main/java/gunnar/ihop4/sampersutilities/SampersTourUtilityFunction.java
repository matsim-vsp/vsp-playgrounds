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

import floetteroed.utilities.Units;
import gunnar.ihop4.sampersutilities.SampersUtilityParameters.Purpose;

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

	// private double earlyArrivalTimeLoss_min(final SampersTour tour) {
	// return Math.max(0.0,
	// SampersParameterUtils.getActivityOpens_min(tour) -
	// SampersParameterUtils.getActivityStart_min(tour));
	// }
	//
	// private double lateDepartureTimeLoss_min(final SampersTour tour) {
	// return Math.max(0.0,
	// SampersParameterUtils.getActivityEnd_min(tour) -
	// SampersParameterUtils.getActivityCloses_min(tour));
	// }

	double getScheduleDelayCost(final SampersTour tour, final Double income_SEK_yr) {
		final Purpose purpose = tour.getPurpose();
		double result = 0.0;

		final Double plannedStart_h = SampersAttributeUtils.getPlannedActivityStart_h(tour);
		if (plannedStart_h != null) {
			final double plannedStart_min = Units.MIN_PER_H * plannedStart_h;
			final double realizedStart_min = Units.MIN_PER_S * tour.getRealizedStartTime_s();
			result += this.utlParams.getScheduleDelayCostEarly_1_min(purpose, income_SEK_yr)
					* Math.max(0.0, (plannedStart_min - realizedStart_min) - this.utlParams.getScheduleDelaySlack_min())
					+ this.utlParams.getScheduleDelayCostLate_1_min(purpose, income_SEK_yr) * Math.max(0.0,
							(realizedStart_min - plannedStart_min) - this.utlParams.getScheduleDelaySlack_min());
		}

		final Double plannedDuration_h = SampersAttributeUtils.getPlannedActivityDuration_h(tour);
		if (plannedDuration_h != null) {
			final double plannedDuration_min = Units.MIN_PER_H * plannedDuration_h;
			final double realizedDuration_min = Units.MIN_PER_S * (tour.getRealizedActivityDuration_s());
			result += this.utlParams.getScheduleDelayCostTooShort_1_min(purpose, income_SEK_yr)
					* Math.max(0.0, plannedDuration_min - realizedDuration_min)
					+ this.utlParams.getScheduleDelayCostTooLong_1_min(purpose, income_SEK_yr)
							* Math.max(0.0, realizedDuration_min - plannedDuration_min);
		}

		return result;
	}

	// -------------------- IMPLEMENTATION --------------------

	double getStuckScore(final Person person) {
		return this.utlParams.getStuckScore(SampersUtilityParameters.Purpose.work,
				SampersAttributeUtils.getIncome_SEK_yr(person));
	}

	double getUtility(final SampersTour tour, final Person person) {

		final Purpose purpose = tour.getPurpose();
		final double income_SEK_yr = SampersAttributeUtils.getIncome_SEK_yr(person);

		/*
		 * Schedule delay.
		 */
		double result = this.getScheduleDelayCost(tour, income_SEK_yr);

		/*
		 * Time.
		 */
		result += this.utlParams.getLinTimeCoeff_1_min(purpose, income_SEK_yr) * tour.getRealizedTravelTime_min();

		/*
		 * Monetary cost.
		 */
		final double cost_SEK = tour.getEventBasedCost_SEK()
				+ this.utlParams.getMonetaryDistanceCost_SEK_km() * tour.getRealizedTravelDistance_km();
		if (cost_SEK < 0) {
			throw new RuntimeException("tour cost = " + cost_SEK + " SEK");
		}
		if (cost_SEK > eps) {
			result += this.utlParams.getLinMoneyCoeff_1_SEK(purpose, income_SEK_yr) * cost_SEK
					+ this.utlParams.getLnMoneyCoeff_lnArgInSEK(purpose, income_SEK_yr) * Math.log(cost_SEK);
		}

		return result;
	}
}
