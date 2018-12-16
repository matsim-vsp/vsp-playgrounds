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
package org.matsim.contrib.pseudosimulation.searchacceleration.datastructures;

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

	private final double ageInertia;

	/* package for testing */ Map<Id<Person>, Integer> personId2age;

	/* package for testing */ int maxAge = 0;

	/* package for testing */ List<Integer> sortedAges;

	public Ages(final double ageInertia, final Set<Id<Person>> populationIds) {
		this.ageInertia = ageInertia;
		this.personId2age = populationIds.stream().collect(Collectors.toMap(id -> id, id -> 0));
		this.updateAgeStats();
		// this.maxAge = this.personId2age.values().stream().max((a, b) ->
		// Integer.compare(a, b)).get();
	}

	private void updateAgeStats() {
		this.sortedAges = new ArrayList<>(this.personId2age.values());
		Collections.sort(this.sortedAges);
		this.maxAge = this.sortedAges.get(this.sortedAges.size() - 1);
	}

	public void update(final Set<Id<Person>> replanners) {
		this.personId2age = this.personId2age.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey(),
				entry -> replanners.contains(entry.getKey()) ? 0 : entry.getValue() + 1));
		// this.maxAge = this.personId2age.values().stream().max((a, b) ->
		// Integer.compare(a, b)).get();
		this.updateAgeStats();
	}

	public double getWeight(final Id<Person> personId) {
		// Assume that 0 < inertia < 1. Having maxAge yields a weight of inertia^0 = 1;
		// having just re-planned (i.e. zero age) yields a weight of inertia^maxAge < 1.
		// An inertia of one means that the age plays no role.
		return Math.pow(this.ageInertia, this.maxAge - this.personId2age.get(personId));
	}

	public List<Integer> getSortedAgesView() {
		return Collections.unmodifiableList(this.sortedAges);
	}

}
