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
/**
 * A class to convert Excel based data into MatSim based transit schedule 
 * data structure, consisting of neccessary attributes for stops and lines.
 * Departure class contains Departure attributes for route departures.
 * 
 * @author Mohammad Saleem
 *
 */
public class Departure {
	private String id;
	private String departuretime;
	private String vehiclerefid;
	Vehicle vehicle;
	public void setDepartureTime(String departuretime){
		this.departuretime = departuretime;
	}
	public void setVehicleRefId(String vehiclerefid){
		this.vehiclerefid = vehiclerefid;
	}
	public void setVehicle(Vehicle vehicle){
		this.vehicle=vehicle;
	}
	public void setId(String id){
		this.id=id;
	}
	public String getDepartureTime(){
		return this.departuretime;
	}
	public String getId(){
		return this.id;
	}
	public Vehicle getVehicle(){
		return this.vehicle;
	}
	public String getVehicleRefId(){
		return this.vehiclerefid;
	}
}
