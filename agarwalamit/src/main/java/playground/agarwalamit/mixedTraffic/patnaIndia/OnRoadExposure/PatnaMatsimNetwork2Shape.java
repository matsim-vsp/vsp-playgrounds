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

package playground.agarwalamit.mixedTraffic.patnaIndia.OnRoadExposure;

import java.io.BufferedWriter;
import java.io.IOException;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.agarwalamit.utils.networkProcessing.MatsimNetwork2Shape;

/**
 * Created by amit on 22.11.17.
 */

public class PatnaMatsimNetwork2Shape {

    public static void main(String[] args) {

        String matsimNetwork = FileUtils.RUNS_SVN+"/patnaIndia/run108/jointDemand/policies/0.15pcu/bau/output_network.xml.gz";
        String shapeDir = FileUtils.RUNS_SVN+"/patnaIndia/run111/onRoadExposure/analysis/networkShape/";
        String epsg = PatnaUtils.EPSG;

        MatsimNetwork2Shape.main(new String[] {matsimNetwork, shapeDir, epsg});
        new PatnaMatsimNetwork2Shape().writeLinkCentroid(matsimNetwork, shapeDir);
    }

    private void writeLinkCentroid(String networkFile, String outputFilesDir){
        BufferedWriter writer = IOUtils.getBufferedWriter(outputFilesDir+"/linkIdToCentroid.txt");
        Network network = LoadMyScenarios.loadScenarioFromNetwork(networkFile).getNetwork();

        try {
            writer.write("linkId\tcentroidX\tcentoridY\n");
            for (Link l : network.getLinks().values()) {
                writer.write(l.getId().toString()+"\t"+l.getCoord().getX()+"\t"+l.getCoord().getY()+"\n");
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }

    }

}
