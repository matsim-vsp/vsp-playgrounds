
package playground.kturner.freightKt;

import java.io.IOException;
import java.util.Collection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.core.controler.OutputDirectoryLogging;

import com.graphhopper.jsprit.analysis.toolbox.GraphStreamViewer;
import com.graphhopper.jsprit.analysis.toolbox.Plotter;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.constraint.HardActivityConstraint;
import com.graphhopper.jsprit.core.problem.constraint.ServiceDeliveriesFirstConstraint;
import com.graphhopper.jsprit.core.problem.constraint.VehicleDependentTimeWindowConstraints;
import com.graphhopper.jsprit.core.problem.job.Delivery;
import com.graphhopper.jsprit.core.problem.job.Pickup;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl.Builder;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;
import com.graphhopper.jsprit.core.util.Coordinate;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.io.problem.VrpXMLWriter;

/**
 * Based on SimpleEnRoutePickupAndDeliveryWithDepotBoundedDeliveriesExample (see com.graphhopper.jsprit.examples)
 * 
 * @author kturner
 *
 */
public class JspritWithShipments {

	private static final Logger log = Logger.getLogger(JspritWithShipments.class);
	
	private static final String OUTPUT_DIR = "../../OutputKMT/projects/freight/Shipments/jspritOnly/PickupAndDeliveryLocation/";

	private static final String LOG_DIR = OUTPUT_DIR + "Logs/";
	
	public static void main(String[] args) throws IOException {
		/*
		 * some preparation - set logging level
		 */
		Logger.getRootLogger().setLevel(Level.DEBUG);
		
		/*
		 * some preparation - create output folder
		 */
		OutputDirectoryLogging.initLoggingWithOutputDirectory(LOG_DIR);

		/*
         * get a vehicle type-builder and build a type with the typeId "vehicleType" and a capacity of 2
		 */
        VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("vehicleType").addCapacityDimension(0, 2).setFixedCost(1000.0);
        VehicleType vehicleType = vehicleTypeBuilder.build();

		/*
         * get a vehicle-builder and build a vehicle located at (10,10) with type "vehicleType"
		 */
        Builder vehicleBuilder1 = VehicleImpl.Builder.newInstance("vehicle1");
        vehicleBuilder1.setStartLocation(loc(Coordinate.newInstance(10, 0)));
        vehicleBuilder1.setType(vehicleType);
        VehicleImpl vehicle1 = vehicleBuilder1.build();
        
        Builder vehicleBuilder2 = VehicleImpl.Builder.newInstance("vehicle2");
        vehicleBuilder2.setStartLocation(loc(Coordinate.newInstance(10, 20)));
        vehicleBuilder2.setType(vehicleType);
        VehicleImpl vehicle2 = vehicleBuilder2.build();
        

//		/*
//         * build shipments at the required locations, each with a capacity-demand of 1.
//		 * 4 shipments
//		 * 1: (5,7)->(6,9)
//		 * 2: (5,13)->(6,11)
//		 * 3: (15,7)->(14,9)
//		 * 4: (15,13)->(14,11)
//		 */
//
//        Shipment shipment1 = Shipment.Builder.newInstance("1").addSizeDimension(0, 1).setPickupLocation(loc(Coordinate.newInstance(5, 7))).setDeliveryLocation(loc(Coordinate.newInstance(6, 9))).build();
//        Shipment shipment2 = Shipment.Builder.newInstance("2").addSizeDimension(0, 1).setPickupLocation(loc(Coordinate.newInstance(5, 13))).setDeliveryLocation(loc(Coordinate.newInstance(6, 11))).build();
//
//        Shipment shipment3 = Shipment.Builder.newInstance("3").addSizeDimension(0, 1).setPickupLocation(loc(Coordinate.newInstance(15, 7))).setDeliveryLocation(loc(Coordinate.newInstance(14, 9))).build();
//        Shipment shipment4 = Shipment.Builder.newInstance("4").addSizeDimension(0, 1).setPickupLocation(loc(Coordinate.newInstance(15, 13))).setDeliveryLocation(loc(Coordinate.newInstance(14, 11))).build();


        //
        /*
         * build deliveries, (implicitly picked up in the depot)
		 * 1: (4,8)
		 * 2: (4,12)
		 * 3: (16,8)
		 * 4: (16,12)
		 */
        Delivery delivery1 = Delivery.Builder.newInstance("5").addSizeDimension(0, 1).setLocation(loc(Coordinate.newInstance(4, 8))).build();
        Delivery delivery2 = Delivery.Builder.newInstance("6").addSizeDimension(0, 1).setLocation(loc(Coordinate.newInstance(4, 12))).build();
        Delivery delivery3 = Delivery.Builder.newInstance("7").addSizeDimension(0, 1).setLocation(loc(Coordinate.newInstance(16, 8))).build();
        Delivery delivery4 = Delivery.Builder.newInstance("8").addSizeDimension(0, 1).setLocation(loc(Coordinate.newInstance(16, 12))).build();
        
//        /*
//         * build pickups, (implicitly picked up in the depot)
//		 * 1: (8,4)
//		 * 2: (12,4)
//		 * 3: (8,16)
//		 * 4: (12,16)
//		 */
//        Pickup pickup1 = Pickup.Builder.newInstance("9").addSizeDimension(0, 1).setLocation(loc(Coordinate.newInstance(8, 4))).build();
//        Pickup pickup2 = Pickup.Builder.newInstance("10").addSizeDimension(0, 1).setLocation(loc(Coordinate.newInstance(12, 4))).build();
//        Pickup pickup3 = Pickup.Builder.newInstance("11").addSizeDimension(0, 1).setLocation(loc(Coordinate.newInstance(8, 16))).build();
//        Pickup pickup4 = Pickup.Builder.newInstance("12").addSizeDimension(0, 1).setLocation(loc(Coordinate.newInstance(12, 16))).build();

        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
        vrpBuilder.addVehicle(vehicle1).addVehicle(vehicle2);
        vrpBuilder
//        	.addJob(shipment1).addJob(shipment2).addJob(shipment3).addJob(shipment4)
            .addJob(delivery1).addJob(delivery2).addJob(delivery3).addJob(delivery4)
//          .addJob(pickup1).addJob(pickup2).addJob(pickup3).addJob(pickup4)
            .build();

        VehicleRoutingProblem problem = vrpBuilder.build();

		/*
         * build the algorithm
		 */

        StateManager stateManager = new StateManager(problem);
        ConstraintManager constraintManager = new ConstraintManager(problem, stateManager);
        constraintManager.addConstraint(new ServiceDeliveriesFirstConstraint(), ConstraintManager.Priority.CRITICAL);
        constraintManager.addConstraint(new VehicleDependentTimeWindowConstraints(stateManager, problem.getTransportCosts(), problem.getActivityCosts()), ConstraintManager.Priority.HIGH);
        
        VehicleRoutingAlgorithm algorithm = Jsprit.Builder.newInstance(problem).setStateAndConstraintManager(stateManager,constraintManager).buildAlgorithm();

		/*
         * and search a solution
		 */
        Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();

		/*
		 * get the best
		 */
        VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

        new VrpXMLWriter(problem, solutions).write(OUTPUT_DIR + "mixed-shipments-services-problem-with-solution.xml");

        SolutionPrinter.print(bestSolution);

		/*
		 * plot
		 */
        Plotter problemPlotter = new Plotter(problem);
        problemPlotter.plotShipments(true);
//        problemPlotter.plot(OUTPUT_DIR + "simpleMixedEnRoutePickupAndDeliveryExample_problem.png", "en-route pd and depot bounded deliveries");

        Plotter solutionPlotter = new Plotter(problem, Solutions.bestOf(solutions));
        solutionPlotter.plotShipments(true);
//        solutionPlotter.plot(OUTPUT_DIR + "simpleMixedEnRoutePickupAndDeliveryExample_solution.png", "en-route pd and depot bounded deliveries");
        
		new GraphStreamViewer(problem).setRenderShipments(true).display();
		new GraphStreamViewer(problem, Solutions.bestOf(solutions)).setRenderDelay(50).display();


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
