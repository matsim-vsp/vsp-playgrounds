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

	public static <L> DynamicData<L> newCounts(final TimeDiscretization timeDiscr,
			final Collection<SpaceTimeIndicators<L>> allIndicators) {
		final DynamicData<L> result = new DynamicData<L>(timeDiscr);
		for (SpaceTimeIndicators<L> indicators : allIndicators) {
			for (int bin = 0; bin < indicators.getTimeBinCnt(); bin++) {
				for (L locObj : indicators.getVisitedSpaceObjects(bin)) {
					result.add(locObj, bin, 1.0);
				}
			}
		}
		return result;
	}

	public static <L> double sumOfCounts2(final DynamicData<L> counts) {
		double result = 0.0;
		for (L locObj : counts.keySet()) {
			for (int bin = 0; bin < counts.getBinCnt(); bin++) {
				final double val = counts.getBinValue(locObj, bin);
				result += val * val;
			}
		}
		return result;
	}

	public static <L> double sumOfCountDifferences2(final DynamicData<L> counts1, final DynamicData<L> counts2) {
		double result = 0.0;
		final Set<L> allLocObj = new LinkedHashSet<>(counts1.keySet());
		allLocObj.addAll(counts2.keySet());
		for (L locObj : allLocObj) {
			for (int bin = 0; bin < Math.min(counts1.getBinCnt(), counts2.getBinCnt()); bin++) {
				final double diff = counts1.getBinValue(locObj, bin) - counts2.getBinValue(locObj, bin);
				result += diff * diff;
			}
		}
		return result;
	}

	public static <L> DynamicData<L> newInteractionResiduals(final DynamicData<L> currentCounts,
			final DynamicData<L> newCounts, final double meanLambda) {
		final DynamicData<L> result = new DynamicData<L>(currentCounts.getStartTime_s(), currentCounts.getBinSize_s(),
				currentCounts.getBinCnt());
		final Set<L> allLocObjs = new LinkedHashSet<>(currentCounts.keySet());
		allLocObjs.addAll(newCounts.keySet());
		for (L locObj : allLocObjs) {
			for (int bin = 0; bin < currentCounts.getBinCnt(); bin++) {
				result.put(locObj, bin,
						meanLambda * (newCounts.getBinValue(locObj, bin) - currentCounts.getBinValue(locObj, bin)));
			}
		}
		return result;
	}
}
