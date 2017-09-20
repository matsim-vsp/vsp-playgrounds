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

package playground.michalm.stockholm;

import java.util.*;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.util.random.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.common.collect.Iterables;

import playground.michalm.demand.taxi.*;

public class StockholmDemandGeneratorFromServedRequests extends AbstractDemandGeneratorFromServedRequests {
	private final static UniformRandom uniform = RandomUtils.getGlobalUniform();

	public StockholmDemandGeneratorFromServedRequests(Scenario scenario) {
		super(scenario);
	}

	@SuppressWarnings("deprecation")
	public void generateDemand(Iterable<StockholmServedRequest> requests, double selectionProbability) {
		for (StockholmServedRequest r : requests) {
			if (!uniform.trueOrFalse(selectionProbability)) {
				continue;
			}

			// skip last hour; we want to have demand from 5am to 4am (we have
			// taxis from 5 to 5)
			if (r.getStartTime().getHours() == StockholmServedRequests.ZERO_HOUR - 1) {
				continue;
			}
			
			generatePassenger(r, calcStartTime(r));
		}
	}

	private int calcStartTime(StockholmServedRequest request) {
		Date startTime = request.getStartTime();
		@SuppressWarnings("deprecation")
		int h = startTime.getHours();
		@SuppressWarnings("deprecation")
		int m = startTime.getMinutes();

		if (h < StockholmServedRequests.ZERO_HOUR) {
			h += 24;
		}

		return h * 3600 + m * 60 + uniform.nextInt(0, 59);
	}

	public static void main(String[] args) {
		String dir = "d:/temp/Stockholm/";
		String networkFile = dir + "network.xml";

		List<TaxiTrace> taxiTraces = new ArrayList<>();
		new TaxiTracesReader(taxiTraces).readFile(dir + "taxi_1_week_2014_10_6-13.csv");
		Iterable<StockholmServedRequest> requests = new TaxiTracesAnalyser(taxiTraces).getServedRequests();

		System.out.println("#All: " + Iterables.size(requests));
		requests = ServedRequests.filterWorkDaysPeriods(requests, StockholmServedRequests.ZERO_HOUR);
		System.out.println("#on weekdays: " + Iterables.size(requests));

//		for (int i = 2; i <= 10; i++) {
			Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);

			StockholmDemandGeneratorFromServedRequests dg = new StockholmDemandGeneratorFromServedRequests(scenario);
			double scale = 0.25;//4 weekdays -> 1 avg weekday
			dg.generateDemand(requests, scale);
			dg.write(dir + "plans/plans5to4_avg_weekday.xml.gz");
//		}
	}
}
