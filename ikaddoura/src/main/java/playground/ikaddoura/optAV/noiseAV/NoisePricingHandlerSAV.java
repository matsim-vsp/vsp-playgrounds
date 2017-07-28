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
package playground.ikaddoura.optAV.noiseAV;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.noise.events.NoiseEventCaused;
import org.matsim.contrib.noise.handler.NoiseEventCausedHandler;
import org.matsim.contrib.noise.personLinkMoneyEvents.PersonLinkMoneyEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

import playground.ikaddoura.optAV.SAVPassengerTracker;


/**
 * This handler calculates agent money events based on the noise damages an agent may cause (NoiseEventCaused).
 * 
 * @author ikaddoura
 *
 */
public class NoisePricingHandlerSAV implements NoiseEventCausedHandler, PersonEntersVehicleEventHandler {
	private static final Logger log = Logger.getLogger(NoisePricingHandlerSAV.class);

	@Inject
	private EventsManager events;
	
	@Inject
	private NoiseContext noiseContext;
	
	@Inject
	private SAVPassengerTracker savPassengerTracker;

	private double amountSum = 0.;
	private boolean chargeNextPassenger = false;
	private final Map<Id<Vehicle>, Double> vehicle2tollToBeChargedFromNextPassenger = new HashMap<>();
	private double amountNotChargedFromPassengers = 0.;

	@Override
	public void reset(int iteration) {
		this.amountSum = 0.;
		this.vehicle2tollToBeChargedFromNextPassenger.clear();
		amountNotChargedFromPassengers = 0.;
	}

	@Override
	public void handleEvent(NoiseEventCaused event) {
		
		// negative amount since from here the amount is interpreted as costs
		double amount = this.noiseContext.getNoiseParams().getNoiseTollFactor() * event.getAmount() * (-1);
		this.amountSum = this.amountSum + amount;
		
		// charge the driver
		
		PersonMoneyEvent moneyEvent = new PersonMoneyEvent(event.getTime(), event.getCausingAgentId(), amount);
		this.events.processEvent(moneyEvent);
		
		PersonLinkMoneyEvent linkMoneyEvent = new PersonLinkMoneyEvent(event.getTime(), event.getCausingAgentId(), event.getLinkId(), amount, event.getLinkEnteringTime(), "noise");
		this.events.processEvent(linkMoneyEvent);
		
		if (this.savPassengerTracker.getTaxiVehicles().contains(event.getCausingVehicleId())) {
			
			// a taxi vehicle, charge the passenger
			
			Id<Person> passenger = this.savPassengerTracker.getVehicle2passenger().get(event.getCausingVehicleId());
			
			if (passenger == null) {
				log.info("No passenger identified for " + event.getCausingVehicleId() + " at " + event.getTime() + " on link " + event.getLinkId() +". Amount: " + amount);
				
				if (chargeNextPassenger) {
					if (vehicle2tollToBeChargedFromNextPassenger.get(event.getCausingVehicleId()) == null) {
						vehicle2tollToBeChargedFromNextPassenger.put(event.getCausingVehicleId(), amount);
					} else {
						double newToll = vehicle2tollToBeChargedFromNextPassenger.get(event.getCausingVehicleId()) + amount;
						vehicle2tollToBeChargedFromNextPassenger.put(event.getCausingVehicleId(), newToll);
					}
					
					log.info("Toll payments to be charged from next passenger: " + vehicle2tollToBeChargedFromNextPassenger.get(event.getCausingVehicleId()));
				
				} else {
					log.info("Charging last passenger...");
					Id<Person> lastPassenger = this.savPassengerTracker.getVehicle2lastPassenger().get(event.getCausingVehicleId());
					if (lastPassenger == null) {
						amountNotChargedFromPassengers = amountNotChargedFromPassengers + amount;
						log.warn("Cumulative amount not passed to the passangers: " + amountNotChargedFromPassengers);
					} else {
						this.events.processEvent(new PersonMoneyEvent(event.getTime(), lastPassenger, amount));
					}
				}
			
			} else {	
				
				PersonMoneyEvent moneyEventPassenger = new PersonMoneyEvent(event.getTime(), passenger, amount);
				this.events.processEvent(moneyEventPassenger);
				
				PersonLinkMoneyEvent linkMoneyEventPassenger = new PersonLinkMoneyEvent(event.getTime(), passenger, event.getLinkId(), amount, event.getLinkEnteringTime(), "noise");
				this.events.processEvent(linkMoneyEventPassenger);
			}
		}
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		
		if (chargeNextPassenger) {
			
			if (this.savPassengerTracker.isTaxiPassenger(event.getPersonId())) {
				// passenger getting into a taxi

				if (this.vehicle2tollToBeChargedFromNextPassenger.get(event.getVehicleId()) != null) {

					log.info("First passenger of vehicle " + event.getVehicleId() + ". Toll: " + vehicle2tollToBeChargedFromNextPassenger.get(event.getVehicleId()));
					
					this.events.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), this.vehicle2tollToBeChargedFromNextPassenger.get(event.getVehicleId())));
					this.vehicle2tollToBeChargedFromNextPassenger.remove(event.getVehicleId());
				}
			}
		}
	}	

	public double getAmountSum() {
		return amountSum;
	}
	
}