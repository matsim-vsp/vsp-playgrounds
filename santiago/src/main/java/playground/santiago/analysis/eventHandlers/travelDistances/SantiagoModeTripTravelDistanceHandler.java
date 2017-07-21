package playground.santiago.analysis.eventHandlers.travelDistances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import playground.santiago.analysis.travelDistances.SantiagoPTDistanceFromPlans;



/**
 * Handler to write the distance traveled by the agents. It should not be used for "pt" legs (or any other mode with an schedule file), as it doesn't 
 * compute the in-vehicle distances. The pt and transit legs are handled separately based on the plans file (only for simplicity), see
 * SantiagoPTDistanceFromPlans.
 * 1) Two categories: congested modes and teleported modes (only departure and arrival events).
 */
public class SantiagoModeTripTravelDistanceHandler
implements PersonDepartureEventHandler, LinkLeaveEventHandler, PersonArrivalEventHandler,
TeleportationArrivalEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, TransitDriverStartsEventHandler {
	private final static Logger LOG = Logger.getLogger(SantiagoModeTripTravelDistanceHandler.class);

	private final Network network;
	private final Config config;
	private final SortedMap<String, Map<Id<Person>, List<String>>> modePersonIdDistances = new TreeMap<>();

	private final SortedMap<String, Map<Id<Person>, Double>> mainModePersonIdDistTemporal = new TreeMap<>();
	private final SortedMap<String, Map<Id<Person>, Double>> teleportedModePersonIdDistTemporal = new TreeMap<>();
	private final Map<Id<Person>, Double> personIdDepartureTime = new HashMap<>();
	private final List<String> mainModes = new ArrayList<>();
	private final Map<Id<Person>, String> personIdDepartureLegModes = new HashMap<>();
	private double maxDist = Double.NEGATIVE_INFINITY;
	private final SortedMap<String, Double> modeNumberOfLegs = new TreeMap<>();

	private final Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();
	private final SantiagoPTDistanceFromPlans delegatePT;
	
	/*Ignoring transit drivers & stuck agents - Same as SantiagoModeTripTravelTimeHandler*/
	private final List<Id<Person>> transitDriverPersons = new ArrayList<>();
	private List<Id<Person>> stuckAgents = new ArrayList<>();



	public SantiagoModeTripTravelDistanceHandler(final Config config, final Network network, final Population population, 
			List<Id<Person>> stuckAgents){
		LOG.info("Route distance will be calculated based on events.");
		LOG.warn("During distance calculation, link from which person is departed or arrived will not be considered.");
		this.delegatePT = new SantiagoPTDistanceFromPlans(population);
		this.config=config;
		this.mainModes.addAll(this.config.qsim().getMainModes());
		this.network = network;
		this.stuckAgents=stuckAgents;

	}

	@Override
	public void reset(int iteration) {
		this.modePersonIdDistances.clear();
		this.mainModePersonIdDistTemporal.clear();
		this.teleportedModePersonIdDistTemporal.clear();
		this.personIdDepartureTime.clear();
		this.mainModes.clear();
		this.personIdDepartureLegModes.clear();		
		this.modeNumberOfLegs.clear();

		this.transitDriverPersons.clear();

	}

	/*LEAVING LINK EVENT*/
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Person> personId = this.delegate.getDriverOfVehicle(event.getVehicleId());
		Id<Link> linkId = event.getLinkId();
		// TODO if a person is in more than two groups, then which one is correct mode ?
		String mode = this.personIdDepartureLegModes.get(personId);

		if(transitDriverPersons.contains(personId)||stuckAgents.contains(personId)) {
			//Omitting...
		} else {
			Map<Id<Person>, Double> personDist = mainModePersonIdDistTemporal.get(mode);
			double distSoFar = personDist.get(personId);
			double distNew = distSoFar+ network.getLinks().get(linkId).getLength();
			personDist.put(personId, distNew);
		}
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		transitDriverPersons.add(event.getDriverId());
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.delegate.handleEvent(event);
	}
	/*DEPARTURE EVENT*/
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		String departureLegMode = event.getLegMode();
		Id<Person> personId = event.getPersonId();

		double departureTime = event.getTime();
		this.personIdDepartureLegModes.put(personId, departureLegMode);

		if(transitDriverPersons.contains(personId)||stuckAgents.contains(personId)){
			//Omitting
		} else {		
			if(departureLegMode.equals(TransportMode.transit_walk)||departureLegMode.equals(TransportMode.pt)){
				//Omitting...
			} else {
				this.personIdDepartureTime.put(personId, departureTime);		
				if(mainModes.contains(departureLegMode)){
					//initialize "main-modes" distance map
					if(mainModePersonIdDistTemporal.containsKey(departureLegMode)){
						Map<Id<Person>, Double> personIdDist = mainModePersonIdDistTemporal.get(departureLegMode);
						if(!personIdDist.containsKey(personId)){
							personIdDist.put(personId, 0.0);
						} else {
							LOG.warn("Person is departing again.");
						}
					} else {
						Map<Id<Person>, Double> personIdDist = new TreeMap<>();
						personIdDist.put(personId, 0.0);
						mainModePersonIdDistTemporal.put(departureLegMode, personIdDist);
					}
				} else {
					//initialize "teleporation-mode" distance map
					if(teleportedModePersonIdDistTemporal.containsKey(departureLegMode)){
						Map<Id<Person>, Double> personIdDist = teleportedModePersonIdDistTemporal.get(departureLegMode);
						if(!personIdDist.containsKey(personId)){
							personIdDist.put(personId, 0.0);
						} else {
							LOG.warn("Person is departing again.");
						}
					} else {
						Map<Id<Person>, Double> personIdDist = new TreeMap<>();
						personIdDist.put(personId, 0.0);
						teleportedModePersonIdDistTemporal.put(departureLegMode, personIdDist);
					}

				}
				//initialize distances map
				//TODO:Trying omitting PT and transit_walk legs*/
				if(departureLegMode.equals(TransportMode.transit_walk)||departureLegMode.equals(TransportMode.pt)){
					//Omitting...
				} else {
					if(modePersonIdDistances.containsKey(departureLegMode)){
						Map<Id<Person>, List<String>> personIdDists = modePersonIdDistances.get(departureLegMode);
						if(!personIdDists.containsKey(personId)){
							personIdDists.put(personId, new ArrayList<>());
						}
					} else {
						Map<Id<Person>, List<String>> personIdDists = new TreeMap<>();
						personIdDists.put(personId, new ArrayList<>());
						modePersonIdDistances.put(departureLegMode, personIdDists);
					}
				}
			}
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		String arrivalLegMode = event.getLegMode();
		Id<Person> personId = event.getPersonId();

		if (transitDriverPersons.remove(personId)||stuckAgents.contains(personId)){
			//Omitting...
		} else {

			if(arrivalLegMode.equals(TransportMode.transit_walk)||arrivalLegMode.equals(TransportMode.pt)){
				//Omitting...
			} else {
				if(!arrivalLegMode.equals(this.personIdDepartureLegModes.get(personId))) throw new RuntimeException("Person is departing and arriving with different travel modes. Can not happen.");

				Map<Id<Person>, List<String>> personIdDists = modePersonIdDistances.get(arrivalLegMode);
				if(mainModes.contains(arrivalLegMode)){
					if(personIdDists.containsKey(personId) ){
						List<String> dists = personIdDists.get(personId); // it might happen, person is departing and arriving on same link.
						double tripDist = mainModePersonIdDistTemporal.get(arrivalLegMode).get(personId);
						if(maxDist<tripDist) maxDist = tripDist;
						double departureTime = 	personIdDepartureTime.remove(personId);
						dists.add(String.valueOf(departureTime)+"-"+String.valueOf(tripDist));
						personIdDists.put(personId, dists);
						mainModePersonIdDistTemporal.get(arrivalLegMode).remove(personId);

					} else throw new RuntimeException("Person is not registered in the map and still arriving. This can not happen.");
				} else {
					List<String> dists = personIdDists.get(personId);
					double tripDist = teleportedModePersonIdDistTemporal.get(arrivalLegMode).get(personId);
					double departureTime = personIdDepartureTime.remove(personId);
					if(maxDist<tripDist) maxDist = tripDist;
					dists.add(String.valueOf(departureTime)+"-"+String.valueOf(tripDist));
					personIdDists.put(personId, dists);
					teleportedModePersonIdDistTemporal.get(arrivalLegMode).remove(personId);
				}
			}
		}
	}
	
	public SortedMap<String,Map<Id<Person>,List<String>>> getPT2PersonId2TravelDistances(List<Id<Person>> stuckAgents){
		return this.delegatePT.getPt2PersonId2TravelDistances(stuckAgents);
	}
	
	public SortedMap<String,Map<Id<Person>,List<String>>> getMode2PersonId2TravelDistances (){
//		SortedMap <String,Map<Id<Person>,List<String>>> Pt2PersonId2TravelDistances = getPT2PersonId2TravelDistances();
//		modePersonIdDistances.putAll(Pt2PersonId2TravelDistances);
		return this.modePersonIdDistances;
	}


	public double getLongestDistance(){
		return maxDist;
	}

	@Override
	public void handleEvent(TeleportationArrivalEvent event) {
		Id<Person> personId = event.getPersonId();
		String mode = this.personIdDepartureLegModes.get(personId);
//		System.out.println(personId + "-" + mode);
		// TODO if a person is in more than two groups, then which one is correct mode ?
		if(mode.equals(TransportMode.transit_walk)||mode.equals(TransportMode.pt)){
			//Omitting...
		} else {
			Map<Id<Person>, Double> personDist = teleportedModePersonIdDistTemporal.get(mode);
			double teleportDist = event.getDistance();
			personDist.put(personId, teleportDist);
		}
	}





}
