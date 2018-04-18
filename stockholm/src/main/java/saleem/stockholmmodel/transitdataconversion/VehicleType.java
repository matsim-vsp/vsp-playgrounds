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
 * Class representing a vehicle type within transit schedule
 * 
 * @author Mohammad Saleem
 */
public class VehicleType {
	private String id;
	private String desciption;
	private int numberofseats;
	private int standingcapacity;
	private double length = 12;
	private double width = 2.5;
	private double accesstime = 1.0;
	private double egresstime = 1.0;
	private String dooroperationmode = "serial";
	private double passengercarequivalents = 1.0;

	public void setLength(double length ){
	this.length=length;
	}
	public void setWidth(double width ){
		this.width=width;
	}
	public void setID(String id ){
		this.id=id;
	}
	public void setDesciption(String desciption ){
		this.desciption=desciption;
	}
	public void setNumberOfSeats(int numberofseats ){
	this.numberofseats=numberofseats;
	}
	public void setStandingCapacity(int standingcapacity ){
		this.standingcapacity=standingcapacity;
	}

	public double getLength( ){
		return this.length;
	}	
	public String getDoorOperationMode( ){
		return this.dooroperationmode;
	}
	public double getPassengerCarEquivalents( ){
		return this.passengercarequivalents;
	}
	public double getWidth(){
		return this.width;
	}
	public double getAccessTime(){
		return this.accesstime;
	}
	public double getEgressTime(){
		return this.egresstime;
	}
	public String getID(){
		return this.id;
	}
	public String getDesciption(){
		return this.desciption;
	}
	public int getNumberOfSeats(){
		return this.numberofseats;
	}
	public int getStandingCapacity(){
		return this.standingcapacity;
	}
}
