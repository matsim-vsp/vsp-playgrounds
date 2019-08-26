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

	private final double inertia;

	// -------------------- MEMBERS --------------------

	private final Map<String, Double> mode2realShare = new LinkedHashMap<>();

	private final Map<Id<Person>, Map<String, Double>> personId2mode2share = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public ModalShareCalibrator(final double inertia) {
		this.inertia = inertia;
	}

	// -------------------- INTERNALS --------------------

	private Set<String> allModes() {
		return Collections.unmodifiableSet(this.mode2realShare.keySet());
	}

	// private Map<String, Double> getMode2usage(final Id<Person> personId) {
	// Map<String, Double> result = this.personId2mode2usage.get(personId);
	// if (result == null) {
	// result = new LinkedHashMap<String, Double>();
	// // for (String mode : this.allModes()) {
	// // result.put(mode, 0.0);
	// // }
	// this.personId2mode2usage.put(personId, result);
	// }
	// return result;
	// }

	// -------------------- IMPLEMENTATION --------------------

	public void addRealData(final String mode, final double share) {
		this.mode2realShare.put(mode, share);
	}

	public void updateModeUsage(final Id<Person> personId, final String lastUsedMode) {
		Map<String, Double> mode2usage = this.personId2mode2share.get(personId);
		if (mode2usage == null) {
			mode2usage = new LinkedHashMap<>();
			for (String mode : this.allModes()) {
				mode2usage.put(mode, 0.0);
			}
			mode2usage.put(lastUsedMode, 1.0);
			this.personId2mode2share.put(personId, mode2usage);
		} else {
			double sum = 0.0;
			for (String mode : this.allModes()) {
				final double value = this.inertia * mode2usage.get(mode)
						+ (mode.equals(lastUsedMode) ? (1.0 - this.inertia) * 1.0 : 0.0);
				mode2usage.put(mode, value);
				sum += value;
			}
			for (String mode : this.allModes()) {
				mode2usage.put(mode, mode2usage.get(mode) / sum);
			}
		}
	}

	public Map<String, Double> getSimulatedModalShares() {

		final Map<String, Double> result = new LinkedHashMap<>();
		for (String mode : this.allModes()) {
			result.put(mode, 0.0);
		}

		for (Map<String, Double> mode2usage : this.personId2mode2share.values()) {
			for (Map.Entry<String, Double> entry : mode2usage.entrySet()) {
				result.put(entry.getKey(), result.get(entry.getKey()) + entry.getValue());
			}
		}

		final double _N = this.personId2mode2share.keySet().size();
		for (Map.Entry<String, Double> entry : result.entrySet()) {
			entry.setValue(entry.getValue() / _N);
		}

		return result;
	}

	public Map<Tuple<String, String>, Double> get_dSimulatedShares_dASCs(final Map<String, Double> simulatedShares) {

		final double _N = this.personId2mode2share.size();
		final Map<Tuple<String, String>, Double> dSimulatedShares_dASCs = new LinkedHashMap<>();

		for (String shareMode : this.allModes()) {
			for (String ascMode : this.allModes()) {
				dSimulatedShares_dASCs.put(new Tuple<>(shareMode, ascMode),
						(shareMode.equals(ascMode) ? simulatedShares.get(shareMode) : 0.0));
			}
		}

		for (Map.Entry<Id<Person>, Map<String, Double>> person2mode2usageEntry : this.personId2mode2share.entrySet()) {
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
		result *= this.personId2mode2share.size() / 2.0;
		return result;
	}

	public Map<String, Double> get_dQ_dASCs(final Map<String, Double> simulatedShares,
			final Map<Tuple<String, String>, Double> dSimulatedShares_dASCs) {

		final Map<String, Double> dQ_dASC = new LinkedHashMap<>();
		for (String mode : this.allModes()) {
			dQ_dASC.put(mode, 0.0);
		}

		final double _N = this.personId2mode2share.size();
		for (String comparedMode : this.allModes()) {
			final double realShare = this.mode2realShare.get(comparedMode);
			final double fact = _N * (simulatedShares.get(comparedMode) - realShare) / realShare / (1.0 - realShare);
			for (String ascMode : this.allModes()) {
				dQ_dASC.put(ascMode,
						dQ_dASC.get(ascMode) + fact * dSimulatedShares_dASCs.get(new Tuple<>(comparedMode, ascMode)));
			}
		}

		return dQ_dASC;
	}
}
