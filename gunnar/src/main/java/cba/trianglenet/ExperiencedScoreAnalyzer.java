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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class ExperiencedScoreAnalyzer {

	private final Map<Id<Person>, List<Double>> personId2scoreList = new LinkedHashMap<>();

	ExperiencedScoreAnalyzer() {
	}

	void add(final Id<Person> personId, final double score) {
		List<Double> list = this.personId2scoreList.get(personId);
		if (list == null) {
			list = new ArrayList<>();
			this.personId2scoreList.put(personId, list);
		}
		list.add(score);
	}

	private double avg2ndHalf(final List<Double> list) {
		// final int startIndex = list.size() / 2;
		final int startIndex = list.size() - 1;
		double sum = 0;
		for (int i = startIndex; i < list.size(); i++) {
			sum += list.get(i);
		}
		return (sum / (list.size() - startIndex));
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		for (Map.Entry<Id<Person>, List<Double>> entry : this.personId2scoreList.entrySet()) {
			result.append(entry.getKey());
			result.append("\t");
			result.append(this.avg2ndHalf(entry.getValue()));
			result.append("\t");
			result.append(entry.getValue());
			result.append("\n");
		}
		return result.toString();
	}
}
