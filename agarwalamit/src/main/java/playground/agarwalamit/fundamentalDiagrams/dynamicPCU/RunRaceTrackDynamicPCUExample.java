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

package playground.agarwalamit.fundamentalDiagrams.dynamicPCU;

import java.util.HashSet;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import playground.agarwalamit.fundamentalDiagrams.FundamentalDiagramDataGenerator;
import playground.agarwalamit.fundamentalDiagrams.RaceTrackLinkProperties;
import playground.agarwalamit.fundamentalDiagrams.dynamicPCU.projectedArea.VehicleProjectedAreaMarker;
import playground.agarwalamit.fundamentalDiagrams.dynamicPCU.projectedArea.VehicleProjectedAreaRatio;
import playground.agarwalamit.utils.FileUtils;

/**
 * Created by amit on 29.06.17.
 */


public class RunRaceTrackDynamicPCUExample {

    public static void main(String[] args) {

        boolean isRunningOnServer = args.length > 0;

        String outDir ;
        boolean isRunningDistribution = false;
        Config config = null;

        if ( isRunningOnServer ) {
            outDir = args[0];
            isRunningDistribution = Boolean.valueOf(args[1]);
            if (args.length>2) {
                config = ConfigUtils.loadConfig(args[2]);
            }
        } else {
            config = ConfigUtils.loadConfig(FileUtils.RUNS_SVN+"/dynamicPCU/raceTrack/input/config.xml");
            outDir = FileUtils.RUNS_SVN+"/dynamicPCU/raceTrack/output/run002/";
        }

        Scenario scenario;
        if (config== null) {
            scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
            Vehicles vehicles = scenario.getVehicles();
            {
                VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car",VehicleType.class));
                car.setPcuEquivalents(1.0);
                car.setMaximumVelocity(60.0/3.6);
                vehicles.addVehicleType(car);
            }
            {
                VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create("bike",VehicleType.class));
                bike.setPcuEquivalents(0.25);
                bike.setMaximumVelocity(15.0/3.6);
                vehicles.addVehicleType(bike);
            }
        } else scenario = ScenarioUtils.loadScenario(config); // network and plans will anywaye be generated internally

        scenario.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
        scenario.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

        Vehicles vehicles = scenario.getVehicles();
        vehicles.getVehicleTypes().values().forEach(vt -> {
            vt.setDescription(
                VehicleProjectedAreaMarker.BEGIN_VEHILCE_PROJECTED_AREA
                        + String.valueOf( VehicleProjectedAreaRatio.getProjectedAreaRatio(vt.getId().toString())  )
                        +VehicleProjectedAreaMarker.END_VEHILCE_PROJECTED_AREA);
        });

        String outFolder ="/"+scenario.getConfig().qsim().getTrafficDynamics()+"_"+scenario.getConfig().qsim().getLinkDynamics()+"/";
        scenario.getConfig().controler().setOutputDirectory(outDir+outFolder);

        // a container, used to store the link properties,
        // all sides of triangle will have these properties (identical links).

        RaceTrackLinkProperties raceTrackLinkProperties = new RaceTrackLinkProperties(1000.0, 1600.0,
                60.0/3.6, 1.0, new HashSet<>(scenario.getConfig().qsim().getMainModes()));

//        FundamentalDiagramDataGenerator fundamentalDiagramDataGenerator = new FundamentalDiagramDataGenerator( scenario );
        FundamentalDiagramDataGenerator fundamentalDiagramDataGenerator = new FundamentalDiagramDataGenerator(raceTrackLinkProperties, scenario);
        fundamentalDiagramDataGenerator.setReduceDataPointsByFactor(3);
        fundamentalDiagramDataGenerator.setIsWritingEventsFileForEachIteration(false);
        fundamentalDiagramDataGenerator.setPlottingDistribution(isRunningDistribution);
        fundamentalDiagramDataGenerator.setUsingLiveOTFVis(false);
        fundamentalDiagramDataGenerator.setUsingDynamicPCU(true);
        fundamentalDiagramDataGenerator.run();
    }
}