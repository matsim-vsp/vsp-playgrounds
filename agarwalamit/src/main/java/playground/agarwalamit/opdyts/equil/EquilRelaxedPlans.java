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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;

/**
 * Created by amit on 11.10.17.
 */

public class EquilRelaxedPlans {

    public static void main(String[] args) {

        String configFile = "/Users/amit/Documents/repos/runs-svn/opdyts/equil/car,bicycle/inputs/config-with-mode-vehicles.xml";

        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));
        scenario.getConfig().controler().setDumpDataAtEnd(true);
        scenario.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding("bicycle").to(FreeSpeedTravelTimeForBike.class);
                addTravelDisutilityFactoryBinding("bicycle").to(carTravelDisutilityFactoryKey());
            }
        });
        controler.run();
    }

}
