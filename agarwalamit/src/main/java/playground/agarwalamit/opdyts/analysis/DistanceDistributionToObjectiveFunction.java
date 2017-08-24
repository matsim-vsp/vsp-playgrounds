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

package playground.agarwalamit.opdyts.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.utils.FileUtils;

/**
 * Created by amit on 07.08.17.
 */

public class DistanceDistributionToObjectiveFunction {

    private static final String baseDir = FileUtils.RUNS_SVN+"/opdyts/patna/output_networkModes/ascAnalysis/";
    private static final double [] ascTrials = {-3.0, -2.5, -1.5, -1.0, -0.5, 0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0};

    private static final String outFile = baseDir+"/ascToLogObjectiveFunction.txt";

    public static void main(String[] args) {

        try(BufferedWriter writer = IOUtils.getBufferedWriter(outFile)) {
        writer.write("ascMotorbike\tascBike\titrNr\tvalueOfObjectiveFunctionWithLog\tvalueOfObjectiveFunctionWithoutLog\n");
        for(double ascMotorbike : ascTrials ) {

            for(double ascBike : ascTrials ) {

                String dir = baseDir+"/bikeASC"+ascBike+"_motorbikeASC"+ascMotorbike+"/distanceDistri/";
                File filesFolder = new File(dir);
                if (! filesFolder.exists() ) continue;

                File[] files = filesFolder.listFiles();
                for(File file : files) {
                    String itrNr = file.getName().substring(0, file.getName().indexOf('.'));

                    BufferedReader reader = IOUtils.getBufferedReader(file.getAbsolutePath());
                    String line = reader.readLine();

                    boolean isMeasureLines = false;
                    boolean isSimulatedLines = false;
                    Map<String, List<String>> mode2measuredOrRealCounts = new HashMap<>();
                    Map<String, List<String>> mode2simulatedCounts = new HashMap<>();

                    while (line!=null) {
                        // check for desired obj fun
                        if (  ( line.contains("begin") && line.contains("objective")  ) // start of measured counts line
                            || isMeasureLines // continue
                                ) {
                            if (! isMeasureLines ) { // nothing to store; header
                                isMeasureLines = true;
                                line = reader.readLine();
                                continue;
                            }
                            if (! line.contains("end")) {
                                String strs [] = line.split("\t");
                                mode2measuredOrRealCounts.put(strs[0], Arrays.asList( Arrays.copyOfRange(strs, 1, strs.length) ));
                            } else {
                                isMeasureLines = false;
                            }
                        }  else if (  ( line.contains("begin") && line.contains("simulation")  ) // start of simulated counts line
                                || isSimulatedLines // continue
                                ) {
                            if (! isSimulatedLines ) { // nothing to store; header
                                isSimulatedLines = true;
                                line = reader.readLine();
                                continue;
                            }
                            if (! line.contains("end")) {
                                String strs [] = line.split("\t");
                                mode2simulatedCounts.put(strs[0], Arrays.asList( Arrays.copyOfRange(strs, 1, strs.length) ));
                            } else {
                                isSimulatedLines = false;
                            }
                        }
                        line = reader.readLine();
                    }
                    double valueOfObjFunLog = getValueOfObjectiveFunction(mode2measuredOrRealCounts, mode2simulatedCounts, true);
                    double valueOfObjFun = getValueOfObjectiveFunction(mode2measuredOrRealCounts, mode2simulatedCounts, false);
                    writer.write(ascMotorbike+"\t"+ascBike+"\t"+itrNr+"\t"+valueOfObjFunLog+"\t"+valueOfObjFun+"\n");
                    writer.flush();
                }
            }
        }

        writer.close();
        } catch (IOException e ) {
            throw new RuntimeException("Data is not written. Reason:"+ e);
        }
    }

    private static double getValueOfObjectiveFunction(Map<String, List<String>> mode2measuredOrRealCounts, Map<String, List<String>> mode2simulatedCounts, boolean usingLog){
        double diff = 0; //
        double measuredValueSum = 0; // R
        for(String mode : mode2measuredOrRealCounts.keySet() ) {
            if (mode2measuredOrRealCounts.get(mode).size() !=1 ) throw new RuntimeException("not implemented yet.");
            else {
                double rValue = 0;
                double sValue = 0;
                if (usingLog) {
                    rValue = Math.log(Double.valueOf(mode2measuredOrRealCounts.get(mode).get(0)));
                    sValue = mode2simulatedCounts.containsKey(mode) ? Math.log(Double.valueOf(mode2simulatedCounts.get(mode).get(0))) : 0;
                } else  {
                    rValue = Double.valueOf(mode2measuredOrRealCounts.get(mode).get(0));
                    sValue = mode2simulatedCounts.containsKey(mode) ? Double.valueOf(mode2simulatedCounts.get(mode).get(0)) : 0;
                }
                diff += (rValue-sValue) * (rValue-sValue);
                measuredValueSum += rValue;
            }
        }
        return diff/(measuredValueSum*measuredValueSum);
    }
}
