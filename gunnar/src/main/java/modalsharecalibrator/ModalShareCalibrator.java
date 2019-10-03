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
package modalsharecalibrator;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class ModalShareCalibrator {

	// -------------------- CONSTANTS --------------------

	private final double initialTrustRegion;

	private final double iterationExponent;

	// -------------------- MEMBERS --------------------

	private final Map<String, Double> mode2realShare = new LinkedHashMap<>();

	private final Map<Id<Person>, Map<String, Double>> personId2mode2proba = new LinkedHashMap<>();

	private final Map<Id<Person>, Map<String, Double>> personId2mode2expCnt = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public ModalShareCalibrator(final double initialTrustRegion, final double iterationExponent) {
		this.initialTrustRegion = initialTrustRegion;
		this.iterationExponent = iterationExponent;
	}

	// -------------------- IMPLEMENTATION --------------------

	public double stepSizeFactor(final int iteration) {
		return Math.pow(1.0 / (1.0 + iteration), this.iterationExponent);
	}

	public Set<Id<Person>> allPersonIdView() {
		return Collections.unmodifiableSet(this.personId2mode2proba.keySet());
	}

	public Set<String> allModesView() {
		return Collections.unmodifiableSet(this.mode2realShare.keySet());
	}

	public void setRealShare(final String mode, final double share) {
		this.mode2realShare.put(mode, share);
	}

	public void updateSimulatedModeUsage(final Id<Person> personId, final Map<String, Integer> mode2count,
			final int iteration) {

		final boolean newEntry = !this.personId2mode2proba.containsKey(personId);
		final double innoWeight = (newEntry ? 1.0 : this.stepSizeFactor(iteration));
		final int tripCnt = mode2count.values().stream().mapToInt(c -> c).sum();

		// Make sure that there are entries for the given person.
		final Map<String, Double> mode2proba = this.personId2mode2proba.computeIfAbsent(personId,
				id -> this.mode2realShare.keySet().stream().collect(Collectors.toMap(mode -> mode, mode -> 0.0)));
		final Map<String, Double> mode2expCnt = this.personId2mode2expCnt.computeIfAbsent(personId,
				id -> this.mode2realShare.keySet().stream().collect(Collectors.toMap(mode -> mode, mode -> 0.0)));

		// Updating of (existing but possibly zero) entries.
		mode2proba.entrySet().forEach(e -> e.setValue(
				innoWeight * mode2count.getOrDefault(e.getKey(), 0) / tripCnt + (1.0 - innoWeight) * e.getValue()));
		mode2expCnt.entrySet().forEach(e -> e
				.setValue(innoWeight * mode2count.getOrDefault(e.getKey(), 0) + (1.0 - innoWeight) * e.getValue()));
	}

	/* package for testing */ void updateSimulatedModeUsage(final Id<Person> personId, final String mode,
			final int iteration) {
		final Map<String, Integer> mode2count = new LinkedHashMap<>();
		mode2count.put(mode, 1);
		this.updateSimulatedModeUsage(personId, mode2count, iteration);
	}

	public Map<String, Double> getMode2simulatedCounts() {
		final Map<String, Double> result = new LinkedHashMap<>();
		this.personId2mode2expCnt.values().stream().forEach(mode2expCnt -> mode2expCnt.entrySet().stream().forEach(
				entry -> result.put(entry.getKey(), result.getOrDefault(entry.getKey(), 0.0) + entry.getValue())));
		return result;
	}

	public Map<String, Double> getMode2simulatedShares(final Map<String, Double> mode2simulatedCounts) {
		final double sum = mode2simulatedCounts.values().stream().mapToDouble(c -> c).sum();
		final Map<String, Double> result = new LinkedHashMap<>();
		mode2simulatedCounts.entrySet().forEach(e -> result.put(e.getKey(), e.getValue() / sum));
		return result;
	}

	public Map<Tuple<String, String>, Double> get_dSimulatedCounts_dASCs(final Map<String, Double> simulatedCounts) {
		final Map<Tuple<String, String>, Double> dSimulatedCounts_dASCs = new LinkedHashMap<>();
		for (String countMode : this.allModesView()) {
			for (String ascMode : this.allModesView()) {
				dSimulatedCounts_dASCs.put(new Tuple<>(countMode, ascMode),
						(countMode.equals(ascMode) ? simulatedCounts.get(countMode) : 0.0));
			}
		}
		for (Id<Person> personId : this.allPersonIdView()) {
			for (Map.Entry<String, Double> mode2expCnt : this.personId2mode2expCnt.get(personId).entrySet()) {
				for (Map.Entry<String, Double> mode2proba : this.personId2mode2proba.get(personId).entrySet()) {
					final Tuple<String, String> key = new Tuple<>(mode2expCnt.getKey(), mode2proba.getKey());
					dSimulatedCounts_dASCs.put(key,
							dSimulatedCounts_dASCs.get(key) - mode2expCnt.getValue() * mode2proba.getValue());
				}
			}
		}
		return dSimulatedCounts_dASCs;
	}

	public double getObjectiveFunctionValue(final Map<String, Double> simulatedCounts) {
		final double sum = simulatedCounts.values().stream().mapToDouble(c -> c).sum();
		double result = 0.0;
		for (Map.Entry<String, Double> realEntry : this.mode2realShare.entrySet()) {
			result += Math.pow(simulatedCounts.get(realEntry.getKey()) - realEntry.getValue() * sum, 2.0)
					/ realEntry.getValue();
		}
		return (0.5 * result);
	}

	public Map<String, Double> get_dQ_dASCs(final Map<String, Double> simulatedCounts,
			final Map<Tuple<String, String>, Double> dSimulatedCounts_dASCs) {
		final Map<String, Double> dQ_dASC = new LinkedHashMap<>();
		for (String mode : this.allModesView()) {
			dQ_dASC.put(mode, 0.0);
		}
		final double sum = simulatedCounts.values().stream().mapToDouble(c -> c).sum();
		for (String comparedMode : this.allModesView()) {
			final double realCount = this.mode2realShare.get(comparedMode) * sum;
			final double fact = (simulatedCounts.get(comparedMode) - realCount) / realCount;
			for (String ascMode : this.allModesView()) {
				dQ_dASC.put(ascMode,
						dQ_dASC.get(ascMode) + fact * dSimulatedCounts_dASCs.get(new Tuple<>(comparedMode, ascMode)));
			}
		}
		return dQ_dASC;
	}

	public Map<String, Double> getDeltaASC(final Map<String, Double> dQ_dASC, final int iteration) {

		double unconstrainedStepLength = 0.0;
		for (String mode : this.allModesView()) {
			unconstrainedStepLength += Math.pow(dQ_dASC.get(mode), 2.0);
		}
		unconstrainedStepLength = Math.sqrt(unconstrainedStepLength);

		final double maxStepLength = this.initialTrustRegion
				* Math.pow(1.0 / (iteration + 1.0), this.iterationExponent);

		final double eta;
		if (unconstrainedStepLength > maxStepLength) {
			eta = maxStepLength / unconstrainedStepLength;
		} else {
			eta = 1.0;
		}

		final Map<String, Double> deltaASC = new LinkedHashMap<>();
		for (String mode : this.allModesView()) {
			deltaASC.put(mode, -eta * dQ_dASC.get(mode));
		}

		return deltaASC;
	}

	public Map<String, Double> getDeltaASC(final int iteration) {
		final Map<String, Double> simulatedCounts = this.getMode2simulatedCounts();
		final Map<Tuple<String, String>, Double> dSimulatedCounts_dASCs = this
				.get_dSimulatedCounts_dASCs(simulatedCounts);
		final Map<String, Double> dQ_dASC = this.get_dQ_dASCs(simulatedCounts, dSimulatedCounts_dASCs);
		return this.getDeltaASC(dQ_dASC, iteration);
	}
}
