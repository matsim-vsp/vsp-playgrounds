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

package playground.agarwalamit.opdyts.equil;

import java.util.Arrays;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import playground.agarwalamit.analysis.modalShare.ModalShareControlerListener;
import playground.agarwalamit.analysis.modalShare.ModalShareEventHandler;
import playground.agarwalamit.analysis.tripTime.ModalTravelTimeControlerListener;
import playground.agarwalamit.analysis.tripTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;
import playground.agarwalamit.opdyts.DistanceDistribution;
import playground.agarwalamit.opdyts.OpdytsScenario;
import playground.agarwalamit.opdyts.analysis.OpdytsModalStatsControlerListener;

/**
 * Created by amit on 11.10.17.
 */

public class EquilRelaxedPlans {

    public static void main(String[] args) {

        String configFile = "/Users/amit/Documents/repos/runs-svn/opdyts/equil/car,bicycle/inputs/config-with-mode-vehicles.xml";

        String relaxedPlans = "/Users/amit/Documents/repos/runs-svn/opdyts/equil/car,bicycle/relaxedPlans_defaultTravelTimeForBicycle/output_plans.xml.gz";

        String outputDir = "/Users/amit/Documents/repos/runs-svn/opdyts/equil/car,bicycle/objFunSensitivity/asc1";

        boolean usingBicycleTravelTime = false;

        double ascBicycle = 1;

        EquilRelaxedPlans.runWithRelaxedPlans(configFile, relaxedPlans, outputDir, ascBicycle, usingBicycleTravelTime);
//        EquilRelaxedPlans.runConfig(configFile, outputDir, usingBicycleTravelTime);

    }

    private static void runConfig(String configFile, String outputDir, boolean usingBicycleTravelTime){
        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));
        scenario.getConfig().controler().setDumpDataAtEnd(true);
        scenario.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        scenario.getConfig().controler().setOutputDirectory(outputDir);

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                if (usingBicycleTravelTime)  addTravelTimeBinding("bicycle").to(FreeSpeedTravelTimeForBike.class);
                this.bind(ModalShareEventHandler.class);
                this.addControlerListenerBinding().to(ModalShareControlerListener.class);

                this.bind(ModalTripTravelTimeHandler.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);

            }
        });
        controler.run();
    }

    private static void runWithRelaxedPlans(String configFile, String relaxedPlans, String outputDir, double ascBicycle, boolean usingBicycleTravelTime){
        Config config = ConfigUtils.loadConfig(configFile);
        config.plans().setInputFile(relaxedPlans);
        config.planCalcScore().getOrCreateModeParams("bicycle").setConstant(ascBicycle);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        scenario.getConfig().controler().setLastIteration(100);
        scenario.getConfig().controler().setOutputDirectory(outputDir);

        scenario.getConfig().controler().setDumpDataAtEnd(true);
        scenario.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);


        OpdytsScenario EQUIL_MIXEDTRAFFIC = OpdytsScenario.EQUIL_MIXEDTRAFFIC;
        DistanceDistribution distanceDistribution = new EquilDistanceDistribution(EQUIL_MIXEDTRAFFIC);
        OpdytsModalStatsControlerListener stasControlerListner = new OpdytsModalStatsControlerListener(Arrays.asList("car","bicycle"),distanceDistribution);


        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                if (usingBicycleTravelTime)  addTravelTimeBinding("bicycle").to(FreeSpeedTravelTimeForBike.class);

                this.bind(ModalShareEventHandler.class);
                this.addControlerListenerBinding().to(ModalShareControlerListener.class);

                this.bind(ModalTripTravelTimeHandler.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);

                this.addControlerListenerBinding().toInstance(stasControlerListner);

            }
        });
        controler.run();
    }

}
