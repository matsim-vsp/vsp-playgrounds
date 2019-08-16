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
  
/**
 * 
 */
package playground.kturner.jsprit;

import java.util.Collection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeWriter;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import com.graphhopper.jsprit.analysis.toolbox.Plotter;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.io.problem.VrpXMLWriter;

/**
 * @author kturner
 *
 */
public class RunJspritOnly {
	
	private static final Logger log = Logger.getLogger(RunJspritOnly.class);
	private static final String INPUT_DIR = "../../demo-input/VW4TS/" ;
	private static final String OUTPUT_DIR = "../../demo-input/VW4TS/Output/" ;


	public static void main(String[] args) {
		Logger.getRootLogger().setLevel(Level.DEBUG);
//		Logger.getRootLogger().setLevel(Level.INFO);

		
		//Create carrier with services
		Carriers carriers = new Carriers() ;
		new CarrierPlanXmlReaderV2(carriers).readFile(INPUT_DIR+ "carrier_definition_debug.xml");

		//Create vehicle type
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		new CarrierVehicleTypeReader(vehicleTypes).readFile(INPUT_DIR + "carrier_vehicletypes.xml");

		// load vehicle types for the carriers
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;

		//load Network and build netbasedCosts
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(INPUT_DIR + "network_editedPt.xml.gz");

		new CarrierPlanXmlWriterV2( carriers ).write( OUTPUT_DIR + "carriers-wo-plans.xml" );
		new CarrierVehicleTypeWriter( CarrierVehicleTypes.getVehicleTypes( carriers ) ).write( OUTPUT_DIR + "carrierTypes.xml" );

		// matrix costs between locations (cost matrix)
		Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance( network, vehicleTypes.getVehicleTypes().values() );
		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build() ;


		// time dependent network (1800 = 30 min) --> (option live request)
		netBuilder.setTimeSliceWidth(900) ; // !!!!, otherwise it will not do anything.

		carriers.getCarriers().values().parallelStream().forEach(carrier -> {
			//Build VRP for jsprit
			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);
			vrpBuilder.setRoutingCost(netBasedCosts) ;
			VehicleRoutingProblem problem = vrpBuilder.build();

			// get the algorithm out-of-the-box, search solution with jsprit and get the best one.
//			VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);
			VehicleRoutingAlgorithm algorithm = Jsprit.createAlgorithm(problem);
			algorithm.setMaxIterations(50);
			Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
			VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

			//Routing bestPlan to Network
			CarrierPlan carrierPlanServicesAndShipments = MatsimJspritFactory.createPlan(carrier, bestSolution) ;
			NetworkRouter.routePlan(carrierPlanServicesAndShipments,netBasedCosts) ;
			carrier.setSelectedPlan(carrierPlanServicesAndShipments) ;

			new VrpXMLWriter(problem, solutions).write(OUTPUT_DIR+ "solutions_" + carrier.getId().toString() + ".xml");
		});
		
		new CarrierPlanXmlWriterV2( carriers ).write( OUTPUT_DIR + "plannedCarriers.xml" );

	}

}
