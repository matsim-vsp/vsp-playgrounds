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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import com.google.inject.Inject;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class WireModalShareCalibratorIntoMATSimControlerListener implements AfterMobsimListener {

	private final ModalShareCalibrator calibrator;

	@Inject
	public WireModalShareCalibratorIntoMATSimControlerListener(final Config config) {

		final ModalShareCalibrationConfigGroup modeCalibrConf = ConfigUtils.addOrGetModule(config,
				ModalShareCalibrationConfigGroup.class);
		this.calibrator = new ModalShareCalibrator(modeCalibrConf.getInitialTrustRegion(),
				modeCalibrConf.getIterationExponent());
		for (ConfigGroup paramSet : modeCalibrConf
				.getParameterSets(ModalShareCalibrationConfigGroup.TransportModeDataSet.TYPE)) {
			final ModalShareCalibrationConfigGroup.TransportModeDataSet modeDataSet = (ModalShareCalibrationConfigGroup.TransportModeDataSet) paramSet;
			this.calibrator.setRealShare(modeDataSet.getMode(), modeDataSet.getShare());
		}
	}

	private Map<String, Integer> extractLegModes(final Plan plan) {
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
	public void notifyAfterMobsim(final AfterMobsimEvent event) {
		for (Person person : event.getServices().getScenario().getPopulation().getPersons().values()) {
			this.calibrator.updateSimulatedModeUsage(person.getId(), this.extractLegModes(person.getSelectedPlan()),
					event.getIteration());
		}
		final Map<String, Double> deltaASC = this.calibrator.getDeltaASC(event.getIteration());
		for (String mode : deltaASC.keySet()) {
			final ModeParams modeParams = event.getServices().getConfig().planCalcScore().getScoringParameters(null)
					.getOrCreateModeParams(mode);
			modeParams.setConstant(modeParams.getConstant() + deltaASC.get(mode));
			Logger.getLogger(this.getClass())
					.info("Set ASC for mode " + mode + " to " + modeParams.getConstant() + ".");
		}
	}
}
