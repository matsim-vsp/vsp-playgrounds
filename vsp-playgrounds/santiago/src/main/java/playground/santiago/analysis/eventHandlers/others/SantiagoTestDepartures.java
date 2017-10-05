package playground.santiago.analysis.eventHandlers.others;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;

public class SantiagoTestDepartures implements PersonDepartureEventHandler {

	final List<String> info = new ArrayList<>();
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Id<Person> personId = event.getPersonId();
		double departureTime = event.getTime();
		String mode = event.getLegMode();
		
		if(personId.equals(Id.createPersonId("14991002_8"))){
			
			info.add("Start Time: " + departureTime + ". Mode: " + mode);			
		}
		

	}
	
	public void printInfo(){
		System.out.println(info);
	}

}
