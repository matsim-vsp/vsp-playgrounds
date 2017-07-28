package playground.santiago.analysis.eventHandlers.travelTimes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.matsim.pt.PtConstants;


/**
 * Handler to write the departures and travel times of each trip of every person by leg mode. 
 * It excludes the transitDriverPersons. Transit users are treated separately, see followings
 * transit_walk - transit_walk --> walk &
 * transit_walk - pt - transit_walk --> pt &
 * transit_walk - pt - transit_walk - pt - transit_walk --> pt
 * 
 * This means that the handler calculate the travel times only between regular activities. The stuck agents are omitted.
 */

public class SantiagoModeTripTravelTimeHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, TransitDriverStartsEventHandler, ActivityStartEventHandler {

	private static final Logger LOGGER = Logger.getLogger(SantiagoModeTripTravelTimeHandler.class);
	private final SortedMap<String, Map<Id<Person>, List<String>>> modePersonIdDepartureTravelTimes;
	private final Map<Id<Person>, Double> personIdDepartureTime;
	

//	private final SantiagoStuckAndAbortEventHandler delegateStuck = new SantiagoStuckAndAbortEventHandler();
	private final List<Id<Person>> transitDriverPersons = new ArrayList<>();
	private List<Id<Person>> stuckAgents = new ArrayList<>();



	private final Map<Id<Person>, Double> transitUserDepartureTime = new HashMap<>();
	private final Map<Id<Person>, Double> transitUserArrivalTime = new HashMap<>();	
	private final Map<Id<Person>, List<String>> modesForTransitUsers = new HashMap<>();



	public SantiagoModeTripTravelTimeHandler(List<Id<Person>> stuckAgents) {
		this.modePersonIdDepartureTravelTimes = new TreeMap<>();
		this.personIdDepartureTime = new HashMap<>();
		LOGGER.warn("Excluding the departure and arrivals of transit drivers. Excluding stuck agents");		
		this.stuckAgents = stuckAgents;
	}

	@Override
	public void reset(int iteration) {
		this.modePersonIdDepartureTravelTimes.clear();
		this.personIdDepartureTime.clear();
		
		this.transitDriverPersons.clear();
		this.transitUserDepartureTime.clear();
		this.transitUserArrivalTime.clear();
		this.modesForTransitUsers.clear();
		
	}

	/*ARRIVAL EVENT*/	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Id<Person> personId = event.getPersonId();
		String legMode = event.getLegMode();
		double arrivalTime = event.getTime();

		if(transitDriverPersons.remove(personId)||stuckAgents.contains(personId)) {
			//Omitting transitDrivers and stuckAgents.
		} else {
			if( legMode.equals(TransportMode.transit_walk) || legMode.equals(TransportMode.pt) ){
				transitUserArrivalTime.put(personId, event.getTime());
			} else {		
				double departureTime = this.personIdDepartureTime.get(personId);
				double travelTime = arrivalTime - departureTime;
				storeData(personId, legMode, departureTime, travelTime);
				this.personIdDepartureTime.remove(personId);				
			}
		}
	}

	/*DEPARTURE EVENT*/
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Id<Person> personId = event.getPersonId();
		double departureTime = event.getTime();
		String legMode = event.getLegMode();

		if(transitDriverPersons.contains(personId)||stuckAgents.contains(personId)) {
			//Omitting transitDrivers and stuckAgents...
		} else {
			if (legMode.equals(TransportMode.transit_walk) || legMode.equals(TransportMode.pt) ) {

				if(modesForTransitUsers.containsKey(personId)) {
					List<String> modes = modesForTransitUsers.get(personId);
					modes.add(legMode);
				} else {
					List<String> modes = new ArrayList<>();
					modes.add(legMode);
					modesForTransitUsers.put(personId, modes);
					transitUserDepartureTime.put(personId, event.getTime());
				}
			} else {
				this.personIdDepartureTime.put(personId, departureTime);
			}
		} 
	}

	/**
	 * @return  trip time for each trip of each person segregated w.r.t. travel modes.
	 */
	public SortedMap<String, Map<Id<Person>, List<String>>> getLegModePesonIdTripDepartureTravelTimes (){
		return this.modePersonIdDepartureTravelTimes;
	}



	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		transitDriverPersons.add(event.getDriverId());
	}

	/*ACTIVITY START EVENT*/
	@Override
	public void handleEvent(ActivityStartEvent event) {
		Id<Person> personId = event.getPersonId();
		if( modesForTransitUsers.containsKey(personId) ) {
			if(! event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE) ) { //Si la persona llega a una actividad regular.
				List<String> modes = modesForTransitUsers.remove(event.getPersonId()); //Se borra de la lista de modos acumulados previo a una actividad regular
				String legMode = modes.contains(TransportMode.pt) ? TransportMode.pt : TransportMode.walk;
				double departureTime = transitUserDepartureTime.remove(event.getPersonId());
				double arrivalTime = transitUserArrivalTime.remove(event.getPersonId());
				storeData(personId, legMode, departureTime, arrivalTime - departureTime);
			} else { 
				// else continue
			}
		} else {
			// nothing to do
		}
	}

	private void storeData(final Id<Person> personId, final String legMode, final double departureTime, final double travelTime){
		if(this.modePersonIdDepartureTravelTimes.containsKey(legMode)){
			Map<Id<Person>, List<String>> personIdDepartureTravelTimes = this.modePersonIdDepartureTravelTimes.get(legMode);
			if(personIdDepartureTravelTimes.containsKey(personId)){
				List<String> departureTravelTimes = personIdDepartureTravelTimes.get(personId);
				departureTravelTimes.add(String.valueOf(departureTime)+"-"+String.valueOf(travelTime));
				personIdDepartureTravelTimes.put(personId, departureTravelTimes);
			} else {
				List<String> departureTravelTimes = new ArrayList<>();
				departureTravelTimes.add(String.valueOf(departureTime)+"-"+String.valueOf(travelTime));
				personIdDepartureTravelTimes.put(personId, departureTravelTimes);
			}
		} else { 
			Map<Id<Person>, List<String>> personIdDepartureTravelTimes = new HashMap<>();
			List<String> departureTravelTimes = new ArrayList<>();
			departureTravelTimes.add(String.valueOf(departureTime)+"-"+String.valueOf(travelTime));
			personIdDepartureTravelTimes.put(personId, departureTravelTimes);
			this.modePersonIdDepartureTravelTimes.put(legMode, personIdDepartureTravelTimes);
		}
	}

}
