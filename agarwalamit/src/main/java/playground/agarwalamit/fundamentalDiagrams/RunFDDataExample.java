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

package playground.agarwalamit.fundamentalDiagrams;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import playground.agarwalamit.fundamentalDiagrams.core.FDModule;
import playground.agarwalamit.utils.FileUtils;

/**
 * Created by amit on 16/02/2017.
 */

public class RunFDDataExample {

    public static void main(String[] args) {

        boolean runUsingConfig = true;

        Scenario scenario ;

        if (runUsingConfig ) {
            String configFile = FileUtils.RUNS_SVN+"/dynamicPCU/raceTrack/input/config.xml";
            scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));
        } else {
            scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
        }

        String myDir = FileUtils.RUNS_SVN+"/dynamicPCU/raceTrack/test";
        String outFolder ="/1lane/";
        scenario.getConfig().controler().setOutputDirectory(myDir+outFolder);

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new FDModule(scenario));
        controler.run();

        FDUtils.cleanOutputDir(scenario.getConfig().controler().getOutputDirectory());
    }
}
