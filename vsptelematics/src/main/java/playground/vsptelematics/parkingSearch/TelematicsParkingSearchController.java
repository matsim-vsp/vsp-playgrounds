/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.vsptelematics.parkingSearch;

import com.google.inject.name.Names;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dynagent.run.DynRoutingModule;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.evaluation.ParkingListener;
import org.matsim.contrib.parking.parkingsearch.evaluation.ParkingSearchEvaluator;
import org.matsim.contrib.parking.parkingsearch.evaluation.ParkingSlotVisualiser;
import org.matsim.contrib.parking.parkingsearch.evaluation.ZoneParkingOccupationListener;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.manager.WalkLegFactory;
import org.matsim.contrib.parking.parkingsearch.manager.ZoneParkingManager;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.NoVehicleTeleportationLogic;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationLogic;
import org.matsim.contrib.parking.parkingsearch.routing.ParkingRouter;
import org.matsim.contrib.parking.parkingsearch.routing.WithinDayParkingRouter;
import org.matsim.contrib.parking.parkingsearch.sim.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.PrepareForSim;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.PopulationModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentConfigurator;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Random;


public class TelematicsParkingSearchController {

    /**
     * assumes that parkZone.txt lies next to the config file and contains a tabular file showing link id's in the first column that define the
     * zone investigated by the ZoneParkingManager
     */
    public static void main(String[] args){

//        String configStr = "C:/Users/Work/VSP/WiMi/TeachParking/input/config.xml";
        String configStr = args[0];

        run(configStr);
    }


    /**
     * @param configStr
     * 			path to the MATSim config
     */
    private static void run(String configStr) {

        Config config = ConfigUtils.loadConfig(configStr);
        String zone = configStr.substring(0,configStr.lastIndexOf("/") + 1 ) + "parkZone.txt";
        String[] zones = new String[1];
        zones[0] = zone;
        //all further input files are set in the config.

        //get the parking search config group to set some parameters, like agent's search strategy or average parking slot length
        ParkingSearchConfigGroup configGroup = ConfigUtils.addOrGetModule(config, ParkingSearchConfigGroup.class);

        config.controler().setLastIteration(1);

        final Scenario scenario = ScenarioUtils.loadScenario(config);
        createPop(scenario);
        Controler controler = new Controler(scenario);
        config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.withHoles);

        config.controler().setCreateGraphs(false);

        controler.addOverridingModule(new AbstractModule() {

            @Override
            public void install() {
                ParkingSlotVisualiser visualiser = new ParkingSlotVisualiser(scenario);
                addEventHandlerBinding().toInstance(visualiser);
                addControlerListenerBinding().toInstance(visualiser);
            }
        });

        int start = (int) (config.qsim().getStartTime() / 3600);
        int end = (int) (config.qsim().getEndTime() / 3600);

        installParkingModules(controler, scenario, zones, start, end);
        controler.run();

    }


    private static void createPop(Scenario scenario) {

        Population population = scenario.getPopulation();
        PopulationFactory fac = population.getFactory();
        Random rand = MatsimRandom.getRandom();

        Id<Link> homeLink = Id.createLinkId("5");
        Id<Link> workLink = Id.createLinkId("77");

        for (int i = 0; i < 120; i++) {
            Person agent = fac.createPerson(Id.createPersonId("Agent_" + i));
            Plan agentPlan = fac.createPlan();
            Activity homeAct1 = fac.createActivityFromLinkId("home", homeLink);
            homeAct1.setEndTime(8 * 3600 + i * 60);
            agentPlan.addActivity(homeAct1);

            agentPlan.addLeg(fac.createLeg("car"));

            Activity workAct = fac.createActivityFromLinkId("work", workLink);
            workAct.setEndTime(9.50 * 3600 + i * 60);
            agentPlan.addActivity(workAct);

            agentPlan.addLeg(fac.createLeg("car"));

            Activity homeAct2 = fac.createActivityFromLinkId("home", homeLink);
            agentPlan.addActivity(homeAct2);
            agent.addPlan(agentPlan);

            population.addPerson(agent);
        }

    }

    private static void installParkingModules(Controler controler, Scenario scenario, String[] pathToZones, int startOfEvaluatedHours, int endOfEvaluatedHours) {
            // No need to route car routes in Routing module in advance, as they are
            // calculated on the fly
            if (!controler.getConfig().getModules().containsKey(DvrpConfigGroup.GROUP_NAME)){
                controler.getConfig().addModule(new DvrpConfigGroup());
            }
            final DynRoutingModule routingModuleCar = new DynRoutingModule(TransportMode.car);
            StageActivityTypes stageActivityTypesCar = new StageActivityTypes() {
                @Override
                public boolean isStageActivity(String activityType) {

                    return (activityType.equals(ParkingUtils.PARKACTIVITYTYPE));
                }
            };
            routingModuleCar.setStageActivityTypes(stageActivityTypesCar);
            controler.addOverridingModule(new DvrpTravelTimeModule());
            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    addRoutingModuleBinding(TransportMode.car).toInstance(routingModuleCar);
                    bind(Network.class).annotatedWith(Names.named(DvrpRoutingNetworkProvider.DVRP_ROUTING)).to(Network.class).asEagerSingleton();
                    bind(WalkLegFactory.class).asEagerSingleton();
                    bind(PrepareForSim.class).to(ParkingSearchPrepareForSimImpl.class);
                    this.install(new ParkingSearchQSimModule());
                    bind(ParkingRouter.class).to(WithinDayParkingRouter.class);

                    //parking manager
                    bind(ParkingSearchManager.class).toInstance(new ZoneParkingManager(scenario, pathToZones));

                    addControlerListenerBinding().to(ParkingListener.class);

                    //analysis
//                    addMobsimListenerBinding().to(TelematicsZoneOccupationListener.class).asEagerSingleton();
                    addMobsimListenerBinding().to(ZoneParkingOccupationListener.class).asEagerSingleton();
                    SearchTimeEvaluator parkingEvaluator = new SearchTimeEvaluator(scenario.getNetwork().getLinks().keySet(), startOfEvaluatedHours, endOfEvaluatedHours);
                    addEventHandlerBinding().toInstance(parkingEvaluator);

                    ParkingSearchEvaluator walkEvaluator = new ParkingSearchEvaluator();
                    addEventHandlerBinding().toInstance(walkEvaluator);
                    addControlerListenerBinding().toInstance(new IterationEndsListener() {
                        @Override
                        public void notifyIterationEnds(IterationEndsEvent event) {
                            String iterationPath = event.getServices().getControlerIO().getIterationPath(event.getIteration());
                            walkEvaluator.writeEgressWalkStatistics(iterationPath);
                        }
                    });

                    addControlerListenerBinding().toInstance(parkingEvaluator);

                    //do not teleport vehicles
                    bind(VehicleTeleportationLogic.class).to(NoVehicleTeleportationLogic.class);
                }
            });

            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    QSimComponentsConfig components = new QSimComponentsConfig();

                    new StandardQSimComponentConfigurator(controler.getConfig()).configure(components);
                    components.removeNamedComponent(PopulationModule.COMPONENT_NAME);
                    components.addNamedComponent(ParkingSearchPopulationModule.COMPONENT_NAME);

                    bind(QSimComponentsConfig.class).toInstance(components);
                }
            });

    }



}
