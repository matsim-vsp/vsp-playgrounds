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
package gunnar.capacities.demo;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.NetworkReaderMatsimV1;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.math.BasicStatistics;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class TimePressureAnalyzer {

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		final String path = "/Users/GunnarF/NoBackup/data-workspace/ihop2/2015-11-23ab_LARGE_RegentMATSim/"
				+ "2015-11-23b_Toll_large/summary/";
		final String configFileName = path + "matsim-config.xml";
		final String networkFile = "/Users/GunnarF/NoBackup/data-workspace/ihop2/ihop2-data/network-output/network.xml";
		final String populationFileName = path + "iteration-3/it.400/400.experienced_plans.xml.gz";
		final String eventsFileName = path + "iteration-3/it.400/400.events.xml.gz";
		final String scatterPlotFileName = "scatter.txt";

		final Config config = ConfigUtils.loadConfig(configFileName);
		final Scenario scenario = ScenarioUtils.createScenario(config);

		new NetworkReaderMatsimV1(scenario.getNetwork()).readFile(networkFile);
		new PopulationReader(scenario).readFile(populationFileName);

		final Map<Id<Person>, Double> personId2timePressure = new LinkedHashMap<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			final Plan plan = person.getSelectedPlan();

			if (plan.getPlanElements().size() > 1) {
				final double leaveHomeTime_s = ((Activity) plan.getPlanElements().get(0)).getEndTime();
				final double reachHomeTime_s = ((Activity) plan.getPlanElements()
						.get(plan.getPlanElements().size() - 1)).getStartTime();
				double realizedDurSum_s = Math.max(leaveHomeTime_s, 0.0) + Math.max(24 * 3600.0 - reachHomeTime_s, 0);
				double typicalDurSum_s = config.planCalcScore().getActivityParams("home").getTypicalDuration();

				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Activity) {
						final Activity act = (Activity) planElement;
						if (!"home".equals(act.getType())) {
							typicalDurSum_s += config.planCalcScore().getActivityParams(act.getType())
									.getTypicalDuration();
							realizedDurSum_s += act.getEndTime() - act.getStartTime();
						}
					}
				}
				personId2timePressure.put(person.getId(), (typicalDurSum_s - realizedDurSum_s) / realizedDurSum_s);
			}
		}

		final Map<Id<Vehicle>, Id<Person>> vehicleId2personId = new LinkedHashMap<>();
		EventsManager eventsManager = new EventsManagerImpl();
		eventsManager.addHandler(new VehicleEntersTrafficEventHandler() {
			@Override
			public void handleEvent(VehicleEntersTrafficEvent event) {
				vehicleId2personId.put(event.getVehicleId(), event.getPersonId());
			}
		});
		new MatsimEventsReader(eventsManager).readFile(eventsFileName);

		final double startTime_s = 7.0 * 3600.0;
		final double endTime_s = 8.0 * 3600;
		final Map<Id<Link>, BasicStatistics> linkId2timePressureStats = new LinkedHashMap<>();
		eventsManager = new EventsManagerImpl();
		eventsManager.addHandler(new LinkEnterEventHandler() {
			@Override
			public void handleEvent(final LinkEnterEvent event) {
				if (event.getTime() >= startTime_s && event.getTime() < endTime_s) {
					BasicStatistics stats = linkId2timePressureStats.get(event.getLinkId());
					if (stats == null) {
						stats = new BasicStatistics();
						linkId2timePressureStats.put(event.getLinkId(), stats);
					}
					stats.add(personId2timePressure.get(vehicleId2personId.get(event.getVehicleId())));
				}
			}
		});
		final TravelTimeCalculator ttCalc = TravelTimeCalculator.create(scenario.getNetwork(),
				config.travelTimeCalculator());
		eventsManager.addHandler(ttCalc);
		new MatsimEventsReader(eventsManager).readFile(eventsFileName);


		final TravelTime tts = ttCalc.getLinkTravelTimes();
		for (Link link : scenario.getNetwork().getLinks().values()) {
			final BasicStatistics stat = linkId2timePressureStats.get(link.getId());
			if (stat != null && stat.size() > 0) {
				final double tt_s = tts.getLinkTravelTime(link, 0.5 * (endTime_s + startTime_s), null, null);
				final double tt0_s = (link.getLength() / link.getFreespeed());
				final double linkPressure = (tt_s - tt0_s) / tt0_s * stat.size();
				final double agentPressure = stat.getAvg() * stat.size();

			}
		}

		System.out.println("... DONE");
	}

}
