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
package utils;

import java.util.Random;

import org.apache.log4j.Logger;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class ReplanningEfficiencyEstimator {

	// -------------------- CONSTANTS --------------------

	// The smallest allowed replanning rate.
	private final double minLambda;

	// The replanning rate leading to same variability as a plain network loading.
	private final double networkLoadingNoiseEquivalentLambda;

	// -------------------- MEMBERS --------------------

	// We assume a uniform starting value in [-1, 1].
	
	// Estimated replanning efficiency.
	private double eta = 0.0;

	// Estimated variance of the replanning efficiency.
	private double etaVar = 4.0 / 12.0;

	// -------------------- CONSTRUCTION --------------------

	public ReplanningEfficiencyEstimator(final double minLambda, final double networkLoadingNoiseEquivalentLambda) {
		this.minLambda = minLambda;
		this.networkLoadingNoiseEquivalentLambda = networkLoadingNoiseEquivalentLambda;
	}

	// -------------------- IMPLEMENTATION --------------------

	public double getLambda() {
		return Math.max(this.eta, this.minLambda);
	}

	private double constrain(final double value, final double min, final double max) {
		return Math.min(Math.max(value, min), max);
	}

	public void update(final double realized, final double expected) {
		final double innovation = realized / Math.max(expected, 1e-8);
		final double innovationVariance = Math.pow(this.getLambda(), 2.0) / 12.0
				+ Math.pow(this.networkLoadingNoiseEquivalentLambda, 2.0) / 12.0;
		final double w = this.etaVar / (this.etaVar + innovationVariance);
		this.eta = this.constrain(w * innovation + (1.0 - w) * this.eta, 0, 1);
		this.etaVar = Math.pow(w, 2.0) * innovationVariance + Math.pow(1 - w, 2.0) * this.etaVar;

		Logger.getLogger(this.getClass()).info("Update with realized = " + realized + ", expected = " + expected
				+ "; leading to eta = " + eta + ", etaVar = " + etaVar + ".");
	}

	// -------------------- TESTING --------------------

	static void test1() {
		final Random rnd = new Random();
		ReplanningEfficiencyEstimator estim = new ReplanningEfficiencyEstimator(0.01, 0.01);
		System.out.println("realized\texpected\teta\tetaStddev");
		for (int k = 0; k < 1000; k++) {
			final double realized = Math.pow(0.9, k) * (1.0 + 0.9 * (rnd.nextDouble() - 0.5))
					+ 0.1 * (rnd.nextDouble() - 0.5);
			final double expected = 0.1 * (1.0 + 0.5 * (rnd.nextDouble() - 0.5))
					+ 0.9 * realized * (1.0 + 0.5 * (rnd.nextDouble() - 0.5));
			estim.update(realized, expected);
			System.out.println(
					(realized + "\t" + expected + "\t" + estim.eta + "\t" + Math.sqrt(estim.etaVar)).replace('.', ','));
		}

	}

	public static void main(String[] args) {
		test1();
	}
}
