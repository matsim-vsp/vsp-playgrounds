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
package org.matsim.contrib.greedo.logging;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.List;

import floetteroed.utilities.statisticslogging.Statistic;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AgesPercentile implements Statistic<LogDataWrapper> {

	public static final String LABEL_PREFIX = "AgePercentile";

	private final int percent;

	public AgesPercentile(final int percent) {
		this.percent = percent;
	}

	@Override
	public String label() {
		return LABEL_PREFIX + this.percent;
	}

	@Override
	public String value(final LogDataWrapper logData) {
		final List<Integer> ages = logData.getSortedAgesView();
		if (ages == null) {
			return Statistic.toString(null);
		} else {
			final int index = max(0, min(ages.size() - 1, (int) ((this.percent / 100.0) * ages.size())));
			return Statistic.toString(ages.get(index));
		}
	}

}
