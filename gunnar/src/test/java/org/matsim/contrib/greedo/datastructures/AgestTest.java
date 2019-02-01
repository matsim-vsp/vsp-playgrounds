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
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.greedo.datastructures.Ages;

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
}
