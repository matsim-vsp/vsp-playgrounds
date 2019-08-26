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

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;

import modalsharecalibrator.ModalShareCalibrator.ImprovementStep;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class ModalShareCalibratorTest {

	final Random rnd = new Random(4711);

	private int indexOfMax(final double[] utils, final Double[] ascs) {
		double[] randomUtils = new double[utils.length];
		for (int i = 0; i < randomUtils.length; i++) {
			randomUtils[i] = utils[i] + ascs[i] + this.rnd.nextGaussian();
		}

		int result = 0;
		for (int i = 1; i < utils.length; i++) {
			if (randomUtils[i] > randomUtils[result]) {
				result = i;
			}
		}
		return result;
	}

	@Test
	public void test() {

		System.out.println("STARTED ...");

		final String[] modes = new String[] { "car", "pt", "walk", "bike" };
		final int _N = 1000;
		final double[][] utils = new double[_N][];
		for (int n = 0; n < _N; n++) {
			utils[n] = new double[] { rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble() };
		}
		final Double[] ascs = new Double[] { 0.0, 0.0, 0.0, 0.0 };

		final double inertia = 0.5;
		double stepSize = 0.01;
		final ModalShareCalibrator calibrator = new ModalShareCalibrator(inertia);

		calibrator.addRealData("car", 0.5);
		calibrator.addRealData("pt", 0.3);
		calibrator.addRealData("walk", 0.1);
		calibrator.addRealData("bike", 0.1);

		Map<String, Double> oldSimulatedShares = null;
		final int _R = 200;
		for (int r = 0; r < _R; r++) {
			for (int n = 0; n < _N; n++) {
				final String choice = modes[this.indexOfMax(utils[n], ascs)];
				calibrator.updateModeUsage(Id.createPersonId(n), choice);
			}

			final Map<String, Double> simulatedShares = calibrator.getSimulatedShares();
			final Map<Tuple<String, String>, Double> dSimulatedShares_dASCs = calibrator
					.get_dSimulatedShares_dASCs(simulatedShares);
			final Map<String, Double> dQ_dASC = calibrator.get_dQ_dASCs(simulatedShares, dSimulatedShares_dASCs);

			final ImprovementStep imprStep = calibrator.getImprovementStep(stepSize, oldSimulatedShares, simulatedShares,
					dSimulatedShares_dASCs, dQ_dASC);
			stepSize = Math.max(0.5 * stepSize, Math.min(2.0 * stepSize, stepSize * imprStep.stepSizeFactor));
			ascs[0] += imprStep.deltaASC.get("car");
			ascs[1] += imprStep.deltaASC.get("pt");
			ascs[2] += imprStep.deltaASC.get("walk");
			ascs[3] += imprStep.deltaASC.get("bike");

			System.out.println(r + "\t" + calibrator.getObjectiveFunctionValue(simulatedShares) + "\t" + simulatedShares
					+ "\t" + Arrays.asList(ascs) + "\t" + stepSize);
			// System.out.println(dSimulatedShares_dASCs);
			
			oldSimulatedShares = simulatedShares;
		}

		System.out.println("... DONE");

	}

}
