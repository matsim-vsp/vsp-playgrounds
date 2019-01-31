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

import static java.lang.Double.POSITIVE_INFINITY;

import gunnar.ihop4.sampersutilities.SampersParameterUtils.Purpose;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class SampersUtilityParameters {

	// ----- INNER CLASS DEFINING PARAMETERS THROUGH A CHAIN OF COMMAND -----

	private class ParameterPerStratum {

		private ParameterPerStratum next = null;

		public void addNext(final ParameterPerStratum next) {
			if (this.next != null) {
				this.next.addNext(next);
			} else {
				this.next = next;
			}
		}

		protected ParameterPerStratum getNext() {
			return this.next;
		}

		public double getOrZero(final Purpose purpose, final double income) {
			if (this.next != null) {
				return this.next.getOrZero(purpose, income);
			} else {
				return 0.0;
			}
		}
	}

	private class ConcreteParameterPerStratum extends ParameterPerStratum {

		private final Purpose tourPurpose;
		private final double minIncome;
		private final double maxIncome;
		private final double parameterValue;

		private ConcreteParameterPerStratum(final Purpose tourPurpose, final double minIncome, final double maxIncome,
				final double parameterValue) {
			this.tourPurpose = tourPurpose;
			this.minIncome = minIncome;
			this.maxIncome = maxIncome;
			this.parameterValue = parameterValue;
		}

		@Override
		public double getOrZero(final Purpose purpose, final double income) {
			if (this.tourPurpose.equals(purpose) && (this.minIncome <= income) && (this.maxIncome > income)) {
				return this.parameterValue;
			} else if (this.getNext() != null) {
				return this.getNext().getOrZero(purpose, income);
			} else {
				return 0.0;
			}
		}
	}

	// -------------------- MEMBERS --------------------

	private final ParameterPerStratum linTimeCoeff_1_min = new ParameterPerStratum();

	private final ParameterPerStratum linCostCoeff_1_SEK = new ParameterPerStratum();
	private final ParameterPerStratum lnCostCoeff_lnArgInSEK = new ParameterPerStratum();

	private final ParameterPerStratum scheduleDelayCostEarly_1_min = new ParameterPerStratum();
	private final ParameterPerStratum scheduleDelayCostLate_1_min = new ParameterPerStratum();

	// -------------------- CONSTRUCTION --------------------

	SampersUtilityParameters() {

		this.linTimeCoeff_1_min.addNext(new ConcreteParameterPerStratum(Purpose.work, 0.0, POSITIVE_INFINITY, -0.039));
		this.linCostCoeff_1_SEK.addNext(new ConcreteParameterPerStratum(Purpose.work, 0, 200 * 1000, -0.019));
		this.linCostCoeff_1_SEK.addNext(new ConcreteParameterPerStratum(Purpose.work, 200 * 1000, 300 * 1000, -0.015));
		this.linCostCoeff_1_SEK
				.addNext(new ConcreteParameterPerStratum(Purpose.work, 300 * 1000, POSITIVE_INFINITY, -0.005));
		this.lnCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(Purpose.work, 300 * 1000, POSITIVE_INFINITY, -0.052));

		this.linTimeCoeff_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.recreation, 0.0, POSITIVE_INFINITY, -0.041));
		this.linCostCoeff_1_SEK.addNext(new ConcreteParameterPerStratum(Purpose.recreation, 0, 50 * 1000, -0.030));
		this.linCostCoeff_1_SEK
				.addNext(new ConcreteParameterPerStratum(Purpose.recreation, 50 * 1000, POSITIVE_INFINITY, -0.014));
		this.lnCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(Purpose.recreation, 50 * 1000, POSITIVE_INFINITY, -0.066));

		this.linTimeCoeff_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.regularShopping, 0.0, POSITIVE_INFINITY, -0.084));
		this.linCostCoeff_1_SEK
				.addNext(new ConcreteParameterPerStratum(Purpose.regularShopping, 0.0, POSITIVE_INFINITY, -0.015));
		this.lnCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(Purpose.regularShopping, 0.0, POSITIVE_INFINITY, -0.421));

		this.linTimeCoeff_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.rareShopping, 0.0, POSITIVE_INFINITY, -0.042));
		this.linCostCoeff_1_SEK
				.addNext(new ConcreteParameterPerStratum(Purpose.rareShopping, 0.0, POSITIVE_INFINITY, -0.012));
		this.lnCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(Purpose.rareShopping, 0.0, POSITIVE_INFINITY, -0.271));

		this.linTimeCoeff_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.gymnasium, 0.0, POSITIVE_INFINITY, -0.040));
		this.linCostCoeff_1_SEK
				.addNext(new ConcreteParameterPerStratum(Purpose.gymnasium, 0.0, POSITIVE_INFINITY, -0.016));
		this.lnCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(Purpose.gymnasium, 0.0, POSITIVE_INFINITY, -0.146));

		this.linTimeCoeff_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.adultEducation, 0.0, POSITIVE_INFINITY, -0.049));

		this.linTimeCoeff_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.visit, 0.0, POSITIVE_INFINITY, -0.02973));
		this.linCostCoeff_1_SEK
				.addNext(new ConcreteParameterPerStratum(Purpose.visit, 0.0, POSITIVE_INFINITY, -0.00965));
		this.lnCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(Purpose.visit, 0.0, POSITIVE_INFINITY, -0.2931));

		this.linTimeCoeff_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.businessFromHome, 0.0, POSITIVE_INFINITY, -0.02843));
		this.lnCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(Purpose.businessFromHome, 0.0, POSITIVE_INFINITY, -0.3137));

		this.linTimeCoeff_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.giveARide, 0.0, POSITIVE_INFINITY, -0.06061));
		this.linCostCoeff_1_SEK
				.addNext(new ConcreteParameterPerStratum(Purpose.giveARide, 0.0, POSITIVE_INFINITY, -0.6268));

		this.linTimeCoeff_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.service, 0.0, POSITIVE_INFINITY, -0.0868));
		this.linCostCoeff_1_SEK
				.addNext(new ConcreteParameterPerStratum(Purpose.service, 0.0, POSITIVE_INFINITY, -0.0174));

		this.linTimeCoeff_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.other, 0.0, POSITIVE_INFINITY, -0.0434));
		this.linCostCoeff_1_SEK.addNext(new ConcreteParameterPerStratum(Purpose.other, 0, 50 * 1000, -0.008927));
		this.linCostCoeff_1_SEK
				.addNext(new ConcreteParameterPerStratum(Purpose.other, 50 * 1000, 200 * 1000, -0.008269));
		this.linCostCoeff_1_SEK
				.addNext(new ConcreteParameterPerStratum(Purpose.other, 200 * 1000, POSITIVE_INFINITY, -0.004279));
		this.lnCostCoeff_lnArgInSEK.addNext(new ConcreteParameterPerStratum(Purpose.other, 0, 50 * 1000, -0.393));
		this.lnCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(Purpose.other, 50 * 1000, 200 * 1000, +0.3348));
		this.lnCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(Purpose.other, 200 * 1000, POSITIVE_INFINITY, +0.3382));

		// TODO Below invented schedule delay costs; these should probably be derived
		// from activity-specific travel costs.

		this.scheduleDelayCostEarly_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.work, 0.0, POSITIVE_INFINITY, -1.0));
		this.scheduleDelayCostEarly_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.recreation, 0.0, POSITIVE_INFINITY, -1.0));
		this.scheduleDelayCostEarly_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.regularShopping, 0.0, POSITIVE_INFINITY, -1.0));
		this.scheduleDelayCostEarly_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.rareShopping, 0.0, POSITIVE_INFINITY, -1.0));
		this.scheduleDelayCostEarly_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.gymnasium, 0.0, POSITIVE_INFINITY, -1.0));
		this.scheduleDelayCostEarly_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.adultEducation, 0.0, POSITIVE_INFINITY, -1.0));
		this.scheduleDelayCostEarly_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.visit, 0.0, POSITIVE_INFINITY, -1.0));
		this.scheduleDelayCostEarly_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.businessFromHome, 0.0, POSITIVE_INFINITY, -1.0));
		this.scheduleDelayCostEarly_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.giveARide, 0.0, POSITIVE_INFINITY, -1.0));
		this.scheduleDelayCostEarly_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.service, 0.0, POSITIVE_INFINITY, -1.0));
		this.scheduleDelayCostEarly_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.other, 0.0, POSITIVE_INFINITY, -1.0));

		this.scheduleDelayCostLate_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.work, 0.0, POSITIVE_INFINITY, -1.0));
		this.scheduleDelayCostLate_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.recreation, 0.0, POSITIVE_INFINITY, -1.0));
		this.scheduleDelayCostLate_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.regularShopping, 0.0, POSITIVE_INFINITY, -1.0));
		this.scheduleDelayCostLate_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.rareShopping, 0.0, POSITIVE_INFINITY, -1.0));
		this.scheduleDelayCostLate_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.gymnasium, 0.0, POSITIVE_INFINITY, -1.0));
		this.scheduleDelayCostLate_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.adultEducation, 0.0, POSITIVE_INFINITY, -1.0));
		this.scheduleDelayCostLate_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.visit, 0.0, POSITIVE_INFINITY, -1.0));
		this.scheduleDelayCostLate_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.businessFromHome, 0.0, POSITIVE_INFINITY, -1.0));
		this.scheduleDelayCostLate_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.giveARide, 0.0, POSITIVE_INFINITY, -1.0));
		this.scheduleDelayCostLate_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.service, 0.0, POSITIVE_INFINITY, -1.0));
		this.scheduleDelayCostLate_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.other, 0.0, POSITIVE_INFINITY, -1.0));
	}

	// -------------------- PARAMETER GETTERS --------------------

	double getLinTimeCoeff_1_min(final Purpose purpose, final double income_money) {
		return this.linTimeCoeff_1_min.getOrZero(purpose, income_money);
	}

	double getLinMoneyCoeff_1_SEK(final Purpose purpose, final double income_money) {
		return this.linCostCoeff_1_SEK.getOrZero(purpose, income_money);
	}

	double getLnMoneyCoeff_lnArgInSEK(final Purpose purpose, final double income_money) {
		return this.lnCostCoeff_lnArgInSEK.getOrZero(purpose, income_money);
	}

	double getScheduleDelayCostEarly_1_min(final Purpose purpose, final Double income_money) {
		// A hopefully not completely nonsensical guess.
		return this.linTimeCoeff_1_min.getOrZero(purpose, income_money);
	}

	double getScheduleDelayCostLate_1_min(final Purpose purpose, final Double income_money) {
		// A hopefully not completely nonsensical guess.
		return this.linTimeCoeff_1_min.getOrZero(purpose, income_money);
	}

	double getStuckScore(final Purpose purpose, final Double income_money) {
		// A hopefully not completely nonsensical guess.
		return this.linTimeCoeff_1_min.getOrZero(purpose, income_money) * 24 * 60;
	}	
}
