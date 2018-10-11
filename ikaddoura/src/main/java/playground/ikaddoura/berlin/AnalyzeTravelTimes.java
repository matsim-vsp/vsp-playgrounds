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

package playground.ikaddoura.berlin;

import org.matsim.contrib.analysis.vsp.traveltimedistance.RunTraveltimeValidationExample;

/**
* @author ikaddoura
*/

public class AnalyzeTravelTimes {

	public static void main(String[] args) {
		String[] arguments = {
				"/Users/ihab/Documents/workspace/public-svn/matsim/scenarios/countries/de/berlin/2018-09-04_output-berlin-v5.2-10pct/2018-09-04_output-berlin-v5.2-10pct/berlin-v5.2-10pct.output_plans.xml.gz",
				"/Users/ihab/Documents/workspace/public-svn/matsim/scenarios/countries/de/berlin/2018-09-04_output-berlin-v5.2-10pct/2018-09-04_output-berlin-v5.2-10pct/berlin-v5.2-10pct.output_events.xml.gz",
				"/Users/ihab/Documents/workspace/public-svn/matsim/scenarios/countries/de/berlin/2018-09-04_output-berlin-v5.2-10pct/2018-09-04_output-berlin-v5.2-10pct/berlin-v5.2-10pct.output_network.xml.gz",
				"EPSG:31468",
				"5FKvGyzG0AB7fmOWzDma",
				"68pL4sT46TlrUmOHa0-9Bg",
				"/Users/ihab/Documents/workspace/public-svn/matsim/scenarios/countries/de/berlin/2018-09-04_output-berlin-v5.2-10pct/2018-09-04_output-berlin-v5.2-10pct/travel-time-analysis",
				"2018-09-07",
				"10000"
		};
		RunTraveltimeValidationExample.main(arguments);
	}

}

