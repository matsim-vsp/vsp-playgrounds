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
package modalsharecalibrator.cadyts;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;

import com.google.inject.Inject;

import floetteroed.utilities.DynamicData;
import modalsharecalibrator.CalibrationModeExtractor;
import modalsharecalibrator.ModalShareCalibrationConfigGroup;
import modalsharecalibrator.ModalShareCalibrationConfigGroup.TransportModeDataSet;
import modalsharecalibrator.ModeASCContainer;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class WireCadytsModalShareCalibratorIntoMATSimControlerListener
		implements BeforeMobsimListener, AfterMobsimListener {

	// -------------------- CONSTANTS --------------------

	private final CadytsModeShareCalibrator calibrator;

	private final ModeASCContainer modeASCContainer;

	private final CalibrationModeExtractor modeExtractor;

	// -------------------- CONSTRUCTION --------------------

	@Inject
	public WireCadytsModalShareCalibratorIntoMATSimControlerListener(final Config config,
			final ModeASCContainer modeASCContainer, final CalibrationModeExtractor modeExtractor,
			final Population population) {
		final ModalShareCalibrationConfigGroup calibrConf = ConfigUtils.addOrGetModule(config,
				ModalShareCalibrationConfigGroup.class);
		this.calibrator = new CadytsModeShareCalibrator(config.controler().getOutputDirectory(),
				3 * population.getPersons().size(), calibrConf.getReproductionWeight(), calibrConf.getInertia());
		for (ConfigGroup paramSet : calibrConf.getParameterSets(TransportModeDataSet.TYPE)) {
			final TransportModeDataSet modeDataSet = (TransportModeDataSet) paramSet;
			this.calibrator.addModeMeasurement(modeDataSet.getMode(), modeDataSet.getShare());
			modeASCContainer.setASC(modeDataSet.getMode(), modeDataSet.getInitialASC());
		}
		this.modeASCContainer = modeASCContainer;
		this.modeExtractor = modeExtractor;
	}

	// --------------- IMPLEMENTATION OF BeforeMobsimListener ---------------

	@Override
	public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
		for (Person person : event.getServices().getScenario().getPopulation().getPersons().values()) {
			this.calibrator.addToDemand(this.modeExtractor.extractTripModes(person.getSelectedPlan()));
		}
	}

	// --------------- IMPLEMENTATION OF AfterMobsimListener ---------------

	@Override
	public void notifyAfterMobsim(final AfterMobsimEvent event) {
		this.calibrator.afterNetworkLoading(event.getIteration());
		final DynamicData<String> modeCostOffsets = this.calibrator.getLinkCostOffsets();
		if (event.getIteration() >= 5) { // TODO hardcoding ...
			for (String mode : modeCostOffsets.keySet()) {
				// Multiplied by two because one Sampers tour consists of two trips with
				// identical modes.
				this.modeASCContainer.setASC(mode, 2.0 * modeCostOffsets.getBinValue(mode, 0));
			}
		}
		Logger.getLogger(this.getClass()).info("---------- UPDATED ASCs ----------");
		Logger.getLogger(this.getClass()).info("mode\tvalue");
		for (String mode : modeCostOffsets.keySet()) {
			Logger.getLogger(this.getClass()).info(mode + "\t" + this.modeASCContainer.getASC(mode));
		}
		Logger.getLogger(this.getClass()).info("--------------------------");
	}
}
