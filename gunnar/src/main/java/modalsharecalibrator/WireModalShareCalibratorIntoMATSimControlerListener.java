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

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.utils.collections.Tuple;

import com.google.inject.Inject;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class WireModalShareCalibratorIntoMATSimControlerListener implements BeforeMobsimListener {

	private final Config config;

	private final ModalShareCalibrator calibrator;

	@Inject
	public WireModalShareCalibratorIntoMATSimControlerListener(final Config config) {
		this.config = config;
		// TODO Get all of this from config; include concrete measurements.
		final double initialTrustRegion = 1.0;
		final double iterationExponent = 0.5;
		this.calibrator = new ModalShareCalibrator(initialTrustRegion, iterationExponent);
	}

	public Map<String, Integer> extractLegModes(final Plan plan) {
		final Map<String, Integer> mode2count = new LinkedHashMap<>();
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				final String mode = ((Leg) pe).getMode();
				mode2count.put(mode, 1 + mode2count.getOrDefault(mode, 0));
			}
		}
		return mode2count;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		
		for (Person person : event.getServices().getScenario().getPopulation().getPersons().values()) {
			this.calibrator.updateModeUsage(person.getId(), this.extractLegModes(person.getSelectedPlan()),
					event.getIteration());
		}

		final Map<String, Double> simulatedShares = this.calibrator.getSimulatedShares();
		final Map<Tuple<String, String>, Double> dSimulatedShares_dASCs = this.calibrator
				.get_dSimulatedShares_dASCs(simulatedShares);
		final Map<String, Double> dQ_dASC = this.calibrator.get_dQ_dASCs(simulatedShares, dSimulatedShares_dASCs);
		final Map<String, Double> deltaASC = this.calibrator.getDeltaASC(dQ_dASC, event.getIteration());

		for (String mode : deltaASC.keySet()) {
			final ModeParams modeParams = this.config.planCalcScore().getScoringParameters(null)
					.getOrCreateModeParams(mode);
			modeParams.setConstant(modeParams.getConstant() + deltaASC.get(mode));
		}
	}
}
