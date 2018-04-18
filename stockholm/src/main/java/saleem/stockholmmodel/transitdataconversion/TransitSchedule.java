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
package saleem.stockholmmodel.transitdataconversion;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This class represents a Transit Schedule data structure used for writing Excel based transit data 
 * into MatSim form. One could also use the built in MatSim Transit Schedule.
 * 
 * @author Mohammad Saleem
 */

public class TransitSchedule {
	List<Stop> stops = new LinkedList<Stop>();
	List<VehicleType> vehicletypes = new LinkedList<VehicleType>();
	List<Vehicle> vehicles = new LinkedList<Vehicle>();
	List<Line> lines = new LinkedList<Line>();
	public void addStop(Stop stop){
		stops.add(stop);
	}
	public void addVehicleType(VehicleType vehicletype){
		vehicletypes.add(vehicletype);
	}
	public void addVehicle(Vehicle vehicle){
		vehicles.add(vehicle);
	}
	public void addLine(Line line){
		lines.add(line);
	}
	public List<Stop> getStops(){
		return stops;
	}
	public List<VehicleType> getVehicleTypes(){
		return vehicletypes;
	}
	public List<Vehicle> getVehicles(){
		return vehicles;
	}
	public Stop getStop(String id){//Returns a stop based on its ID
		Iterator iter = stops.iterator();
		while(iter.hasNext()){
			Stop stop = (Stop)iter.next();
			if(stop.getId().equals(id)){
				return stop;
			}
			
		}
		return null;
	}
	public void removeStop(Stop stop){//Removes a Stop
		stops.remove(stop);
	}
	public List<Line> getLines(){
		return lines;
	}
}
