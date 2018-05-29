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

package playground.agarwalamit.fundamentalDiagrams.headwayMethod;

import java.util.Arrays;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.qsim.qnetsimengine.DynamicHeadwayQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import playground.agarwalamit.fundamentalDiagrams.FDUtils;
import playground.agarwalamit.fundamentalDiagrams.core.FDConfigGroup;
import playground.agarwalamit.fundamentalDiagrams.core.FDModule;

/**
 * Created by amit on 14.04.18.
 */

public class RunDynamicHeadwayExample {

    public static void main(String[] args) {

        Config config = ConfigUtils.createConfig();
        
        config.controler().setOutputDirectory("../../svnit/outputFiles/carBicycle/passing/equalModalSplit_holeSpeed15KPH/");
        config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

        QSimConfigGroup qsim = config.qsim();
        qsim.setMainModes(Arrays.asList("car","bicycle"));
//        qsim.setMainModes(Arrays.asList("car"));
        qsim.setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
        qsim.setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
//        qsim.setSeepModes(Collections.singletonList("bicycle"));

//        qsim.setStuckTime(100*3600.); // --> complete grid lock.

        FDConfigGroup fdConfigGroup = ConfigUtils.addOrGetModule(config, FDConfigGroup.class);
        fdConfigGroup.setModalShareInPCU("1.0");
        fdConfigGroup.setReduceDataPointsByFactor(1);
//        fdConfigGroup.setTrackLinkCapacity(3600.);
        
        Scenario scenario = ScenarioUtils.loadScenario(config);
        
        Vehicles vehicles = scenario.getVehicles();
        VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car",VehicleType.class));
        car.setPcuEquivalents(1.0);
        car.setMaximumVelocity(60/3.6);
        car.setLength(7.5);
        vehicles.addVehicleType(car);
        
        VehicleType bicycle = VehicleUtils.getFactory().createVehicleType(Id.create("bicycle",VehicleType.class));
        bicycle.setPcuEquivalents(0.25);
        bicycle.setMaximumVelocity(15/3.6);
        bicycle.setLength(7.5/2);
        vehicles.addVehicleType(bicycle);

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new FDModule(scenario));

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(QNetworkFactory.class).to(DynamicHeadwayQNetworkFactory.class);
                bindMobsim().toProvider(DynamicHeadwayFDQSimProvider.class);

                bind(HeadwayHandler.class).asEagerSingleton();
                addEventHandlerBinding().to(HeadwayHandler.class); // put speeds in attributes
                addControlerListenerBinding().to(HeadwayHandler.class);
            }
        });

        controler.run();

        FDUtils.cleanOutputDir(scenario.getConfig().controler().getOutputDirectory());
    }

}
