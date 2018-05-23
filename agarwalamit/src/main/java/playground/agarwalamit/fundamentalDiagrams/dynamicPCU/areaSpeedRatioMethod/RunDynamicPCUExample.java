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

package playground.agarwalamit.fundamentalDiagrams.dynamicPCU.areaSpeedRatioMethod;

import java.util.Arrays;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import playground.agarwalamit.fundamentalDiagrams.FDUtils;
import playground.agarwalamit.fundamentalDiagrams.core.FDConfigGroup;
import playground.agarwalamit.fundamentalDiagrams.core.FDModule;
import playground.agarwalamit.fundamentalDiagrams.dynamicPCU.PCUMethod;
import playground.agarwalamit.fundamentalDiagrams.dynamicPCU.areaSpeedRatioMethod.estimation.ChandraSikdarPCUUpdator;
import playground.agarwalamit.fundamentalDiagrams.dynamicPCU.areaSpeedRatioMethod.projectedArea.VehicleProjectedAreaMarker;
import playground.agarwalamit.fundamentalDiagrams.dynamicPCU.areaSpeedRatioMethod.projectedArea.VehicleProjectedAreaRatio;
import playground.agarwalamit.fundamentalDiagrams.headwayMethod.HeadwayHandler;

/**
 * Created by amit on 29.06.17.
 */


public class RunDynamicPCUExample {

    public static void main(String[] args) {

        Config config = ConfigUtils.createConfig();

        config.controler().setOutputDirectory("../../svnit/outputFiles/carBicycle/passing/staticPCU/equalModalSplit_holes/");
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

        QSimConfigGroup qsim = config.qsim();
        qsim.setMainModes(Arrays.asList("car","bicycle"));
//        qsim.setMainModes(Arrays.asList("car"));
        qsim.setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
        qsim.setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
//        qsim.setSeepModes(Collections.singletonList("bicycle"));

//        qsim.setStuckTime(100*3600.); // --> complete grid lock.

        FDConfigGroup fdConfigGroup = ConfigUtils.addOrGetModule(config, FDConfigGroup.class);
        fdConfigGroup.setModalShareInPCU("1.0,1.0");
        fdConfigGroup.setReduceDataPointsByFactor(5);
//        fdConfigGroup.setTrackLinkCapacity(3600.);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Vehicles vehicles = scenario.getVehicles();
        VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car",VehicleType.class));
        car.setPcuEquivalents(1.0);
        car.setMaximumVelocity(60/3.6);
        car.setLength(3.72);
        car.setDescription(VehicleProjectedAreaMarker.BEGIN_VEHILCE_PROJECTED_AREA
                + String.valueOf( VehicleProjectedAreaRatio.getProjectedAreaRatio(car.getId().toString())  )
                +VehicleProjectedAreaMarker.END_VEHILCE_PROJECTED_AREA);
        vehicles.addVehicleType(car);

        VehicleType bicycle = VehicleUtils.getFactory().createVehicleType(Id.create("bicycle",VehicleType.class));
        bicycle.setPcuEquivalents(0.25);
        bicycle.setMaximumVelocity(15/3.6);
        bicycle.setLength(1.9);
        bicycle.setDescription(VehicleProjectedAreaMarker.BEGIN_VEHILCE_PROJECTED_AREA
                + String.valueOf( VehicleProjectedAreaRatio.getProjectedAreaRatio(bicycle.getId().toString())  )
                +VehicleProjectedAreaMarker.END_VEHILCE_PROJECTED_AREA);
        vehicles.addVehicleType(bicycle);

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new FDModule(scenario));
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {

                bind(ChandraSikdarPCUUpdator.class).asEagerSingleton();
                addEventHandlerBinding().to(ChandraSikdarPCUUpdator.class);
                addControlerListenerBinding().to(ChandraSikdarPCUUpdator.class);

                bind(HeadwayHandler.class).asEagerSingleton();
                addEventHandlerBinding().to(HeadwayHandler.class);
                addControlerListenerBinding().to(HeadwayHandler.class);

                bind(PCUMethod.class).toInstance(PCUMethod.SPEED_AREA_RATIO);
            }
        });
        controler.run();

        FDUtils.cleanOutputDir(scenario.getConfig().controler().getOutputDirectory());
    }
}