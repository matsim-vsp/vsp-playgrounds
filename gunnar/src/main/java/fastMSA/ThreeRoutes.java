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
package fastMSA;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ThreeRoutes {

	public static void main(String[] args) {

		final double mu = 0.2;
		final double totalDemand = 1000;

		final double[] ttFreeFlow = new double[] { 100, 10, 10 };
		final double[] ttPerVeh = new double[] { 0.1, 1.0, 1.0 };

		final double[] shares = new double[] { 0.6, 0.3, 0.1 };
		final double[] avgTTs = new double[3];

		final int iterations = 100;

		for (int r = 1; r <= iterations; r++) {

			final double[] instTTs = new double[3];
			for (int i = 0; i < 3; i++) {
				instTTs[i] = ttFreeFlow[i] + ttPerVeh[i] * (shares[i] * totalDemand);
				avgTTs[i] = 1.0 / r * instTTs[i] + (1.0 - 1.0 / r) * avgTTs[i];
			}

			double maxRate = Double.NEGATIVE_INFINITY;
			final double replanRates[] = new double[3];
			for (int i = 0; i < 3; i++) {
				replanRates[i] = Math.exp(mu * ((-avgTTs[i]) - (-instTTs[i])));
				maxRate = Math.max(maxRate, replanRates[i]);
			}
			final double allowedMaxRate = 1;
			if (maxRate > allowedMaxRate) {
				for (int i = 0; i < 3; i++) {
					replanRates[i] *= allowedMaxRate / maxRate;
				}
			}

			/////

			System.out.print(shares[0] + "\t" + shares[1] + "\t" + shares[2] + "\t");
			System.out.print(avgTTs[0] + "\t" + avgTTs[1] + "\t" + avgTTs[2] + "\t");
			System.out.print(replanRates[0] + "\t" + replanRates[1] + "\t" + replanRates[2] + "\t");
			System.out.print(instTTs[0] + "\t" + instTTs[1] + "\t" + instTTs[2] + "\n");

			final double[] newProbas = new double[3];
			double denom = 0;
			for (int i = 0; i < 3; i++) {
				final double utl = -avgTTs[i];
				final double maxUtl = Math.max(Math.max(-avgTTs[0], -avgTTs[1]), -avgTTs[2]);
				newProbas[i] = Math.exp(mu * (utl - maxUtl));
				denom += newProbas[i];
			}
			for (int i = 0; i < 3; i++) {
				newProbas[i] /= denom;
			}

			// original
			// for (int i = 0; i < 3; i++) {
			// probas[i] = newProbas[i];
			// }

			// modified
			// final double[] modifiedProbas = new double[3];
			// double modifiedProbasSum = 0;
			// for (int i = 0; i < 3; i++) {
			// modifiedProbas[i] = replanRates[i] * newProbas[i];
			// modifiedProbasSum += modifiedProbas[i];
			// }
			// for (int i = 0; i < 3; i++) {
			// modifiedProbas[i] /= modifiedProbasSum;
			// }

			double num = 0;
			for (int i = 0; i < 3; i++) {
				num += replanRates[i] * shares[i];
			}
//			System.out.println(num);
			for (int i = 0; i < 3; i++) {
				shares[i] = (1.0 - replanRates[i]) * shares[i] + newProbas[i] * num;
				// shares[i] = (1.0 - replanRates[i]) * shares[i] +
				// modifiedProbas[i] * num;
			}

		}
	}
}
