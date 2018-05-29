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

package playground.agarwalamit.fundamentalDiagrams.dynamicPCU.surveyData;

import java.util.List;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
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
import playground.agarwalamit.fundamentalDiagrams.core.pointsToRun.FDAgentsGenerator;
import playground.agarwalamit.fundamentalDiagrams.dynamicPCU.PCUMethod;
import playground.agarwalamit.fundamentalDiagrams.dynamicPCU.areaSpeedRatioMethod.estimation.ChandraSikdarPCUUpdator;

/**
 * Created by amit on 23.05.18.
 */

public class RunSurveyData {

    public static void main(String[] args) {

        String parentDir = "../../svnit/outputFiles/mixedModes/passing/staticPCU/surveyData/";
        String file = "../../svnit/surveyData/inputFiles/vehiclesData.txt";
        TrafficDynamics trafficDynamics = TrafficDynamics.queue;
        double trackLinkLength = 6000.0;

        if (args.length>0) {
            parentDir = args[0];
            file = args[1];
            trafficDynamics = TrafficDynamics.valueOf(args[2]);
            trackLinkLength = Double.valueOf(args[3]);
        }

        Config config = ConfigUtils.createConfig();

        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

        QSimConfigGroup qsim = config.qsim();
        List<String> mainModes = SurveyDataUtils.modes;

        qsim.setMainModes(mainModes);
        qsim.setTrafficDynamics(trafficDynamics);
        qsim.setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);

        config.controler().setOutputDirectory( parentDir+trafficDynamics+"/trackLinkLength_"+(int) trackLinkLength+"m/" );

        FDConfigGroup fdConfigGroup = ConfigUtils.addOrGetModule(config, FDConfigGroup.class);
        fdConfigGroup.setTrackLinkCapacity(6300.0);
        fdConfigGroup.setTrackLinkLanes(3.0);
        fdConfigGroup.setTrackLinkSpeed(80.0/3.6);
        // max density is more than 2410 pcu/km for 3 lanes i.e., 803 pcu/km/lane--> length must be higher than 6km
        fdConfigGroup.setTrackLinkLength(trackLinkLength);
        fdConfigGroup.setWriteDataIfNoStability(true);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Vehicles vehicles = scenario.getVehicles();

        for (String mode : mainModes){
            AttributableVehicleType veh = new AttributableVehicleType(Id.create(mode,VehicleType.class));

            veh.setPcuEquivalents(SurveyDataUtils.getPCU(mode));
            veh.setMaximumVelocity(SurveyDataUtils.getSpeed(mode));
            veh.setLength(SurveyDataUtils.getLength(mode));
            veh.getAttributes().putAttribute(ChandraSikdarPCUUpdator.projected_area_ratio, SurveyDataUtils.getProjectedArea(mode));
            vehicles.addVehicleType(veh);
        }

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new FDModule(scenario));
        final String inputFile = file;
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {

//                    bind(String.class)
//                            .annotatedWith(Names.named(FDAgentsGeneratorForGivenSetOfAgentsImpl.survey_data_file_place_holder))
//                            .toInstance(file);
                bind(Key.get(String.class, Names.named(FDAgentsGeneratorForGivenSetOfAgentsImpl.survey_data_file_place_holder))).toInstance(inputFile);
                bind(FDAgentsGenerator.class).to(FDAgentsGeneratorForGivenSetOfAgentsImpl.class);

                bind(ChandraSikdarPCUUpdator.class).asEagerSingleton();
                addEventHandlerBinding().to(ChandraSikdarPCUUpdator.class);
                addControlerListenerBinding().to(ChandraSikdarPCUUpdator.class);

                bind(PCUMethod.class).toInstance(PCUMethod.SPEED_AREA_RATIO);
            }
        });
        controler.run();

        FDUtils.cleanOutputDir(scenario.getConfig().controler().getOutputDirectory());

    }
}
