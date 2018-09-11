/* *********************************************************************** *
 * project: org.matsim.*
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
  
package playground.kturner.gettingStartedAgain;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import playground.kturner.utils.MergeFileVisitor;

public class StartAnything {
	
	private static final String OUTPUT_DIR = "../../OutputKMT/projects/freight/studies/reAnalysing_MA/MATSim/Grid/base_log2/" ;
	private static final String TEMP_DIR = "../../OutputKMT/Temp/";

	public static void main(String[] args) throws IOException {
//	
		Path startingDir = FileSystems.getDefault().getPath(TEMP_DIR);
        Path destDir = FileSystems.getDefault().getPath(OUTPUT_DIR);
        
        System.out.println("start dir path: "+ startingDir);
        System.out.println("dest dir path: " + destDir);
        
        File file = new File(TEMP_DIR + "test.txt");
        System.out.println("AbsolutePath: "+ file.getAbsolutePath());
        System.out.println("Path: "+ file.getPath());
		System.out.println("CanonicalPath: " + file.getCanonicalPath());
        System.out.println("Name: "+ file.getName());
        
        Path path = file.toPath();
        System.out.println("Filename from path: " + path.getFileName());

//        new File(OUTPUT_DIR).mkdirs(); // make target dir(s)
//        
//		try {
//			Files.walkFileTree(startingDir, new CopyDirVisitor(startingDir, destDir, StandardCopyOption.REPLACE_EXISTING));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
        
        System.out.println("FINISHED!");
	}
	
//	public static void main(String[] args) throws IOException {
//	try {
//		Files.walkFileTree(FileSystems.getDefault().getPath(OUTPUT_DIR + "Logs/"), new MergeFileVisitor(new File(OUTPUT_DIR + "Logs/logfile.log")) );
//		Files.walkFileTree(FileSystems.getDefault().getPath(OUTPUT_DIR + "Logs/"), new MergeFileVisitor(new File(OUTPUT_DIR + "Logs/logfileWarningsErrors.log"), true) );
////		Files.walkFileTree(FileSystems.getDefault().getPath(OUTPUT_DIR + "Logs/"), new MergeFileVisitor(FileSystems.getDefault().getPath(OUTPUT_DIR + "Logs/"), new File(OUTPUT_DIR + "Logs/logfile.log") ));
////		Files.walkFileTree(FileSystems.getDefault().getPath(OUTPUT_DIR + "Logs/"), new MergeFileVisitor(FileSystems.getDefault().getPath(OUTPUT_DIR + "Logs/"), new File(OUTPUT_DIR + "Logs/logfileWarningsErrors.log") ));
//	} catch (IOException e) {
//		e.printStackTrace();
////	}
//	}

}
