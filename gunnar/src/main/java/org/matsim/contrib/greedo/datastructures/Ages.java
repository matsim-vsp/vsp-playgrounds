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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Ages {

	// -------------------- MEMBERS --------------------

	/* package for testing */ Map<Id<Person>, Integer> personId2age = null;

	private List<Integer> sortedAges = null;

	private double averageAge;

	private double averageWeight;

	/* package for testing */ int maxAge;

	private Map<Id<Person>, Double> personId2weight = null;

	// -------------------- CONSTRUCTION --------------------

	public Ages(final Set<Id<Person>> populationIds) {
		this.personId2age = Collections
				.unmodifiableMap(populationIds.stream().collect(Collectors.toMap(id -> id, id -> 0)));
		this.internalUpdate(new double[] { 1.0 });
	}

	// -------------------- INTERNALS --------------------

	private void internalUpdate(final double[] ageWeights) {

		this.sortedAges = new ArrayList<>(this.personId2age.values());
		Collections.sort(this.sortedAges);
		this.maxAge = this.sortedAges.get(this.sortedAges.size() - 1);
		this.averageAge = this.sortedAges.stream().mapToDouble(age -> age).average().getAsDouble();

		this.personId2weight = Collections.unmodifiableMap(this.personId2age.entrySet().stream()
				.collect(Collectors.toMap(entry -> entry.getKey(), entry -> ageWeights[entry.getValue()])));
		this.averageWeight = this.personId2weight.values().stream().mapToDouble(weight -> weight).average()
				.getAsDouble();
	}

	// -------------------- IMPLEMENTATION --------------------

	public void update(final Set<Id<Person>> replanners, final double[] ageWeights) {
		this.personId2age = Collections
				.unmodifiableMap(this.personId2age.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey(),
						entry -> replanners.contains(entry.getKey()) ? 0 : entry.getValue() + 1)));
		this.internalUpdate(ageWeights);
	}

	public List<Integer> getSortedAgesView() {
		return Collections.unmodifiableList(this.sortedAges);
	}

	public Map<Id<Person>, Double> getPersonWeights() {
		return this.personId2weight;
	}

	public double getAverageAge() {
		return this.averageAge;
	}

	public double getAverageWeight() {
		return this.averageWeight;
	}
}
