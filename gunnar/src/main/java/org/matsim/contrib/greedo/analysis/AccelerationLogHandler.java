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
package org.matsim.contrib.greedo.analysis;

import org.matsim.contrib.greedo.logging.AvgAnticipatedDeltaUtility;
import org.matsim.contrib.greedo.logging.AvgRealizedUtility;
import org.matsim.contrib.greedo.logging.LambdaRealized;
import org.matsim.contrib.greedo.logging.MATSimIteration;

import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class AccelerationLogHandler extends AbstractTabularFileHandlerWithHeaderLine {

	// -------------------- MEMBERS --------------------

	private final Double[] betas;
	private final Double[] realizedLambdas;
	private final Double[] realizedUtilities;
	private final Double[] expectedUtilityChanges;
	private final Double[] performanceCorrelations;
	private final Double[] ageCorrelations;

	// -------------------- CONSTRUCTION --------------------

	public AccelerationLogHandler(final Double[] betas, final Double[] realizedLambdas,
			final Double[] realizedUtilities, final Double[] expectedUtilityChanges,
			final Double[] performanceCorrelations, final Double[] ageCorrelations) {
		this.betas = betas;
		this.realizedLambdas = realizedLambdas;
		this.realizedUtilities = realizedUtilities;
		this.expectedUtilityChanges = expectedUtilityChanges;
		this.performanceCorrelations = performanceCorrelations;
		this.ageCorrelations = ageCorrelations;
	}

	// -------------------- INTERNALS --------------------

	private Double doubleOrNull(final String val) {
		if ((val == null) || "".equals(val)) {
			return null;
		} else {
			return Double.parseDouble(val);
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	@Override
	public void startDataRow(final String[] row) {
		final int iteration = this.getIntValue(MATSimIteration.class.getSimpleName());
		this.betas[iteration] = this.doubleOrNull(this.getStringValue("Beta"));
		this.realizedLambdas[iteration] = this.doubleOrNull(this.getStringValue(LambdaRealized.class.getSimpleName()));
		this.realizedUtilities[iteration] = this
				.doubleOrNull(this.getStringValue(AvgRealizedUtility.class.getSimpleName()));

		if (this.label2index.containsKey(AvgAnticipatedDeltaUtility.class.getSimpleName())) {
			this.expectedUtilityChanges[iteration] = this
					.doubleOrNull(this.getStringValue(AvgAnticipatedDeltaUtility.class.getSimpleName()));
		} else {
			this.expectedUtilityChanges[iteration] = null;
		}

		this.performanceCorrelations[iteration] = this
				.doubleOrNull(this.getStringValue("Corr(DeltaX2,DeltaU-DeltaU*)"));
		this.ageCorrelations[iteration] = this
				.doubleOrNull(this.getStringValue("Corr(Age*ExpDeltaUtility;Similarity)"));

	}
}
