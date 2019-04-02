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
package org.matsim.contrib.greedo.datastructures;

import java.util.Collection;

import org.matsim.core.utils.collections.Tuple;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class CountIndicatorUtils {

	private CountIndicatorUtils() {
	}

	public static <L> Tuple<DynamicData<L>, DynamicData<L>> newWeightedAndUnweightedCounts(
			final TimeDiscretization timeDiscr, final Collection<SpaceTimeIndicators<L>> allIndicators) {
		final DynamicData<L> weightedCounts = new DynamicData<L>(timeDiscr);
		final DynamicData<L> unweightedCounts = new DynamicData<L>(timeDiscr);
		for (SpaceTimeIndicators<L> indicators : allIndicators) {
			addIndicatorsToTotalsTreatingNullAsZero(weightedCounts, unweightedCounts, indicators, 1.0);
		}
		return new Tuple<>(weightedCounts, unweightedCounts);
	}

	public static <L> void addIndicatorsToTotalsTreatingNullAsZero(final DynamicData<L> weightedTotals,
			final DynamicData<L> unweightedTotals, final SpaceTimeIndicators<L> indicators, final double factor) {
		if (indicators != null) {
			for (int bin = 0; bin < indicators.getTimeBinCnt(); bin++) {
				for (SpaceTimeIndicators<L>.Visit visit : indicators.getVisits(bin)) {
					weightedTotals.add(visit.spaceObject, bin, factor * visit.weight);
					unweightedTotals.add(visit.spaceObject, bin, factor * 1.0);
				}
			}
		}
	}
}
