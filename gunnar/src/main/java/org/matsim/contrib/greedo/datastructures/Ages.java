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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.greedo.GreedoConfigGroup;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Ages {

	// -------------------- MEMBERS --------------------

	private final GreedoConfigGroup greedoConfig;
	
	/* package for testing */ Map<Id<Person>, Integer> personId2age = null;

	private List<Integer> sortedAges = null;

	private double averageAge;

	private double averageWeight;

	private double weightAtAverageAge;

	/* package for testing */ int maxAge;

	private Map<Id<Person>, Double> personId2weight = null;

	// -------------------- CONSTRUCTION --------------------

	public Ages(final Set<Id<Person>> populationIds, final GreedoConfigGroup greedoConfig) {
		this.personId2age = populationIds.stream().collect(Collectors.toMap(id -> id, id -> 0));
		this.greedoConfig = greedoConfig;
		this.internalUpdate();
	}

	// -------------------- INTERNALS --------------------

	/* package for testing */ void internalUpdate() {

		this.sortedAges = new ArrayList<>(this.personId2age.values());
		Collections.sort(this.sortedAges);
		this.maxAge = this.sortedAges.get(this.sortedAges.size() - 1);
		this.averageAge = this.sortedAges.stream().mapToDouble(age -> age).average().getAsDouble();

		this.personId2weight = Collections.unmodifiableMap(this.personId2age.entrySet().stream()
				.collect(Collectors.toMap(entry -> entry.getKey(), entry -> this.greedoConfig.getAgeWeight(entry.getValue())))); // ageWeights[entry.getValue()]
		this.averageWeight = this.personId2weight.values().stream().mapToDouble(weight -> weight).average()
				.getAsDouble();

		final int averageAgeFloor = (int) Math.floor(this.averageAge);
		final int averageAgeCeil = (int) Math.ceil(this.averageAge);
		final double ceilWeight = this.averageAge - averageAgeFloor;
		this.weightAtAverageAge = (1.0 - ceilWeight) * this.greedoConfig.getAgeWeight(averageAgeFloor) // ageWeights[averageAgeFloor]
				+ ceilWeight * this.greedoConfig.getAgeWeight(averageAgeCeil); // ageWeights[averageAgeCeil];
	}

	// -------------------- IMPLEMENTATION --------------------

	public void update(final Set<Id<Person>> replanners) { // final double[] ageWeights) {
		this.personId2age = Collections
				.unmodifiableMap(this.personId2age.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey(),
						entry -> replanners.contains(entry.getKey()) ? 0 : entry.getValue() + 1)));
		this.internalUpdate();
	}

	public List<Integer> getSortedAgesView() {
		return Collections.unmodifiableList(this.sortedAges);
	}

	public Map<Id<Person>, Integer> getAges() {
		return this.personId2age;
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

	public double getWeightAtAverageAge() {
		return this.weightAtAverageAge;
	}

	public List<Set<Id<Person>>> stratifyByAgeWithMinumStratumSize(final int minStratumSize) {

		// Create a list of one empty list per possible age.
		final LinkedList<LinkedList<Id<Person>>> allAgeGroups = new LinkedList<>();
		for (int age = 0; age <= this.maxAge; age++) {
			allAgeGroups.add(new LinkedList<>());
		}

		// Put all persons into their age-corresponding list.
		for (Map.Entry<Id<Person>, Integer> entry : this.personId2age.entrySet()) {
			allAgeGroups.get(entry.getValue()).add(entry.getKey());
		}

		// Shuffle all lists.
		for (List<Id<Person>> ageGroup : allAgeGroups) {
			Collections.shuffle(ageGroup);
		}

		// Combine the lists into strata.
		final List<Set<Id<Person>>> strata = new ArrayList<>();
		Set<Id<Person>> currentStratum = null;
		while (allAgeGroups.size() > 0) {

			if (currentStratum == null) {
				// A new stratum. Start by adding the entire next age group.
				currentStratum = new LinkedHashSet<>();
				currentStratum.addAll(allAgeGroups.removeFirst());
			} else if (currentStratum.size() + allAgeGroups.getFirst().size() <= minStratumSize) {
				// An existing stratum; adding the next age group does not exceed
				// minimumStrataSize.
				currentStratum.addAll(allAgeGroups.removeFirst());
			} else {
				// An existing stratum; adding the entire next age group would exceed
				// minimumStrataSize.
				while (currentStratum.size() < minStratumSize) {
					currentStratum.add(allAgeGroups.getFirst().removeFirst());
				}

			}

			if (currentStratum.size() >= minStratumSize) {
				strata.add(currentStratum);
				currentStratum = null;
			}
		}

		// The last stratum may not have been added.
		if (currentStratum != null) {
			if (strata.size() > 0) {
				// Remaining stratum is too small and can be merged.
				strata.get(strata.size() - 1).addAll(currentStratum);
			} else {
				// Remaining stratum is too small but cannot be merged.
				strata.add(currentStratum);
			}
		}

		return strata;

	}

	@Deprecated
	public List<Set<Id<Person>>> stratifyByAgeUniformly(final int numberOfStrata) {

		// Create a list of one empty list per possible age.
		final List<List<Id<Person>>> allAgeGroups = new ArrayList<>(this.maxAge + 1);
		for (int age = 0; age <= this.maxAge; age++) {
			allAgeGroups.add(new ArrayList<>());
		}

		// Put all persons into the corresponding arrays.
		for (Map.Entry<Id<Person>, Integer> entry : this.personId2age.entrySet()) {
			allAgeGroups.get(entry.getValue()).add(entry.getKey());
		}

		// Shuffle the age-specific arrays and concatenate them in increasing age order.
		final LinkedList<Id<Person>> allPersonsInAgeOrder = new LinkedList<>();
		for (List<Id<Person>> ageGroup : allAgeGroups) {
			Collections.shuffle(ageGroup);
			allPersonsInAgeOrder.addAll(ageGroup);
		}

		// Now cut the concatenated array into strata of approximately equal size.
		final List<Set<Id<Person>>> strata = new ArrayList<>(numberOfStrata);
		for (int stratumIndex = 0; stratumIndex < numberOfStrata; stratumIndex++) {
			final int stratumSize = allPersonsInAgeOrder.size() / (numberOfStrata - stratumIndex);
			final Set<Id<Person>> stratum = new LinkedHashSet<>(stratumSize);
			strata.add(stratum);
			for (int personIndex = 0; personIndex < stratumSize; personIndex++) {
				stratum.add(allPersonsInAgeOrder.removeFirst());
			}
		}

		return strata;
	}

}
