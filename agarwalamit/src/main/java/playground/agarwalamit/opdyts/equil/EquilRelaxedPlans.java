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
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareEventHandler;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;
import playground.agarwalamit.opdyts.DistanceDistribution;
import playground.agarwalamit.opdyts.OpdytsScenario;
import playground.agarwalamit.opdyts.analysis.OpdytsModalStatsControlerListener;
import playground.agarwalamit.utils.FileUtils;

/**
 * Created by amit on 11.10.17.
 */

public class EquilRelaxedPlans {

    public static void main(String[] args) {

        String configFile = FileUtils.RUNS_SVN+"/opdyts/equil/carPt/inputs/config.xml";

        String relaxedPlans = FileUtils.RUNS_SVN+"/opdyts/equil/carPt/relaxedPlans/output_plans.xml.gz";

        String outputDir = FileUtils.RUNS_SVN+"/opdyts/equil/carPt/objFunSensitivity/asc2.5/";

        boolean usingBicycleTravelTime = false;

        String mode = "pt";
        double ascMode = 2.5;

        EquilRelaxedPlans.runWithRelaxedPlans(configFile, relaxedPlans, outputDir, mode, ascMode, usingBicycleTravelTime);
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

    private static void runWithRelaxedPlans(String configFile, String relaxedPlans, String outputDir, String mode, double ascBicycle, boolean usingBicycleTravelTime){
        if (! mode.equals("bicycle")) usingBicycleTravelTime = false;

        Config config = ConfigUtils.loadConfig(configFile);
        config.plans().setInputFile(relaxedPlans);
        config.planCalcScore().getOrCreateModeParams(mode).setConstant(ascBicycle);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        scenario.getConfig().controler().setLastIteration(100);
        scenario.getConfig().controler().setOutputDirectory(outputDir);

        scenario.getConfig().controler().setDumpDataAtEnd(true);
        scenario.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);


        OpdytsScenario EQUIL_MIXEDTRAFFIC = mode.equals("bicycle") ? OpdytsScenario.EQUIL_MIXEDTRAFFIC :OpdytsScenario.EQUIL;

        DistanceDistribution distanceDistribution = new EquilDistanceDistribution(EQUIL_MIXEDTRAFFIC);
        OpdytsModalStatsControlerListener stasControlerListner = new OpdytsModalStatsControlerListener(Arrays.asList("car",mode), distanceDistribution);

        Controler controler = new Controler(scenario);
        boolean finalUsingBicycleTravelTime = usingBicycleTravelTime;
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                if (finalUsingBicycleTravelTime)  addTravelTimeBinding("bicycle").to(FreeSpeedTravelTimeForBike.class);

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
