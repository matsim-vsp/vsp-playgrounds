/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
package playground.kturner.utils;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.vehicles.VehicleType;

/**
 * @author kturner
 * Consolidate TimeWindows of Vehicles (e.g. Veh A1: 09-14h, Veh A2, 14-19h --> Veh A: 09-19h
 * In same step: Open timeWindows for shipments to the same level
 * (Reason: eperation in severeal tiemWindows was in most cases only for technical reasons -> having traffic during the whole day even when vehicles in VRPs with Services
 * one drive one tour. Now with Shipments we can have multipleTours and thus do not need the technical separation any more. 
 *
 */
class CombineTimeWindowsforCarrier {
	
	private static final Logger log = Logger.getLogger(CombineTimeWindowsforCarrier.class);
	
	public static Carriers consolidateVehicles(Carriers carriers) {
		Carriers carriersWcombinedTW = new Carriers();
		TimeWindow timeWindow = null;
		for (Carrier carrier : carriers.getCarriers().values()){
			 timeWindow = getMaxTimeWindow(carrier);
		}
		
		for (Carrier carrier : carriers.getCarriers().values()){
			Carrier carrierComb = CarrierImpl.newInstance(carrier.getId());
			createAndSetCarrierCapabilities(carrierComb, carrier, timeWindow);
			copyShipmentsAndResetTimeWindow(carrierComb, carrier, timeWindow);
			if (carrier.getServices().size() > 0 ) {
				log.error("Services are not supported here at the moment.");
			}
//			carrier.setCarrierCapabilities(carrier.getCarrierCapabilities()); //vehicles and other carrierCapabilites
			carriersWcombinedTW.addCarrier(carrierComb);
		}
		return carriersWcombinedTW;
	}
	
	private static TimeWindow getMaxTimeWindow(Carrier carrier) {
		double earliestStart = Double.MAX_VALUE; 	// initialize with extreme value 
		double latestEnd = Double.MIN_VALUE; 	// initialize with extreme value 
		for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles()) {
			if (carrierVehicle.getEarliestStartTime() < earliestStart) {
				earliestStart = carrierVehicle.getEarliestStartTime();
			}
			if (carrierVehicle.getLatestEndTime() > latestEnd) {
				latestEnd = carrierVehicle.getLatestEndTime();
			}
		}
		return TimeWindow.newInstance(earliestStart, latestEnd);
	}
	
	private static void createAndSetCarrierCapabilities(Carrier carrierComb, Carrier carrier, TimeWindow timeWindow) {
		//TODO collect veh abh. von location und vehicleType
		//TODO: IRGENDWIE merkt er sich zu viele VehTypes und erstellt die dann neu. Zeitfenster und Orte passen. KMT 10.12.18
		HashMap<Id<Link>, ArrayList<Id<VehicleType>>> vehTypesAtDepot = new HashMap<>();
		
		CarrierCapabilities cc = CarrierCapabilities.newInstance();
		
		for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles()) {
			if (!vehTypesAtDepot.containsKey(carrierVehicle.getLocation())) {
				ArrayList<Id<VehicleType>> arrayList = new ArrayList();
				log.debug(carrierVehicle.toString() + " vehType: " + carrierVehicle.getVehicleTypeId());
				arrayList.add(carrierVehicle.getVehicleTypeId());
				vehTypesAtDepot.put(carrierVehicle.getLocation(), arrayList); 
			} else if (!vehTypesAtDepot.get(carrierVehicle.getLocation()).contains(carrierVehicle.getVehicleTypeId())) {
				ArrayList<Id<VehicleType>> arrayList = vehTypesAtDepot.get(carrierVehicle.getLocation());
				arrayList.add(carrierVehicle.getVehicleTypeId());
				vehTypesAtDepot.put(carrierVehicle.getLocation(), arrayList); 
			}
		}
		
		//add Vehicles
		for (Id<Link> vehLocationId :  vehTypesAtDepot.keySet()) {
			for (Id<VehicleType> vehTypeId : vehTypesAtDepot.get(vehLocationId)) {
				CarrierVehicle carrierVehicle = CarrierVehicle.Builder.newInstance(Id.createVehicleId(vehTypeId.toString()), vehLocationId)
						.setEarliestStart(timeWindow.getStart())
						.setLatestEnd(timeWindow.getEnd())
						.setTypeId(vehTypeId)
						.build();
				cc.getCarrierVehicles().add(carrierVehicle);
			}
		}		
		cc.setFleetSize(carrier.getCarrierCapabilities().getFleetSize());
		carrierComb.setCarrierCapabilities(cc);
	}


	/**
	 * Copy all shipments from the existing carrier to the new carrier with shipments.
	 * @param carrierWS		the "new" carrier with Shipments
	 * @param carrier		the already existing carrier
	 * @param timeWindow	timeWindow to be set for all Shipments (comes from vehicleAvailablity
	 */
	private static void copyShipmentsAndResetTimeWindow(Carrier carrierWS, Carrier carrier, TimeWindow timeWindow) {
		for (CarrierShipment carrierShipment: carrier.getShipments()){
			log.debug("Copy CarrierShipment: " + carrierShipment.toString() + " and set TimWindow to: "+  timeWindow.toString());
			CarrierShipment cs = CarrierShipment.Builder.newInstance(carrierShipment.getId(), carrierShipment.getFrom(), carrierShipment.getTo(), carrierShipment.getSize())
					.setPickupTimeWindow(timeWindow)
					.setDeliveryTimeWindow(timeWindow)
					.setDeliveryServiceTime(carrierShipment.getDeliveryServiceTime())
					.setPickupServiceTime(carrierShipment.getPickupServiceTime())
					.build();
			carrierWS.getShipments().add(cs);
		}	
	}

}
