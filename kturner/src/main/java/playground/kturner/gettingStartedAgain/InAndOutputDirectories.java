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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.log4j.Logger;

/**
 * Just getting started again with MATSim
 * 
 * 1st: work with dynamic in and output dir.
 * 
 * @author kturner
 * 
 */

class InAndOutputDirectories {
	
	static Logger log = Logger.getLogger(InAndOutputDirectories.class);

	public static void main(String[] args) throws IOException {
//		String relativ = new File(".").getCanonicalPath(); //gibt den Pfad an. --> Note: In this Case it is [git-location]\project folder, here e.g. Z:\git\playgrounds\kturner
//		String ordner = "\\TestordnerKMT";
//		File absolut = new File(relativ + ordner); 	//if / or \\ makes no difference
//		absolut.mkdirs();									//Create folder an all missing folders
//		
//		System.out.println("Der Absolute Pfad ist folgender: " + absolut.toString());
//		
//		FileWriter fw1 = new FileWriter(absolut + "/ausgabeKMT_1.txt");
//		FileWriter fw2 = new FileWriter("./TestordnerKMT/ausgabeKMT_2.txt");
//				
//	    writeTestDataToFile(fw1);
//	    writeTestDataToFile(fw2);
	    
	    copyInputFilesToOutputDirectory();
	    
	    System.out.println("done");    
	    
	    
	    
	}

	private static void writeTestDataToFile(FileWriter fw) throws IOException {
		BufferedWriter bw = new BufferedWriter(fw);

	    bw.write("test test test");
	    bw.newLine();
	    bw.write("tset tset tset");

	    bw.close();
	    
	    System.out.println("Testdata written");
	}
	
	private static void copyInputFilesToOutputDirectory() throws IOException {
		
		final String INPUT_DIR = "../../shared-svn/projects/freight/studies/MA_Turner-Kai/input/Berlin_Szenario/" ;
		final String OUTPUT_DIR = "../../OutputKMT/TestsOutput/" ;
		
		//Dateinamen
		final String NETFILE_NAME = "network.xml" ;
		final String VEHTYPEFILE_NAME = "vehicleTypes.xml" ;
		final String CARRIERFILE_NAME = "carrierLEH_v2_withFleet.xml" ;
		final String ALGORITHMFILE_NAME = "mdvrp_algorithmConfig_2.xml" ;
		final String TOLLFILE_NAME = "toll_cordon20.xml";		//Zur Mautberechnung
		final String TOLLAREAFILE_NAME = "toll_area.xml";  //Zonendefinition (Links) für anhand eines Maut-Files
		
		final String NETFILE = INPUT_DIR + NETFILE_NAME ;
		final String VEHTYPEFILE = INPUT_DIR + VEHTYPEFILE_NAME;
		final String CARRIERFILE = INPUT_DIR + CARRIERFILE_NAME;
		final String ALGORITHMFILE = INPUT_DIR + ALGORITHMFILE_NAME;
		final String TOLLFILE = INPUT_DIR + TOLLFILE_NAME;
		final String TOLLAREAFILE = INPUT_DIR + TOLLAREAFILE_NAME;
		
		File saveInputDirectory = new File(OUTPUT_DIR + "Input");
		createDir(saveInputDirectory);
		Files.copy(new File(NETFILE).toPath(), saveInputDirectory.toPath(), StandardCopyOption.REPLACE_EXISTING); //TODOSchreibt es in Datei "Input" -> Weg finden, das es wieder in korrekt benannte Datei kommt. ;)
	}
	
	private static void createDir(File file) {
		if (!file.exists()){
			log.info("Create directory: " + file + " : " + file.mkdirs());
		} else
			log.warn("Directory already exists! Check for older stuff: " + file.toString());
	}
		
//		private static final String NETFILE = INPUT_DIR + NETFILE_NAME ;
//		private static final String VEHTYPEFILE = INPUT_DIR + VEHTYPEFILE_NAME;
//		private static final String CARRIERFILE = INPUT_DIR + CARRIERFILE_NAME;
//		private static final String ALGORITHMFILE = INPUT_DIR + ALGORITHMFILE_NAME;
//		private static final String TOLLFILE = INPUT_DIR + TOLLFILE_NAME;
//		private static final String TOLLAREAFILE = INPUT_DIR + TOLLAREAFILE_NAME;
	
}
