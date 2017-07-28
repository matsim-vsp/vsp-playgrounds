package playground.santiago.analysis.eventHandlers.trafficVolumes;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;

public class SantiagoLinkVolumeHandler
		implements LinkLeaveEventHandler {
	
	private static final Logger LOG = Logger.getLogger(SantiagoLinkVolumeHandler.class);
	private Map<Id<Link>, Map<Integer,Double>> linksVolumes = new HashMap<>();
	
	
	public SantiagoLinkVolumeHandler(){
		//Assuming no vehicles file by the time.
		//nothing to do.
	}

	private int getSlot(double time){
		return (int)time/3600;
	}
	
	@Override
	public void reset(int iteration) {
		this.linksVolumes.clear();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Map<Integer,Double> volsInTime = new HashMap<>();
		Id<Link> linkId = event.getLinkId();
		int slotInt = getSlot(event.getTime());

		if(this.linksVolumes.containsKey(linkId)){
			volsInTime = this.linksVolumes.get(linkId);

			if(volsInTime.containsKey(slotInt)){
				volsInTime.put(slotInt, volsInTime.get(slotInt)+1);
			}else {
				volsInTime.put(slotInt,1.0);
			}

		}else {
			volsInTime.put(slotInt,1.0);
		}

		this.linksVolumes.put(linkId,volsInTime);

	}
	
	public Map<Id<Link>,Map<Integer,Double>> getLinksVolumes (){
		return this.linksVolumes;
	}

}
