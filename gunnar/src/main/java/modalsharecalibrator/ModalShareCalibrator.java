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

	private final Map<Id<Person>, Map<String, Double>> personId2mode2simulatedShare = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public ModalShareCalibrator(final double initialTrustRegion, final double iterationExponent) {
		this.initialTrustRegion = initialTrustRegion;
		this.iterationExponent = iterationExponent;
	}

	// -------------------- IMPLEMENTATION --------------------

	public double stepSizeFactor(final int iteration) {
		return Math.pow(1.0 / (1.0 + iteration), this.iterationExponent);
	}

	public Set<String> allModesView() {
		return Collections.unmodifiableSet(this.mode2realShare.keySet());
	}

	public void addRealData(final String mode, final double share) {
		this.mode2realShare.put(mode, share);
	}

	public void updateSimulatedModeUsage(final Id<Person> personId, final Map<String, Integer> mode2count,
			final int iteration) {

		// Make sure that there is an entry for the given person.
		Map<String, Double> mode2simulatedUsage = this.personId2mode2simulatedShare.get(personId);
		if (mode2simulatedUsage == null) {
			mode2simulatedUsage = new LinkedHashMap<>();
			for (String mode : this.allModesView()) {
				mode2simulatedUsage.put(mode, 0.0);
			}
			this.personId2mode2simulatedShare.put(personId, mode2simulatedUsage);
		}

		// Update parameters.
		final double innovationWeight = this.stepSizeFactor(iteration);

		// Forgetting of old information.
		for (String mode : this.allModesView()) {
			mode2simulatedUsage.put(mode, (1.0 - innovationWeight) * mode2simulatedUsage.get(mode));
		}

		// Insertion of new modal information.
		for (Map.Entry<String, Integer> entry : mode2count.entrySet()) {
			mode2simulatedUsage.put(entry.getKey(), innovationWeight * entry.getValue());
		}

		// TODO At this point, mode2simulatedUsage contains iteration-filtered *counts*,
		// but not *probabilities*.

	}

	// TODO The problem here is that one person uses more than one mode.
	public void updateSimulatedModeUsage(final Id<Person> personId, final String simulatedMode, final int iteration) {
		Map<String, Double> mode2simulatedUsage = this.personId2mode2simulatedShare.get(personId);
		if (mode2simulatedUsage == null) {
			mode2simulatedUsage = new LinkedHashMap<>();
			for (String mode : this.allModesView()) {
				mode2simulatedUsage.put(mode, 0.0);
			}
			mode2simulatedUsage.put(simulatedMode, 1.0);
			this.personId2mode2simulatedShare.put(personId, mode2simulatedUsage);
		} else {
			double sum = 0.0;
			for (String mode : this.allModesView()) {
				final double inertia = 1.0 - this.stepSizeFactor(iteration);
				final double value = inertia * mode2simulatedUsage.get(mode)
						+ (mode.equals(simulatedMode) ? (1.0 - inertia) * 1.0 : 0.0);
				mode2simulatedUsage.put(mode, value);
				sum += value;
			}
			for (String mode : this.allModesView()) {
				mode2simulatedUsage.put(mode, mode2simulatedUsage.get(mode) / sum);
			}
		}
	}

	public Map<String, Double> getSimulatedShares() {
		final Map<String, Double> result = new LinkedHashMap<>();
		for (String mode : this.allModesView()) {
			result.put(mode, 0.0);
		}
		for (Map<String, Double> mode2usage : this.personId2mode2simulatedShare.values()) {
			for (Map.Entry<String, Double> entry : mode2usage.entrySet()) {
				result.put(entry.getKey(), result.get(entry.getKey()) + entry.getValue());
			}
		}
		final double _N = this.personId2mode2simulatedShare.keySet().size();
		for (Map.Entry<String, Double> entry : result.entrySet()) {
			entry.setValue(entry.getValue() / _N);
		}
		return result;
	}

	public Map<Tuple<String, String>, Double> get_dSimulatedShares_dASCs(final Map<String, Double> simulatedShares) {
		final double _N = this.personId2mode2simulatedShare.size();
		final Map<Tuple<String, String>, Double> dSimulatedShares_dASCs = new LinkedHashMap<>();
		for (String shareMode : this.allModesView()) {
			for (String ascMode : this.allModesView()) {
				dSimulatedShares_dASCs.put(new Tuple<>(shareMode, ascMode),
						(shareMode.equals(ascMode) ? simulatedShares.get(shareMode) : 0.0));
			}
		}
		for (Map.Entry<Id<Person>, Map<String, Double>> person2mode2usageEntry : this.personId2mode2simulatedShare
				.entrySet()) {
			for (Map.Entry<String, Double> mode2usageEntry1 : person2mode2usageEntry.getValue().entrySet()) {
				for (Map.Entry<String, Double> mode2usageEntry2 : person2mode2usageEntry.getValue().entrySet()) {
					final Tuple<String, String> key = new Tuple<>(mode2usageEntry1.getKey(), mode2usageEntry2.getKey());
					dSimulatedShares_dASCs.put(key, dSimulatedShares_dASCs.get(key)
							- (1.0 / _N) * mode2usageEntry1.getValue() * mode2usageEntry2.getValue());
				}
			}
		}
		return dSimulatedShares_dASCs;
	}

	public double getObjectiveFunctionValue(final Map<String, Double> simulatedShares) {
		double result = 0.0;
		for (Map.Entry<String, Double> realEntry : this.mode2realShare.entrySet()) {
			result += Math.pow(simulatedShares.get(realEntry.getKey()) - realEntry.getValue(), 2.0)
					/ realEntry.getValue() / (1.0 - realEntry.getValue());
		}
		result *= this.personId2mode2simulatedShare.size() / 2.0;
		return result;
	}

	public Map<String, Double> get_dQ_dASCs(final Map<String, Double> simulatedShares,
			final Map<Tuple<String, String>, Double> dSimulatedShares_dASCs) {
		final Map<String, Double> dQ_dASC = new LinkedHashMap<>();
		for (String mode : this.allModesView()) {
			dQ_dASC.put(mode, 0.0);
		}
		final double _N = this.personId2mode2simulatedShare.size();
		for (String comparedMode : this.allModesView()) {
			final double realShare = this.mode2realShare.get(comparedMode);
			final double fact = _N * (simulatedShares.get(comparedMode) - realShare) / realShare / (1.0 - realShare);
			for (String ascMode : this.allModesView()) {
				dQ_dASC.put(ascMode,
						dQ_dASC.get(ascMode) + fact * dSimulatedShares_dASCs.get(new Tuple<>(comparedMode, ascMode)));
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
}
