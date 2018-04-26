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
package cba.toynet;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class AverageTravelTime implements TravelTime {

	// -------------------- CONSTANTS --------------------

	private final TimeDiscretization timeDiscr;

	private final TravelTime travelTime;

	// -------------------- CONSTRUCTION --------------------

	AverageTravelTime(final TimeDiscretization timeDiscr, final TravelTime travelTime) {
		this.timeDiscr = timeDiscr;
		this.travelTime = travelTime;
	}

	// -------------------- IMPLEMENTATION --------------------
	
	public double getAvgLinkTravelTime(final Link link, final Person person, final Vehicle vehicle) {
		double sum = 0.0;
		for (int bin = 0; bin < this.timeDiscr.getBinCnt(); bin++) {
			sum += this.travelTime.getLinkTravelTime(link, this.timeDiscr.getBinStartTime_s(bin), person, vehicle);
		}
		return (sum / this.timeDiscr.getBinCnt());		
	}

	// -------------------- IMPLEMENTATION OF TravelTime --------------------

	@Override
	public double getLinkTravelTime(final Link link, final double time, final Person person, final Vehicle vehicle) {
		return this.getAvgLinkTravelTime(link, person, vehicle);
	}

}
