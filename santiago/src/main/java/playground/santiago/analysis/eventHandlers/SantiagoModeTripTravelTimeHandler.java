package playground.santiago.analysis.eventHandlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.pt.PtConstants;


public class SantiagoModalTripTravelTimeHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler,
		PersonStuckEventHandler, TransitDriverStartsEventHandler, ActivityStartEventHandler {
	
	private static final Logger LOGGER = Logger.getLogger(SantiagoModalTripTravelTimeHandler.class);
	private static final int MAX_STUCK_AND_ABORT_WARNINGS = 15;
	private final SortedMap<String, Map<Id<Person>, List<String>>> mode2PersonId2TravelTimes;
	private final Map<Id<Person>, Double> personId2DepartureTime;
	private int warnCount = 0;
	private final List<Id<Person>> transitDriverPersons = new ArrayList<>();
	// agents who first departs with transitWalk and their subsequent modes are stored here until it starts a regular act (home/work/leis/shop)
	private final Map<Id<Person>, List<String>> modesForTransitUsers = new HashMap<>();
	private final Map<Id<Person>, Double> transitUserDepartureTime = new HashMap<>();
	private final Map<Id<Person>, Double> transitUserArrivalTime = new HashMap<>(); 

	public SantiagoModalTripTravelTimeHandler() {
		this.mode2PersonId2TravelTimes = new TreeMap<>();
		this.personId2DepartureTime = new HashMap<>();
		LOGGER.warn("Excluding the departure and arrivals of transit drivers.");
	}

	@Override
	public void reset(int iteration) {
		this.mode2PersonId2TravelTimes.clear();
		this.personId2DepartureTime.clear();
		this.modesForTransitUsers.clear();
		this.transitDriverPersons.clear();
		this.modesForTransitUsers.clear();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Id<Person> personId = event.getPersonId();
		String legMode = event.getLegMode();
		double arrivalTime = event.getTime();

		if(transitDriverPersons.remove(personId)) {
			// exclude arrivals of transit drivers ;
		}
		else {
			if( legMode.equals(TransportMode.transit_walk) || legMode.equals(TransportMode.pt) ){
				transitUserArrivalTime.put(personId, event.getTime()); // store the arrival time
			} else { // rest of the modes
				double travelTime = arrivalTime - this.personId2DepartureTime.get(personId);
				storeData(personId, legMode, this.personId2DepartureTime.get(personId),travelTime);
				this.personId2DepartureTime.remove(personId);
			}
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Id<Person> personId = event.getPersonId();
		double deartureTime = event.getTime();
		String legMode = event.getLegMode();

		if(transitDriverPersons.contains(personId)) {
			// exclude departures of transit drivers and remove them from arrivals
		} else {
			if (legMode.equals(TransportMode.transit_walk) || legMode.equals(TransportMode.pt) ) { 
				// transit_walk - transit_walk || transit_walk - pt
				if(modesForTransitUsers.containsKey(personId)) {
					List<String> modes = modesForTransitUsers.get(personId);
					modes.add(legMode);
				} else {
					List<String> modes = new ArrayList<>();
					modes.add(legMode);
					modesForTransitUsers.put(personId, modes);
					transitUserDepartureTime.put(personId, event.getTime()); // store the departure time, when agent is left after a regular activity
				}
			} else {
				this.personId2DepartureTime.put(personId, deartureTime);
			}
		} 
	}

	/**
	 * @return  trip time for each trip of each person segregated w.r.t. travel modes.
	 */
	public SortedMap<String, Map<Id<Person>, List<String>>> getLegMode2PesonId2TripTimes (){
		return this.mode2PersonId2TravelTimes;
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.warnCount++;
		if(this.warnCount <= MAX_STUCK_AND_ABORT_WARNINGS){
			LOGGER.warn("'StuckAndAbort' event is thrown for person "+event.getPersonId()+" on link "+event.getLinkId()+" at time "+event.getTime()+
					". \n Correctness of travel time for such persons can not be guaranteed.");
			if(this.warnCount== MAX_STUCK_AND_ABORT_WARNINGS) LOGGER.warn(Gbl.FUTURE_SUPPRESSED);
		}
	}
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		transitDriverPersons.add(event.getDriverId());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if( modesForTransitUsers.containsKey(event.getPersonId()) ) {
			if(! event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE) ) { 
				List<String> modes = modesForTransitUsers.remove(event.getPersonId());
				String legMode = modes.contains(TransportMode.pt) ? TransportMode.pt : TransportMode.walk;
				double departureTime = transitUserDepartureTime.remove(event.getPersonId());
				double arrivalTime = transitUserArrivalTime.remove(event.getPersonId());
				storeData(event.getPersonId(), legMode, departureTime,arrivalTime - departureTime);
			} else { 
				// else continue
			}
		} else {
			// nothing to do
		}
	}

	private void storeData(final Id<Person> personId, final String legMode, final double startTime, final double travelTime){
		if(this.mode2PersonId2TravelTimes.containsKey(legMode)){
			Map<Id<Person>, List<String>> personId2TravelTimes = this.mode2PersonId2TravelTimes.get(legMode);
			if(personId2TravelTimes.containsKey(personId)){
				List<String> travelTimes = personId2TravelTimes.get(personId);
				travelTimes.add(String.valueOf(startTime)+"-"+String.valueOf(travelTime));
				personId2TravelTimes.put(personId, travelTimes);
			} else {
				List<String> travelTimes = new ArrayList<>();
				travelTimes.add(String.valueOf(startTime)+"-"+String.valueOf(travelTime));
				personId2TravelTimes.put(personId, travelTimes);
			}
		} else { 
			Map<Id<Person>, List<String>> personId2TravelTimes = new HashMap<>();
			List<String> travelTimes = new ArrayList<>();
			travelTimes.add(String.valueOf(startTime)+"-"+String.valueOf(travelTime));
			personId2TravelTimes.put(personId, travelTimes);
			this.mode2PersonId2TravelTimes.put(legMode, personId2TravelTimes);
		}
	}
	
}
