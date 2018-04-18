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
package besttimeresponse;

import java.util.Arrays;

import floetteroed.utilities.Time;
import floetteroed.utilities.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TestTimeAllocation {

	public static void main(String[] args) {

		// specify the problem

		final TimeDiscretization discr = new TimeDiscretization(0, 3600, 24);

		final double betaDur_1_s = 1;
		final double betaTravel_1_s = -1;
		final double betaLateArr_1_s = -1;
		final double betaEarlyDpt_1_s = -1;

		final PlannedActivity<String, String> home = new PlannedActivity<>("home", "car", 8.0 * 3600, 1.0, null, null,
				null, null, betaDur_1_s, betaEarlyDpt_1_s, betaLateArr_1_s, betaTravel_1_s);
		final PlannedActivity<String, String> work = new PlannedActivity<>("office", "car", 8.0 * 3600, 1.0, 6.0 * 3600,
				18.0 * 3600, null, null, betaDur_1_s, betaEarlyDpt_1_s, betaLateArr_1_s, betaTravel_1_s);
		final PlannedActivity<String, String> shop = new PlannedActivity<>("store", "car", 1.0 * 3600, 1.0, 8.0 * 3600,
				21.0 * 3600, null, null, betaDur_1_s, betaEarlyDpt_1_s, betaLateArr_1_s, betaTravel_1_s);

		final double dptFromHome_s = Time.secFromStr("12:00:00");
		final double dptFromWork_s = Time.secFromStr("12:00:00");
		final double dptFromShop_s = Time.secFromStr("12:00:00");

		// solve the problem

		final TimeAllocator<String, String> timeAlloc = new TimeAllocator<>(discr,
				new TripTravelTimes<String, String>() {
					@Override
					public double getTravelTime_s(String origin, String destination, double dptTime_s, String mode) {
						return 1800;
						// return 3600.0 * 0.5 * (1.0 - Math.cos(4.0 * Math.PI *
						// dptTime_s / Units.S_PER_D));
					}
				}, 
				true, true, true);
		final double[] result = timeAlloc.optimizeDepartureTimes(Arrays.asList(home, work, shop),
				new double[] { dptFromHome_s, dptFromWork_s, dptFromShop_s });

		System.out.println();
		System.out.println("dpt. from home at " + Time.strFromSec((int) result[0]));
		System.out.println("dpt. from work at " + Time.strFromSec((int) result[1]));
		System.out.println("dpt. from shop at " + Time.strFromSec((int) result[2]));
	}
}
