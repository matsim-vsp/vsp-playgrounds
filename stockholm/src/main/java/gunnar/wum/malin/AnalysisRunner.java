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
package gunnar.wum.malin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;

import cadyts.utilities.math.MathHelpers;
import gunnar.ihop2.regent.demandreading.ZonalSystem;
import gunnar.wum.malin.PersonTravelStatistics.PersonEntry;
import saleem.stockholmmodel.utils.StockholmTransformationFactory;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AnalysisRunner {

	public static void main(String[] args) throws FileNotFoundException {
		System.out.println("STARTED ...");

		System.out.println("System.exit(0);");
		System.exit(0);

		final String zonesShapeFileName = "/Users/GunnarF/OneDrive - VTI/My Data/ihop2/ihop2-data/demand-input/sverige_TZ_EPSG3857.shp";
		final ZonalSystem zonalSystem = new ZonalSystem(zonesShapeFileName,
				StockholmTransformationFactory.WGS84_EPSG3857);

		final Config config = ConfigUtils.createConfig();
		config.network().setInputFile("/Users/GunnarF/NoBackup/data-workspace/wum/2019-02-27b/output_network.xml.gz");
		config.transit().setUseTransit(true);
		config.transit().setTransitScheduleFile(
				"/Users/GunnarF/NoBackup/data-workspace/wum/2019-02-27b/output_transitSchedule.xml.gz");
		// "/Users/GunnarF/OneDrive - VTI/My
		// Data/wum/data/output/transitSchedule_reduced.xml.gz");
		config.transit().setVehiclesFile(
				"/Users/GunnarF/OneDrive - VTI/My Data/wum/data/output/transitVehiclesDifferentiated.xml.gz");

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		zonalSystem.addNetwork(scenario.getNetwork(), StockholmTransformationFactory.WGS84_SWEREF99);

		final EventsManager manager = EventsUtils.createEventsManager();

		// final EventsToLegs events2legs = new EventsToLegs(scenario);
		// final EventsToActivities events2acts = new EventsToActivities();
		// manager.addHandler(events2legs);
		// manager.addHandler(events2acts);

		// zoneStats

		// final InterZonalStatistics zoneStats = new InterZonalStatistics(zonalSystem,
		// scenario);
		//
		// zoneStats.addOrigin("720113");
		// zoneStats.addOrigin("720112");
		// zoneStats.addOrigin("720111");
		// zoneStats.addOrigin("720103");
		//
		// zoneStats.addDestination("720113");
		// zoneStats.addDestination("720112");
		// zoneStats.addDestination("720111");
		// zoneStats.addDestination("720103");
		//
		// events2legs.addLegHandler(zoneStats);
		// events2acts.addActivityHandler(zoneStats);

		// personStats

		// final PersonTravelStatistics personStats = new PersonTravelStatistics(
		// act -> !act.getAgentId().toString().startsWith("pt") &&
		// !act.getActivity().getType().startsWith("pt"),
		// leg -> !leg.getAgentId().toString().startsWith("pt"), scenario.getNetwork(),
		// zonalSystem);
		// events2legs.addLegHandler(personStats);
		// events2acts.addActivityHandler(personStats);

		// linkStats

		// final LinkTravelStatistic linkStats = new
		// LinkTravelStatistic(scenario.getNetwork(),
		// time_s -> (time_s >= 8 * 3600 && time_s < 9 * 3600), linkId -> true,
		// personId -> !personId.toString().startsWith("pt"), vehicleId -> true);
		// manager.addHandler(linkStats);
		// EventsUtils.readEvents(manager,
		// "/Users/GunnarF/NoBackup/data-workspace/wum/2019-02-27b/output_events.xml.gz");

		// System.out.println("valid: " + zoneStats.getValidCnt());
		// System.out.println("invalid: " + zoneStats.getInvalidCnt());
		// zoneStats.toFolder(new File("./malin"));

		// personStats.writePersonData("./malin/personStats.csv");

		// linkStats.writeLinkData("./malin/linkStats8-9.csv");

		writeInterZonalStatistics(scenario, zonalSystem, manager);
		// writeZonalUsageStatistics(scenario, zonalSystem, manager);
		// writeLinkDataPrivateCars(scenario, manager);
		// writeLinkDataPublicTransport(scenario, manager);

		System.out.println("... DONE");
	}

	static void writeInterZonalStatistics(final Scenario scenario, final ZonalSystem zonalSystem,
			final EventsManager manager) throws FileNotFoundException {

		final EventsToLegs events2legs = new EventsToLegs(scenario);
		final EventsToActivities events2acts = new EventsToActivities();
		manager.addHandler(events2legs);
		manager.addHandler(events2acts);

		final InterZonalStatistics zoneStats = new InterZonalStatistics(zonalSystem, scenario);
		events2legs.addLegHandler(zoneStats);
		events2acts.addActivityHandler(zoneStats);

		for (String zoneId : zonalSystem.getId2zoneView().keySet()) {
			zoneStats.addOrigin(zoneId);
			zoneStats.addDestination(zoneId);
		}

		EventsUtils.readEvents(manager, "/Users/GunnarF/NoBackup/data-workspace/wum/2019-02-27b/output_events.xml.gz");

		zoneStats.toFolder(new File("./malin/zones"));
	}

	static void writeZonalUsageStatistics(final Scenario scenario, final ZonalSystem zonalSystem,
			final EventsManager manager) throws FileNotFoundException {

		final EventsToLegs events2legs = new EventsToLegs(scenario);
		final EventsToActivities events2acts = new EventsToActivities();
		manager.addHandler(events2legs);
		manager.addHandler(events2acts);

		final PersonTravelStatistics personStats = new PersonTravelStatistics(
				act -> !act.getAgentId().toString().startsWith("pt") && !act.getActivity().getType().startsWith("pt"),
				leg -> !leg.getAgentId().toString().startsWith("pt"), scenario.getNetwork(), zonalSystem);
		events2legs.addLegHandler(personStats);
		events2acts.addActivityHandler(personStats);

		final int start_h = 8;
		final int end_h = 9;
		final ZoneUsageAnalyzer zoneUsageAnalyzer = new ZoneUsageAnalyzer(zonalSystem, scenario, start_h * 3600,
				end_h * 3600, personId -> !personId.toString().startsWith("pt"));
		manager.addHandler(zoneUsageAnalyzer);

		EventsUtils.readEvents(manager, "/Users/GunnarF/NoBackup/data-workspace/wum/2019-02-27b/output_events.xml.gz");

		zoneUsageAnalyzer.complete();

		final PrintWriter writer = new PrintWriter("./malin/zonalStats_" + start_h + "-" + end_h + ".csv");
		writer.println("zone,persons,travelTime[s],travelDistance[m]");
		for (Map.Entry<String, Set<Id<Person>>> entry : zoneUsageAnalyzer.getZoneId2personIdsView().entrySet()) {
			int cnt = 0;
			double distanceSum_m = 0.0;
			double timeSum_s = 0.0;
			for (Id<Person> personId : entry.getValue()) {
				cnt++;
				final PersonEntry personEntry = personStats.personId2entry.get(personId);
				if (personEntry != null) {
					distanceSum_m += personEntry.totalTravelDistance_m;
					timeSum_s += personEntry.totalTravelTime_s;
				}
			}
			if (cnt > 0) {
				writer.println(entry.getKey() + "," + cnt + "," + MathHelpers.round(timeSum_s / cnt) + ","
						+ MathHelpers.round(distanceSum_m / cnt));
			}
		}
		writer.flush();
		writer.close();
	}

	static void writeLinkDataPrivateCars(final Scenario scenario, final EventsManager manager)
			throws FileNotFoundException {

		final LinkTravelStatistic linkStats = new LinkTravelStatistic(scenario.getNetwork(),
				time_s -> (time_s >= 8 * 3600 && time_s < 9 * 3600), linkId -> true,
				personId -> !personId.toString().startsWith("pt"),
				vehicleId -> !scenario.getTransitVehicles().getVehicles().containsKey(vehicleId), true);
		manager.addHandler(linkStats);

		EventsUtils.readEvents(manager, "/Users/GunnarF/NoBackup/data-workspace/wum/2019-02-27b/output_events.xml.gz");

		linkStats.writeLinkData("./malin/linkStats_8-9_privateCars.csv");
	}

	static void writeLinkDataPublicTransport(final Scenario scenario, final EventsManager manager)
			throws FileNotFoundException {

		final LinkTravelStatistic linkStats = new LinkTravelStatistic(scenario.getNetwork(),
				time_s -> (time_s >= 8 * 3600 && time_s < 9 * 3600), linkId -> true,
				personId -> !personId.toString().startsWith("pt"),
				vehicleId -> scenario.getTransitVehicles().getVehicles().containsKey(vehicleId), false);
		manager.addHandler(linkStats);

		EventsUtils.readEvents(manager, "/Users/GunnarF/NoBackup/data-workspace/wum/2019-02-27b/output_events.xml.gz");

		linkStats.writeLinkData("./malin/linkStats_8-9_publicTransport.csv");
	}

}
