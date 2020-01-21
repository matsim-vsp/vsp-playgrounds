package playground.gleich.pt;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.pt.utils.TransitScheduleValidator.ValidationResult;

import playground.vsp.andreas.utils.pt.TransitLineRemover;
import playground.vsp.andreas.utils.pt.TransitScheduleCleaner;

public class TransitLinesHeadwayBasedRemover {

	private static final Logger log = Logger.getLogger(TransitLinesHeadwayBasedRemover.class);
	
	public static void main(String[] args) {
		final String inScheduleFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule.xml.gz";
		final String inNetworkFile  = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
		final String outScheduleFileBase = "/home/gregor/git/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule-";

		double maxHeadway = 15 * 60.0; // min
		double measurePeriodStart = 7 * 3600.0 + 30 * 60; // h; better use pm peak, because e.g. Bus 100 has 25 min gaps before 8:00!
		double measurePeriodEnd = 19 * 3600.0; // h
		double minShareOfStopsWithHeadway = 0.5; // parts of the line may be served less frequently
		int minDeparturesForStopToBeConsidered = 50; // ignore branch routes served only twice a day etc.
		
		DecimalFormat df0 = new DecimalFormat("#");
		df0.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.ENGLISH));
		DecimalFormat df1 = new DecimalFormat("#.#");
		df1.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.ENGLISH));
		DecimalFormat df2 = new DecimalFormat("#.##");
		df2.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.ENGLISH));
		
		final String outScheduleFile = outScheduleFileBase + 
				"-measurePeriodHH" + df1.format(measurePeriodStart / 3600) + 
				"-" + df1.format(measurePeriodEnd / 3600) +
				"-shareStops" + df2.format(minShareOfStopsWithHeadway) +
				"-minDepsAnaStop" + minDeparturesForStopToBeConsidered +
				"-maxHeadwayMM" + df0.format(maxHeadway / 60) + 
				".xml.gz";
		
//		log.setLevel(Level.ALL);
		
		Config config = ConfigUtils.createConfig();
		config.transit().setTransitScheduleFile(inScheduleFile);
		config.network().setInputFile(inNetworkFile);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		TransitSchedule inTransitSchedule = scenario.getTransitSchedule();
		
		Map<String, List<TransitLine>> gtfsRouteShortName2TransitLines = getGtfsRouteShortName2TransitLines(inTransitSchedule.getTransitLines().values());
		
		Map<String, List<TransitLine>> gtfsRouteShortName2BVGBusTransitLines = filterBVGBus(gtfsRouteShortName2TransitLines);
				
		Set<Id<TransitLine>> linesToRemove = gtfsRouteShortName2BVGBusTransitLines.entrySet().stream().
				filter(entry -> !maxHeadwayBelowThreshold(entry.getValue(), maxHeadway, measurePeriodStart, measurePeriodEnd, minShareOfStopsWithHeadway, minDeparturesForStopToBeConsidered)).
				map(entry -> entry.getValue().stream().
						map(line -> line.getId()).
						collect(Collectors.toSet())).
				collect(HashSet::new, Set::addAll, Set::addAll);
		
		linesToRemove.stream().sorted().forEach(l -> log.info(l.toString()));
				
		TransitSchedule outTransitSchedule = TransitLineRemover.removeTransitLinesFromTransitSchedule(inTransitSchedule, linesToRemove);
		
		TransitSchedule outTransitScheduleCleaned = TransitScheduleCleaner.removeStopsNotUsed(outTransitSchedule);
		
		ValidationResult validationResult = TransitScheduleValidator.validateAll(outTransitScheduleCleaned, scenario.getNetwork());
		log.warn(validationResult);
		
		new TransitScheduleWriter(outTransitScheduleCleaned).writeFile(outScheduleFile);
	}
	
	private static Map<String, List<TransitLine>> filterBVGBus(
			Map<String, List<TransitLine>> gtfsRouteShortName2TransitLines) {
		
		Map<String, List<TransitLine>> gtfsRouteShortName2BVGBusTransitLines = new HashMap<>();
		
		for (Entry<String, List<TransitLine>> routeName2Lines: gtfsRouteShortName2TransitLines.entrySet()) {
			// filter for only BVG TransitLines
			List<TransitLine> bvgLines = routeName2Lines.getValue().stream().
			filter(line -> line.getAttributes().getAttribute("gtfs_agency_id").equals("796")).collect(Collectors.toList()); // is a BVG line
			
			if (bvgLines.size() > 0) {
				// determine whether all TransitLines of that gtfs route short name are buses (exclude e.g. rail replacement buses)
				boolean allBVGLinesAreBus = bvgLines.stream().
				allMatch(line -> line.getAttributes().getAttribute("gtfs_route_type").equals("3") || // is a bus line
									line.getAttributes().getAttribute("gtfs_route_type").equals("700")); // is a bus line
				if (allBVGLinesAreBus) {
					gtfsRouteShortName2BVGBusTransitLines.put(routeName2Lines.getKey(), bvgLines);
				}
			}
		}
		return gtfsRouteShortName2BVGBusTransitLines;
	}

	private static Map<String, List<TransitLine>> getGtfsRouteShortName2TransitLines(Collection<TransitLine> transitLines) {
		Map<String, List<TransitLine>> gtfsRouteShortName2TransitLines = new HashMap<>();
		for (TransitLine line: transitLines) {
			Object routeShortNameObj = line.getAttributes().getAttribute("gtfs_route_short_name");
			String routeShortName = null == routeShortNameObj ? null : (String) routeShortNameObj;
			if (!gtfsRouteShortName2TransitLines.containsKey(routeShortName)) {
				gtfsRouteShortName2TransitLines.put(routeShortName, new ArrayList<>());
			}
			gtfsRouteShortName2TransitLines.get(routeShortName).add(line);
		}
		return gtfsRouteShortName2TransitLines;
	}

	private static boolean maxHeadwayBelowThreshold(List<TransitLine> lines, double maxHeadway, double measurePeriodStart,
			double measurePeriodEnd, double minShareOfStopsWithHeadway, int minDeparturesForStopToBeConsidered) {
		
		if (measurePeriodStart >= measurePeriodEnd) {
			throw new RuntimeException("measurePeriodStart >= measurePeriodEnd");
		}
		
		/* 
		 * many real stops are split up into several TransitStopFacilities.
		 * Using their field "name" as identifier does not allow to differentiate directions of service.
		 * Better try to reduce Matsim Id<TransitStopFacility> to original gtfs ids.
		 */
		Map<String, List<Double>> stop2departureTimes = new HashMap<>();
		Map<String, Double> stop2maxHeadway = new HashMap<>();
		
		lines.forEach(line -> line.getRoutes().values().forEach(route -> addDeparturesPerStop(stop2departureTimes, route)));
		
		for (Entry<String, List<Double>> stopAndDepartureTimes: stop2departureTimes.entrySet()) {	
			 // ignore branch routes served only twice a day etc.
			if (stopAndDepartureTimes.getValue().size() > minDeparturesForStopToBeConsidered) {
				stop2maxHeadway.put(stopAndDepartureTimes.getKey(), 
						findMaxHeadwayPerStop(stopAndDepartureTimes.getValue(), measurePeriodStart, measurePeriodEnd));
				log.debug(stopAndDepartureTimes.getKey() + " : " + stop2maxHeadway.get(stopAndDepartureTimes.getKey()) + " : " + stopAndDepartureTimes.getValue());
			}
		}
		
		long stopsWithMaxHeadwayEqualOrSmallerCounter = stop2maxHeadway.values().stream().
				filter(maxHeadwayFound -> maxHeadwayFound <= maxHeadway).
				count();
		
		double shareOfStopsWithMaxHeadwayEqualOrSmaller = ((double) stopsWithMaxHeadwayEqualOrSmallerCounter) / stop2maxHeadway.size();
		
		return shareOfStopsWithMaxHeadwayEqualOrSmaller >= minShareOfStopsWithHeadway;
	}

	private static void addDeparturesPerStop(Map<String, List<Double>> stop2departureTimes, TransitRoute route) {
		for (TransitRouteStop stop: route.getStops()) {
			for (Departure dep: route.getDepartures().values()) {
				// try to reduce Matsim Id<TransitStopFacility> to original gtfs id
				String originalStopId = stop.getStopFacility().getId().toString().split("\\.")[0];
				log.debug(stop.getStopFacility().getId().toString() + " " + originalStopId);
				
				double departureTimeAtStop = dep.getDepartureTime() + stop.getDepartureOffset();
				if (!stop2departureTimes.containsKey(originalStopId)) {
					stop2departureTimes.put(originalStopId, new ArrayList<>());
				}
				stop2departureTimes.get(originalStopId).add(departureTimeAtStop);
			}
		}
	}
	
	private static double findMaxHeadwayPerStop(List<Double> departureTimes, double measurePeriodStart, double measurePeriodEnd) {
		Collections.sort(departureTimes);

		// TODO: This is not 100% accurate, unclear what headway to return
		double resultHeadwayIfLessThan2DepartureTimes = Double.POSITIVE_INFINITY;
		double maxHeadwayFound = 0.0d;

		if (departureTimes.size() < 2) {
			return resultHeadwayIfLessThan2DepartureTimes;
		}

		int indexLastDepartureBeforeOrAtMeasureStart = -1;
		int indexFirstDepartureAtOrAfterMeasureEnd = -1;

		for (int i = 1; i < departureTimes.size(); i++) {
			if (departureTimes.get(i) >= measurePeriodStart) {
				indexLastDepartureBeforeOrAtMeasureStart = i - 1;
				break;
			}
		}

		if (indexLastDepartureBeforeOrAtMeasureStart < 0) {
			return resultHeadwayIfLessThan2DepartureTimes;
		}

		for (int i = indexLastDepartureBeforeOrAtMeasureStart; i < departureTimes.size(); i++) {
			if (departureTimes.get(i) >= measurePeriodEnd) {
				indexFirstDepartureAtOrAfterMeasureEnd = i;
				break;
			}
		}

		if (indexFirstDepartureAtOrAfterMeasureEnd < 0) {
			return resultHeadwayIfLessThan2DepartureTimes;
		}

		double previousDeparture = departureTimes.get(indexLastDepartureBeforeOrAtMeasureStart);
		for (int i = indexLastDepartureBeforeOrAtMeasureStart + 1; i <= indexFirstDepartureAtOrAfterMeasureEnd; i++) {
			double headway = departureTimes.get(i) - previousDeparture;
			if (headway > maxHeadwayFound) {
				maxHeadwayFound = headway;
			}
			previousDeparture = departureTimes.get(i);
		}

		return maxHeadwayFound;
	}

}
