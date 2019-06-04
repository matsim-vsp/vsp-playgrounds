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
package org.matsim.contrib.greedo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Utilities {

	// -------------------- INNER Entry CLASS --------------------

	private static class Entry {

		private Double lastRealizedUtility = null;
		private Double lastExpectedUtility = null;
		private Double lastExpectedUtilityChange = null;
		private Double lastRealizedUtilityChange = null;

		Entry() {
		}

		void updateExpectedUtility(final double expectedUtility) {
			this.lastExpectedUtility = expectedUtility;
			if (this.lastRealizedUtility != null) {
				this.lastExpectedUtilityChange = expectedUtility - this.lastRealizedUtility;
			}
		}

		void updateRealizedUtility(final double realizedUtility) {
			if (this.lastRealizedUtility != null) {
				this.lastRealizedUtilityChange = realizedUtility - this.lastRealizedUtility;
			}
			this.lastRealizedUtility = realizedUtility;
		}

		Double getLastRealizedUtility() {
			return this.lastRealizedUtility;
		}

		Double getLastExpectedUtility() {
			return this.lastExpectedUtility;
		}

		Double getLastExpectedUtilityChange() {
			return this.lastExpectedUtilityChange;
		}
		
		Double getLastRealizedUtilityChange() {
			return this.lastRealizedUtilityChange;
		}
		
	}

	// -------------------- INNER SummaryStatistics CLASS --------------------

	// TODO decide about scope; this is used in the logging package
	public class SummaryStatistics {

		// set to an unmodifiable instance
		public final Map<Id<Person>, Double> personId2expectedUtilityChange;

		final double expectedUtilitySum;
		final double realizedUtilitySum;
		final double expectedUtilityChangeSum;
		final double realizedUtilityChangeSum;

		final int validExpectedUtilityCnt;
		final int validRealizedUtilityCnt;
		final int validExpectedUtilityChangeCnt;
		final int validRealizedUtilityChangeCnt;
		
		private SummaryStatistics() {

			final Map<Id<Person>, Double> personId2expectedUtilityChange = new LinkedHashMap<>();

			double expectedUtilitySum = 0.0;
			double realizedUtilitySum = 0.0;
			double expectedUtilityChangeSum = 0.0;
			double realizedUtilityChangeSum = 0.0;

			int validExpectedUtilityCnt = 0;
			int validRealizedUtilityCnt = 0;
			int validExpectedUtilityChangeCnt = 0;
			int validRealizedUtilityChangeCnt = 0;
			
			for (Map.Entry<Id<Person>, Entry> mapEntry : personId2entry.entrySet()) {
				final Id<Person> personId = mapEntry.getKey();
				final Entry entry = mapEntry.getValue();

				if (entry.getLastExpectedUtility() != null) {
					expectedUtilitySum += entry.getLastExpectedUtility();
					validExpectedUtilityCnt++;
				}
				if (entry.getLastRealizedUtility() != null) {
					realizedUtilitySum += entry.getLastRealizedUtility();
					validRealizedUtilityCnt++;
				}
				if (entry.getLastExpectedUtilityChange() != null) {
					personId2expectedUtilityChange.put(personId, entry.getLastExpectedUtilityChange());
					expectedUtilityChangeSum += entry.getLastExpectedUtilityChange();
					validExpectedUtilityChangeCnt++;
				}
				if (entry.getLastRealizedUtilityChange() != null) {
					realizedUtilityChangeSum += entry.getLastRealizedUtilityChange();
					validRealizedUtilityChangeCnt++;
				}
			}

			this.personId2expectedUtilityChange = Collections.unmodifiableMap(personId2expectedUtilityChange);

			this.expectedUtilitySum = expectedUtilitySum;
			this.realizedUtilitySum = realizedUtilitySum;
			this.expectedUtilityChangeSum = expectedUtilityChangeSum;
			this.realizedUtilityChangeSum = realizedUtilityChangeSum;

			this.validExpectedUtilityCnt = validExpectedUtilityCnt;
			this.validRealizedUtilityCnt = validRealizedUtilityCnt;
			this.validExpectedUtilityChangeCnt = validExpectedUtilityChangeCnt;
			this.validRealizedUtilityChangeCnt = validRealizedUtilityChangeCnt;

			Logger.getLogger(this.getClass()).info("Created instance:");
			Logger.getLogger(this.getClass()).info(this.toString());
		}

		@Override
		public String toString() {
			final StringBuffer result = new StringBuffer();
			result.append("Expected utility sum: " + this.expectedUtilitySum + "; number of valid persons: "
					+ this.validExpectedUtilityCnt + "\n");
			result.append("Realized utility sum: " + this.realizedUtilitySum + "; number of valid persons: "
					+ this.validRealizedUtilityCnt + "\n");
			result.append("Expected utility change sum: " + this.expectedUtilityChangeSum
					+ "; number of valid persons: " + this.validExpectedUtilityChangeCnt + "\n");
			result.append(
					"number of individual expected utility changes: " + this.personId2expectedUtilityChange.size());
			result.append("Realized utility change sum: " + this.realizedUtilityChangeSum
					+ "; number of valid persons: " + this.validRealizedUtilityChangeCnt + "\n");
			return result.toString();
		}
	}

	// -------------------- MEMBERS --------------------

	private final Map<Id<Person>, Entry> personId2entry = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	Utilities() {
	}

	// -------------------- INTERNALS --------------------

	private Entry getOrCreateEntry(final Id<Person> personId) {
		Entry entry = this.personId2entry.get(personId);
		if (entry == null) {
			entry = new Entry();
			this.personId2entry.put(personId, entry);
		}
		return entry;
	}

	// -------------------- IMPLEMENTATION --------------------

	void updateExpectedUtility(final Id<Person> personId, final double expectedUtility) {
		this.getOrCreateEntry(personId).updateExpectedUtility(expectedUtility);
	}

	void updateRealizedUtility(final Id<Person> personId, final Double realizedUtility) {
		this.getOrCreateEntry(personId).updateRealizedUtility(realizedUtility);
	}

	SummaryStatistics newSummaryStatistics() {
		return new SummaryStatistics();
	}
}
