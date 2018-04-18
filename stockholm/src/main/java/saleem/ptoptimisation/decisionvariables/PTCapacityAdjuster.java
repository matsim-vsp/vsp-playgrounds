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
package saleem.ptoptimisation.decisionvariables;

import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
/**
 * A class for adjusting capacity of vehicles, such that big vehicles serve peak hours and small vehicles serve off-peak hours.
 * 
 * @author Mohammad Saleem
 *
 */
public class PTCapacityAdjuster {
	/*This function adjusts capacity of vehicles, such that big vehicles serve peak hours 
	 * and small vehicles serve off-peak hours. Note that SBUS, LBUS, STRAIN, LTRAIN, STRAM, LTRAM etc. must be defined in Vehicles.xml file.
	 */
	public void adjustCapacity(Vehicles vehicles, TransitSchedule schedule, double factorline, double factorroute){
		Iterator<TransitLine> lines = schedule.getTransitLines().values().iterator();
		Map<Id<Vehicle>, Vehicle> vehicleinstances = vehicles.getVehicles();
		while(lines.hasNext()) {
			TransitLine tline = lines.next();
			Vehicle vehicle = vehicleinstances.get(tline.getRoutes().values().iterator().next().getDepartures().values().
					  iterator().next().getVehicleId());
			if(Math.random()<=factorline && !vehicle.getType().getId().toString().equals("FERRY")){//With factorline*100 percent probability; Exclude Ferries
				Iterator<TransitRoute> routes = tline.getRoutes().values().iterator();
				while(routes.hasNext()) {
					TransitRoute route = routes.next();
					if(Math.random()<=factorroute){//With factorroute*100 percent probability
						Iterator<Departure> departures = route.getDepartures().values().iterator();
						while(departures.hasNext()) {
							Departure departure = departures.next();
							if(inPeakHour(departure.getDepartureTime())){
								Vehicle veh = vehicleinstances.get(departure.getVehicleId());
								if(!veh.getType().getId().toString().startsWith("L") && 
										!veh.getType().getId().toString().startsWith("S")){//If capacity not already decreased/increased
									VehicleType vtype = vehicles.getVehicleTypes().get(Id.create("L"+veh.getType().
											getId().toString(), VehicleType.class));//Increase capacity
									if(vtype!=null){
										vehicles.removeVehicle(veh.getId());
										veh = new VehicleImpl(veh.getId(), vtype);
										vehicles.addVehicle(veh);
									}
								}
								
								
							}else{
								Vehicle veh = vehicleinstances.get(departure.getVehicleId());
								if(!veh.getType().getId().toString().startsWith("S") && 
										!veh.getType().getId().toString().startsWith("L")){//If capacity not already decreased/increased
									VehicleType vtype = vehicles.getVehicleTypes().get(Id.create("S"+veh.getType().
											getId().toString(), VehicleType.class));//Decrease capacity
									if(vtype!=null){
										vehicles.removeVehicle(veh.getId());
										veh = new VehicleImpl(veh.getId(), vtype);
										vehicles.addVehicle(veh);
									}

								}
							}
						}
						
					}
				}
			}
		}
	}
	//Is the departure in morning or evening Peak hour??
	public boolean inPeakHour(double time){
		return (time>=25200 && time<=34200) || (time>=57600 && time<=66600);
	}
}
