package playground.santiago.analysis.eventHandlers.travelDistances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import playground.santiago.network.GetLinksOSMCategories;
import playground.santiago.network.GetTollwayAndSecondaryLinks;

/**
 * Only for cars.
 * Get distance traveled in each OSM-category.
 */

public class SantiagoOSMDistanceHandler
implements PersonDepartureEventHandler, LinkLeaveEventHandler, PersonArrivalEventHandler,
VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, TransitDriverStartsEventHandler {
	
	private final static Logger LOG = Logger.getLogger(SantiagoOSMDistanceHandler.class);
	
	private final Network network;
	private final Map<Id<Link>, String> linkOSMCategories;
	private final Map<Id<Person>, String> personIdDepartureLegModes = new HashMap<>();
	private final Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();
	private final List<Id<Person>> transitDriverPersons = new ArrayList<>();
	private List<Id<Person>> stuckAgents = new ArrayList<>();
	private double sum_motorway;
	private double sum_trunk;
	private double sum_motorway_link;
	private double sum_trunk_primary_link;
	private double sum_primary;
	private double sum_secondary;
	private double sum_tertiary;

	public SantiagoOSMDistanceHandler(final Network network, List<Id<Person>> stuckAgents, String shapeFile){
		LOG.info("Route distance will be calculated based on events.");
		LOG.warn("During distance calculation, link from which person is departed or arrived will not be considered.");
		this.network = network;
		this.stuckAgents=stuckAgents;
		GetLinksOSMCategories analyzer = new GetLinksOSMCategories(shapeFile,network);
		linkOSMCategories = analyzer.getLinkOSMCategories();
	}

	@Override
	public void reset(int iteration) {
		personIdDepartureLegModes.clear();
		linkOSMCategories.clear();
		stuckAgents.clear();
		sum_trunk = 0;
		sum_motorway = 0;
		sum_motorway_link = 0;
		sum_trunk_primary_link = 0;
		sum_primary = 0;
		sum_secondary = 0;
		sum_tertiary = 0;
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Person> personId = this.delegate.getDriverOfVehicle(event.getVehicleId());
		Id<Link> linkId = event.getLinkId();		
		String mode = this.personIdDepartureLegModes.get(personId);
		if(transitDriverPersons.contains(personId)||stuckAgents.contains(personId)||!mode.equals(TransportMode.car)){
			//Omitting...
		} else {
			
			String category = linkOSMCategories.get(linkId);
			
			switch(category) {
			
			case "trunk":
				sum_trunk = sum_trunk + network.getLinks().get(linkId).getLength();
				
			case "motorway":
				sum_motorway = sum_motorway + network.getLinks().get(linkId).getLength();
				
			case "trunk_link_primary_link":
				sum_trunk_primary_link = sum_trunk_primary_link + network.getLinks().get(linkId).getLength();
				
			case "primary":
				sum_primary = sum_primary + network.getLinks().get(linkId).getLength();
				
			case "motorway_link":
				sum_motorway_link = sum_motorway_link + network.getLinks().get(linkId).getLength();
				
			case "secondary":
				sum_secondary = sum_secondary + network.getLinks().get(linkId).getLength();
				
			case "tertiary":
				sum_tertiary = sum_tertiary + network.getLinks().get(linkId).getLength();
				
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
	
	
	public double getTrunkDistance(){
		return sum_trunk;
	}
	
	public double getMotorwayDistance() {
		return sum_motorway;
	}
	
	public double getTrunkPrimaryLinkDistance() {
		return sum_trunk_primary_link;
	}
	
	public double getPrimaryDistance() {
		return sum_primary;
	}
	
	public double getMotorwayLinkDistance() {
		return sum_motorway_link;
	}
	
	public double getSecondaryDistance() {
		return sum_secondary;
	}
	
	public double getTertiaryDistance() {
		return sum_tertiary;
	}
	


}
