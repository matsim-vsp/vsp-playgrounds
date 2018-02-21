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

package playground.michalm.poznan.demand.taxi;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.prep.PreparedPolygon;

import playground.michalm.demand.taxi.ServedRequests;
import playground.michalm.poznan.zone.PoznanZones;

public class PoznanServedRequests {
	public static final int ZERO_HOUR = 4;

	// e.g. months = "234"
	public static List<PoznanServedRequest> readRequests(int... months) {
		List<PoznanServedRequest> requests = new ArrayList<>();
		String path = "d:/PP-rad/taxi/poznan-supply/zlecenia_obsluzone/Zlecenia_obsluzone_2014-0";

		for (int m : months) {
			new PoznanServedRequestsReader(requests).readFile(path + m + ".csv");
		}

		return requests;
	}

	public static Iterable<PoznanServedRequest> filterRequestsWithinAgglomeration(
			Iterable<PoznanServedRequest> requests) {
		MultiPolygon area = PoznanZones.readAgglomerationArea();
		final PreparedPolygon preparedPolygon = new PreparedPolygon(area);
		return Iterables.filter(requests, r -> ServedRequests.isWithinArea(r, preparedPolygon));
	}

	public static Iterable<PoznanServedRequest> filterNormalPeriods(Iterable<PoznanServedRequest> requests) {
		// February - 1-28 (4 full weeks)
		// March - 2-29 (4 full weeks) - exclude: 1, 30-31 (daylight saving time shift)
		// April - 1-14 + 23-29 (3 full weeks), exclude: 15-22, 30 (Easter and May's long weekend)

		Date fromDate1 = midnight("01-03");
		Date toDate1 = midnight("02-03");

		Date fromDate2 = midnight("30-03");
		Date toDate2 = midnight("01-04");

		Date fromDate3 = midnight("15-04");
		Date toDate3 = midnight("23-04");

		Date fromDate4 = midnight("30-04");
		Date toDate4 = midnight("01-05");

		Predicate<PoznanServedRequest> orPredicate = Predicates.or(
				r -> ServedRequests.isBetweenDates(r, fromDate1, toDate1),
				r -> ServedRequests.isBetweenDates(r, fromDate2, toDate2),
				r -> ServedRequests.isBetweenDates(r, fromDate3, toDate3),
				r -> ServedRequests.isBetweenDates(r, fromDate4, toDate4));

		return Iterables.filter(requests, Predicates.not(orPredicate));
	}

	private static Date midnight(String date) {
		// format: "dd-MM-yyyy HH:mm:ss"
		return PoznanServedRequestsReader.parseDate(date + "-2014 00:00:00");
	}

	public static Iterable<PoznanServedRequest> filterNext24Hours(Iterable<PoznanServedRequest> requests,
			Date fromDate) {
		Date toDate = new Date(fromDate.getTime() + 24 * 3600 * 1000);
		return Iterables.filter(requests, r -> ServedRequests.isBetweenDates(r, fromDate, toDate));
	}
}
