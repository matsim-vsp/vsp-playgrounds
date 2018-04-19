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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import playground.agarwalamit.fundamentalDiagrams.core.FundamentalDiagramConfigGroup;
import playground.agarwalamit.fundamentalDiagrams.core.FundamentalDiagramDataGenerator;

/**
 * Created by amit on 14.04.18.
 */

public class RunDynamicPCUExample {

    public static void main(String[] args) {

        Config config = ConfigUtils.createConfig();
        
        config.controler().setOutputDirectory("../svnit/outputFiles/bicycle_equalModalShare_holeSpeed15kph/master18Apr/");
//        config.controler().setOutputDirectory("../svnit/outputFiles/carBicycleSeepage_equalModalShare_holeSpeed15kph/branch18Apr/");
        config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

        QSimConfigGroup qsim = config.qsim();
        qsim.setMainModes(Arrays.asList("bicycle"));
        qsim.setTrafficDynamics(TrafficDynamics.withHoles);
//        qsim.setLinkDynamics(QSimConfigGroup.LinkDynamics.SeepageQ);
//        qsim.setSeepModes(Collections.singletonList("bicycle"));
        
        FundamentalDiagramConfigGroup fdConfigGroup = ConfigUtils.addOrGetModule(config, FundamentalDiagramConfigGroup.class);
        fdConfigGroup.setModalShareInPCU("1.0");
        fdConfigGroup.setReduceDataPointsByFactor(15);
        
        Scenario scenario = ScenarioUtils.loadScenario(config);
        
        Vehicles vehicles = scenario.getVehicles();
//        VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car",VehicleType.class));
//        car.setPcuEquivalents(1.0);
//        car.setMaximumVelocity(60/3.6);
//        vehicles.addVehicleType(car);
        
        VehicleType bicycle = VehicleUtils.getFactory().createVehicleType(Id.create("bicycle",VehicleType.class));
        bicycle.setPcuEquivalents(0.25);
        bicycle.setMaximumVelocity(15/3.6);
        vehicles.addVehicleType(bicycle);

        FundamentalDiagramDataGenerator fd = new FundamentalDiagramDataGenerator(scenario);
        fd.addOverridingModules(new AbstractModule() {
            @Override
            public void install() {
                bind(QNetworkFactory.class).to(DynamicPCUQNetworkFactory.class);
            }
        });
        fd.run();
    }
}
