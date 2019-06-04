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

import floetteroed.utilities.math.Vector;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class RobustBivariateRegression {

	// -------------------- MEMBERS --------------------

	private final RecursiveMovingAverage x;

	private final RecursiveMovingAverage y;

	private boolean cacheValid = false;

	private double cachedSlope = 0.0;

	private double cachedOffset = 0.0;

	private double cachedCorrelation = 0.0;

	// -------------------- CONSTRUCTION --------------------

	public RobustBivariateRegression(final int memory) {
		this.x = new RecursiveMovingAverage(memory);
		this.y = new RecursiveMovingAverage(memory);
	}

	// -------------------- INTERNALS --------------------

	private void forceUpdate() {
		final double[] xData = this.x.getDataAsPrimitiveDoubleArray();
		if (xData.length > 0) {
			final double[] yData = this.y.getDataAsPrimitiveDoubleArray();
			final LeastAbsoluteDeviations yOfX = new LeastAbsoluteDeviations();
			final LeastAbsoluteDeviations xOfY = new LeastAbsoluteDeviations();
			for (int i = 0; i < xData.length; i++) {
				final double xVal = xData[i];
				final double yVal = yData[i];
				yOfX.add(new Vector(xVal, 1.0), yVal);
				xOfY.add(new Vector(yVal, 1.0), xVal);
			}
			yOfX.solve();
			xOfY.solve();
			this.cachedSlope = yOfX.getCoefficients().get(0);
			this.cachedOffset = yOfX.getCoefficients().get(1);
			this.cachedCorrelation = Math.sqrt(this.cachedSlope * xOfY.getCoefficients().get(0));
			this.cacheValid = true;
		}
	}

	private void updateIfNecessary() {
		if (!this.cacheValid) {
			this.forceUpdate();
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	public void add(final double x, final double y) {
		this.cacheValid = false;
		this.x.add(x);
		this.y.add(y);
	}

	public double getSlope() {
		this.updateIfNecessary();
		return this.cachedSlope;
	}

	public double getOffset() {
		this.updateIfNecessary();
		return this.cachedOffset;
	}

	public double getCorrelation() {
		this.updateIfNecessary();
		return this.cachedCorrelation;
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {
		System.out.println("STARTED ...");
		final int points = 100;
		final double slope = 2.0;
		final Random rnd = new Random();
		System.out.println("errorFraction\tslope\toffset\tcorrelation");
		for (double errorFraction : new double[] { 0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.5, 2.0, 2.5,
				3.0 }) {
			RobustBivariateRegression regr = new RobustBivariateRegression(Integer.MAX_VALUE);
			for (int i = 0; i < points; i++) {
				final double x = rnd.nextDouble();
				regr.add(x, slope * x);
			}
			for (int i = 0; i < errorFraction * points; i++) {
				final double x = rnd.nextDouble();
				regr.add(x, slope * (1.0 - x));
				// regr.add(rnd.nextDouble(), rnd.nextDouble());
			}
			System.out.println(
					errorFraction + "\t" + regr.getSlope() + "\t" + regr.getOffset() + "\t" + regr.getCorrelation());
		}

		System.out.println("... DONE.");
	}

}
