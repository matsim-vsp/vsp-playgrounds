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
  
package playground.kturner.utils;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.controler.OutputDirectoryLogging;

import playground.kturner.freightKt.FreightWithShipments;

/**
 * Converts a given carriers file to a carrier file with only shipments
 * 
 * KMT Sep18:
 * Note: In this version the carrier file needs a solution of the VRP to create the shipments. Otherwise the "from"-location of shipment is missing and creating the shipment will fail.
 * TODO: Add a rule how to assign services to one specific depot if solution is missing (a) clear if carrier has onlay one, (b) select available depot with the shortest beeline distance ?
 * @param args
 * @author kturner
 */
public class ConvertCarriersToShipmentBasedCarriers {

	private static final Logger log = Logger.getLogger(FreightWithShipments.class);

	private static final String INPUT_DIR = "../../shared-svn/projects/freight/studies/MA_Turner-Kai/input/Berlin_Szenario/" ;
	private static final String OUTPUT_DIR = INPUT_DIR + "CarriersWShipments/";

	private static final String LOG_DIR = OUTPUT_DIR + "Logs/";
	
	private static final String CARRIERFILE_NAME_INPUT = "carrierLEH_v2_withFleet.xml" ;
	private static final String CARRIERFILE_NAME_OUTPUT = "carrierLEH_v2_withFleet_Shipment.xml";
	private static final String ABC = CARRIERFILE_NAME_INPUT.substring(CARRIERFILE_NAME_INPUT.length() - 4, CARRIERFILE_NAME_INPUT.length()) + "_Shipments.mxl";
	
	private static final String CARRIERFILE_INPUT = INPUT_DIR + CARRIERFILE_NAME_INPUT;
	private static final String CARRIERFILE_OUTPUT = OUTPUT_DIR + CARRIERFILE_NAME_OUTPUT;

	public static void main(String[] args) throws IOException {
		
		/*
		 * some preparation - set logging level
		 */
		Logger.getRootLogger().setLevel(Level.DEBUG);
		
		/*
		 * some preparation - create output folder
		 */
		OutputDirectoryLogging.initLoggingWithOutputDirectory(LOG_DIR);

		
		//Read in carrierfile
		Carriers carriersInput = new Carriers() ;
		new CarrierPlanXmlReaderV2(carriersInput).readFile(new File(CARRIERFILE_INPUT).getCanonicalPath().toString());
		
		//Creation of carrier with converted VRP
		Carriers carriersOutput = new Carriers();
		carriersOutput = FreightUtils.createShipmentVRPCarrierFromServiceVRPSolution(carriersInput);
		
//		log.info("Writing convertet carriers to file: " + CARRIERFILE_OUTPUT);
		log.info("Writing convertet carriers to file: " + ABC);
//		new CarrierPlanXmlWriterV2(carriersOutput).write(new File(CARRIERFILE_OUTPUT).getCanonicalPath().toString());
		
		log.info("#### Finish ####");

	}

}
