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
  
package playground.kturner.freightKt.analyse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.ShipmentBasedActivity;
import org.matsim.contrib.freight.carrier.Tour.TourActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * Writes out the aggregated demand sizes of load and unload parts of a tour.
 * The demand sizes is always summed up for a sequence of all activities of the same type (load or unload) 
 *  * 
 * @author kturner
 *
 */
class FreightLoadAndUnloadSeqence {

//	private static final String INPUT_DIR = "../../OutputKMT/projects/freight/studies/reAnalysing_MA/MATSim/Berlin-MultipleToursSingleTimeWindow/I-Base/Run_1/";
//	private static final String INPUT_DIR = "../../OutputKMT/projects/freight/studies/reAnalysing_MA/MATSim/CEP-Wilmersdorf_Bike/MultipleTours/Run_1/";
	private static final String INPUT_DIR = "../../OutputKMT/projects/freight/studies/reAnalysing_MA/MATSim/CEP-Wilmersdorf_Bike/SingleTour/Run_1/";
	
	private static final String CARRIERFILE_NAME_INPUT = "jsprit_plannedCarriers.xml" ;
	private static final String VEHTYPEFILE_NAME = "output_vehicleTypes.xml.gz" ;
	
	private static final String CARRIERFILE_INPUT = INPUT_DIR + CARRIERFILE_NAME_INPUT;
	private static final String VEHTYPEFILE = INPUT_DIR + VEHTYPEFILE_NAME;
	
	private static final Logger log = Logger.getLogger(FreightAnalyseKT.class);
	
	public static void main(String[] args) throws UncheckedIOException, IOException {
		/*
		 * some preparation - set logging level
		 */
		Logger.getRootLogger().setLevel(Level.INFO);
		
		log.info("Starting");
		
		//Read in carriers file
		Carriers carriers = new Carriers() ;
		new CarrierPlanXmlReader(carriers).readFile(new File(CARRIERFILE_INPUT).getCanonicalPath().toString() );
		
		//Read in vehicleTypes file
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		new CarrierVehicleTypeReader(vehicleTypes).readFile(VEHTYPEFILE) ;
		
		
		System.out.println("Demand size of load (+) /unload (-) -sequences for each vehicle of a carrier");
		System.out.println("Carrier Id; vehicle Id; vehicle type; vehicle capacity; sequence of activities");
		for (Carrier carrier : carriers.getCarriers().values()) {
			for (ScheduledTour scTour: carrier.getSelectedPlan().getScheduledTours()) {
				ArrayList<Integer> aggregatedTE = new ArrayList<>();
				int sum = 0;
				
				String typeOfPreviousAct = null;
				for (TourElement te : scTour.getTour().getTourElements()) {
					if (te instanceof TourActivity) {
						TourActivity ta = (TourActivity) te;
						log.debug("TourActivity" + ta.toString() + "previous ActType " + typeOfPreviousAct);
						if ((typeOfPreviousAct == null) || (typeOfPreviousAct == ta.getActivityType())) { 		//erstmalig oder gleicher Typ -> aufsummieren
							if (ta instanceof ShipmentBasedActivity) {
								ShipmentBasedActivity shipmentActivity = (ShipmentBasedActivity) ta;
								if (ta.getActivityType() == "pickup") {
									sum = sum + shipmentActivity.getShipment().getSize();
								}
								if (ta.getActivityType() == "delivery") {
									sum = sum - shipmentActivity.getShipment().getSize();
								}
							}
							else if (ta instanceof ServiceActivity) {
								ServiceActivity serviceActivity = (ServiceActivity) ta;
									sum = sum + serviceActivity.getService().getCapacityDemand();
							}
							typeOfPreviousAct = ta.getActivityType();
						} else if ((typeOfPreviousAct != ta.getActivityType())) {
							aggregatedTE.add(new Integer(sum));
							sum = 0 ; //reset
							if (ta instanceof ShipmentBasedActivity) {
								ShipmentBasedActivity shipmentActivity = (ShipmentBasedActivity) ta;
								if (ta.getActivityType() == "pickup") {
									sum = sum + shipmentActivity.getShipment().getSize();
								}
								if (ta.getActivityType() == "delivery") {
									sum = sum - shipmentActivity.getShipment().getSize();
								}
							}
							else if (ta instanceof ServiceActivity) {		//sollte nicht vorkommen.
								log.warn("Service should not have change in activity types in tour plan order during the tour");
//								ServiceActivity serviceActivity = (ServiceActivity) ta;
//								sum = sum + serviceActivity.getService().getCapacityDemand();
						}
							typeOfPreviousAct = ta.getActivityType();
						}
					}	
				}
				aggregatedTE.add(new Integer(sum)); //last value
				
				//TODO: sicherstellen, dass nur load und anload gezält wird (Nochmal nachlesen,w elche es genau sind.
				
				//TODO: FarhzeugCapacity noch einfügen
				System.out.println(carrier.getId() + "; " + scTour.getVehicle().getId() + "; "
							+ scTour.getVehicle().getVehicleTypeId() + "; " 
							+ vehicleTypes.getVehicleTypes().get( scTour.getVehicle().getVehicleTypeId() ).getCapacity().getOther().intValue() + "; "
							+ aggregatedTE.toString());
			}
		}
		
		
		log.info("### Finished ###");
	}

}
