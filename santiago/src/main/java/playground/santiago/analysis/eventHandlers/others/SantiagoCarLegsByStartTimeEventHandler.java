package playground.santiago.analysis.eventHandlers.others;

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
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.PtConstants;

/**
 *
 */
public class SantiagoCarLegsByStartTimeEventHandler implements PersonDepartureEventHandler, TransitDriverStartsEventHandler {


		private final SortedMap<String,List<Double>> carLeg2StartTime = new TreeMap<>();
		private final List<Id<Person>> transitDriverPersons = new ArrayList<>();


		@Override
		public void reset(int iteration) {
			this.carLeg2StartTime.clear();
			this.transitDriverPersons.clear();
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			String legMode = event.getLegMode();			
			Id<Person> personId = event.getPersonId();
			double startTime = event.getTime();

			if( transitDriverPersons.remove(personId) ) {
				// transit driver drives "car" which should not be counted in the modal share.
			} else {
				if (legMode.equals(TransportMode.car)){
					//storing only the car legs.
					storeMode(legMode, startTime);
				}

			}
		}

		@Override
		public void handleEvent(TransitDriverStartsEvent event) {
			transitDriverPersons.add(event.getDriverId());
		}


		private void storeMode(String legMode, double startTime) {
			if(carLeg2StartTime.containsKey(legMode)){
				List<Double> timesList = carLeg2StartTime.get(legMode);
				timesList.add(startTime);
			} else {
				List<Double> timesList = new ArrayList<>();
				timesList.add(startTime);
				carLeg2StartTime.put(legMode, timesList);
			}
		}



		public SortedMap<String,List<Double>> getCarLegs2StartTime() {
			return carLeg2StartTime;
		}
	}
