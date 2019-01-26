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

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.utils.objectattributes.attributable.Attributes;

import gunnar.ihop4.sampersutilities.SampersParameterUtils.Purpose;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class SampersTour {

	// -------------------- MEMBERS --------------------

	private Leg firstLeg = null;

	private Activity act = null;

	private Attributes activityAttrs = null;
	
	private Purpose purpose = null;

	private Leg secondLeg = null;

	private double cost_SEK = 0.0;

	// -------------------- CONSTRUCTION / BUILDING --------------------

	SampersTour() {
	}

	private String buildStatus() {
		return "First leg is " + (this.firstLeg == null ? "" : "not ") + "null, activity is "
				+ (this.act == null ? "" : "not ") + "null, second leg is " + (this.secondLeg == null ? "" : "not ")
				+ "null.";
	}

	void addLeg(final Leg leg) {
		if ((this.firstLeg == null) && (this.act == null) && (this.secondLeg == null)) {
			this.firstLeg = leg;
		} else if ((this.firstLeg != null) && (this.act != null) && (this.secondLeg == null)) {
			this.secondLeg = leg;
		} else {
			throw new RuntimeException("Cannot add a leg: " + this.buildStatus());
		}
	}

	void addActivity(final Activity act, final Attributes attrs) {
		if ((this.firstLeg != null) && (this.act == null) && (this.secondLeg == null)) {
			this.act = act;
			this.activityAttrs = attrs;
			this.purpose = SampersParameterUtils.Purpose.valueOf(act.getType());
		} else {
			throw new RuntimeException("Cannot add an activity: " + this.buildStatus());
		}
	}

	void addCost_SEK(final double cost_SEK) {
		this.cost_SEK += cost_SEK;
	}

	boolean isComplete() {
		return ((this.firstLeg != null) && (this.act != null) && (this.secondLeg != null));
	}

	// -------------------- GETTERS --------------------

	Leg getFirstLeg() {
		return this.firstLeg;
	}

	Leg getSecondLeg() {
		return this.secondLeg;
	}

	Activity getActivity() {
		return this.act;
	}
	
	Attributes getActivityAttrs() {
		return this.activityAttrs;
	}

	Purpose getPurpose() {
		return this.purpose;
	}

	double getTravelTime_min() {
		return (this.firstLeg.getTravelTime() + this.secondLeg.getTravelTime()) / 60.0;
	}

	double getCost_SEK() {
		return this.cost_SEK;
	}

}
