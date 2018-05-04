/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import playground.agarwalamit.fundamentalDiagrams.core.FundamentalDiagramConfigGroup;
import playground.agarwalamit.fundamentalDiagrams.core.FundamentalDiagramDataGenerator;

/**
 * Created by amit on 14.04.18.
 */

public class RunDynamicPCUExample {

    static final String VEHICLE_SPEED = "vehicleSpeed";

    public static void main(String[] args) {

        Config config = ConfigUtils.createConfig();
        
        config.controler().setOutputDirectory("../../svnit/outputFiles/carBicycle/passing/equalModalSplit_holeSpeedRandom15_20KPH/");
//        config.controler().setOutputDirectory("../svnit/outputFiles/carBicycleSeepage_equalModalShare_holeSpeed15kph/branch18Apr/");
        config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

        QSimConfigGroup qsim = config.qsim();
        qsim.setMainModes(Arrays.asList("car","bicycle"));
        qsim.setTrafficDynamics(TrafficDynamics.withHoles);
        qsim.setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
        qsim.setSeepModes(Collections.singletonList("bicycle"));
        
        FundamentalDiagramConfigGroup fdConfigGroup = ConfigUtils.addOrGetModule(config, FundamentalDiagramConfigGroup.class);
        fdConfigGroup.setModalShareInPCU("1.0");
        fdConfigGroup.setReduceDataPointsByFactor(1);
        
        Scenario scenario = ScenarioUtils.loadScenario(config);
        
        Vehicles vehicles = scenario.getVehicles();
        VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car",VehicleType.class));
        car.setPcuEquivalents(1.0);
        car.setMaximumVelocity(60/3.6);
        vehicles.addVehicleType(car);
        
        VehicleType bicycle = VehicleUtils.getFactory().createVehicleType(Id.create("bicycle",VehicleType.class));
        bicycle.setPcuEquivalents(0.25);
        bicycle.setMaximumVelocity(15/3.6);
        vehicles.addVehicleType(bicycle);

        FundamentalDiagramDataGenerator fd = new FundamentalDiagramDataGenerator(scenario);
        fd.addOverridingModules(new AbstractModule() {
            @Override
            public void install() {
                bind(QNetworkFactory.class).to(DynamicPCUQNetworkFactory.class);
                addEventHandlerBinding().to(SpeedHandler.class); // put speeds in attributes
            }
        });
        fd.run();
    }

    static class SpeedHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

        @Inject
        private Vehicles vehicles;
        @Inject
        private Network network;

        private Map<Id<Vehicle>, Double> linkEnterTime = new HashMap<>();

        @Override
        public void reset(int iteration){
            this.linkEnterTime.clear();
        }

        //todo may be just get track speed and then put it to vehicle type....
        @Override
        public void handleEvent(LinkLeaveEvent event) {
            Link l = network.getLinks().get(event.getLinkId());
            double speed = l.getLength() / (event.getTime()-this.linkEnterTime.get(event.getVehicleId()));
            //TODO this means, the speed on last link ...not on the current link.
            vehicles.getVehicleAttributes().putAttribute(event.getVehicleId().toString(), VEHICLE_SPEED, speed );
            l.getAttributes().putAttribute(event.getVehicleId().toString(), speed);
        }

        @Override
        public void handleEvent(LinkEnterEvent event) {
            this.linkEnterTime.put(event.getVehicleId(), event.getTime());
        }
    }
}
