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

/** This class consists of neccessary information about Transit Routes, 
 * and is used in conversion of Excel based transit data into MatSim readable transit schedule data structure.
 * 
 * @author Mohammad Saleem
 */
public class TransitRoute {
	private String id;
	private String transportmode;
	private List<Stop> routeprofile = new LinkedList<Stop>();// Sorted list of Stops
	private List<Link> route = new LinkedList<Link>();// Sorted list of Links
	private List<Departure> departures = new LinkedList<Departure>();// Departures from first stop of the transit route
	public void addStop(Stop stop){
		routeprofile.add(stop);
	}
	public void addLink(Link link){
		route.add(link);
	}
	public void addDeparture(Departure departure){
		departures.add(departure);
	}
	public List<Departure> getDepartures(){
		return departures;
	}
	public String getID(){
		return id;
	}
	public Stop getLastStop(){
		return routeprofile.get(routeprofile.size()-1);
	}
	public Stop getFirstStop(){
		return routeprofile.get(0);
	}
	public List<Stop> getRouteProfile(){
		return routeprofile;
	}
	public List<Link> getRoute(){
		return route;
	}
	public void setID(String id){
		this.id=id;
	}
	public String getTransportMode(){
		return this.transportmode;
	}
	public void setTransportMode(String transportmode){
		this.transportmode=transportmode;
	}
	public void removeStop(Stop stop){//Removes a Stop
		routeprofile.remove(stop);
	}
	public String toString() {
		String str = "";
		Iterator<Stop> iter = routeprofile.iterator();
		while(iter.hasNext()){
			Stop stop = iter.next();
			str=str+stop.getId();
		}
		return str;
	}
}
