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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AgestTest {

	@Test
	public void test() {

		boolean verbose = false;

		Id<Person> tom = Id.createPersonId("Tom");
		Id<Person> ben = Id.createPersonId("Ben");
		Id<Person> joe = Id.createPersonId("Joe");

		Set<Id<Person>> populationIds = new LinkedHashSet<>();
		populationIds.addAll(Arrays.asList(tom, ben, joe));

		Ages ages = new Ages(populationIds);
		if (verbose) {
			System.out.println(ages.personId2age + ", max=" + ages.maxAge);
		}
		final Map<Id<Person>, Integer> expectedAges = new LinkedHashMap<>();
		expectedAges.put(tom, 0);
		expectedAges.put(ben, 0);
		expectedAges.put(joe, 0);
		Assert.assertEquals(expectedAges, ages.personId2age);
		Assert.assertEquals(0, ages.maxAge);
		final Map<Id<Person>, Double> expectedWeights = new LinkedHashMap<>();
		expectedWeights.put(tom, 1.0);
		expectedWeights.put(ben, 1.0);
		expectedWeights.put(joe, 1.0);
		Assert.assertEquals(expectedWeights, ages.getPersonWeights());

		// max age 1 after update
		ages.update(new LinkedHashSet<>(), new double[] { 1.0, 0.5 });
		if (verbose) {
			System.out.println(ages.personId2age + ", max=" + ages.maxAge);
		}
		expectedAges.put(tom, 1);
		expectedAges.put(ben, 1);
		expectedAges.put(joe, 1);
		Assert.assertEquals(expectedAges, ages.personId2age);
		Assert.assertEquals(1, ages.maxAge);
		expectedWeights.put(tom, 0.5);
		expectedWeights.put(ben, 0.5);
		expectedWeights.put(joe, 0.5);
		Assert.assertEquals(expectedWeights, ages.getPersonWeights());

		// max age 2 after update
		ages.update(new LinkedHashSet<>(Arrays.asList(tom, ben)), new double[] { 1.0, 0.5, 0.25 });
		if (verbose) {
			System.out.println(ages.personId2age + ", max=" + ages.maxAge);
		}
		expectedAges.put(tom, 0);
		expectedAges.put(ben, 0);
		expectedAges.put(joe, 2);
		Assert.assertEquals(expectedAges, ages.personId2age);
		Assert.assertEquals(2, ages.maxAge);
		expectedWeights.put(tom, 1.0);
		expectedWeights.put(ben, 1.0);
		expectedWeights.put(joe, 0.25);
		Assert.assertEquals(expectedWeights, ages.getPersonWeights());

		// max age 1 after update
		ages.update(new LinkedHashSet<>(Arrays.asList(joe)), new double[] { 1.0, 0.5 });
		if (verbose) {
			System.out.println(ages.personId2age + ", max=" + ages.maxAge);
		}
		expectedAges.put(tom, 1);
		expectedAges.put(ben, 1);
		expectedAges.put(joe, 0);
		Assert.assertEquals(expectedAges, ages.personId2age);
		Assert.assertEquals(1, ages.maxAge);
		expectedWeights.put(tom, 0.5);
		expectedWeights.put(ben, 0.5);
		expectedWeights.put(joe, 1.0);
		Assert.assertEquals(expectedWeights, ages.getPersonWeights());

	}

	private static Ages createAges() {
		Ages ages = new Ages(new LinkedHashSet<>(Arrays.asList(Id.createPersonId("dummy"))));
		ages.personId2age.clear(); // get rid of dummy

		ages.personId2age.put(Id.createPersonId("1age0"), 0);
		ages.personId2age.put(Id.createPersonId("2age0"), 0);
		ages.personId2age.put(Id.createPersonId("3age0"), 0);

		ages.personId2age.put(Id.createPersonId("1age1"), 1);

		ages.personId2age.put(Id.createPersonId("1age2"), 2);
		ages.personId2age.put(Id.createPersonId("2age2"), 2);
		ages.personId2age.put(Id.createPersonId("3age2"), 2);
		ages.personId2age.put(Id.createPersonId("4age2"), 2);

		ages.personId2age.put(Id.createPersonId("1age3"), 3);

		ages.internalUpdate(new double[] { 1e0, 1e-1, 1e-2, 1e-3 });

		return ages;
	}

	@Test
	public void testStratifyByAgeWithMinumStrataSize() {

		final Ages ages = createAges();

		List<Set<Id<Person>>> strata;
		for (int minStratumSize = 1; minStratumSize <= 5; minStratumSize++) {
			strata = ages.stratifyByAgeWithMinumStratumSize(minStratumSize);
			int sumOfStratumSizes = 0;
			Integer prevMaxAge = null;
			for (Set<Id<Person>> stratum : strata) {
				sumOfStratumSizes += stratum.size();
				Assert.assertTrue((stratum.size() >= minStratumSize) || (ages.personId2age.size() < minStratumSize));
				int minAge = Integer.MAX_VALUE;
				int maxAge = Integer.MIN_VALUE;
				for (Id<Person> personId : stratum) {
					minAge = Math.min(minAge, ages.personId2age.get(personId));
					maxAge = Math.max(maxAge, ages.personId2age.get(personId));
				}
				Assert.assertTrue((prevMaxAge == null) || (prevMaxAge <= minAge));
				prevMaxAge = maxAge;
			}
			Assert.assertEquals(ages.personId2age.size(), sumOfStratumSizes);
		}
	}

	@Test
	@Deprecated
	public void testStratifyByAgeUniformly() {

		final Ages ages = createAges();

		final List<Set<Id<Person>>> strata = ages.stratifyByAgeUniformly(2);
		int maxAgeInFirstStratum = Integer.MIN_VALUE;
		for (Id<Person> personId : strata.get(0)) {
			maxAgeInFirstStratum = Math.max(maxAgeInFirstStratum, ages.personId2age.get(personId));
		}
		int minAgeInSecondStratum = Integer.MAX_VALUE;
		for (Id<Person> personId : strata.get(1)) {
			minAgeInSecondStratum = Math.min(minAgeInSecondStratum, ages.personId2age.get(personId));
		}
		Assert.assertTrue(maxAgeInFirstStratum <= minAgeInSecondStratum);
		Assert.assertTrue(Math.abs(strata.get(0).size() - strata.get(1).size()) <= 1);
	}
}
