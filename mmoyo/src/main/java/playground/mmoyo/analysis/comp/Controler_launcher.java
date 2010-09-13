/* *********************************************************************** *
 * project: org.matsim.*
 * AdaptedControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mmoyo.analysis.comp;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.controler.Controler;
import playground.mmoyo.utils.TransScenarioLoader;

/**
 * @author manuel
 * 
 * invokes a standard MATSim transit simulation
 */
public class Controler_launcher {
	
	public static void main(String[] args) {
		String configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
		ScenarioImpl scenario = new TransScenarioLoader().loadScenario(configFile);
		Controler controler = new Controler(scenario);
		controler.setCreateGraphs(false);
		controler.setOverwriteFiles(true);
		controler.setWriteEventsInterval(5); 
		controler.run();
	}
}
