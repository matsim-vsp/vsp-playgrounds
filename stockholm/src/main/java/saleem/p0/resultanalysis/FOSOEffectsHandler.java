/*
 * Copyright 2018 Mohammad Saleem
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: salee@kth.se
 *
 */ 
package saleem.p0.resultanalysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.scoring.EventsToScore;
/**
 * An event handling class to help analyse first order second order effects of P0.
 * I.e. for vehicles passing through signlised junctions (first order), and vehicle not passing through junctions (second order)
 * 
 * @author Mohammad Saleem
 */
public class FOSOEffectsHandler implements BasicEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler{//first order second order effects of P0
	private Map<Id<Person>, Double> times = new HashMap<>();
	private Map<Id<Person>, ? extends Person> persons = new HashMap<>();
	EventsToScore scoring;

	double totaltrips = 0, totaltime=0;
	public FOSOEffectsHandler(EventsToScore scoring, Map<Id<Person>, ? extends Person> persons, EventsManager manager) {
		this.persons=persons;
		this.scoring = scoring;
		// TODO Auto-generated constructor stub
	}
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if(persons.containsKey(event.getPersonId())){
		// TODO Auto-generated method stub
			totaltime += (event.getTime()-times.get(event.getPersonId()));
			totaltrips++;
			times.remove(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// TODO Auto-generated method stub
		if(persons.containsKey(event.getPersonId())){
			times.put(event.getPersonId(), event.getTime());
		}
			}
	public double getTT(){//get travel time
		System.out.println("Average Travel Time: " + totaltime/totaltrips);
		return totaltime/totaltrips;
	}
	public double getTotalrips(){//get travel time
		System.out.println("Total Trips: " + totaltrips);
		return totaltrips;
	}
	@Override
	public void handleEvent(Event event) {
		scoring.handleEvent(event);

	}
}
