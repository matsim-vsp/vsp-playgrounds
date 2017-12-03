package playground.gleich.av_bus.runScenario;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.av.intermodal.router.VariableAccessTransitRouterModule;
import org.matsim.contrib.av.intermodal.router.config.VariableAccessConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigConsistencyChecker;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.gleich.av_bus.analysis.DrtEventsReader;
import playground.gleich.av_bus.analysis.DrtPtTripEventHandler;
import playground.gleich.av_bus.analysis.ExperiencedTripsWriter;

public class RunDrt {
	public static void main(String[] args) {
		if (args.length != 2) {
			throw new RuntimeException("Wrong number of args to main method. Should be path to config and path to outputDir");
		}
		Config config = ConfigUtils.loadConfig(args[0],
				new DrtConfigGroup(), new DvrpConfigGroup(), new VariableAccessConfigGroup());

		config.controler().setOutputDirectory(IOUtils.newUrl(config.getContext(), args[1]).getFile());
		config.addConfigConsistencyChecker(new DrtConfigConsistencyChecker());
		config.checkConsistency();
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = DrtControlerCreator.createControler(config, false);
		controler.addOverridingModule(new VariableAccessTransitRouterModule());
		controler.run();
		
		// Analysis
		EventsManager events = EventsUtils.createEventsManager();
		Set<String> monitoredModes = new HashSet<>();
		monitoredModes.add(TransportMode.pt);
		monitoredModes.add(TransportMode.transit_walk);
		monitoredModes.add("drt");
		
		Set<Id<Link>> monitoredStartAndEndLinks = new HashSet<>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(IOUtils.newUrl(config.getContext(), "../network/linksInArea.csv").getFile()));
			if (reader.readLine().startsWith("id")) {
				for (String line = reader.readLine(); line != null; line = reader.readLine()) {
					monitoredStartAndEndLinks.add(Id.createLinkId(line.split(",")[0]));
				}
				reader.close();
			} else {
				reader.close();
				throw new RuntimeException("linksInArea.csv : first column in header should be id.");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		DrtPtTripEventHandler eventHandler = new DrtPtTripEventHandler(scenario.getNetwork(), scenario.getTransitSchedule(), 
				monitoredModes, monitoredStartAndEndLinks);
		events.addHandler(eventHandler);
		new DrtEventsReader(events).readFile(config.controler().getOutputDirectory() + "/output_events.xml.gz");
		System.out.println("Start writing trips of " + eventHandler.getPerson2ExperiencedTrips().size() + " agents.");
		ExperiencedTripsWriter tripsWriter = new ExperiencedTripsWriter(config.controler().getOutputDirectory() +
				"/experiencedTrips.csv", 
				eventHandler.getPerson2ExperiencedTrips(), monitoredModes);
		tripsWriter.writeExperiencedTrips();
		ExperiencedTripsWriter legsWriter = new ExperiencedTripsWriter(config.controler().getOutputDirectory() + 
				"/experiencedLegs.csv", 
				eventHandler.getPerson2ExperiencedTrips(), monitoredModes);
		legsWriter.writeExperiencedLegs();
	}

}
