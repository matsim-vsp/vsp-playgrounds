package playground.santiago.analysis.eventHandlers.travelTimes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

/**
 * @author Get the information of the link enter and spent times by every agent that use the networks. Stuck agents are considered, be aware.
 */

public class SantiagoLinkSpentTimeHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	private static final Logger LOG = Logger.getLogger(SantiagoLinkSpentTimeHandler.class);
	
	private final  Map<Id<Link>,Map<Id<Person>,Double>> linkPersonEnterTime = new HashMap<>();	
	private final Map<Id<Link>,Map<Id<Person>,List<String>>> linkPersonEnterSpentTime = new HashMap<>();	
	private final Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

	
	public SantiagoLinkSpentTimeHandler(){
		//nothing to do.
	}
	
	
	@Override
	public void reset(int iteration) {		
		linkPersonEnterTime.clear();
		linkPersonEnterSpentTime.clear();
		this.delegate.reset(iteration);
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
	public void handleEvent(LinkEnterEvent event) {
		
		Id<Link> linkId = event.getLinkId();
		Id<Person> personId = this.delegate.getDriverOfVehicle(event.getVehicleId());
		double enterTime = event.getTime();
		

		if(linkPersonEnterTime.containsKey(linkId)){
			Map<Id<Person>,Double> linkEnterTimes = linkPersonEnterTime.get(linkId);
			linkEnterTimes.put(personId, enterTime);			
		} else {			
			Map<Id<Person>,Double> linkEnterTimes = new HashMap<>();			
			linkEnterTimes.put(personId, enterTime);	
			linkPersonEnterTime.put(linkId, linkEnterTimes);			
	
		}		
		
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Link> linkId = event.getLinkId();
		Id<Person> personId = this.delegate.getDriverOfVehicle(event.getVehicleId());
		double leaveTime = event.getTime();
		
		if(!linkPersonEnterTime.containsKey(linkId)  || !linkPersonEnterTime.get(linkId).containsKey(personId)) return;	

		if(linkPersonEnterSpentTime.containsKey(linkId)){						
			Map<Id<Person>,List<String>> peopleTimes = linkPersonEnterSpentTime.get(linkId);
			
			List<String> personTimes;			
			if(peopleTimes.containsKey(personId)){
				personTimes = linkPersonEnterSpentTime.get(linkId).get(personId);
				double spentTime =  leaveTime - linkPersonEnterTime.get(linkId).get(personId);
				personTimes.add(String.valueOf(linkPersonEnterTime.get(linkId).get(personId))+"-"+String.valueOf(spentTime));
			} else {
				personTimes = new ArrayList<>();
				double spentTime =  leaveTime - linkPersonEnterTime.get(linkId).get(personId);
				personTimes.add( String.valueOf(linkPersonEnterTime.get(linkId).get(personId))+"-"+String.valueOf(spentTime));
			}			
			peopleTimes.put(personId, personTimes);			
			linkPersonEnterTime.get(linkId).remove(personId); //The person is no longer in the link.
			
		} else {
			
			Map<Id<Person>,List<String>> peopleTimes = new HashMap<>();
			List<String> personTimes = new ArrayList<>();
			double spentTime = leaveTime - linkPersonEnterTime.get(linkId).get(personId);
			personTimes.add(String.valueOf(linkPersonEnterTime.get(linkId).get(personId))+"-"+String.valueOf(spentTime));
			peopleTimes.put(personId, personTimes);
			linkPersonEnterSpentTime.put(linkId, peopleTimes);
			linkPersonEnterTime.get(linkId).remove(personId); //The person is no longer in the link.
		}

		
	}


	public Map<Id<Link>,Map<Id<Person>,List<String>>> getLinkPersonEnterSpentTime(){
		return linkPersonEnterSpentTime;
	}
	
}
