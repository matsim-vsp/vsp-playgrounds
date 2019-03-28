/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
  
package playground.kturner.utils;

import java.util.LinkedList;

import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.Carriers;

public class CountCarriersMainData {

	public static void main(String[] args) {
		Carriers carriers = new Carriers();
		new CarrierPlanXmlReaderV2(carriers).readFile("../../OutputKMT/projects/freight/studies/reAnalysing_MA/MATSim/Berlin-MultipleToursSingleTimeWindow/I-Base/Run_1/jsprit_plannedCarriers.xml");
		
		int numOfCarriers = 0;
		int numOfShipments = 0;
		int numOfServices = 0;
		int numOfRequestLocations = 0;
		LinkedList<Object> requestLocations = new LinkedList<>();
		
		for (Carrier carrier : carriers.getCarriers().values()) {
			numOfCarriers++;
			numOfShipments += carrier.getShipments().size();
			numOfServices += carrier.getServices().size();
			 
			for (CarrierShipment shipment : carrier.getShipments()) {
				if (!requestLocations.contains(shipment.getTo())) {
					requestLocations.add(shipment.getTo());
					numOfRequestLocations++;
				}	
			}
			for (CarrierService service : carrier.getServices()) {
				if (!requestLocations.contains(service.getLocationLinkId())) {
					requestLocations.add(service.getLocationLinkId());
					numOfRequestLocations++;
				}	
			}
			
		}
		System.out.println("Number of Carriers: " + numOfCarriers);
		System.out.println("Number of Shipments: " + numOfShipments);
		System.out.println("Number of Services: " + numOfServices);
		System.out.println("Number of RequestLocations: " + numOfRequestLocations);
	}
}
