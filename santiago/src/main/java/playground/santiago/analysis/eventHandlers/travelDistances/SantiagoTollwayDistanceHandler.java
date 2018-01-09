package playground.santiago.analysis.eventHandlers.travelDistances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


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
import org.matsim.core.config.Config;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import playground.santiago.network.GetTollwayAndSecondaryLinks;

/**
 * Only for cars.
 * Only for toll-way links
 */

public class SantiagoTollwayDistanceHandler
implements PersonDepartureEventHandler, LinkLeaveEventHandler, PersonArrivalEventHandler,
VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, TransitDriverStartsEventHandler {
	
	private final static Logger LOG = Logger.getLogger(SantiagoTollwayDistanceHandler.class);
	
	private final Network network;
	private final Map<Id<Link>, String> tollwayLinks;
	private final SortedMap<String, ArrayList<String>> periodLinkIdLengthTollway = new TreeMap<>();
	private final Map<Id<Person>, String> personIdDepartureLegModes = new HashMap<>();
	private final Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();
	private final List<Id<Person>> transitDriverPersons = new ArrayList<>();
	private List<Id<Person>> stuckAgents = new ArrayList<>();

	public SantiagoTollwayDistanceHandler(final Config config, final Network network, List<Id<Person>> stuckAgents, String shapeFile){
		LOG.info("Route distance will be calculated based on events.");
		LOG.warn("During distance calculation, link from which person is departed or arrived will not be considered.");
		this.network = network;
		this.stuckAgents=stuckAgents;
		GetTollwayAndSecondaryLinks gts = new GetTollwayAndSecondaryLinks(shapeFile,network);
		tollwayLinks = gts.getTollwayLinks();
	}

	@Override
	public void reset(int iteration) {
		periodLinkIdLengthTollway.clear();
		personIdDepartureLegModes.clear();
		tollwayLinks.clear();
		stuckAgents.clear();
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Person> personId = this.delegate.getDriverOfVehicle(event.getVehicleId());
		Id<Link> linkId = event.getLinkId();		
		String mode = this.personIdDepartureLegModes.get(personId);
		double time = event.getTime();
		double midHour = midHourByTime(time);
//		if(time>1800){
//		System.out.println(time);
//		System.out.println(midHour);
//		}
		if(transitDriverPersons.contains(personId)||stuckAgents.contains(personId)||!mode.equals(TransportMode.car)){
			//Omitting...
		} else {
			if(tollwayLinks.containsKey(linkId)){
				String concesion = tollwayLinks.get(linkId);
				String concesionPeriod = concesion + "-" + midHour;
				String linkWithLength = linkId.toString()+"-"+String.valueOf(network.getLinks().get(linkId).getLength());
				
				if(periodLinkIdLengthTollway.containsKey(concesionPeriod)){
					ArrayList<String> LinkIdLength = periodLinkIdLengthTollway.get(concesionPeriod);
					LinkIdLength.add(linkWithLength);					
				} else {
					ArrayList<String> LinkIdLength = new ArrayList<>();
					LinkIdLength.add(linkWithLength);
					periodLinkIdLengthTollway.put(concesionPeriod, LinkIdLength);
				}
			}
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

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		String departureLegMode = event.getLegMode();
		Id<Person> personId = event.getPersonId();
		//person starts its journey.
		personIdDepartureLegModes.put(personId, departureLegMode);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		String arrivalLegMode = event.getLegMode();
		Id<Person> personId = event.getPersonId();
		if(!arrivalLegMode.equals(personIdDepartureLegModes.get(personId))) 
			throw new RuntimeException("Person is departing and arriving with different travel modes. Can not happen.");
		//person ends its journey.
		personIdDepartureLegModes.remove(personId);
	}
	
	
	public SortedMap<String, ArrayList<String>> getPeriodLinkIdLengthTollway (){
		return periodLinkIdLengthTollway;
	}


	public double midHourByTime(double time){
		double midHour = Math.floor(time/1800);
		return midHour;
	}

}
