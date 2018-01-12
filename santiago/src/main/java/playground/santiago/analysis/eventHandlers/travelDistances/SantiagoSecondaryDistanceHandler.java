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
import playground.santiago.network.GetTollwayAndSecondaryLinks;

/**
 * Only for cars.
 * Only for secondary links
 */

public class SantiagoSecondaryDistanceHandler
implements PersonDepartureEventHandler, LinkLeaveEventHandler, PersonArrivalEventHandler,
VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, TransitDriverStartsEventHandler {
	
	private final static Logger LOG = Logger.getLogger(SantiagoSecondaryDistanceHandler.class);
	
	private final Network network;
	private final Set<Id<Link>> secondaryLinks;
	private final SortedMap<String, ArrayList<String>> periodLinkIdLengthSecondary = new TreeMap<>();
	private final Map<Id<Person>, String> personIdDepartureLegModes = new HashMap<>();
	private final Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();
	private final List<Id<Person>> transitDriverPersons = new ArrayList<>();
	private List<Id<Person>> stuckAgents = new ArrayList<>();
	private double sum;

	public SantiagoSecondaryDistanceHandler(final Config config, final Network network, List<Id<Person>> stuckAgents, String shapeFile){
		LOG.info("Route distance will be calculated based on events.");
		LOG.warn("During distance calculation, link from which person is departed or arrived will not be considered.");
		this.network = network;
		this.stuckAgents=stuckAgents;
		GetTollwayAndSecondaryLinks gts = new GetTollwayAndSecondaryLinks(shapeFile,network);
		secondaryLinks = gts.getSecondaryLinks();
	}

	@Override
	public void reset(int iteration) {
		periodLinkIdLengthSecondary.clear();
		personIdDepartureLegModes.clear();
		secondaryLinks.clear();
		stuckAgents.clear();
		sum = 0;
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Person> personId = this.delegate.getDriverOfVehicle(event.getVehicleId());
		Id<Link> linkId = event.getLinkId();		
		String mode = this.personIdDepartureLegModes.get(personId);
		if(transitDriverPersons.contains(personId)||stuckAgents.contains(personId)||!mode.equals(TransportMode.car)){
			//Omitting...
		} else {
			if(secondaryLinks.contains(linkId)){ //Be aware that some links should be omitted. This would be handled by GetTollwayAndSecondaryLinks class.
				sum = sum + network.getLinks().get(linkId).getLength();
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
	
	
	public double getSecondaryDistance (){
		return sum;
	}


}
