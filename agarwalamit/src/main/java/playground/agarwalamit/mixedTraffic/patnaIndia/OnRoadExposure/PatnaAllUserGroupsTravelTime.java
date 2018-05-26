/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.OnRoadExposure;

import playground.agarwalamit.analysis.tripTime.ModalTravelTimeAnalyzer;

/**
 * Created by amit on 26.05.18.
 */

public class PatnaAllUserGroupsTravelTime {

    public static void main(String[] args) {

        String eventsFile = "../../runs-svn/patnaIndia/run111/onRoadExposure/bauLastItr/output/output_events.xml.gz";
        String outFile = "../../runs-svn/patnaIndia/run111/onRoadExposure/bauLastItr/analysis/allUsersTravelTime.txt";

        ModalTravelTimeAnalyzer analyzer = new ModalTravelTimeAnalyzer(eventsFile);
        analyzer.run();
        analyzer.writeResults(outFile);

    }
}
