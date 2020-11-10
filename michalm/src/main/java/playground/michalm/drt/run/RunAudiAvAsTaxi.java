/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.michalm.drt.run;

import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.fare.TaxiFareParams;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiControlerCreator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author Michal Maciejewski (michalm)
 */
public class RunAudiAvAsTaxi {
	public static void main(String[] args) {
		String configFile = "D:/matsim-repos/runs-svn/audi_av_with_ridesharing/audi_av_10pct_2015_10_new/config_11k_taxi.xml";
		Config config = ConfigUtils.loadConfig(configFile, new DvrpConfigGroup(), new MultiModeTaxiConfigGroup(),
				new OTFVisConfigGroup(), new TaxiFareParams());
		TaxiControlerCreator.createControler(config, false).run();
	}
}
