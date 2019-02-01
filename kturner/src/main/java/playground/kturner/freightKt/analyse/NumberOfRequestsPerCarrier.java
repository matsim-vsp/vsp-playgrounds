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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.core.utils.io.UncheckedIOException;

class NumberOfRequestsPerCarrier {

	private static final String INPUT_DIR = "../../OutputKMT/projects/freight/studies/reAnalysing_MA/MATSim/Berlin-MultipleToursSingleTimeWindow/I-Base/Run_1/";
//	private static final String INPUT_DIR = "../../OutputKMT/projects/freight/studies/reAnalysing_MA/MATSim/CEP-Wilmersdorf_Bike/MultipleTours/Run_1/";
	private static final String CARRIERFILE_NAME_INPUT = "jsprit_plannedCarriers.xml" ;
	
	private static final String CARRIERFILE_INPUT = INPUT_DIR + CARRIERFILE_NAME_INPUT;
	
	private static final Logger log = Logger.getLogger(FreightAnalyseKT.class);
	
	public static void main(String[] args) throws UncheckedIOException, IOException {
		/*
		 * some preparation - set logging level
		 */
		Logger.getRootLogger().setLevel(Level.DEBUG);
		
		log.info("Starting");
		
		//Read in carrierfile
		Carriers carriers = new Carriers() ;
		new CarrierPlanXmlReaderV2(carriers).readFile(new File(CARRIERFILE_INPUT).getCanonicalPath().toString());
		
		System.out.println("Id; # of Services; # of Shipments");
		for (Carrier carrier : carriers.getCarriers().values()) {
			System.out.println(carrier.getId() + "; " + carrier.getServices().size() + "; " + carrier.getShipments().size());
		}
		
		
		log.info("### Finished ###");
	}

}
