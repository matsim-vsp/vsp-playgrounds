package playground.santiago.analysis.eventHandlers;

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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import playground.agarwalamit.analysis.tripDistance.TripDistanceHandler;
import playground.sebhoerl.mexec.ConfigUtils;

public class SantiagoModeTripTravelDistanceHandler
		implements PersonDepartureEventHandler, LinkLeaveEventHandler, PersonArrivalEventHandler,
		TeleportationArrivalEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
	private final static Logger LOG = Logger.getLogger(SantiagoModeTripTravelDistanceHandler.class);

	private final Network network;
	private final Config config;
	private final SortedMap<String, Map<Id<Person>, List<String>>> mode2PersonId2Distances = new TreeMap<>();
	
	private final SortedMap<String, Map<Id<Person>, Double>> mode2PersonId2OneTripdist = new TreeMap<>();
	private final SortedMap<String, Map<Id<Person>, Double>> mode2PersonId2TeleportDist = new TreeMap<>();
	private final Map<Id<Person>, Double> personIdDepartureTime = new HashMap<>();
	private final List<String> mainModes = new ArrayList<>();
	private final Map<Id<Person>, String> personId2LegModes = new HashMap<>();
	private double maxDist = Double.NEGATIVE_INFINITY;
	private final SortedMap<String, Double> mode2NumberOfLegs = new TreeMap<>();

	private final Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

	public SantiagoModeTripTravelDistanceHandler(final Config config, final Network network){
		LOG.info("Route distance will be calculated based on events.");
		LOG.warn("During distance calculation, link from which person is departed or arrived will not be considered.");
		this.config=config;
		this.mainModes.addAll(this.config.qsim().getMainModes());
		this.network = network;
		

	}

	@Override
	public void reset(int iteration) {
		this.mode2NumberOfLegs.clear();
		this.mode2PersonId2OneTripdist.clear();
		this.mode2PersonId2TeleportDist.clear();
		this.mainModes.clear();
		this.personId2LegModes.clear();
		this.mode2NumberOfLegs.clear();
	}

	/*LEAVING LINK EVENT*/
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Person> personId = this.delegate.getDriverOfVehicle(event.getVehicleId());
		Id<Link> linkId = event.getLinkId();
		// TODO if a person is in more than two groups, then which one is correct mode ?
		String mode = this.personId2LegModes.get(personId);
		
		Map<Id<Person>, Double> person2Dist = mode2PersonId2OneTripdist.get(mode);
		double distSoFar = person2Dist.get(personId);
		double distNew = distSoFar+ network.getLinks().get(linkId).getLength();
		person2Dist.put(personId, distNew);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		String legMode = event.getLegMode();
		Id<Person> personId = event.getPersonId();
		
		double departureTime = event.getTime();
		this.personIdDepartureTime.put(personId, departureTime);
		
		this.personId2LegModes.put(personId, legMode);
		
		if(mainModes.contains(legMode)){
			//initialize one trip distance map
			if(mode2PersonId2OneTripdist.containsKey(legMode)){
				Map<Id<Person>, Double> personId2Dist = mode2PersonId2OneTripdist.get(legMode);
				if(!personId2Dist.containsKey(personId)){
					personId2Dist.put(personId, 0.0);
				} else {
					LOG.warn("Person is departing again.");
				}
			} else {
				Map<Id<Person>, Double> personId2Dist = new TreeMap<>();
				personId2Dist.put(personId, 0.0);
				mode2PersonId2OneTripdist.put(legMode, personId2Dist);
			}
		} else {
			//initialize teleporation dist map
			if(mode2PersonId2TeleportDist.containsKey(legMode)){
				Map<Id<Person>, Double> personId2Dist = mode2PersonId2TeleportDist.get(legMode);
				if(!personId2Dist.containsKey(personId)){
					personId2Dist.put(personId, 0.0);
				} else {
					LOG.warn("Person is departing again.");
				}
			} else {
				Map<Id<Person>, Double> personId2Dist = new TreeMap<>();
				personId2Dist.put(personId, 0.0);
				mode2PersonId2TeleportDist.put(legMode, personId2Dist);
			}
		}
		//initialize distances map
		if(mode2PersonId2Distances.containsKey(legMode)){
			Map<Id<Person>, List<String>> personId2Dists = mode2PersonId2Distances.get(legMode);
			if(!personId2Dists.containsKey(personId)){
				personId2Dists.put(personId, new ArrayList<>());
			}
		} else {
			Map<Id<Person>, List<String>> personId2Dists = new TreeMap<>();
			personId2Dists.put(personId, new ArrayList<>());
			mode2PersonId2Distances.put(legMode, personId2Dists);
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		String travelMode = event.getLegMode();
		Id<Person> personId = event.getPersonId();
		if(!travelMode.equals(this.personId2LegModes.get(personId))) throw new RuntimeException("Person is leaving and arriving with different travel modes. Can not happen.");

		Map<Id<Person>, List<String>> personId2Dists = mode2PersonId2Distances.get(travelMode);
		if(mainModes.contains(travelMode)){
			if(personId2Dists.containsKey(personId) ){
				List<String> dists = personId2Dists.get(personId); // it might happen, person is departing and arriving on same link.
				double tripDist = mode2PersonId2OneTripdist.get(travelMode).get(personId);
				if(maxDist<tripDist) maxDist = tripDist;
				double departureTime = 	personIdDepartureTime.remove(personId);
				dists.add(String.valueOf(departureTime)+"-"+String.valueOf(tripDist));
				personId2Dists.put(personId, dists);
				mode2PersonId2OneTripdist.get(travelMode).remove(personId);

			} else throw new RuntimeException("Person is not registered in the map and still arriving. This can not happen.");
		} else {
			List<String> dists = personId2Dists.get(personId);
			double tripDist = mode2PersonId2TeleportDist.get(travelMode).get(personId);
			double departureTime = personIdDepartureTime.remove(personId);
			if(maxDist<tripDist) maxDist = tripDist;
			dists.add(String.valueOf(departureTime)+"-"+String.valueOf(tripDist));
			personId2Dists.put(personId, dists);
			mode2PersonId2TeleportDist.get(travelMode).remove(personId);
		}
	}

	public SortedMap<String,Map<Id<Person>,List<String>>> getMode2PersonId2TravelDistances (){
		return this.mode2PersonId2Distances;
	}

	public SortedSet<String> getUsedModes (){
		SortedSet<String> modes = new TreeSet<>();
		modes.addAll(mode2PersonId2Distances.keySet());
		return modes;
	}

	public double getLongestDistance(){
		return maxDist;
	}

	@Override
	public void handleEvent(TeleportationArrivalEvent event) {
		Id<Person> personId = event.getPersonId();
		String mode = this.personId2LegModes.get(personId);
		// TODO if a person is in more than two groups, then which one is correct mode ?
		Map<Id<Person>, Double> person2Dist = mode2PersonId2TeleportDist.get(mode);
		double teleportDist = event.getDistance();
		person2Dist.put(personId, teleportDist);
	}




}
