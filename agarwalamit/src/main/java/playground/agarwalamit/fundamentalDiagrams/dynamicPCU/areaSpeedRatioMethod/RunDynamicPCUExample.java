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
import java.util.List;
import org.apache.commons.lang.StringUtils;
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
import org.matsim.vehicles.Vehicles;
import playground.agarwalamit.fundamentalDiagrams.AttributableVehicleType;
import playground.agarwalamit.fundamentalDiagrams.FDUtils;
import playground.agarwalamit.fundamentalDiagrams.core.FDConfigGroup;
import playground.agarwalamit.fundamentalDiagrams.core.FDModule;
import playground.agarwalamit.fundamentalDiagrams.dynamicPCU.PCUMethod;
import playground.agarwalamit.fundamentalDiagrams.dynamicPCU.areaSpeedRatioMethod.estimation.ChandraSikdarPCUUpdator;
import playground.agarwalamit.fundamentalDiagrams.dynamicPCU.areaSpeedRatioMethod.projectedArea.VehicleProjectedAreaRatio;
import playground.agarwalamit.mixedTraffic.MixedTrafficVehiclesUtils;

/**
 * Created by amit on 29.06.17.
 */


public class RunDynamicPCUExample {

    public static void main(String[] args) {

        boolean updatePCU = true;
        PCUMethod pcuMethod = PCUMethod.SPEED_AREA_RATIO;

        String parentDir = "../../svnit/outputFiles/mixedModes/passing/staticPCU/";
        if(updatePCU) {
            parentDir = "../../svnit/outputFiles/mixedModes/passing/dynamicPCU/"+pcuMethod+"/";
        }

        Config config = ConfigUtils.createConfig();

        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

        QSimConfigGroup qsim = config.qsim();
        List<String> mainModes = Arrays.asList("car", "truck");
        qsim.setMainModes(mainModes);
        qsim.setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
        qsim.setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
//        qsim.setStuckTime(100*3600.); // --> complete grid lock.

        FDConfigGroup fdConfigGroup = ConfigUtils.addOrGetModule(config, FDConfigGroup.class);
        fdConfigGroup.setModalShareInPCU("1.0,3.0");
        if (! mainModes.contains("truck")) fdConfigGroup.setReduceDataPointsByFactor(2);

        config.controler().setOutputDirectory(parentDir+StringUtils.join(mainModes,'_')+"/"+fdConfigGroup.getModalShareInPCUAsString()+"/");

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Vehicles vehicles = scenario.getVehicles();

        for (String mode : mainModes){
            AttributableVehicleType veh = new AttributableVehicleType(Id.create(mode,VehicleType.class));
            veh.setPcuEquivalents(MixedTrafficVehiclesUtils.getPCU(mode));
            veh.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed(mode));
            veh.setLength(MixedTrafficVehiclesUtils.getLength(mode));
            veh.getAttributes().putAttribute(ChandraSikdarPCUUpdator.projected_area_ratio, VehicleProjectedAreaRatio.getProjectedAreaRatio(mode));
            vehicles.addVehicleType(veh);
        }

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new FDModule(scenario));
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {

                bind(ChandraSikdarPCUUpdator.class).asEagerSingleton();
                addEventHandlerBinding().to(ChandraSikdarPCUUpdator.class);
                addControlerListenerBinding().to(ChandraSikdarPCUUpdator.class);

                if (updatePCU) bind(PCUMethod.class).toInstance(pcuMethod);// bind if updating pcus
            }
        });
        controler.run();

        FDUtils.cleanOutputDir(scenario.getConfig().controler().getOutputDirectory());
    }
}