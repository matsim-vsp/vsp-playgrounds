/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.michalm.demand.taxi;

import java.util.Arrays;
import java.util.Date;

import org.matsim.core.utils.geometry.geotools.MGC;

import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.prep.PreparedPolygon;

public class ServedRequests {
	public enum WeekDay {
		SUN, MON, TUE, WED, THU, FRI, SAT;// weird ordering imposed by Date

		@SuppressWarnings("deprecation")
		public static WeekDay getWeekDay(Date date) {
			return WeekDay.values()[date.getDay()];
		}
	}

	public static boolean isWithinArea(ServedRequest request, PreparedPolygon preparedPolygon) {
		Point from = MGC.coord2Point(request.getFrom());
		Point to = MGC.coord2Point(request.getTo());
		return preparedPolygon.contains(from) && preparedPolygon.contains(to);
	}

	public static boolean isBetweenDates(ServedRequest request, Date fromDate, Date toDate) {
		long assignedTime = request.getStartTime().getTime();
		return assignedTime >= fromDate.getTime() && assignedTime < toDate.getTime();
	}

	public static boolean isOnWeekDays(ServedRequest request, WeekDay... weekDays) {
		WeekDay wd = WeekDay.getWeekDay(request.getStartTime());
		return Arrays.asList(weekDays).contains(wd);
	}

	@SuppressWarnings("deprecation")
	public static <T extends ServedRequest> Iterable<T> filterWorkDaysPeriods(Iterable<T> requests,
			final int zeroHour) {
		return Iterables.filter(requests, request -> {
			WeekDay wd = WeekDay.getWeekDay(request.getStartTime());
			switch (wd) {
				case MON:
					return request.getStartTime().getHours() >= zeroHour;

				case TUE:
				case WED:
				case THU:
					return true;

				case SAT:
				case SUN:
					return false;

				case FRI:
					return request.getStartTime().getHours() < zeroHour;

				default:
					throw new IllegalArgumentException();
			}
		});
	}
}
