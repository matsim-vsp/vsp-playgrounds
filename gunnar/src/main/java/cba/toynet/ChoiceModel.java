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
package cba.toynet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Provider;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class ChoiceModel {

	// -------------------- MEMBERS --------------------

	private final int sampleCnt;

	private final Random rnd;

	private final Scenario scenario;

	private final Provider<TripRouter> tripRouterProvider;

	private final Map<String, TravelTime> mode2travelTime;

	private final int maxTrials;

	private final int maxFailures;

	private final List<TourSequence> tourSeqAlts = new ArrayList<>(TourSequence.Type.values().length);

	private final boolean usePTto1;

	private final boolean usePTto2;

	private final double betaTravelSampers_1_h;

	private final SampersCarDelay sampersCarDelay;

	private final double sampersLogitScale;

	// private String lastUtilitiesToString = null;

	// -------------------- CONSTRUCTION --------------------

	ChoiceModel(final int sampleCnt, final Random rnd, final Scenario scenario,
			final Provider<TripRouter> tripRouterProvider, final Map<String, TravelTime> mode2travelTime,
			final int maxTrials, final int maxFailures, final boolean usePTto1, final boolean usePTto2,
			final double betaTravelSampers_1_h, final SampersCarDelay sampersCarDelay, final double sampersLogitScale) {
		this.sampleCnt = sampleCnt;
		this.rnd = rnd;
		this.scenario = scenario;
		this.tripRouterProvider = tripRouterProvider;
		this.mode2travelTime = mode2travelTime;
		this.maxTrials = maxTrials;
		this.maxFailures = maxFailures;
		this.usePTto1 = usePTto1;
		this.usePTto2 = usePTto2;
		this.betaTravelSampers_1_h = betaTravelSampers_1_h;
		this.sampersCarDelay = sampersCarDelay;
		this.sampersLogitScale = sampersLogitScale;

		for (TourSequence.Type type : TourSequence.Type.values()) {
			final TourSequence tourSeq = new TourSequence(type);
			this.tourSeqAlts.add(tourSeq);
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	ChoiceRunner newChoiceRunner(final Person person) {
		return new ChoiceRunner(person, this.sampleCnt, this.rnd, this.scenario, this.tripRouterProvider,
				this.mode2travelTime, this.maxTrials, this.maxFailures, this.usePTto1, this.usePTto2,
				this.betaTravelSampers_1_h, this.sampersCarDelay, this.sampersLogitScale);
	}

	// String getLastUtilitiesToString() {
	// return this.lastUtilitiesToString;
	// }
}
