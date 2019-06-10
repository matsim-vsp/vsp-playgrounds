/*
 * Copyright 2018 Gunnar Flötteröd
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
 * contact: gunnar.flotterod@gmail.com
 *
 */
package gunnar.wum.malin;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class LineUsageStatistics implements PersonEntersVehicleEventHandler {

	private final Predicate<Id<Vehicle>> vehicleSelector;
	
	private final Predicate<Double> timeSelector;
	
	private final List<Id<Person>> travelers = new LinkedList<>();
	
	LineUsageStatistics(final Predicate<Id<Vehicle>> vehicleSelector, final Predicate<Double> timeSelector) {
		this.vehicleSelector = vehicleSelector;
		this.timeSelector = timeSelector;		
	}
	
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.vehicleSelector.test(event.getVehicleId()) && this.timeSelector.test(event.getTime())) {
			this.travelers.add(event.getPersonId());
		}
	}
	
	List<Id<Person>> getTravelers() {
		return this.travelers;
	}

}

