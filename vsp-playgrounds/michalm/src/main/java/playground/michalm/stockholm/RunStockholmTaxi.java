/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.michalm.stockholm;

import org.matsim.contrib.taxi.benchmark.RunTaxiBenchmark;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.*;

public class RunStockholmTaxi {
	public static void main(String[] args) {
		int runs = 1;
		String dir = "d:/temp/Stockholm/";
		Config config = ConfigUtils.loadConfig(dir + "configTaxi.xml", new TaxiConfigGroup());

		// String baseDir = "../../../shared-svn/projects/maciejewski/Mielec/";
		// config.plans().setInputFile(
		// baseDir + "2014_02_base_scenario/plans_taxi/plans_only_taxi_" +
		// demand + ".xml.gz");
		// config.controler()
		// .setOutputDirectory(config.controler().getOutputDirectory() + "_" +
		// demand);

		RunTaxiBenchmark.createControler(config, runs).run();

	}

}
