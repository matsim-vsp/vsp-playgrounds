/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package playground.gleich.misc;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;

public class UpdatePlans {

    public static void main (String[] args) {
//        Config config = ConfigUtils.createConfig();

        Config config = ConfigUtils.loadConfig("/home/gregor/git/matsim/examples/scenarios/pt-tutorial/0.config.xml");
//        config.controler().setLastIteration(0);
//        config.plans().setHandlingOfPlansWithoutRoutingMode(PlansConfigGroup.HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);
//        Scenario scenario = ScenarioUtils.loadScenario(config);
//        Controler controler = new Controler(scenario);
//        controler.run();


//        Config config = this.utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("pt-tutorial"), "0.config.xml"));
        config.controler().setLastIteration(1);
        config.plans().setHandlingOfPlansWithoutRoutingMode(PlansConfigGroup.HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);


//        try {
            Controler controler = new Controler(config);
//            final EnterVehicleEventCounter enterVehicleEventCounter = new EnterVehicleEventCounter();
//            final StageActivityDurationChecker stageActivityDurationChecker = new StageActivityDurationChecker();
//            controler.addOverridingModule( new AbstractModule(){
//                @Override public void install() {
//                    this.addEventHandlerBinding().toInstance( enterVehicleEventCounter );
//                    this.addEventHandlerBinding().toInstance( stageActivityDurationChecker );
//                }
//            });
            controler.run();
    }


}
