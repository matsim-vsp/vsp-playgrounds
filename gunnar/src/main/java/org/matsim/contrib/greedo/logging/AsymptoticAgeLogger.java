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
import java.util.Map;

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

	public void dump(final Map<Id<Person>, Integer> personId2age, final Map<Id<Person>, Double> personId2similarity,
			final Map<Id<Person>, Double> personId2expectedUtilityChange, final Double unconstrainedBeta,
			final int iteration) {
		if (iteration % this.dumpInterval == 0) {
			try {
				final PrintWriter writer = new PrintWriter(this.fullFileName(iteration));
				writer.println("asymptoticAge\trealizedAge");
				for (Map.Entry<Id<Person>, Integer> entry : personId2age.entrySet()) {
					final Id<Person> personId = entry.getKey();
					final Double similarity = personId2similarity.get(personId);
					final Double expectedUtilityChange = personId2expectedUtilityChange.get(personId);
					if (similarity != null && expectedUtilityChange != null && unconstrainedBeta != null) {
						final double asymptoticAge = (similarity / expectedUtilityChange)
								* (2.0 * personId2age.size() / unconstrainedBeta);
						if (asymptoticAge >= -1e6 && asymptoticAge <= 1e6) {
							writer.println(asymptoticAge + "\t" + entry.getValue());
						}
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
