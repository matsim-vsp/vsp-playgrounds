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
package opdytsintegration.example.roadpricing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class OrderedValuesRandomizer {

	// -------------------- INTERFACE DEFINITION --------------------

	public static interface ValueRandomizer {

		public double newUnconstrained(double oldValue);

		public double constrain(double originalValue);
	}

	// -------------------- MEMBERS --------------------

	private final ValueRandomizer valueRandomizer;

	// -------------------- CONSTRUCTION --------------------

	public OrderedValuesRandomizer(final ValueRandomizer valueRandomizer) {
		this.valueRandomizer = valueRandomizer;
	}

	// -------------------- IMPLEMENTATION --------------------

	public List<List<Double>> newRandomizedPair(final List<Double> values) {
		List<Double> result1 = null;
		List<Double> result2 = null;
		// do {
		result1 = new ArrayList<>(values.size());
		result2 = new ArrayList<>(values.size());
		for (double value : values) {
			final double delta = this.valueRandomizer.newUnconstrained(value)
					- value;
			result1.add(this.valueRandomizer.constrain(value + delta));
			result2.add(this.valueRandomizer.constrain(value - delta));
		}
		// } while ((result1.size() != values.size())
		// || (result2.size() != values.size()));

		Collections.sort(result1);
		Collections.sort(result2);

		final List<List<Double>> resultPair = new ArrayList<>(2);
		resultPair.add(result1);
		resultPair.add(result2);
		return resultPair;
	}
}
