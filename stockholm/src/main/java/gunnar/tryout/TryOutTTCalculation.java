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
package gunnar.tryout;

import java.io.FileNotFoundException;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class TryOutTTCalculation {

	public static void main(String[] args) throws FileNotFoundException {

		System.out.println("STARTED ...");

		// load the scenario ...

		final MatsimServices controler = new Controler(
				"test/input/zurich/config_base-case.xml");
		final Scenario scenario = controler.getScenario();

		// configure travel time calculations

		int timeBinSize = 15 * 60;
		int endTime = 12 * 3600;

		TravelTimeCalculator ttcalc = new TravelTimeCalculator(
				scenario.getNetwork(), timeBinSize, endTime, scenario
						.getConfig().travelTimeCalculator());

		// run the event handling

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(ttcalc);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile("test/input/zurich/output_base-case/ITERS/it.0/run0.0.events.xml.gz");

		// now try to get something out of this ... some travel times ...

		final TravelTime linkTTs = ttcalc.getLinkTravelTimes();

		for (Map.Entry<Id<Link>, ? extends Link> id2link : scenario
				.getNetwork().getLinks().entrySet()) {
			System.out
					.println(id2link.getKey()
							+ "\t"
							+ (linkTTs.getLinkTravelTime(id2link.getValue(),
									8 * 3600, null, null) / (id2link.getValue()
									.getLength() / id2link.getValue()
									.getFreespeed())));
		}

		// ... and some routing!

		LeastCostPathTree lcpt = new LeastCostPathTree(linkTTs,
				new OnlyTimeDependentTravelDisutility(linkTTs));

		for (Node node : scenario.getNetwork().getNodes().values()) {
			System.out.println("computing a shortest path tree for node "
					+ node.getId());
			lcpt.calculate(scenario.getNetwork(), node, 8 * 3600);
		}

		System.out.println("... DONE");
	}
}
