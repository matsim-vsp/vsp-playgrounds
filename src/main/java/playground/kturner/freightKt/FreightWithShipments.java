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

package playground.kturner.freightKt;

import com.graphhopper.jsprit.analysis.toolbox.GraphStreamViewer;
import com.graphhopper.jsprit.analysis.toolbox.Plotter;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;
import com.graphhopper.jsprit.core.util.Coordinate;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.io.problem.VrpXMLWriter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.vehicles.VehicleType;

/**
 * @author kturner
 * Simple Pickup and Delivery example from jsprit -> see https://github.com/graphhopper/jsprit/blob/v1.6/jsprit-examples/src/main/java/jsprit/examples/SimpleEnRoutePickupAndDeliveryExample.java
 */
public class FreightWithShipments {

	private static final Logger log = Logger.getLogger(FreightWithShipments.class);

	////Beginn Namesdefinition KT Für Test-Szenario (Grid)
	private static final String INPUT_DIR = "../../shared-svn/projects/freight/studies/MA_Turner-Kai/input/Grid_Szenario/" ;
	private static final String OUTPUT_DIR = "../../OutputKMT/projects/freight/Shipments/grid/pickupAtDepot/";

	private static final String TEMP_DIR = "../../OutputKMT/projects/freight/studies/testing/Temp/";
	private static final String LOG_DIR = OUTPUT_DIR + "Logs/";
	
	//Dateinamen
	private static final String NETFILE_NAME = "grid-network.xml" ;
	private static final String VEHTYPEFILE_NAME = "grid-vehTypesCap3.xml" ;
	private static final String CARRIERFILE_NAME = "grid-carrier_kt.xml" ;
	
	private static final String NETFILE = INPUT_DIR + NETFILE_NAME ;
	private static final String VEHTYPEFILE = INPUT_DIR + VEHTYPEFILE_NAME;
	private static final String CARRIERFILE = INPUT_DIR + CARRIERFILE_NAME;


	public static void main(String[] args) throws IOException {
		/*
		 * some preparation - set logging level
		 */
		Logger.getRootLogger().setLevel(Level.INFO);
		
		/*
		 * some preparation - create output folder
		 */
		OutputDirectoryLogging.initLoggingWithOutputDirectory(LOG_DIR);
		

		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		new CarrierVehicleTypeReader(vehicleTypes).readFile(VEHTYPEFILE) ;
		
		//Create carrier with shipments
		Carriers carriers = new Carriers() ;
	
		Carrier carrier = CarrierImpl.newInstance(Id.create("ShipmentCarrier", Carrier.class));
		carrier.getShipments().add(getMatsimShipment("shipment1", "i(6,0)", "i(7,4)R", 1));
		carrier.getShipments().add(getMatsimShipment("shipment2", "i(6,0)", "i(3,9)", 2));
		
		CarrierVehicleType carrierVehType = CarrierVehicleType.Builder.newInstance(Id.create("gridType", VehicleType.class)).build();
		
		CarrierCapabilities.Builder ccBuilder = CarrierCapabilities.Builder.newInstance() 
				.addType(carrierVehType)
				.addVehicle(CarrierVehicle.Builder.newInstance(Id.create("gridVehicle", org.matsim.vehicles.Vehicle.class), Id.createLinkId("i(6,0)")).setEarliestStart(0.0).setLatestEnd(36000.0).setType(carrierVehType).build())
				.setFleetSize(FleetSize.INFINITE);				
		carrier.setCarrierCapabilities(ccBuilder.build());
		
		carriers.addCarrier(carrier);
		
		// assign vehicle types to the carriers
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;		//TODO: Hat offenbar Problme vehTypes zu laden
		
		//load Network
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(NETFILE);

		//Convert to jsprit VRP
		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);

		
		
// Jsprit only
//		/*
//		 * get a vehicle type-builder and build a type with the typeId "vehicleType" and a capacity of 2
//		 */
//		VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("vehicleType").addCapacityDimension(0, 2);
//		VehicleType vehicleType = vehicleTypeBuilder.build();
//
//		/*
//		 * get a vehicle-builder and build a vehicle located at (10,10) with type "vehicleType"
//		 */
//		Builder vehicleBuilder = VehicleImpl.Builder.newInstance("vehicle");
//		vehicleBuilder.setStartLocation(loc(Coordinate.newInstance(10, 10)));
//		vehicleBuilder.setType(vehicleType);
//		VehicleImpl vehicle = vehicleBuilder.build();
//
//		/*
//		 * build shipments at the required locations, each with a capacity-demand of 1.
//		 * 4 shipments
//		 * 1: (5,7)->(6,9)
//		 * 2: (5,13)->(6,11)
//		 * 3: (15,7)->(14,9)
//		 * 4: (15,13)->(14,11)
//		 */
//
//		Shipment shipment1 = Shipment.Builder.newInstance("1").addSizeDimension(0, 1).setPickupLocation(loc(Coordinate.newInstance(5, 7))).setDeliveryLocation(loc(Coordinate.newInstance(6, 9))).build();
//		Shipment shipment2 = Shipment.Builder.newInstance("2").addSizeDimension(0, 1).setPickupLocation(loc(Coordinate.newInstance(5, 13))).setDeliveryLocation(loc(Coordinate.newInstance(6, 11))).build();
//
//		Shipment shipment3 = Shipment.Builder.newInstance("3").addSizeDimension(0, 1).setPickupLocation(loc(Coordinate.newInstance(15, 7))).setDeliveryLocation(loc(Coordinate.newInstance(14, 9))).build();
//		Shipment shipment4 = Shipment.Builder.newInstance("4").addSizeDimension(0, 1).setPickupLocation(loc(Coordinate.newInstance(15, 13))).setDeliveryLocation(loc(Coordinate.newInstance(14, 11))).build();
//
//
//		VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
//		vrpBuilder.addVehicle(vehicle);
//		vrpBuilder.addJob(shipment1).addJob(shipment2).addJob(shipment3).addJob(shipment4);

		VehicleRoutingProblem problem = vrpBuilder.build();
		

		/*
		 * get the algorithm out-of-the-box. 
		 */
		VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);

		/*
		 * and search a solution
		 */
		Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();

		/*
		 * get the best 
		 */
		VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

		/*
		 * write out problem and solution to xml-file
		 */
		new VrpXMLWriter(problem, solutions).write(OUTPUT_DIR + "shipment-problem-with-solution.xml");

		/*
		 * print nRoutes and totalCosts of bestSolution
		 */
		SolutionPrinter.print(bestSolution);

		/*
		 * plot problem without solution
		 */
		Plotter problemPlotter = new Plotter(problem);
		problemPlotter.plotShipments(true);
		problemPlotter.plot(OUTPUT_DIR + "simpleEnRoutePickupAndDeliveryExample_problem.png", "en-route pickup and delivery");

		/*
		 * plot problem with solution
		 */
		Plotter solutionPlotter = new Plotter(problem,Arrays.asList(Solutions.bestOf(solutions).getRoutes().iterator().next()));
		solutionPlotter.plotShipments(true);
		solutionPlotter.plot(OUTPUT_DIR + "simpleEnRoutePickupAndDeliveryExample_solution.png", "en-route pickup and delivery");

		new GraphStreamViewer(problem).setRenderShipments(true).display();


		log.info("#### Finished ####");
		/*
		 * close logging
		 */
		OutputDirectoryLogging.closeOutputDirLogging();

	}

	private static Location loc(Coordinate coordinate) {
		return Location.Builder.newInstance().setCoordinate(coordinate).build();
	}
	
	private static CarrierShipment getMatsimShipment(String id, String from, String to, int size) {
		return CarrierShipment.Builder.newInstance(Id.create(id, CarrierShipment.class), Id.create(from, Link.class), Id.create(to, Link.class), size)
				.setDeliveryServiceTime(30.0)
				.setDeliveryTimeWindow(TimeWindow.newInstance(3600.0, 3600.0))
				.setPickupServiceTime(5.0)
				.setPickupTimeWindow(TimeWindow.newInstance(0.0, 7200.0))
				.build();
	}

}
