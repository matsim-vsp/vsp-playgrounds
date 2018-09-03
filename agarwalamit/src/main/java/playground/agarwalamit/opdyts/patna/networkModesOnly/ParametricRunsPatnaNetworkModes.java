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

package playground.agarwalamit.opdyts.patna.networkModesOnly;

import playground.agarwalamit.opdyts.ModeChoiceRandomizer.ASCRandomizerStyle;
import playground.agarwalamit.opdyts.ObjectiveFunctionEvaluator;
import playground.vsp.parametricRuns.PrepareParametricRuns;

/**
 * A class to create a job script, write it on remote and then run the job based on the given parameters.
 *
 * Created by amit on 04.10.17.
 */

public class ParametricRunsPatnaNetworkModes {

    public static void main(String[] args) {
        int runCounter= 286;

        String baseOutDir = "/net/ils4/agarwal/patnaOpdyts/networkModes/calibration/output_selectExpBeta/";
        String matsimDir = "r_2a43a63d3725c3cd6688107b0153d1d4e325ce8c_patnaOpdyts_05July";

        StringBuilder buffer = new StringBuilder();
        PrepareParametricRuns parametricRuns = new PrepareParametricRuns("~/.ssh/known_hosts","~/.ssh/id_rsa_tub_math","agarwal");

        String ascStyles [] = {ASCRandomizerStyle.axial_fixedVariation.toString()
//                ,ASCRandomizerStyle.grid_randomVariation.toString(),
//                ASCRandomizerStyle.axial_randomVariation.toString(),ASCRandomizerStyle.grid_fixedVariation.toString()
        };

        double [] stepSizes = {0.25, 0.5, 1.0, 2.0};
        Integer [] convIterations = {600};
        Integer [] avgIts = {200};
        double [] selfTuningWts = {1.0};
        Integer [] warmUpIts = {5};

        boolean useConfigRandomSeed = false;
        boolean updateStepSize = true;

        String objFunTypes [] = {ObjectiveFunctionEvaluator.ObjectiveFunctionType.SUM_SCALED_LOG.toString(),
                ObjectiveFunctionEvaluator.ObjectiveFunctionType.SUM_SQR_DIFF_LOG.toString()};

        Integer [] opdytsIterations = {10, 20, 30};

        buffer.append("runNr\tascStyle\tstepSize\titerations2Convergence\titeration4averaging\tselfTunerWt\twarmUpIts\tuseConfigRandomSeedOnly\tupdateStepSize\tobjectiveFunctionTyppe\topdytsIteration")
              .append(PrepareParametricRuns.newLine);

        for (String ascStyle : ascStyles ) {
            for(double stepSize :stepSizes){
                for (int conIts : convIterations) {
                    for (int avgIt : avgIts) {
                        for (double selfTunWt : selfTuningWts) {
                            for (int warmUpIt : warmUpIts) {
                                for (String objFunTyp : objFunTypes) {
                                    for (int opdytsIt : opdytsIterations) {

                                        String jobName = "run" + String.valueOf(runCounter++);
                                        String params = ascStyle + " " + stepSize + " " + conIts + " " + avgIt+ " "+ selfTunWt + " "
                                                + warmUpIt + " " +useConfigRandomSeed + " " + updateStepSize + " " + objFunTyp + " " + opdytsIt ;

                                        String[] additionalLines = {
                                                "echo \"========================\"",
                                                "echo \" " + matsimDir + " \" ",
                                                "echo \"========================\"",
                                                PrepareParametricRuns.newLine,

                                                "cd /net/ils4/agarwal/matsim/" + matsimDir + "/",
                                                PrepareParametricRuns.newLine,

                                                "java -Djava.awt.headless=true -Xmx58G -cp agarwalamit-0.11.0-SNAPSHOT.jar " +
                                                        "playground/agarwalamit/opdyts/patna/networkModesOnly/PatnaNetworkModesOpdytsCalibrator " +
                                                        "/net/ils4/agarwal/patnaOpdyts/networkModes/calibration/inputs/config_networkModesOnly.xml " +
                                                        baseOutDir + "/" + jobName + "/ " +
                                                        "/net/ils4/agarwal/patnaOpdyts/networkModes/relaxedPlans/output_selectExpBeta/output_plans.xml.gz " +
                                                        params
                                        };

                                        parametricRuns.appendJobParameters("-l mem_free=15G");// 4 cores with 15G each
                                        parametricRuns.run(additionalLines, baseOutDir, jobName);
                                        buffer.append(jobName)
                                              .append("\t")
                                              .append(params.replace(' ', '\t'))
                                              .append(PrepareParametricRuns.newLine);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        parametricRuns.writeNewOrAppendToRemoteFile(buffer, baseOutDir+"/runInfo.txt");
        parametricRuns.close();
    }

}
