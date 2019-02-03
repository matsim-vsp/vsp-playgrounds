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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
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
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * Created by amit on 18.11.17.
 */

public class MultiModeNetworkTest {
    @Rule
    public MatsimTestUtils helper = new MatsimTestUtils();

    private final String transportModes [] = new String [] {"bike","car"};

    @Test
    public void useLinksTest(){
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        createMultiModeNetwork(scenario);
        Population population = scenario.getPopulation();

        VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create(transportModes[0], VehicleType.class));
        bike.setMaximumVelocity(5);
        bike.setPcuEquivalents(0.25);

        VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(transportModes[1], VehicleType.class));
        car.setMaximumVelocity(20);
        car.setPcuEquivalents(1.0);

        VehicleType [] vehTypes = {bike, car};

        for(int i=0;i<2;i++){
            String mode = transportModes[i];
            Id<Person> id = Id.create(i, Person.class);
            Person p = population.getFactory().createPerson(id);
            Plan plan = population.getFactory().createPlan();
            p.addPlan(plan);

            if (mode.equals("car")) {
                Activity a1 = population.getFactory().createActivityFromLinkId("h", Id.createLinkId("1"));
                a1.setEndTime(8*3600+i*5);
                Leg leg = population.getFactory().createLeg(mode);
                plan.addActivity(a1);
                plan.addLeg(leg);
                LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
                NetworkRoute route = (NetworkRoute) factory.createRoute(Id.createLinkId("1"), Id.createLinkId("3"));
                route.setLinkIds(Id.createLinkId("1"), Arrays.asList(Id.createLinkId("2")), Id.createLinkId("3"));
                leg.setRoute(route);

                Activity a2 = population.getFactory().createActivityFromLinkId("w", Id.createLinkId("3"));
                plan.addActivity(a2);
                population.addPerson(p);
            } else {
                Activity a1 = population.getFactory().createActivityFromLinkId("h", Id.createLinkId("12_bike"));
                a1.setEndTime(8*3600+i*5);
                Leg leg = population.getFactory().createLeg(mode);
                plan.addActivity(a1);
                plan.addLeg(leg);
                // it looks, like, there is no exception if route is not provided.
                LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
                NetworkRoute route = (NetworkRoute) factory.createRoute(Id.createLinkId("12_bike"), Id.createLinkId("3_bike"));
                route.setLinkIds(Id.createLinkId("12_bike"), Arrays.asList(), Id.createLinkId("3_bike"));
                leg.setRoute(route);

                Activity a2 = population.getFactory().createActivityFromLinkId("w", Id.createLinkId("3_bike"));
                plan.addActivity(a2);
                population.addPerson(p);
            }

            //adding vehicle type -- vehicleSource is modeVehicleTypesFromVehiclesData
            if(! scenario.getVehicles().getVehicleTypes().containsKey(vehTypes[i].getId())) {
                scenario.getVehicles().addVehicleType(vehTypes[i]);
            }
        }

        Config config = scenario.getConfig();

        List<String> networkModes = new ArrayList<>(Arrays.asList( transportModes));
        config.qsim().setMainModes(networkModes);
        config.plansCalcRoute().setNetworkModes(networkModes);
        config.travelTimeCalculator().setAnalyzedModesAsString( StringUtils.join(networkModes, "," ) );
        config.travelTimeCalculator().setSeparateModes(true);

        config.planCalcScore().getOrCreateModeParams("bike").setConstant(0.);

        config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);

        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

        // reset all mode routing params.
        config.plansCalcRoute().getOrCreateModeRoutingParams("xxx").setTeleportedModeFreespeedFactor(1.);

        config.controler().setOutputDirectory(helper.getOutputDirectory());

        config.controler().setLastIteration(0);

        PlanCalcScoreConfigGroup.ActivityParams homeAct = new PlanCalcScoreConfigGroup.ActivityParams("h");
        PlanCalcScoreConfigGroup.ActivityParams workAct = new PlanCalcScoreConfigGroup.ActivityParams("w");
        homeAct.setTypicalDuration(1. * 3600.);
        workAct.setTypicalDuration(1. * 3600.);

        config.planCalcScore().addActivityParams(homeAct);
        config.planCalcScore().addActivityParams(workAct);

        final Controler cont = new Controler(scenario);
        cont.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        final Map<Id<Person>, Tuple<Id<Link>,String>> person2departurelink2mode = new HashMap<>();
        final Map<Id<Person>, Tuple<Id<Link>,String>> person2arrivallink2mode = new HashMap<>();
        cont.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(new PersonDepartureEventHandler() {
                    @Override
                    public void handleEvent(PersonDepartureEvent event) {
                        person2departurelink2mode.put(event.getPersonId(), new Tuple<>(event.getLinkId(), event.getLegMode()));
                    }
                });
                addEventHandlerBinding().toInstance(new PersonArrivalEventHandler() {
                    @Override
                    public void handleEvent(PersonArrivalEvent event) {
                        person2arrivallink2mode.put(event.getPersonId(), new Tuple<>(event.getLinkId(), event.getLegMode()));
                    }
                });
            }
        });

        cont.run();

        // check if they depart and arrive on right links...
        {
            //person 0
            Id<Person> personId = Id.createPersonId("0");
            Assert.assertEquals("Wrong mode for person "+ personId, person2departurelink2mode.get(personId).getSecond(), "bike" );
            Assert.assertEquals("Wrong departure link for person "+ personId, person2departurelink2mode.get(personId).getFirst(), Id.createLinkId("12_bike"));

            Assert.assertEquals("Wrong mode for person "+ personId, person2arrivallink2mode.get(personId).getSecond(), "bike" );
            Assert.assertEquals("Wrong departure link for person "+ personId, person2arrivallink2mode.get(personId).getFirst(), Id.createLinkId("3_bike"));
        }
        {
            //person 1
            Id<Person> personId = Id.createPersonId("1");
            Assert.assertEquals("Wrong mode for person "+ personId, person2departurelink2mode.get(personId).getSecond(), "car" );
            Assert.assertEquals("Wrong departure link for person "+ personId, person2departurelink2mode.get(personId).getFirst(), Id.createLinkId("1") );

            Assert.assertEquals("Wrong mode for person "+ personId, person2arrivallink2mode.get(personId).getSecond(), "car" );
            Assert.assertEquals("Wrong departure link for person "+ personId, person2arrivallink2mode.get(personId).getFirst(), Id.createLinkId("3") );
        }
    }

//    @Test
    public void useCoordsTest(){
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        createMultiModeNetwork(scenario);
        List<Coord> coords = Arrays.asList(new Coord(-101.,0.),new Coord(0.,1101.) );

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
            Activity a1 = population.getFactory().createActivityFromCoord("h", coords.get(0));
            a1.setEndTime(8*3600+i*5);
            Leg leg = population.getFactory().createLeg(transportModes[i]);
            plan.addActivity(a1);
            plan.addLeg(leg);

            Activity a2 = population.getFactory().createActivityFromCoord("w", coords.get(1));
            plan.addActivity(a2);
            population.addPerson(p);

            //adding vehicle type -- vehicleSource is modeVehicleTypesFromVehiclesData
            if(! scenario.getVehicles().getVehicleTypes().containsKey(vehTypes[i].getId())) {
                scenario.getVehicles().addVehicleType(vehTypes[i]);
            }
        }

        Config config = scenario.getConfig();

        List<String> networkModes = new ArrayList<>(Arrays.asList( transportModes));
        config.qsim().setMainModes(networkModes);
        config.plansCalcRoute().setNetworkModes(networkModes);
        config.travelTimeCalculator().setAnalyzedModesAsString( StringUtils.join(networkModes, "," ) );
        config.travelTimeCalculator().setSeparateModes(true);

        config.planCalcScore().getOrCreateModeParams("bike").setConstant(0.);

        config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);

        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

        // reset all mode routing params.
        config.plansCalcRoute().getOrCreateModeRoutingParams("xxx").setTeleportedModeFreespeedFactor(1.);

        config.controler().setOutputDirectory(helper.getOutputDirectory());

        config.controler().setLastIteration(0);

        PlanCalcScoreConfigGroup.ActivityParams homeAct = new PlanCalcScoreConfigGroup.ActivityParams("h");
        PlanCalcScoreConfigGroup.ActivityParams workAct = new PlanCalcScoreConfigGroup.ActivityParams("w");
        homeAct.setTypicalDuration(1. * 3600.);
        workAct.setTypicalDuration(1. * 3600.);

        config.planCalcScore().addActivityParams(homeAct);
        config.planCalcScore().addActivityParams(workAct);

        final Controler cont = new Controler(scenario);
        cont.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        final Map<Id<Person>, Tuple<Id<Link>,String>> person2departurelink2mode = new HashMap<>();
        final Map<Id<Person>, Tuple<Id<Link>,String>> person2arrivallink2mode = new HashMap<>();
        cont.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(new PersonDepartureEventHandler() {
                    @Override
                    public void handleEvent(PersonDepartureEvent event) {
                        person2departurelink2mode.put(event.getPersonId(), new Tuple<>(event.getLinkId(), event.getLegMode()));
                    }
                });
                addEventHandlerBinding().toInstance(new PersonArrivalEventHandler() {
                    @Override
                    public void handleEvent(PersonArrivalEvent event) {
                        person2arrivallink2mode.put(event.getPersonId(), new Tuple<>(event.getLinkId(), event.getLegMode()));
                    }
                });
            }
        });

        cont.run();

        // check if they depart and arrive on right links...
        {
            //person 0
            Id<Person> personId = Id.createPersonId("0");
            Assert.assertEquals("Wrong mode for person "+ personId, person2departurelink2mode.get(personId).getSecond(), "bike" );
            Assert.assertEquals("Wrong departure link for person "+ personId,  person2departurelink2mode.get(personId).getFirst(), Id.createLinkId("12_bike").toString());

            Assert.assertEquals("Wrong mode for person "+ personId,  person2arrivallink2mode.get(personId).getSecond(), "bike" );
            Assert.assertEquals("Wrong departure link for person "+ personId, person2arrivallink2mode.get(personId).getFirst(), Id.createLinkId("3_bike").toString());
        }
        {
            //person 1
            Id<Person> personId = Id.createPersonId("1");
            Assert.assertEquals("Wrong mode for person "+ personId, person2departurelink2mode.get(personId).getSecond(), "car" );
            Assert.assertEquals("Wrong departure link for person "+ personId, person2departurelink2mode.get(personId).getFirst(), Id.createLinkId("1").toString() );

            Assert.assertEquals("Wrong mode for person "+ personId, person2arrivallink2mode.get(personId).getSecond(), "car" );
            Assert.assertEquals("Wrong departure link for person "+ personId, person2arrivallink2mode.get(personId).getFirst(), Id.createLinkId("3").toString() );
        }
    }

    private void createMultiModeNetwork(Scenario scenario){
        Network network = scenario.getNetwork();

        double x = -100.0;
        Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(x, 0.0));
        Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(0.0, 0.0));
        Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord(0.0, 1000.0));
        Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord(0.0, 1100.0));

        { //car links
            Link link1 = NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), node1, node2, (double) 100, (double) 25, (double) 600, (double) 1, null, "22");
            Link link2 = NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), node2, node3, (double) 1000, (double) 25, (double) 600, (double) 1, null, "22");
            Link link3 = NetworkUtils.createAndAddLink(network,Id.create("3", Link.class), node3, node4, (double) 100, (double) 25, (double) 600, (double) 1, null, "22");

            link1.setAllowedModes(new HashSet<>(Arrays.asList("car")));
            link2.setAllowedModes(new HashSet<>(Arrays.asList("car")));
            link3.setAllowedModes(new HashSet<>(Arrays.asList("car")));
        }
        {//bikeLinks
            Link link1 = NetworkUtils.createAndAddLink(network,Id.create("12_bike", Link.class), node1, node3, (double) 1100, (double) 25, (double) 600, (double) 1, null, "22");
            Link link3 = NetworkUtils.createAndAddLink(network,Id.create("3_bike", Link.class), node3, node4, (double) 100, (double) 25, (double) 600, (double) 1, null, "22");

            link1.setAllowedModes(new HashSet<>(Arrays.asList("bike")));
            link3.setAllowedModes(new HashSet<>(Arrays.asList("bike")));
        }
    }
}
