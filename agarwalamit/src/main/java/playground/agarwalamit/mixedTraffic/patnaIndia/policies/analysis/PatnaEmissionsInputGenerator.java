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

package playground.agarwalamit.mixedTraffic.patnaIndia.policies.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * Created by amit on 20.11.17.
 */

public class PatnaEmissionsInputGenerator {

    public static void writeRoadTypeMappingFile(String inputNetworkFile, String roadTypeMappingFile, String outputNetworkFile) {
        Scenario scenario = LoadMyScenarios.loadScenarioFromNetwork(inputNetworkFile);

        if (! new File(roadTypeMappingFile).exists()) {
            BufferedWriter writer = IOUtils.getBufferedWriter(roadTypeMappingFile);
            try {
                writer.write("VISUM_RT_NR" + ";" + "VISUM_RT_NAME" + ";"
                        + "HBEFA_RT_NAME" + "\n");
                writer.write("01" + ";" + "30kmh" + ";" + "URB/Access/30" + "\n");
                writer.write("02" + ";" + "40kmh" + ";" + "URB/Access/40"+ "\n");
                writer.write("031" + ";" + "50kmh-1l" + ";" + "URB/Local/50"+ "\n");
                writer.write("032" + ";" + "50kmh-2l" + ";" + "URB/Distr/50"+ "\n");
                writer.write("033" + ";" + "50kmh-3+l" + ";" + "URB/Trunk-City/50"+ "\n");
                writer.write("041" + ";" + "60kmh-1l" + ";" + "URB/Local/60"+ "\n");
                writer.write("042" + ";" + "60kmh-2l" + ";" + "URB/Trunk-City/60"+ "\n");
                writer.write("043" + ";" + "60kmh-3+l" + ";" + "URB/MW-City/60"+ "\n");
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException("Data is not written/read. Reason : " + e);
            }
        }

        for (Link l : scenario.getNetwork().getLinks().values() ) {
            double freeSpeed = l.getFreespeed() * 3.6;
            int numberOfLanes = (int) l.getNumberOfLanes();
            if ( Math.round(freeSpeed) >= 60.0 )  {
                if ( numberOfLanes >=3) {
                    NetworkUtils.setType(l, "043");
                } else if (numberOfLanes >=2 ) {
                    NetworkUtils.setType(l, "042");
                } else {
                    NetworkUtils.setType(l, "041");
                }
            } else if ( Math.round(freeSpeed) >= 50.0 ) {  //
                if ( numberOfLanes >=3) {
                    NetworkUtils.setType(l, "033");
                } else if (numberOfLanes >=2 ) {
                    NetworkUtils.setType(l, "032");
                } else {
                    NetworkUtils.setType(l, "031");
                }
            } else if ( Math.round(freeSpeed) >= 40.0 ) {
                NetworkUtils.setType(l, "02");
            } else {
                NetworkUtils.setType(l, "01");
            }
        }
        new NetworkWriter(scenario.getNetwork()).write(outputNetworkFile);
    }

}
