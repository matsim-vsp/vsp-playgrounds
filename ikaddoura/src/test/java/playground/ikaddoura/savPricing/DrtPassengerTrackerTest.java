/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package playground.ikaddoura.savPricing;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

/**
 * @author ikaddoura
 *
 */
public class DrtPassengerTrackerTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public final void test1(){
		
		Id<Vehicle> taxiId = Id.createVehicleId("taxiVehicle1");
		Id<Person> passengerId = Id.createPersonId("passenger1");

		SAVPassengerTrackerImpl handler = new SAVPassengerTrackerImpl(TransportMode.drt);
		handler.handleEvent(new ActivityEndEvent(1., Id.createPersonId("taxiDriver1"), Id.createLinkId("link1"), null, VrpAgentLogic.BEFORE_SCHEDULE_ACTIVITY_TYPE));
		handler.handleEvent(new PersonDepartureEvent(2. ,passengerId, Id.createLinkId("link1"), TransportMode.drt));
	
		Assert.assertEquals(true, handler.isTaxiPassenger(passengerId) == true);

		handler.handleEvent(new PersonEntersVehicleEvent(10., passengerId, taxiId));
				
		Assert.assertEquals(true, handler.getTaxiVehicles().size() == 1);
		Assert.assertEquals(true, handler.isTaxiPassenger(passengerId) == true);
		Assert.assertEquals(true, handler.getTaxiVehicles().contains(taxiId));

		Assert.assertEquals(true, handler.getTaxiDrivers().size() == 1);
		Assert.assertEquals(true, handler.getTaxiDrivers().contains(Id.createPersonId("taxiDriver1")));
		
		Assert.assertEquals(true, handler.getVehicle2passenger().size() == 1);
		Assert.assertEquals(true, handler.getVehicle2passenger().get(taxiId).toString().equals(passengerId.toString()));

		Assert.assertEquals(true, handler.getVehicle2lastPassenger().get(taxiId).toString().equals(passengerId.toString()));

		handler.handleEvent(new PersonLeavesVehicleEvent(20., passengerId, taxiId));
		
		Assert.assertEquals(true, handler.isTaxiPassenger(passengerId) == true);

		handler.handleEvent(new PersonArrivalEvent(21., passengerId, Id.createLinkId("link1"), TransportMode.drt));

		Assert.assertEquals(true, handler.isTaxiPassenger(passengerId) == false);

		Assert.assertEquals(true, handler.getTaxiVehicles().size() == 1);
		Assert.assertEquals(true, handler.getTaxiVehicles().contains(taxiId));

		Assert.assertEquals(true, handler.getTaxiDrivers().size() == 1);
		Assert.assertEquals(true, handler.getTaxiDrivers().contains(Id.createPersonId("taxiDriver1")));
		
		Assert.assertEquals(true, handler.getVehicle2passenger().size() == 0);
		
		Assert.assertEquals(true, handler.getVehicle2lastPassenger().get(taxiId).toString().equals(passengerId.toString()));


	}
		
}
