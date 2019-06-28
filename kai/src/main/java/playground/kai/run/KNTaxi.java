/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.kai.run;

import java.net.URL;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiControlerCreator;
import org.matsim.contrib.util.PopulationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

class KNTaxi {
	/**
	 * @param configUrl               configuration (e.g. read from a param file)
	 * @param removeNonPassengers     if {@code true}, only taxi traffic is simulated
	 * @param endActivitiesAtTimeZero if {@code true}, everybody calls taxi at time 0
	 * @param otfvis                  if {@code true}, OTFVis is launched
	 */
	public static void run(URL configUrl, boolean removeNonPassengers, boolean endActivitiesAtTimeZero,
			boolean otfvis) {
		if (!removeNonPassengers && endActivitiesAtTimeZero) {
			throw new RuntimeException(
					"endActivitiesAtTimeZero makes sense only in combination with removeNonPassengers");
		}

		Config config = ConfigUtils.loadConfig(configUrl, new TaxiConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		OTFVisConfigGroup otfConfig = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.class);
		otfConfig.setAgentSize(otfConfig.getAgentSize() * 2);

		Controler controler = TaxiControlerCreator.createControlerWithSingleModeDrt(config, otfvis);

		if (removeNonPassengers) {
			PopulationUtils.removePersonsNotUsingMode(TransportMode.taxi, controler.getScenario());
		}

		if (endActivitiesAtTimeZero) {
			setEndTimeForFirstActivities(controler.getScenario(), 0);
		}

		controler.run();
	}

	private static void setEndTimeForFirstActivities(Scenario scenario, double time) {
		Map<Id<Person>, ? extends Person> persons = scenario.getPopulation().getPersons();
		for (Person p : persons.values()) {
			Activity activity = (Activity)p.getSelectedPlan().getPlanElements().get(0);
			activity.setEndTime(time);
		}
	}

	public static void main(String... args) {
		URL configUrl = IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_taxi_config.xml");
		run(configUrl, true, false, true);
	}
}
