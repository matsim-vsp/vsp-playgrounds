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

package playground.agarwalamit.opdyts.equil;

import java.io.File;
import java.io.IOException;

/**
 * Created by amit on 08.10.17.
 */

public class EqualParametricRunsFilesMover {

    private static boolean ignoreIfExists = true;

    public static void main(String[] args) {

        EqualParametricRunsFilesMover.moveFiles();
    }

    public static void moveFiles(){
        String srcDir = "/Users/amit/Documents/cluster/tub/agarwal/equilOpdyts/carBicycle/output/";
        String destinationDir = "/Users/amit/Documents/repos/runs-svn/opdyts/equil/car,bicycle/output/";

        for (String dir : new File(srcDir).list()) {
            if (dir.contains(".DS_Store")) {
                continue;
            }

            if (new File(dir).isFile()) {
                try {
                    org.apache.commons.io.FileUtils.copyFile( new File(srcDir+"/"+dir), new File (destinationDir+"/"+dir) );
                } catch (IOException e) {
                    throw new RuntimeException("Data is not copied. Reason : " + e);
                }
            } else {

                boolean added = new File(destinationDir+"/"+dir+"/").mkdir();
                if (!added && ignoreIfExists) continue; // dont do anything if dir already exists

                for (String childDir : new File(srcDir+"/"+dir).list()) {
                    if ( new File(srcDir+"/"+dir+"/"+childDir+"/").isFile() || childDir.contains(".DS_Store")) {
                        continue;
                    }
                    String dirsToAdd = destinationDir+"/"+dir+"/"+childDir+"/";

                    added = new File(dirsToAdd).mkdir();
                    if (!added && ignoreIfExists) continue; // dont do anything if dir already exists

                    try {
                        org.apache.commons.io.FileUtils.copyFile(new File(srcDir+"/"+dir+"/"+childDir+"/"+"convergence.png"),new File (dirsToAdd+"convergence.png"));
                        org.apache.commons.io.FileUtils.copyFile(new File(srcDir+"/"+dir+"/"+childDir+"/"+"decisionVariableVsASC.png"),new File (dirsToAdd+"decisionVariableVsASC.png"));
                        org.apache.commons.io.FileUtils.copyFile(new File(srcDir+"/"+dir+"/"+childDir+"/"+"opdyts.log"),new File (dirsToAdd+"opdyts.log"));
                        org.apache.commons.io.FileUtils.copyFile(new File(srcDir+"/"+dir+"/"+childDir+"/"+"opdyts.con"),new File (dirsToAdd+"opdyts.con"));
                        org.apache.commons.io.FileUtils.copyFile(new File(srcDir+"/"+dir+"/"+childDir+"/"+"opdyts.sum"),new File (dirsToAdd+"opdyts.sum"));
                    } catch (IOException e) {
                        throw new RuntimeException("Data is not copied. Reason : " + e);
                    }

                }
            }
        }
    }

}
