package playground.gleich.analysis.pt;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

public class PtVehicleOperationCostAnalysis {
	
	Scenario scenario;

	private final String networkFile = "/home/gregor/git/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz";
	private final String scheduleFile = "/home/gregor/git/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule--measurePeriodHH16.5-17-shareStops0.5-minDepsAnaStop20-maxHeadwayMM1.xml.gz";
	private static final String resultFileRoutes = "/home/gregor/git/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule--measurePeriodHH16.5-17-shareStops0.5-minDepsAnaStop20-maxHeadwayMM1.perRoute.csv";
	private static final String resultFileLines = "/home/gregor/git/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-transit-schedule--measurePeriodHH16.5-17-shareStops0.5-minDepsAnaStop20-maxHeadwayMM1.perLine.csv";
	
	private final double costPerM = 0.0023;
	private final double costPerS = 0.0;
	
	Map<Id<TransitRoute>, Id<TransitLine>> route2line = new HashMap<>();
	Map<Id<TransitRoute>, Double> route2length = new HashMap<>();
	Map<Id<TransitRoute>, Double> route2time = new HashMap<>();
	Map<Id<TransitRoute>, Integer> route2numDepartures = new HashMap<>();
	Map<Id<TransitRoute>, Double> route2cost = new HashMap<>();
	
	private String sep = ";";
	private BufferedWriter bw;

	public static void main(String[] args) {
		PtVehicleOperationCostAnalysis analysis = new PtVehicleOperationCostAnalysis();
		analysis.run();
		analysis.writeResultsPerTransitRoute(resultFileRoutes);
		analysis.writeResultsPerTransitLine(resultFileLines);
	}


	public void run() {
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		new TransitScheduleReader(scenario).readFile(scheduleFile);
		
		for (TransitLine line: scenario.getTransitSchedule().getTransitLines().values()) {
				processTransitLine(line);
		}
	}
	
	private void processTransitLine(TransitLine line) {
		for (TransitRoute route: line.getRoutes().values()) {
			double length = calculateLength(route);
			double time = route.getStops().get(route.getStops().size() - 1).getDepartureOffset().seconds() -
					route.getStops().get(0).getArrivalOffset().seconds();
			int numDepartures = route.getDepartures().size();
			double cost = length * numDepartures * costPerM + time * numDepartures * costPerS;
			route2line.put(route.getId(), line.getId());
			route2length.put(route.getId(), length);
			route2time.put(route.getId(), time);
			route2numDepartures.put(route.getId(), numDepartures);
			route2cost.put(route.getId(), cost);
		}
	}

	/** 
	 * Sums up the whole link length no matter where exactly on this link the TransitStop is located.
	 * However, at last on the way back, the transit vehicle has to drive the rest of the link.
	 * So the length per TransitRoute might be slightly inexact, however as long as every vehicle travels back
	 * on another TransitRoute to the same start TransitStop, the total length travelled should be the same
	 * as if the exact position of the stop on the link would have been taken into account.
	 * 
	 * NetworkRoute.getDistance() and NetworkRoute.getTravelCost() produce only Nans.
	 */
	private double calculateLength(TransitRoute route) {
		/* Start and end link ids are missing in route.getRoute().getLinkIds()! Add manually */
		double length = scenario.getNetwork().getLinks().get(route.getRoute().getStartLinkId()).getLength();
		for (Id<Link> link: route.getRoute().getLinkIds()) {
			length += scenario.getNetwork().getLinks().get(link).getLength();
		}
		length += scenario.getNetwork().getLinks().get(route.getRoute().getEndLinkId()).getLength();
		return length;
	}
	
	public void writeResultsPerTransitRoute(String file) {
		try {
			bw = IOUtils.getBufferedWriter(file);
			// write header
			bw.write("transitLineId" + sep + "transitRouteId" + sep + "length" + sep + "timeDriven" + sep + "numDepartures" + sep +
					"cost");
			bw.newLine();
			for (TransitLine line: scenario.getTransitSchedule().getTransitLines().values()) {
				for (TransitRoute route: line.getRoutes().values()) {
					bw.write(route2line.get(route.getId()).toString() + sep + route.getId().toString() + sep +
							route2length.get(route.getId()) + sep + route2time.get(route.getId()) + sep + 
							route2numDepartures.get(route.getId()) + sep + route2cost.get(route.getId()));
					bw.newLine();
				}
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}
	
	public void writeResultsPerTransitLine(String file) {
		try {
			bw = IOUtils.getBufferedWriter(file);
			// write header
			bw.write("transitLineId" + sep + "maxLength" + sep + "maxTimeDriven" + sep + "sumDepartures" + sep +
					"sumDistanceDriven" + sep + "sumTimeDriven" + sep + "sumCost");
			bw.newLine();

			for (TransitLine line: scenario.getTransitSchedule().getTransitLines().values()) {
				double maxLength = 0;
				double maxTimeDriven = 0;
				int sumDepartures = 0;
				double sumDistanceDriven = 0;
				double sumTimeDriven = 0;
				double sumCost = 0;
				for (TransitRoute route: line.getRoutes().values()) {
					if (route2length.get(route.getId()) > maxLength) {
						maxLength = route2length.get(route.getId());
					}
					if (route2time.get(route.getId()) > maxTimeDriven) {
						maxTimeDriven = route2time.get(route.getId());
					}
					sumDepartures += route2numDepartures.get(route.getId());
					sumDistanceDriven += route2numDepartures.get(route.getId()) * route2length.get(route.getId());
					sumTimeDriven += route2numDepartures.get(route.getId()) * route2time.get(route.getId());
					sumCost += route2cost.get(route.getId());
				}
				bw.write(line.getId().toString() + sep + maxLength + sep + maxTimeDriven + sep + sumDepartures +
						sep + sumDistanceDriven + sep + sumTimeDriven + sep + sumCost);
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}
}
