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
package saleem.stockholmmodel.resultanalysis;

import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
/**
 * A class for stuck persons and vehicles analysis.
 * 
 * @author Mohammad Saleem
 *
 */
public class HandleStuckVehicles implements PersonStuckEventHandler, TransitDriverStartsEventHandler{
	int count = 0;int countveh=0;
	String persons="Stuck Persons Are: ";
	String vehicles="Vehicles Are: ";
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		count++;
		persons+= event.getPersonId() + ", ";
		
	}
	void initiate(){
		count = 0;
		countveh=0;
		persons="Stuck Persons Are: ";
		vehicles="Vehicles Are: ";
	}
	public void printStuckPersonsAndVehicles(){
		System.out.println(persons);//By ID
		System.out.println("Total Stuck Persons Are: "+ count);
		System.out.println("Total Vehicles Are: "+ countveh);
		initiate();
		
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		// TODO Auto-generated method stub
		countveh++;
		vehicles+= event.getVehicleId() + ", ";
	}

}
