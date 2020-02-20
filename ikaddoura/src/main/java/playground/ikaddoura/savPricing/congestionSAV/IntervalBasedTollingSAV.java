/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.savPricing.congestionSAV;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.decongestion.data.DecongestionInfo;
import org.matsim.contrib.decongestion.handler.IntervalBasedTolling;
import org.matsim.contrib.noise.personLinkMoneyEvents.PersonLinkMoneyEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

import playground.ikaddoura.savPricing.SAVPassengerTracker;
import playground.ikaddoura.savPricing.SAVPricingConfigGroup;

/**
 * Throws agent money events for the tolled links and time bins.
 * Throw money events for all vehicle drivers (car + taxi drivers) including the passengers inside / waiting for the taxis.
 * 
 * @author ikaddoura
 */

public class IntervalBasedTollingSAV implements LinkLeaveEventHandler, IntervalBasedTolling, PersonEntersVehicleEventHandler {
	private static final Logger log = Logger.getLogger(IntervalBasedTollingSAV.class);

	@Inject
	private EventsManager eventsManager;
	
	@Inject
	private DecongestionInfo decongestionInfo;
	
	@Inject
	private SAVPassengerTracker tracker;
	
	@Inject
	private SAVPricingConfigGroup optAVParams;
	
	private double totalTollPayments;
	private Map<Id<Vehicle>, Double> vehicle2tollToBeChargedFromNextPassenger = new HashMap<>();

	@Override
	public void reset(int iteration) {
		this.totalTollPayments = 0.;
		this.vehicle2tollToBeChargedFromNextPassenger.clear();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		if (!decongestionInfo.getTransitVehicleIDs().contains(event.getVehicleId()) && decongestionInfo.getlinkInfos().get(event.getLinkId()) != null) {
						
			int currentTimeBin = (int) (event.getTime() / this.decongestionInfo.getScenario().getConfig().travelTimeCalculator().getTraveltimeBinSize());
			
			if (decongestionInfo.getlinkInfos().get(event.getLinkId()).getTime2toll().get(currentTimeBin) != null) {
								
				double toll = decongestionInfo.getlinkInfos().get(event.getLinkId()).getTime2toll().get(currentTimeBin);
				
				if (toll != 0.) {
					
					this.totalTollPayments = this.totalTollPayments + toll;
					
					if (this.tracker.getTaxiVehicles().contains(event.getVehicleId())) {
						
						// a taxi vehicle
						
						if (optAVParams.isChargeTollsFromSAVDriver()) {
							
							// charge the driver
							
							this.eventsManager.processEvent(new PersonMoneyEvent(event.getTime(), this.decongestionInfo.getVehicleId2personId().get(event.getVehicleId()), -1. * toll, null, null));
							this.eventsManager.processEvent(new PersonLinkMoneyEvent(event.getTime(), this.decongestionInfo.getVehicleId2personId().get(event.getVehicleId()), event.getLinkId(), -1. * toll, event.getTime(), "congestion"));
						}
						
						if (optAVParams.isChargeSAVTollsFromPassengers()) {
							
							// charge the passenger
							
							Id<Person> passenger = this.tracker.getVehicle2passenger().get(event.getVehicleId());
							
							if (passenger == null) {
//								log.info("No passenger identified for " + event.getVehicleId() + " at " + event.getTime() + " on link " + event.getLinkId() +".");
								if (vehicle2tollToBeChargedFromNextPassenger.get(event.getVehicleId()) == null) {
									vehicle2tollToBeChargedFromNextPassenger.put(event.getVehicleId(), toll);
								} else {
									double newToll = vehicle2tollToBeChargedFromNextPassenger.get(event.getVehicleId()) + toll;
									vehicle2tollToBeChargedFromNextPassenger.put(event.getVehicleId(), newToll);
								}	
//								log.info("Toll payments to be charged from next passenger: " + vehicle2tollToBeChargedFromNextPassenger.get(event.getVehicleId()));
								
							} else {
								this.eventsManager.processEvent(new PersonMoneyEvent(event.getTime(), passenger, -1. * toll, null, null));
								this.eventsManager.processEvent(new PersonLinkMoneyEvent(event.getTime(), passenger, event.getLinkId(), -1. * toll, event.getTime(), "congestion"));
							}
							
						} else {
							// do not charge the passenger
						}
						
					} else {
						
						// a car user
						
						if (optAVParams.isChargeTollsFromCarUsers()) {
							
							// charge the car user
							
							this.eventsManager.processEvent(new PersonMoneyEvent(event.getTime(), this.decongestionInfo.getVehicleId2personId().get(event.getVehicleId()), -1. * toll, null, null));
							this.eventsManager.processEvent(new PersonLinkMoneyEvent(event.getTime(), this.decongestionInfo.getVehicleId2personId().get(event.getVehicleId()), event.getLinkId(), -1. * toll, event.getTime(), "congestion"));
						}
					}
					
				} else {
					// do not throw money events if toll is zero.
				}
			}		
		}
	}

	@Override
	public double getTotalTollPayments() {
		log.info("total payments: " + this.totalTollPayments);
		return totalTollPayments;
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		
		if (this.tracker.isTaxiPassenger(event.getPersonId())) {
			// passenger getting into a taxi

			if (this.vehicle2tollToBeChargedFromNextPassenger.get(event.getVehicleId()) != null) {

//				log.info("First passenger of vehicle " + event.getVehicleId() + ". Toll: " + vehicle2tollToBeChargedFromNextPassenger.get(event.getVehicleId()));
				
				this.eventsManager.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), -1. * this.vehicle2tollToBeChargedFromNextPassenger.get(event.getVehicleId()), null, null));
				this.vehicle2tollToBeChargedFromNextPassenger.remove(event.getVehicleId());
			}
		}
	}	
}

