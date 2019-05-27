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
import org.matsim.contrib.greedo.LogDataWrapper;

import floetteroed.utilities.math.Covariance;
import floetteroed.utilities.math.Matrix;
import floetteroed.utilities.math.Vector;
import floetteroed.utilities.statisticslogging.Statistic;

/**
 * 
 * Validates the assumption of
 * 
 * replanningRate ~ expectedUtilityChange / similarity
 * 
 * given a 1 / age weighting. Translates the above into the equivalent statement
 * 
 * ageAtReplanning ~ similarity / expectedUtilityChange
 * 
 * Computes the correlation coefficient (across the population in a given
 * iteration) between averageAgeAtReplanning (per individual over iterations)
 * and averageSimilarity / averageExpectedUtilityChange (both per individual
 * over iterations).
 *
 * @author Gunnar Flötteröd
 *
 */
public class AsymptoticAgeLogger {

	// -------------------- INNER CLASS --------------------

	private class Entry {

		private double lastExpectedUtilityChange = 0.0;
		private double lastSimilarity = 0.0;
		private int lastAge = 0;

		private double expectedUtilityChangeSum = 0.0;
		private double similaritySum = 0.0;
		private int ageSum = 0;

		private int updateCnt = 0;

		Entry() {
		}

		void update(final double expectedUtilityChange, final double similarity, final int age) {
			this.lastExpectedUtilityChange = expectedUtilityChange;
			this.lastSimilarity = similarity;
			this.lastAge = age;
			this.expectedUtilityChangeSum += expectedUtilityChange;
			this.similaritySum += similarity;
			this.ageSum += age;
			this.updateCnt++;
		}

		double getLastExpectedUtilityChange() {
			return this.lastExpectedUtilityChange;
		}

		double getLastSimilarity() {
			return this.lastSimilarity;
		}

		int getLastAge() {
			return this.lastAge;
		}

		double getAvgExpectedUtilityChange() {
			return (this.expectedUtilityChangeSum / this.updateCnt);
		}

		double getAvgSimilarity() {
			return (this.similaritySum / this.updateCnt);
		}

		double getAvgAge() {
			return ((double) this.ageSum) / this.updateCnt;
		}
	}

	// -------------------- CONSTANTS --------------------

	private final File folder;

	private final String prefix;

	private final String postfix;

	// -------------------- MEMBERS --------------------

	private final Map<Id<Person>, Entry> personId2entry = new LinkedHashMap<>();

	private Double ageVsSimilarityByExpDeltaUtilityCorrelation = null;

	private Double avgAgeVsAvgSimilarityByAvgExpDeltaUtilityCorrelation = null;

	private Double ageTimesExpDeltaUtilityVsSimilarityCorrelation = null;

	private Double avgAgeTimesAvgExpDeltaUtilityVsAvgSimilarityCorrelation = null;

	// -------------------- CONSTRUCTION --------------------

	public AsymptoticAgeLogger(final File folder, final String prefix, final String postFix) {
		this.folder = folder;
		this.prefix = prefix;
		this.postfix = postFix;

		if (!folder.exists()) {
			try {
				FileUtils.forceMkdir(folder);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	// -------------------- INTERNALS / HELPERS --------------------

	public static File fullFileName(final File folder, final String prefix, final int iteration, final String postfix) {
		return FileUtils.getFile(folder, prefix + iteration + postfix);
	}

	private File fullFileName(final int iteration) {
		return fullFileName(this.folder, this.prefix, iteration, this.postfix);
	}

	private Entry getOrCreateEntry(final Id<Person> personId) {
		Entry entry = this.personId2entry.get(personId);
		if (entry == null) {
			entry = new Entry();
			this.personId2entry.put(personId, entry);
		}
		return entry;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void dump(final Map<Id<Person>, Integer> personId2ageAtReplanning,
			final Map<Id<Person>, Double> personId2expectedUtilityChange,
			final Map<Id<Person>, Double> personId2similarity, final Set<Id<Person>> replannerIds,
			final int iteration) {
		try {

			final PrintWriter writer = new PrintWriter(this.fullFileName(iteration));

			writer.print("ageAtReplanning\tsimilarity/expDeltaUtility");
			writer.print("\t");
			writer.print("<ageAtReplanning>\t<similarity>/<expDeltaUtility>");
			writer.print("\t");
			writer.print("ageAtReplanning*expDeltaUtility\tsimilarity");
			writer.print("\t");
			writer.print("<ageAtReplanning>*<expDeltaUtility>\t<similarity>");
			writer.println();

			final Covariance ageVsSimilarityByUtilityCovariance = new Covariance(2, 2);
			final Covariance avgAgeVsAvgSimilarityByAvgUtilityCovariance = new Covariance(2, 2);
			final Covariance ageTimesUtilityVsSimilarityCovariance = new Covariance(2, 2);
			final Covariance avgAgeTimesAvgUtilityVsAvgSimilarityCovariance = new Covariance(2, 2);

			for (Id<Person> replannerId : replannerIds) {

				final Double expectedUtilityChange = personId2expectedUtilityChange.get(replannerId);
				final Double similarity = personId2similarity.get(replannerId);
				final Integer age = personId2ageAtReplanning.get(replannerId);

				if ((similarity != null) && (expectedUtilityChange != null) && (age != null)) {

					final Entry entry = this.getOrCreateEntry(replannerId);
					entry.update(expectedUtilityChange, similarity, age);

					{
						final double similarityByUtility = entry.getLastSimilarity()
								/ entry.getLastExpectedUtilityChange();
						writer.print(entry.getLastAge() + "\t" + similarityByUtility);
						final Vector x = new Vector(entry.getLastAge(), similarityByUtility);
						ageVsSimilarityByUtilityCovariance.add(x, x);
					}
					writer.print("\t");
					{
						final double avgSimilarityByAvgUtility = entry.getAvgSimilarity()
								/ entry.getAvgExpectedUtilityChange();
						writer.print(entry.getAvgAge() + "\t" + avgSimilarityByAvgUtility);
						final Vector x = new Vector(entry.getAvgAge(), avgSimilarityByAvgUtility);
						avgAgeVsAvgSimilarityByAvgUtilityCovariance.add(x, x);
					}
					writer.print("\t");
					{
						final double ageTimesDeltaUtility = entry.getLastAge() * entry.getLastExpectedUtilityChange();
						writer.print(ageTimesDeltaUtility + "\t" + entry.getLastSimilarity());
						final Vector x = new Vector(ageTimesDeltaUtility, entry.getLastSimilarity());
						ageTimesUtilityVsSimilarityCovariance.add(x, x);
					}
					writer.print("\t");
					{
						final double avgAgeTimesAvgDeltaUtility = entry.getAvgAge()
								* entry.getAvgExpectedUtilityChange();
						writer.print(avgAgeTimesAvgDeltaUtility + "\t" + entry.getAvgSimilarity());
						final Vector x = new Vector(avgAgeTimesAvgDeltaUtility, entry.getAvgSimilarity());
						avgAgeTimesAvgUtilityVsAvgSimilarityCovariance.add(x, x);
					}
					writer.println();
				}
			}
			writer.flush();
			writer.close();

			{
				final Matrix _C = ageVsSimilarityByUtilityCovariance.getCovariance();
				this.ageVsSimilarityByExpDeltaUtilityCorrelation = _C.get(1, 0)
						/ Math.sqrt(_C.get(0, 0) * _C.get(1, 1));
			}
			{
				final Matrix _C = avgAgeVsAvgSimilarityByAvgUtilityCovariance.getCovariance();
				this.avgAgeVsAvgSimilarityByAvgExpDeltaUtilityCorrelation = _C.get(1, 0)
						/ Math.sqrt(_C.get(0, 0) * _C.get(1, 1));
			}
			{
				final Matrix _C = ageTimesUtilityVsSimilarityCovariance.getCovariance();
				this.ageTimesExpDeltaUtilityVsSimilarityCorrelation = _C.get(1, 0)
						/ Math.sqrt(_C.get(0, 0) * _C.get(1, 1));
			}
			{
				final Matrix _C = avgAgeTimesAvgUtilityVsAvgSimilarityCovariance.getCovariance();
				this.avgAgeTimesAvgExpDeltaUtilityVsAvgSimilarityCorrelation = _C.get(1, 0)
						/ Math.sqrt(_C.get(0, 0) * _C.get(1, 1));
			}

		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	// -------------------- Statistic FACTORY --------------------

	public Statistic<LogDataWrapper> newAgeVsSimilarityByExpDeltaUtilityCorrelationStatistic() {
		return new Statistic<LogDataWrapper>() {

			@Override
			public String label() {
				return "Corr(Age;Similarity/ExpDeltaUtility)";
			}

			@Override
			public String value(LogDataWrapper arg0) {
				return Statistic.toString(ageVsSimilarityByExpDeltaUtilityCorrelation);
			}
		};
	}

	public Statistic<LogDataWrapper> newAvgAgeVsAvgSimilarityByAvgExpDeltaUtilityCorrelationStatistic() {
		return new Statistic<LogDataWrapper>() {

			@Override
			public String label() {
				return "Corr(<Age>;<Similarity>/<ExpDeltaUtility>)";
			}

			@Override
			public String value(LogDataWrapper arg0) {
				return Statistic.toString(avgAgeVsAvgSimilarityByAvgExpDeltaUtilityCorrelation);
			}
		};
	}

	public Statistic<LogDataWrapper> newAgeTimesExpDeltaUtilityVsSimilarityCorrelationStatistic() {
		return new Statistic<LogDataWrapper>() {

			@Override
			public String label() {
				return "Corr(Age*ExpDeltaUtility;Similarity)";
			}

			@Override
			public String value(LogDataWrapper arg0) {
				return Statistic.toString(ageTimesExpDeltaUtilityVsSimilarityCorrelation);
			}
		};
	}

	public Statistic<LogDataWrapper> newAvgAgeTimesAvgExpDeltaUtilityVsAvgSimilarityCorrelationStatistic() {
		return new Statistic<LogDataWrapper>() {

			@Override
			public String label() {
				return "Corr(<Age>*<ExpDeltaUtility>;<Similarity>)";
			}

			@Override
			public String value(LogDataWrapper arg0) {
				return Statistic.toString(avgAgeTimesAvgExpDeltaUtilityVsAvgSimilarityCorrelation);
			}
		};
	}
}
