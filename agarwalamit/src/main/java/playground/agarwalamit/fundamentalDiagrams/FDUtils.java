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

package playground.agarwalamit.fundamentalDiagrams;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.matsim.core.utils.io.IOUtils;

/**
 * Created by amit on 21.05.18.
 */

public class FDUtils {

    public static void cleanOutputDir(String outputDir){
        IOUtils.deleteDirectoryRecursively(new File(outputDir+"/ITERS/").toPath());
        IOUtils.deleteDirectoryRecursively(new File(outputDir+"/tmp/").toPath());
        new File(outputDir+"/logfile.log").delete();
        new File(outputDir+"/logfileWarningsErrors.log").delete();
        new File(outputDir+"/scorestats.txt").delete();
        new File(outputDir+"/modestats.txt").delete();
        new File(outputDir+"/stopwatch.txt").delete();
        new File(outputDir+"/traveldistancestats.txt").delete();
    }


    private void updateTransimFileNameAndDir(List<Integer> runningPoint, String outputDir) {
//        String outputDir = scenario.getConfig().controler().getOutputDirectory();
        //Check if Transim veh dir exists, if not create it
        if(! new File(outputDir+"/TransVeh/").exists() ) new File(outputDir+"/TransVeh/").mkdir();
        //first, move T.veh.gz file
        String sourceTVehFile = outputDir+"/ITERS/it.0/0.T.veh.gz";
        String targetTVehFilen = outputDir+"/TransVeh/T_"+runningPoint.toString()+".veh.gz";
        try {
            Files.move(new File(sourceTVehFile).toPath(), new File(targetTVehFilen).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("File not found.");
        }
    }

}
