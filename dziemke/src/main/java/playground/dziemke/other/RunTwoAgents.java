/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.dziemke.other;

import java.util.Arrays;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author dziemke
 */
public class RunTwoAgents {

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("/Users/dominik/Workspace/matsim/examples/scenarios/equil/config.xml");
		config.controler().setLastIteration(0);
		config.global().setNumberOfThreads(1);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		config.plansCalcRoute().removeModeRoutingParams("pt");
		config.travelTimeCalculator().setSeparateModes(true);
		config.travelTimeCalculator().setAnalyzedModes("car,pt");
		config.plansCalcRoute().setNetworkModes(Arrays.asList("car","pt"));
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);		
		controler.run();
	}
}