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
package cba.trianglenet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;
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

	private final boolean includeMATSimScore;

	// -------------------- CONSTRUCTION --------------------

	ChoiceModel(final int sampleCnt, final Random rnd, final Scenario scenario,
			final Provider<TripRouter> tripRouterProvider, final Map<String, TravelTime> mode2travelTime,
			final int maxTrials, final int maxFailures, final boolean includeMATSimScore) {
		this.sampleCnt = sampleCnt;
		this.rnd = rnd;
		this.scenario = scenario;
		this.tripRouterProvider = tripRouterProvider;
		this.mode2travelTime = mode2travelTime;
		this.maxTrials = maxTrials;
		this.maxFailures = maxFailures;
		this.includeMATSimScore = includeMATSimScore;
	}

	ChoiceRunnerForResampling newChoiceRunner(final Link homeLoc, final Person person, final int workAlts,
			final int otherAlts, final boolean carAvailable) {
		return new ChoiceRunnerForResampling(this.sampleCnt, this.rnd, this.scenario, this.tripRouterProvider,
				this.mode2travelTime, homeLoc, person,
				newTourSeqAlternatives(this.scenario, workAlts, otherAlts, carAvailable, homeLoc), this.maxTrials,
				this.maxFailures, this.includeMATSimScore);
	}

	// -------------------- IMPLEMENTATION --------------------

	private List<Link> sampleLinks(final Scenario scenario, final int cnt, final Set<Link> excluded) {
		final LinkedList<Link> fullList = new LinkedList<>(scenario.getNetwork().getLinks().values());
		final ArrayList<Link> result = new ArrayList<>(cnt);
		while (result.size() < cnt) {
			final int index = MatsimRandom.getRandom().nextInt(fullList.size());
			if (!excluded.contains(fullList.get(index))) {
				result.add(fullList.remove(index));
			}
		}
		return result;
	}

	private List<TourSequence> newTourSeqAlternatives(final Scenario scenario, final int workLocCnt,
			final int otherLocCnt, final boolean carAvailable, final Link homeLoc) {

		final Set<Link> excludedFromSampling = new LinkedHashSet<>(3);
		excludedFromSampling.add(homeLoc);
		final List<Link> workLocs = sampleLinks(scenario, workLocCnt, excludedFromSampling);
		excludedFromSampling.addAll(workLocs);
		final List<Link> otherLocs = sampleLinks(scenario, otherLocCnt, excludedFromSampling);

		final Tour.Mode[] availableModes;
		if (carAvailable) {
			availableModes = new Tour.Mode[] { Tour.Mode.car, Tour.Mode.pt };
		} else {
			availableModes = new Tour.Mode[] { Tour.Mode.pt };
		}

		/*
		 * CREATE ALL ALTERNATIVES ONCE.
		 */

		final List<TourSequence> tmpAlternatives = new ArrayList<>();

		// NO TOUR -- TODO special departure time case, omitted
		// {
		// final TourSequence alternative = new TourSequence();
		// tmpAlternatives.add(alternative);
		// }

		// ONE TOUR (WORK OR OTHER)
		// {
		// for (Link loc : workLocs) {
		// for (Tour.Act act : new Tour.Act[] { Tour.Act.work, Tour.Act.other })
		// {
		// for (Tour.Mode mode : availableModes) {
		// final TourSequence alternative = new TourSequence();
		// alternative.tours.add(new Tour(loc, act, mode));
		// tmpAlternatives.add(alternative);
		// }
		// }
		// }
		// }

		// TWO TOURS (WORK, THEN OTHER)
		{
			for (Link workLoc : workLocs) {
				for (Tour.Mode workMode : availableModes) {
					for (Link otherLoc : otherLocs) {
						for (Tour.Mode otherMode : availableModes) {
							final TourSequence alternative = new TourSequence();
							alternative.tours.add(new Tour(workLoc, Tour.Act.work, workMode));
							alternative.tours.add(new Tour(otherLoc, Tour.Act.other, otherMode));
							tmpAlternatives.add(alternative);
						}
					}
				}
			}
		}

		return Collections.unmodifiableList(tmpAlternatives);
	}
}
