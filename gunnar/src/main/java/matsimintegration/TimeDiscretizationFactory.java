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
package matsimintegration;

import org.matsim.core.config.Config;

import floetteroed.utilities.TimeDiscretization;
import floetteroed.utilities.Units;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TimeDiscretizationFactory {

	private TimeDiscretizationFactory() {
	}

	public static TimeDiscretization newInstance(final Config config) {
		final int startTime_s = 0;
		final int binSize_s = config.travelTimeCalculator().getTraveltimeBinSize();
		final int binCnt = (int) Math.ceil(Units.S_PER_D / binSize_s);
		return new TimeDiscretization(startTime_s, binSize_s, binCnt);
	}
	
}
