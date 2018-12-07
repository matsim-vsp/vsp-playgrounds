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

package playground.agarwalamit.opdyts.patna.allModes;

import playground.agarwalamit.opdyts.ModeChoiceRandomizer;
import playground.vsp.parametricRuns.PrepareParametricRuns;

/**
 * A class to create a job script, write it on remote and then run the job based on the given parameters.
 *
 * Created by amit on 04.10.17.
 */

public class ParametricRunsPatnaAllModes {

    public static void main(String[] args) {
        int runCounter= 485;

        String baseOutDir = "/net/ils4/agarwal/patnaOpdyts/allModes/calibration/output/";
        String matsimDir = "r_5a207c4ba06fa4017620044f18a82170122eacf4_patnaOpdyts_26Oct";

        StringBuilder buffer = new StringBuilder();
        PrepareParametricRuns parametricRuns = new PrepareParametricRuns("~/.ssh/known_hosts","~/.ssh/id_rsa_tub_math","agarwal");

        String ascStyles [] = {
                ModeChoiceRandomizer.ASCRandomizerStyle.axial_fixedVariation.toString(),
                ModeChoiceRandomizer.ASCRandomizerStyle.grid_fixedVariation.toString(),
                ModeChoiceRandomizer.ASCRandomizerStyle.axial_randomVariation.toString(),
                ModeChoiceRandomizer.ASCRandomizerStyle.grid_randomVariation.toString()
        };
        double [] stepSizes = {0.05, 0.1, 0.2};
        Integer [] convIterations = {600};
        double [] selfTuningWts = {1.0};
        Integer [] warmUpIts = {5};

//        buffer.append("runNr\tascStyle\tstepSize\titerations2Convergence\tselfTunerWt\twarmUpIts\tteleportationModesZoneType"+ PrepareParametricRuns.newLine);
        buffer.append(
                "runNr\tascStyle\tstepSize\titerations2Convergence\tselfTunerWt\twarmUpIts\tteleportationModesZoneType" + "\tinputPlans")
              .append(PrepareParametricRuns.newLine);

        for (String ascStyle : ascStyles ) {
            for(double stepSize :stepSizes){
                for (int conIts : convIterations) {
                    for (double selfTunWt : selfTuningWts) {
                        for (int warmUpIt : warmUpIts) {

                            String patnaTeleportationModesZonesType = "clusterAlgoKmeans";

                            String jobName = "run"+String.valueOf(runCounter++);
                            String params= ascStyle + " "+ stepSize + " " + conIts + " " + selfTunWt + " " + warmUpIt + " "+patnaTeleportationModesZonesType;

                            String [] additionalLines = {
                                    "echo \"========================\"",
                                    "echo \" "+matsimDir+" \" ",
                                    "echo \"========================\"",
                                    PrepareParametricRuns.newLine,

                                    "cd /net/ils4/agarwal/matsim/"+matsimDir+"/",
                                    PrepareParametricRuns.newLine,

                                    "java -Djava.awt.headless=true -Xmx29G -cp agarwalamit-0.10.0-SNAPSHOT.jar " +
                                            "playground/agarwalamit/opdyts/patna/allModes/PatnaAllModesOpdytsCalibrator " +
//                                            "/net/ils4/agarwal/patnaOpdyts/allModes/calibration/inputs/config_allModes.xml " +
                                            "/net/ils4/agarwal/patnaOpdyts/allModes/calibration/inputs/config_allModes_418_8.xml " +
                                            "/net/ils4/agarwal/patnaOpdyts/allModes/calibration/output/"+jobName+"/ " +
//                                            "/net/ils4/agarwal/patnaOpdyts/allModes/relaxedPlans/output/output_plans.xml.gz "+
                                            "/net/ils4/agarwal/patnaOpdyts/allModes/calibration/output/run418/_8/output_plans.xml.gz "+
                                            params+" "
                            };

                            parametricRuns.run(additionalLines, baseOutDir, jobName);
//                            buffer.append(jobName+"\t" + params.replace(' ','\t') + PrepareParametricRuns.newLine);
                            buffer.append(jobName)
                                  .append("\t")
                                  .append(params.replace(' ', '\t'))
                                  .append("\trun418/_8/output_plans.xml.gz")
                                  .append(PrepareParametricRuns.newLine);
                        }
                    }
                }
            }
        }

        parametricRuns.writeNewOrAppendToRemoteFile(buffer, baseOutDir+"/runInfo.txt");
        parametricRuns.close();
    }

}
