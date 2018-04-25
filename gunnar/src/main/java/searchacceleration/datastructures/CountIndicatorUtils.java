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
package searchacceleration.datastructures;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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

	public static <L> DynamicData<L> newWeightedCounts(final TimeDiscretization timeDiscr,
			final Collection<SpaceTimeIndicators<L>> allIndicators, final Map<L, Double> weights) {
		final DynamicData<L> result = new DynamicData<L>(timeDiscr);
		for (SpaceTimeIndicators<L> indicators : allIndicators) {
			for (int bin = 0; bin < indicators.getTimeBinCnt(); bin++) {
				for (L locObj : indicators.getVisitedSpaceObjects(bin)) {
					result.add(locObj, bin, weights.get(locObj));
				}
			}
		}
		return result;
	}

	public static <L> double sumOfEntries2(final DynamicData<L> data) {
		double result = 0.0;
		for (L locObj : data.keySet()) {
			for (int bin = 0; bin < data.getBinCnt(); bin++) {
				final double val = data.getBinValue(locObj, bin);
				result += val * val;
			}
		}
		return result;
	}

	public static <L> double sumOfDifferences2(final DynamicData<L> counts1, final DynamicData<L> counts2) {
		if (counts1.getBinCnt() != counts2.getBinCnt()) {
			throw new RuntimeException(
					"counts1 has " + counts1.getBinCnt() + " bins; counts2 has " + counts2.getBinCnt() + " bins.");
		}
		double result = 0.0;
		final Set<L> allLocObj = new LinkedHashSet<>(counts1.keySet());
		allLocObj.addAll(counts2.keySet());
		for (L locObj : allLocObj) {
			for (int bin = 0; bin < counts1.getBinCnt(); bin++) {
				final double diff = counts1.getBinValue(locObj, bin) - counts2.getBinValue(locObj, bin);
				result += diff * diff;
			}
		}
		return result;
	}

	public static <L> DynamicData<L> newInteractionResiduals(final DynamicData<L> currentWeightedCounts,
			final DynamicData<L> newWeightedCounts, final double meanLambda) {
		if (currentWeightedCounts.getBinCnt() != newWeightedCounts.getBinCnt()) {
			throw new RuntimeException("currentWeightedCounts has " + currentWeightedCounts.getBinCnt()
					+ " bins; newWeightedCounts has " + newWeightedCounts.getBinCnt() + " bins.");
		}
		final DynamicData<L> result = new DynamicData<L>(currentWeightedCounts.getStartTime_s(),
				currentWeightedCounts.getBinSize_s(), currentWeightedCounts.getBinCnt());
		final Set<L> allLocObjs = new LinkedHashSet<>(currentWeightedCounts.keySet());
		allLocObjs.addAll(newWeightedCounts.keySet());
		for (L locObj : allLocObjs) {
			for (int bin = 0; bin < currentWeightedCounts.getBinCnt(); bin++) {
				result.put(locObj, bin, meanLambda * (newWeightedCounts.getBinValue(locObj, bin)
						- currentWeightedCounts.getBinValue(locObj, bin)));
			}
		}
		return result;
	}
}
