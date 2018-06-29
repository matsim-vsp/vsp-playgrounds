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
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl.Builder;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;
import com.graphhopper.jsprit.core.util.Coordinate;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.io.problem.VrpXMLWriter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.core.controler.OutputDirectoryLogging;

/**
 * @author kturner
 * Simple Pickup and Delivery example from jsprit -> see https://github.com/graphhopper/jsprit/blob/v1.6/jsprit-examples/src/main/java/jsprit/examples/SimpleEnRoutePickupAndDeliveryExample.java
 */
public class FreightWithShipments {

	private static final Logger log = Logger.getLogger(FreightWithShipments.class);

	private static final String OUTPUT_DIR = "../../OutputKMT/projects/freight/Shipments/onlyJsprit/pickupAndDelivery/";


	public static void main(String[] args) throws IOException {
		/*
		 * some preparation - set logging level
		 */
		Logger.getRootLogger().setLevel(Level.INFO);
		
		/*
		 * some preparation - create output folder
		 */
		OutputDirectoryLogging.initLoggingWithOutputDirectory(OUTPUT_DIR);

		/*
		 * get a vehicle type-builder and build a type with the typeId "vehicleType" and a capacity of 2
		 */
		VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("vehicleType").addCapacityDimension(0, 2);
		VehicleType vehicleType = vehicleTypeBuilder.build();

		/*
		 * get a vehicle-builder and build a vehicle located at (10,10) with type "vehicleType"
		 */
		Builder vehicleBuilder = VehicleImpl.Builder.newInstance("vehicle");
		vehicleBuilder.setStartLocation(loc(Coordinate.newInstance(10, 10)));
		vehicleBuilder.setType(vehicleType);
		VehicleImpl vehicle = vehicleBuilder.build();

		/*
		 * build shipments at the required locations, each with a capacity-demand of 1.
		 * 4 shipments
		 * 1: (5,7)->(6,9)
		 * 2: (5,13)->(6,11)
		 * 3: (15,7)->(14,9)
		 * 4: (15,13)->(14,11)
		 */

		Shipment shipment1 = Shipment.Builder.newInstance("1").addSizeDimension(0, 1).setPickupLocation(loc(Coordinate.newInstance(5, 7))).setDeliveryLocation(loc(Coordinate.newInstance(6, 9))).build();
		Shipment shipment2 = Shipment.Builder.newInstance("2").addSizeDimension(0, 1).setPickupLocation(loc(Coordinate.newInstance(5, 13))).setDeliveryLocation(loc(Coordinate.newInstance(6, 11))).build();

		Shipment shipment3 = Shipment.Builder.newInstance("3").addSizeDimension(0, 1).setPickupLocation(loc(Coordinate.newInstance(15, 7))).setDeliveryLocation(loc(Coordinate.newInstance(14, 9))).build();
		Shipment shipment4 = Shipment.Builder.newInstance("4").addSizeDimension(0, 1).setPickupLocation(loc(Coordinate.newInstance(15, 13))).setDeliveryLocation(loc(Coordinate.newInstance(14, 11))).build();


		VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
		vrpBuilder.addVehicle(vehicle);
		vrpBuilder.addJob(shipment1).addJob(shipment2).addJob(shipment3).addJob(shipment4);

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

}
