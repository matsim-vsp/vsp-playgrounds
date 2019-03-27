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
package org.matsim.contrib.greedo.datastructures;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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

		Entry() {
		}

		void updateExpectedUtility(final double expectedUtility) {
			this.lastExpectedUtility = expectedUtility;
			if (this.lastRealizedUtility != null) {
				this.lastExpectedUtilityChange = expectedUtility - this.lastRealizedUtility;
			}
		}

		void updateRealizedUtility(final double realizedUtility) {
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
	}

	// -------------------- INNER SummaryStatistics CLASS --------------------

	public class SummaryStatistics {

		public final double expectedUtilitySum;

		public final double realizedUtilitySum;

		public final double deltaUtilitySum;

		// set to an unmodifiable instance
		public final Map<Id<Person>, Double> personId2currentDeltaUtility;

		public final int validExpectedUtilityCnt;
		public final int validRealizedUtilityCnt;
		public final int validDeltaUtilityCnt;
		
		private SummaryStatistics() {

			int validExpectedUtilityCnt = 0;
			int validRealizedUtilityCnt = 0;
			int validDeltaUtilityCnt = 0;

			double expectedUtilitySum = 0.0;
			double realizedUtilitySum = 0.0;
			double deltaUtilitySum = 0.0;
			Map<Id<Person>, Double> personId2currentDeltaUtility = new LinkedHashMap<>();

			for (Map.Entry<Id<Person>, Entry> mapEntry : personId2entry.entrySet()) {
				final Id<Person> personId = mapEntry.getKey();
				final Entry entry = mapEntry.getValue();

				if (entry.getLastExpectedUtility() != null) {
					validExpectedUtilityCnt++;
					expectedUtilitySum += entry.getLastExpectedUtility();
				}

				if (entry.getLastRealizedUtility() != null) {
					validRealizedUtilityCnt++;
					realizedUtilitySum += entry.getLastRealizedUtility();
				}

				if (entry.getLastExpectedUtilityChange() != null) {
					validDeltaUtilityCnt++;
					deltaUtilitySum += entry.getLastExpectedUtilityChange();
					personId2currentDeltaUtility.put(personId, entry.getLastExpectedUtilityChange());
				}
			}

			this.expectedUtilitySum = expectedUtilitySum;
			this.realizedUtilitySum = realizedUtilitySum;
			this.deltaUtilitySum = deltaUtilitySum;
			
			this.personId2currentDeltaUtility = Collections.unmodifiableMap(personId2currentDeltaUtility);
			
			this.validExpectedUtilityCnt = validExpectedUtilityCnt;
			this.validRealizedUtilityCnt = validRealizedUtilityCnt;
			this.validDeltaUtilityCnt = validDeltaUtilityCnt;
		}
	}

	// -------------------- MEMBERS --------------------

	private final Map<Id<Person>, Entry> personId2entry = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public Utilities() {
	}

	// -------------------- CONTENT ACCESS --------------------

	private Entry getOrCreateEntry(final Id<Person> personId) {
		Entry entry = this.personId2entry.get(personId);
		if (entry == null) {
			entry = new Entry();
			this.personId2entry.put(personId, entry);
		}
		return entry;
	}

	public void updateExpectedUtility(final Id<Person> personId, final double expectedUtility) {
		this.getOrCreateEntry(personId).updateExpectedUtility(expectedUtility);
	}

	public void updateRealizedUtility(final Id<Person> personId, final Double realizedUtility) {
		this.getOrCreateEntry(personId).updateRealizedUtility(realizedUtility);
	}

	public SummaryStatistics newSummaryStatistics() {
		return new SummaryStatistics();
	}
}
