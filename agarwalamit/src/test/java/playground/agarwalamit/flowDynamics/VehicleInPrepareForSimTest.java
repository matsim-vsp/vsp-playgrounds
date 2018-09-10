/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.flowDynamics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * Created by amit on 28.08.17.
 */

@RunWith(Parameterized.class)
public class VehicleInPrepareForSimTest {

    private final QSimConfigGroup.VehiclesSource vehicleSource;
    private final boolean modeChoice;

    @Rule
    public MatsimTestUtils helper = new MatsimTestUtils();

    private final String transportModes [] = new String [] {"bike","car"};

    public VehicleInPrepareForSimTest(QSimConfigGroup.VehiclesSource vehicleSource, boolean modeChoice) {
        this.vehicleSource = vehicleSource;
        this.modeChoice = modeChoice;
    }

    @Parameterized.Parameters(name = "{index}: vehicleSource == {0}; isUsingModeChoice == {1}")
    public static Collection<Object[]> parameterObjects () {
        int nrow = QSimConfigGroup.VehiclesSource.values().length * 2;
        Object [] [] vehicleSources = new Object [nrow][2];
        int index = 0;
        for (QSimConfigGroup.VehiclesSource vs : QSimConfigGroup.VehiclesSource.values()) {
            vehicleSources[index++] = new Object [] {vs, true};
            vehicleSources[index++] = new Object [] {vs, false};
        }
        return Arrays.asList(vehicleSources);
    }

    /**
     * test if same vehicle is used over the iterations if mode choice is used or not.
     */
    @Test
    public void vehicleTest() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        createNetwork(scenario);
        createPlans(scenario);

        Config config = scenario.getConfig();

        List<String> networkModes = new ArrayList<>(Arrays.asList( transportModes));
        config.qsim().setMainModes(networkModes);
        config.plansCalcRoute().setNetworkModes(networkModes);
        config.travelTimeCalculator().setAnalyzedModes( StringUtils.join(networkModes, ",") );
        config.travelTimeCalculator().setSeparateModes(true);

        config.planCalcScore().getOrCreateModeParams("bike").setConstant(0.);

        config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);

        config.qsim().setVehiclesSource(this.vehicleSource);

        // reset all mode routing params.
        config.plansCalcRoute().getOrCreateModeRoutingParams("xxx").setTeleportedModeFreespeedFactor(1.);

        StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
        if (this.modeChoice) {
            strategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode);
            config.changeMode().setModes(transportModes);
        } else {
            strategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
        }
        strategySettings.setWeight(1);

        config.strategy().addStrategySettings(strategySettings);

        config.controler().setOutputDirectory(helper.getOutputDirectory());

        config.controler().setLastIteration(5);
        config.controler().setWriteEventsInterval(0);

        PlanCalcScoreConfigGroup.ActivityParams homeAct = new PlanCalcScoreConfigGroup.ActivityParams("h");
        PlanCalcScoreConfigGroup.ActivityParams workAct = new PlanCalcScoreConfigGroup.ActivityParams("w");
        homeAct.setTypicalDuration(1. * 3600.);
        workAct.setTypicalDuration(1. * 3600.);

        config.planCalcScore().addActivityParams(homeAct);
        config.planCalcScore().addActivityParams(workAct);

        final Controler cont = new Controler(scenario);
        cont.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        final Map<Id<Person>, Map<Integer, List<Vehicle>>> personId2Iteration2Vehicles = new HashMap<>();

        cont.addControlerListener(new BeforeMobsimListener() {
            @Inject private Scenario injectedScenario;
            @Override
            public void notifyBeforeMobsim(BeforeMobsimEvent event) {
                // at this step, vehicles must be available for all persons irrespective of vehicleSource
                for (Person person : injectedScenario.getPopulation().getPersons().values()) {
                    Map<Integer, List<Vehicle>> iteration2vehicles = personId2Iteration2Vehicles.get(person.getId());

                    if (iteration2vehicles == null ) {
                        iteration2vehicles = new HashMap<>();
                    }

                    List<Vehicle> vehicleList = injectedScenario.getVehicles()
                                                                .getVehicles()
                                                                .values()
                                                                .stream()
                                                                .filter(v -> v.getId()
                                                                              .toString()
                                                                              .startsWith(person.getId().toString()))
                                                                .collect(Collectors.toList());

                    iteration2vehicles.put(event.getIteration(), vehicleList);
                    personId2Iteration2Vehicles.put(person.getId(), iteration2vehicles);
                }
            }
        });

        cont.run();

        // now check if the vehicles are same in all iterations.

        for(Id<Person> personId : personId2Iteration2Vehicles.keySet()) {
            boolean allVehiclesSame = true;
            Iterator<List<Vehicle>> vehiclesListIterator = personId2Iteration2Vehicles.get(personId).values().iterator();
            List<Vehicle> firstIterationVehicles = vehiclesListIterator.next();

            // if using mode choice, different vehicles are created on the first place based on the network modes
            if (this.vehicleSource.equals(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData) && this.modeChoice ) {
                Assert.assertEquals(" Number of vehicles for person must be two.", 2, firstIterationVehicles.size(), MatsimTestUtils.EPSILON);

                Assert.assertTrue("None of the vehicle id has suffix bike.",
                        firstIterationVehicles.stream()
                                              .filter(v -> v.getId().toString().split("_").length>1)
                                              .map(v -> v.getId().toString().split("_")[1])
                                              .collect(Collectors.toList())
                                              .contains("bike"));
            }

            while (vehiclesListIterator.hasNext()) {
                List<Vehicle> nextIterationVehicle = vehiclesListIterator.next();

                if (firstIterationVehicles.size() != nextIterationVehicle.size()) throw new RuntimeException("Number of vehicles for person "+ personId + " are not samve over iteration.");

                for(int index = 0; index < Math.max(firstIterationVehicles.size(), nextIterationVehicle.size()); index++) {
                    System.out.println(nextIterationVehicle.get(index).getId().toString());
                    if (! firstIterationVehicles.get(index).equals(nextIterationVehicle.get(index)) ) {
                        allVehiclesSame = false;
                        break;
                    }
                }

                if(!allVehiclesSame) {
                    break;
                }
            }

            if(!allVehiclesSame) {
                throw new RuntimeException("All vehicles are not same for person "+ personId);
            }
        }
    }

    private void createNetwork(Scenario scenario){
        Network network = scenario.getNetwork();

        double x = -100.0;
        Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(x, 0.0));
        Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(0.0, 0.0));
        Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord(0.0, 1000.0));
        Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord(0.0, 1100.0));

        Link link1 = NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), node1, node2, (double) 100, (double) 25, (double) 600, (double) 1, null, "22");
        Link link2 = NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), node2, node3, (double) 1000, (double) 25, (double) 600, (double) 1, null, "22");
        Link link3 = NetworkUtils.createAndAddLink(network,Id.create("3", Link.class), node3, node4, (double) 100, (double) 25, (double) 600, (double) 1, null, "22");

        link1.setAllowedModes(new HashSet<>(Arrays.asList(transportModes)));
        link2.setAllowedModes(new HashSet<>(Arrays.asList(transportModes)));
        link3.setAllowedModes(new HashSet<>(Arrays.asList(transportModes)));
    }

    private void createPlans(Scenario scenario){

        Population population = scenario.getPopulation();

        VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create(transportModes[0], VehicleType.class));
        bike.setMaximumVelocity(5);
        bike.setPcuEquivalents(0.25);

        VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(transportModes[1], VehicleType.class));
        car.setMaximumVelocity(20);
        car.setPcuEquivalents(1.0);

        VehicleType [] vehTypes = {bike, car};

        for(int i=0;i<2;i++){
            Id<Person> id = Id.create(i, Person.class);
            Person p = population.getFactory().createPerson(id);
            Plan plan = population.getFactory().createPlan();
            p.addPlan(plan);
            Activity a1 = population.getFactory().createActivityFromLinkId("h", Id.createLinkId("1"));
            a1.setEndTime(8*3600+i*5);
            Leg leg = population.getFactory().createLeg(transportModes[i]);
            plan.addActivity(a1);
            plan.addLeg(leg);
            LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
            NetworkRoute route = (NetworkRoute) factory.createRoute(Id.createLinkId("1"), Id.createLinkId("3"));
            route.setLinkIds(Id.createLinkId("1"), Arrays.asList(Id.createLinkId("2")), Id.createLinkId("3"));
            leg.setRoute(route);

            Activity a2 = population.getFactory().createActivityFromLinkId("w", Id.createLinkId("3"));
            plan.addActivity(a2);
            population.addPerson(p);

            //adding vehicle type and vehicle to scenario if vehicleSource is not modeVehicleTypesFromVehiclesData

            switch (this.vehicleSource) {
                case defaultVehicle:
                    break;
                case modeVehicleTypesFromVehiclesData:
                    if(! scenario.getVehicles().getVehicleTypes().containsKey(vehTypes[i].getId())) {
                        scenario.getVehicles().addVehicleType(vehTypes[i]);
                    }
                    break;
                case fromVehiclesData:
                    if(! scenario.getVehicles().getVehicleTypes().containsKey(vehTypes[i].getId())) {
                        scenario.getVehicles().addVehicleType(vehTypes[i]);
                    }

                    Id<Vehicle> vId = Id.create(p.getId(),Vehicle.class);
                    Vehicle v = VehicleUtils.getFactory().createVehicle(vId, vehTypes[i]);
                    scenario.getVehicles().addVehicle(v);
                    break;
                default:
                    throw new RuntimeException("not implemented yet.");
            }
        }
    }
}
