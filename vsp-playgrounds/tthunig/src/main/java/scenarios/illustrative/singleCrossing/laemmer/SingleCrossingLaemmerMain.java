/* *********************************************************************** *
 * project: org.matsim.*
 * TaSingleCrossingMain
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package scenarios.illustrative.singleCrossing.laemmer;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import signals.CombinedSignalsModule;


/**
 * @author dgrether
 * @author tthunig
 *
 */
public class SingleCrossingLaemmerMain {
	
	/**
	 * @param args first entry gives the demand in west-east direction in percentage terms between 0 and the maximum flow value (flow capacity).
	 * If nothing is set here, a value of 0.5 (50%) is used.
	 */
	public static void main(String[] args) {
		double lambdaWestEast = 0.5;
		if (args != null && args.length != 0){
			lambdaWestEast = Double.parseDouble(args[0]);
		}
		
		Scenario scenario = new DgSingleCrossingScenario().createScenario(lambdaWestEast, false);
		
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new CombinedSignalsModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.run();
	}

}
