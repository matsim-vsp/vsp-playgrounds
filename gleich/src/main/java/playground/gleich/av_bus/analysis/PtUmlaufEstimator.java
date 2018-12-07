package playground.gleich.av_bus.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import playground.gleich.av_bus.FilePaths;

// not tested
public class PtUmlaufEstimator {

	private final Set<Id<TransitLine>> linesToAnalyse;
	private final double maxTimeBetweenFullBreaks = 90*60;
	private final double delayAllowanceRatio = 0.1;
	private final String sep = ";";
	
	Scenario scenario;
	private Map<Id<TransitStopFacility>, TerminusStop> terminusStops = new HashMap<>();
	private SortedSet<DepartureOnUmlauf> departuresUnassigned = new TreeSet<>();
	private Map<Id<Vehicle>, Umlauf> vehId2umlauf = new HashMap<>();
	private Map<Id<Vehicle>, Id<VehicleType>> vehId2type = new HashMap<>();
	private BufferedWriter bw;
	
	public static void main(String[] args) {
		String scheduleFile = FilePaths.PATH_BASE_DIRECTORY + FilePaths.PATH_TRANSIT_SCHEDULE_BERLIN_100PCT;
		String vehicleFile = FilePaths.PATH_BASE_DIRECTORY + FilePaths.PATH_TRANSIT_VEHICLES_BERLIN_100PCT_45MPS;
		String resultFile = FilePaths.PATH_BASE_DIRECTORY + "data/analysis/operationCost/umlaufEstimator_transitSchedule.100pct.base.csv";
		Set<Id<TransitLine>> linesToAnalyse = new HashSet<>();
		linesToAnalyse.add(Id.create("124-B-124", TransitLine.class));
		linesToAnalyse.add(Id.create("133-B-133", TransitLine.class));
		linesToAnalyse.add(Id.create("222-B-222", TransitLine.class));
		linesToAnalyse.add(Id.create("324-B-324", TransitLine.class));
		linesToAnalyse.add(Id.create("N22-B-922", TransitLine.class));
		linesToAnalyse.add(Id.create("N24-B-924", TransitLine.class));
		PtUmlaufEstimator analysis = new PtUmlaufEstimator(scheduleFile, vehicleFile, linesToAnalyse);
		analysis.writeResults(resultFile);
	}

	/**
	 * @param scheduleFile
	 * @param vehicleFile
	 * @param linesToAnalyse
	 */
	PtUmlaufEstimator(String scheduleFile, String vehicleFile, Set<Id<TransitLine>> linesToAnalyse) {
		this.linesToAnalyse = linesToAnalyse;
		
		Config config = ConfigUtils.createConfig();
		config.transit().setTransitScheduleFile(scheduleFile);
		config.transit().setVehiclesFile(vehicleFile);
		scenario = ScenarioUtils.loadScenario(config);
		
		findVehicleTypes();
		findDeparturesAndTerminusStops();
		buildUmlaufe();
	}

	/*
	 * Some arbitrary assumptions: In order to cater for delays, a break of 
	 * waitTimeAtNextFullBreak = delayAllowanceRatio * operationTimeSinceLastFullBreak is necessary at the terminus. 
	 * Shorter breaks are allowed as long as there is a full break compensating for that at least every 
	 * maxTimeBetweenFullBreaks seconds.
	 */
	private class Umlauf {
		private final Id<Vehicle> veh;
		private final Id<VehicleType> vehType;
		private List<DepartureOnUmlauf> departures = new ArrayList<>();
		private double endTimeOfLastFullBreak = 0;
		private double waitTimeAtNextFullBreak = 0;

		Umlauf(Id<Vehicle> veh, Id<VehicleType> vehType) {
			this.veh = veh;
			this.vehType = vehType;
		}

		List<DepartureOnUmlauf> getDepartures() {
			return departures;
		}

		// if maxTimeBetweenFullBreaks < travel time between first and last stop, the departure can never be added
		boolean addDepartureIfPossible(DepartureOnUmlauf departure) {
			double breakTimeAtLastTerminus;
			if (departures.size() > 0) {
				breakTimeAtLastTerminus = departure.departureTimeAtFirstStop - 
						departures.get(departures.size() - 1).departureTimeAtLastStop;
			} else {
				// first departure
				breakTimeAtLastTerminus = 0;
			}
			double delayAllowanceForThisDeparture = delayAllowanceRatio * 
					(departure.departureTimeAtLastStop - departure.departureTimeAtFirstStop);
			if (breakTimeAtLastTerminus >= waitTimeAtNextFullBreak) {
				// add departure after full break
				departures.add(departure);
				waitTimeAtNextFullBreak = delayAllowanceForThisDeparture;
				endTimeOfLastFullBreak = departure.departureTimeAtFirstStop;
				return true;
			} else if (departure.departureTimeAtLastStop - endTimeOfLastFullBreak <= maxTimeBetweenFullBreaks) {
				// add departure after partial break
				waitTimeAtNextFullBreak = waitTimeAtNextFullBreak + 
						delayAllowanceForThisDeparture - breakTimeAtLastTerminus;						
				return true;
			} else {
				// departure can not be added
				return false;
			}
		}
		
		double getNextPossibleDepartureTime() {
			return departures.get(departures.size() - 1).departureTimeAtLastStop;
		}
	}
	
	private class DepartureOnUmlauf implements Comparable<DepartureOnUmlauf> {
		private Id<Vehicle> umlauf;
		private final Departure departure;
		final Id<VehicleType> vehType;
		private final Id<TransitLine> lineId;
		private final Id<TransitRoute> routeId;
		private final Id<TransitStopFacility> firstStop;
		private final Id<TransitStopFacility> lastStop;
		private final double departureTimeAtFirstStop;
		private final double departureTimeAtLastStop;
		/**
		 * @param departure
		 * @param line
		 * @param route
		 * @param arrivalTimeAtFirstStop
		 * @param departureTimeAtLastStop
		 */
		DepartureOnUmlauf(Departure departure, Id<TransitLine> lineId, TransitRoute route) {
			this.departure = departure;
			this.lineId = lineId;
			this.routeId = route.getId();
			vehType = vehId2type.get(departure.getVehicleId());
			firstStop = findStopGroup(route.getStops().get(0).getStopFacility().getId());
			lastStop = findStopGroup(route.getStops().get(route.getStops().size() - 1).getStopFacility().getId());
			this.departureTimeAtFirstStop = departure.getDepartureTime();
			this.departureTimeAtLastStop = route.getStops().get(route.getStops().size() - 1).getDepartureOffset();
		}

		void setUmlauf(Id<Vehicle> umlauf) {
			if (this.umlauf == null) {				
				this.umlauf = umlauf;
			} else {
				throw new RuntimeException("DepartureOnUmlauf with departure id " + departure.getId() + 
						" is already assigned to Umlauf " + this.umlauf + " and cannot be assigned to " + umlauf);
			}
		}

		@Override
		public int compareTo(DepartureOnUmlauf o) {
			return Double.compare(this.departureTimeAtFirstStop, o.departureTimeAtFirstStop);
		}
	}
	
	private class TerminusStop {
		final Id<TransitStopFacility> id;
		Map<Id<VehicleType>, List<Umlauf>> vehType2incomingUmlauf = new HashMap<>();

		/**
		 * @param id
		 */
		TerminusStop(Id<TransitStopFacility> id) {
			this.id = id;
		}
	}
	
	private void findVehicleTypes() {
		for (Vehicle veh: scenario.getTransitVehicles().getVehicles().values()) {
			vehId2type.put(veh.getId(), veh.getType().getId());
		}
	}

	private void findDeparturesAndTerminusStops() {
		int i = 0;
		for (Id<TransitLine> lineId: linesToAnalyse) {
			for (TransitRoute route: scenario.getTransitSchedule().getTransitLines().get(lineId).getRoutes().values()) {
				Id<TransitStopFacility> firstStop = findStopGroup(route.getStops().get(0).getStopFacility().getId());
				if (! terminusStops.containsKey(firstStop)) {					
					terminusStops.put(firstStop, new TerminusStop(firstStop));
				}
				Id<TransitStopFacility> lastStop = findStopGroup(route.getStops().get(0).getStopFacility().getId());
				if (! terminusStops.containsKey(lastStop)) {					
					terminusStops.put(lastStop, new TerminusStop(lastStop));
				}
				for (Departure depSchedule: route.getDepartures().values()) {
					DepartureOnUmlauf depUmlauf = new DepartureOnUmlauf(depSchedule, lineId, route);
					departuresUnassigned.add(depUmlauf);
					// initialize vehType at TerminusStops
					if (! terminusStops.get(firstStop).vehType2incomingUmlauf.containsKey(depUmlauf.vehType)) {					
						terminusStops.get(firstStop).vehType2incomingUmlauf.put(depUmlauf.vehType, new ArrayList<>());
					}
					if (! terminusStops.get(lastStop).vehType2incomingUmlauf.containsKey(depUmlauf.vehType)) {					
						terminusStops.get(lastStop).vehType2incomingUmlauf.put(depUmlauf.vehType, new ArrayList<>());
					}
					i++;
				}
			}
		}
		System.out.println(i);
	}

	/**
	 * Some TransitStops are divided into several sub-TransitStops, so a pt vehicle that turns 
	 * around / terminates there arrives at one TransitStop and departs at another TransitStop, 
	 * even though these stops are basically located at the same place and have the same name. 
	 * Therefore look for the part of the id that all these connected TransitStops have in
	 * common. For the Berlin Scenario that is the part before the dot. 
	 * 
	 * @param id of a TransitStopFacility
	 * @return part of the id before the separator "."
	 */
	private Id<TransitStopFacility> findStopGroup(Id<TransitStopFacility> id) {
		return Id.create(id.toString().split("\\.")[0], TransitStopFacility.class);
	}

	// Check available Umlauf starting with erliest available one
	private class UmlaufComparator implements Comparator<Umlauf> {

		@Override
		public int compare(Umlauf o1, Umlauf o2) {
			int result = Double.compare(o1.getNextPossibleDepartureTime(), o2.getNextPossibleDepartureTime());
			if (result == 0) {
				return o1.veh.compareTo(o2.veh);
			}
			return result;
		}
		
	}
	
	private void buildUmlaufe() {
		// Process all unassigned departures in a chronological order
		while (! departuresUnassigned.isEmpty()) {
			DepartureOnUmlauf departure = departuresUnassigned.first();
			departuresUnassigned.remove(departure);
			
			List<Umlauf> availableUmlauf = 
					terminusStops.get(departure.firstStop).vehType2incomingUmlauf.get(departure.vehType);
			boolean departureAssigned = false;
			
			Collections.sort(availableUmlauf, new UmlaufComparator());
			for (int i = 0; i < availableUmlauf.size(); i++) {
				// Check if the Umlauf considered can be assigned to the departure and if so add the departure to the Umlauf 
				departureAssigned = availableUmlauf.get(i).addDepartureIfPossible(departure);
				if (departureAssigned) {
					// and add it to the terminus stop of this departure
					terminusStops.get(departure.lastStop).vehType2incomingUmlauf.get(departure.vehType).add(availableUmlauf.get(i));
					departure.setUmlauf(availableUmlauf.get(i).veh);
				}
			}
			if (! departureAssigned) {
				// no suitable Umlauf could be found -> add new Umlauf
				Id<Vehicle> vehId = departure.departure.getVehicleId();
				Umlauf umlauf = new Umlauf(vehId, vehId2type.get(vehId));
				vehId2umlauf.put(vehId, umlauf);
				umlauf.addDepartureIfPossible(departure);
				// and add it to the terminus stop of this departure
				System.out.println(departure.vehType);
				for(Id<VehicleType> type: terminusStops.get(departure.lastStop).vehType2incomingUmlauf.keySet()) {System.out.print(type + " ");}
				System.out.println(terminusStops.get(departure.lastStop).id);
				System.out.println(terminusStops.get(departure.lastStop)
				.vehType2incomingUmlauf.size());
				System.out.println(terminusStops.get(departure.lastStop)
				.vehType2incomingUmlauf.get(departure.vehType).size());
				terminusStops.get(departure.lastStop)
				.vehType2incomingUmlauf.get(departure.vehType)
				.add(umlauf);
			}
		}
		
	}

	public void writeResults(String file) {
		try {
			bw = IOUtils.getBufferedWriter(file);
			// write header
			bw.write("vehicleId" + sep + "firstStop" + sep + "departureTimeFirstStop" + sep + "lineId" + sep + "routeId" + sep +
					"departureId" + sep + "departureTimeLastStop" + sep + "lastStop");
			bw.newLine();

			for (Umlauf u: vehId2umlauf.values()) {
				bw.write(u.veh.toString());
				for (DepartureOnUmlauf depUmlauf: u.departures) {
					bw.write(sep + depUmlauf.firstStop.toString() + sep + depUmlauf.departureTimeAtFirstStop + sep + 
							depUmlauf.lineId.toString() + sep + depUmlauf.routeId.toString() + sep + depUmlauf.departure.getId().toString() +
							sep + depUmlauf.departureTimeAtLastStop + sep + depUmlauf.lastStop.toString());
				}
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}
}
