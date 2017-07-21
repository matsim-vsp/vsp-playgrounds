package playground.santiago.analysis.eventHandlers.others;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class SantiagoStuckAndAbortEventHandler implements PersonStuckEventHandler {

	private final SortedMap<String,Map<Id<Person>,List<Double>>> mode2IdPerson2Time = new TreeMap<>();
	private final List<Id<Person>> stuckAgents = new ArrayList<>();
	
	private int warnCount = 0;
	private static final Logger LOGGER = Logger.getLogger(SantiagoStuckAndAbortEventHandler.class);
	
	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.warnCount++;
		
		String mode = event.getLegMode();
		Id<Person> personId = event.getPersonId();
		double time = event.getTime();
		
		this.stuckAgents.add(personId);
		
		if(this.mode2IdPerson2Time.containsKey(mode)){
			Map<Id<Person>,List<Double>> idPerson2Time = mode2IdPerson2Time.get(mode);
			if(idPerson2Time.containsKey(personId)){
				List<Double> times = idPerson2Time.get(personId);
				times.add(time);
				
			}else{
				List<Double> times = new ArrayList<>();
				times.add(time);
				idPerson2Time.put(personId, times);
			}
						
			
		} else {
			
			Map<Id<Person>,List<Double>> idPerson2Time = new TreeMap<>();
			List<Double> times = new ArrayList<>();
			
			times.add(time);
			idPerson2Time.put(personId, times);
			mode2IdPerson2Time.put(mode, idPerson2Time);
			
			
		}	
		
		LOGGER.warn("'StuckAndAbort' event is thrown for person "+event.getPersonId()+" on link "+event.getLinkId()+" at time "+event.getTime()+
				". \n Correctness of travel time for such persons can not be guaranteed. The number of stuckAndAbort events are " + warnCount);

	}
	
	public SortedMap<String,Map<Id<Person>,List<Double>>> getMode2IdAgentsStuck2Time() {
		return this.mode2IdPerson2Time;
		
	}
	
	public List<Id<Person>> getAgentsStuck(){
		return this.stuckAgents;
	}

}
