/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package scenarios.cottbus.run;

import playground.agarwalamit.parametricRuns.PrepareParametricRuns;

/**
 * @author tthunig
 */
public class ParametricOpdytsCottbusRuns {

	public static void main(String[] args) {
		int runCounter= 485;
		
		String baseOutDir = "/net/ils3/thunig/runs-svn/cottbus/opdyts/calibration/";
        String matsimDir = "tthunig-0.10.0-SNAPSHOT_4";
        
        StringBuilder buffer = new StringBuilder();
        PrepareParametricRuns parametricRuns = new PrepareParametricRuns("~/.ssh/known_hosts","~/.ssh/id_rsa_tub_math","thunig");

//        boolean[] useMSA = {true, false};
        boolean[] useMSA = {false};
        int[] opdytsIterations = {30,50,100};
//        int[] opdytsIterations = {30};
        int [] stepSizes = {5, 10, 20};
//        int [] stepSizes = {10};
        double [] selfTuningWts = {1.0, 4.0};
//        double [] selfTuningWts = {1.0};
        int [] warmUpIts = {5, 10};
//        int [] warmUpIts = {5};
		
        buffer.append(
                "runNr\tuseMSA\topdytsIts\tstepSize\tselfTunerWt\twarmUpIts")
              .append(PrepareParametricRuns.newLine);

        for (boolean useMSAflag : useMSA ) {
        	for (int opdytsIt : opdytsIterations) {
            for(int stepSize : stepSizes){
                    for (double selfTunWt : selfTuningWts) {
                        for (int warmUpIt : warmUpIts) {

                            String jobName = "run"+String.valueOf(runCounter++);
                            String params= useMSAflag + " " + opdytsIt + " "+ stepSize + " " + selfTunWt + " " + warmUpIt;

                            String [] additionalLines = {
                                    "echo \"========================\"",
                                    "echo \" "+matsimDir+" \" ",
                                    "echo \"========================\"",
                                    PrepareParametricRuns.newLine,

                                    "cd /net/ils3/thunig/matsim/"+matsimDir+"/",
                                    PrepareParametricRuns.newLine,

                                    "java -Djava.awt.headless=true -Xmx29G -cp tthunig-0.10.0-SNAPSHOT.jar " +
                                            "scenarios/cottbus/run/TtRunCottbusSimulation " +
                                            baseOutDir+jobName+"/ " +
                                            params+" "
                            };

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

        parametricRuns.writeNewOrAppendToRemoteFile(buffer, baseOutDir+"/runInfo.txt");
        parametricRuns.close();
	}

}
