package playground.gleich.pt;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;

import playground.vsp.andreas.utils.pt.TransitLineRemover;

public class TransitLines300sRemover {

	public static void main(String[] args) {
		final String inScheduleFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule.xml.gz";
		final String inNetworkFile  = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
		final String outScheduleFile = "/home/gregor/git/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule_wo300s.xml.gz";

		Config config = ConfigUtils.createConfig();
		config.transit().setTransitScheduleFile(inScheduleFile);
		config.network().setInputFile(inNetworkFile);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		TransitSchedule inTransitSchedule = scenario.getTransitSchedule();
		
		Set<Id<TransitLine>> linesToRemove = inTransitSchedule.getTransitLines().values().stream()
				.filter(line -> {			
					Object routeShortNameObj = line.getAttributes().getAttribute("gtfs_route_short_name");
					String routeShortName = null == routeShortNameObj ? null : (String) routeShortNameObj;
					int lineNumber = -1;
					try {
						lineNumber = Integer.parseInt(routeShortName);
					} catch (NumberFormatException e) {
						// not a line we were looking for
						return false;
					}
					return line.getAttributes().getAttribute("gtfs_agency_id").equals("796") &&  // is a BVG line
							lineNumber >= 301 && lineNumber <= 399; // line number 301-399
				})
				.map(line -> line.getId())
				.collect(Collectors.toSet());
				
		TransitSchedule outTransitSchedule = TransitLineRemover.removeTransitLinesFromTransitSchedule(inTransitSchedule, linesToRemove);
		new TransitScheduleWriter(outTransitSchedule).writeFile(outScheduleFile);
	}

}
