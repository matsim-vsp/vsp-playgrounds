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
package org.matsim.contrib.greedo.logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AsymptoticAgeLogger {

	private final File folder;

	private final String prefix;

	private final String postfix;

	private final int dumpInterval;

	private final Map<Id<Person>, Double> personId2deltaUtilitySum = new LinkedHashMap<>();
	private final Map<Id<Person>, Double> personId2similaritySum = new LinkedHashMap<>();
	private final Map<Id<Person>, Double> personId2replanCnt = new LinkedHashMap<>();
	private int numberOfDumps = 0;

	public AsymptoticAgeLogger(final File folder, final String prefix, final String postFix, final int dumpInterval) {
		this.folder = folder;
		this.prefix = prefix;
		this.postfix = postFix;
		this.dumpInterval = dumpInterval;

		if (!folder.exists()) {
			try {
				FileUtils.forceMkdir(folder);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static File fullFileName(final File folder, final String prefix, final int iteration, final String postfix) {
		return FileUtils.getFile(folder, prefix + iteration + postfix);
	}

	private File fullFileName(final int iteration) {
		return fullFileName(this.folder, this.prefix, iteration, this.postfix);
	}

	private static double addOrPut(final Map<Id<Person>, Double> map, final Id<Person> key, final double value) {
		final Double oldValue = map.get(key);
		final double newValue = (oldValue != null) ? (value + oldValue) : value;
		map.put(key, newValue);
		return newValue;
	}

	public void dump(final Map<Id<Person>, Integer> personId2age, final Map<Id<Person>, Double> personId2similarity,
			final Map<Id<Person>, Double> personId2expectedUtilityChange, final Double unconstrainedBeta,
			final int iteration, final Set<Id<Person>> replannerIds) {
		if ((iteration % this.dumpInterval == 0) && (unconstrainedBeta != null)) {
			try {
				this.numberOfDumps++;
				final PrintWriter writer = new PrintWriter(this.fullFileName(iteration));
				writer.print("(DeltaU*beta*)asymptoticAge\t(DeltaU*beta*)realizedAge");
				writer.print("\t");
				writer.print("(<DeltaU>*beta*)<asymptoticAge>\t(<DeltaU>*beta*)<realizedAge>");
				writer.println();
				for (Id<Person> replannerId : replannerIds) {
					final Double similarity = personId2similarity.get(replannerId);
					final Double expectedUtilityChange = personId2expectedUtilityChange.get(replannerId);
					if ((similarity != null) && (expectedUtilityChange != null)) {
						{
							final double preMultipliedAsymptoticAge = 2.0 * personId2age.size() * similarity;
							final double preMultipliedRealAge = expectedUtilityChange * unconstrainedBeta
									* personId2age.get(replannerId);
							writer.print(preMultipliedAsymptoticAge + "\t" + preMultipliedRealAge);
						}
						writer.print("\t");
						{
							final double replanCnt = addOrPut(this.personId2replanCnt, replannerId, 1.0);
							final double avgDeltaUtility = addOrPut(this.personId2deltaUtilitySum, replannerId,
									expectedUtilityChange) / replanCnt;
							final double avgSimilarity = addOrPut(this.personId2similaritySum, replannerId, similarity)
									/ replanCnt;
							final double avgAge = ((double) this.numberOfDumps) / replanCnt;

							final double avgPreMultipliedAsymptoticAge = 2.0 * personId2age.size() * avgSimilarity;
							final double avgPreMultipliedRealAge = avgDeltaUtility * unconstrainedBeta * avgAge;
							writer.print(avgPreMultipliedAsymptoticAge + "\t" + avgPreMultipliedRealAge);
						}
						writer.println();
					}
				}
				writer.flush();
				writer.close();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
