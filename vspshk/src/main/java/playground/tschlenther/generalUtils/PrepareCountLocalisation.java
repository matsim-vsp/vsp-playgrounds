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

/**
 * 
 */
package playground.tschlenther.generalUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

/**
 * @author tschlenther
 *
 */
public class PrepareCountLocalisation {

	private static String rootDir = null;
	private static String outputDir = null;
	Logger log = Logger.getLogger(PrepareCountLocalisation.class);

	private Map<String,String> countLocations = new HashMap<String,String>();
	private Map<String,Integer> countLocationsOccurences = new HashMap<String,Integer>();
	
	/**
	 * 
	 */
	public PrepareCountLocalisation(String inputRootDir, String outputDir) {
		this.rootDir = inputRootDir;
		this.outputDir = outputDir;
	}
	
	
	public void run(){
		readFiles();
		write(outputDir + "Nemo_xyPlot_CountLocations_UTM33N.csv", outputDir + "Nemo_kurzfristZaehlstellen_OSMNodeIDs_UTM33N.csv");
	}
	
	private void readFiles(){
		int count = 0;
		int count2 = 0;
		UnZipFile unzipper = new UnZipFile();
		
		File root = new File(rootDir);
		 File[] filesInRoot = root.listFiles();
		  if (filesInRoot != null) {
		    for (File fileInRootDir : filesInRoot) {
		    	log.info("looking at Directory: " + fileInRootDir.getName());
		    	if(fileInRootDir.isDirectory()){
		    		File[] allCountDataDirs = fileInRootDir.listFiles();
		    		for(File countDir : allCountDataDirs){
		    			File unzippedFolder = null;
		    			String countID = null;
		    			if(countDir.getName().endsWith("zip")){
		    				countID = countDir.getName().substring(0,countDir.getName().lastIndexOf("."));
		    				try {
			    				if(!this.countLocations.containsKey(countID)){
			    					unzippedFolder = unzipper.unZipFile(countDir);
			    				}
			    				else{
			    					continue;
			    				}
		    				} catch (Exception e) {
		    					// TODO Auto-generated catch block
		    					e.printStackTrace();
		    				}
		    			} else{
		    				unzippedFolder = countDir;
		    				countID= unzippedFolder.getName();
		    			}
		    			if(!this.countLocations.containsKey(countID)){
		    				count ++;
			    			if(unzippedFolder.isDirectory()){
				    				File[] countData = unzippedFolder.listFiles();
									if(countData.length > 1){
										
										String cntName = unzippedFolder.getName();
										Double longitude = null;
										Double latitude = null;
										boolean excelRead = false;
										
										CoordinateTransformation transformer = 
												 TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:32633");
										
										try {
											for(File data : countData){
												if(data.getName().endsWith("txt")){
													BufferedReader reader = new BufferedReader(new FileReader(data));
													
													String line;
														line = reader.readLine();
													while(line != null){
														if(line.startsWith("GPS long")){
															longitude = Double.parseDouble(line.substring(line.lastIndexOf("=") + 1, line.length()-1).replaceAll(",", "."));
														}
														if(line.startsWith("GPS lat")){
															latitude = Double.parseDouble(line.substring(line.lastIndexOf("=")+1, line.length()-1).replaceAll(",", ".")) ;
														}
														if(longitude != null && latitude != null){
															reader.close();
															break;
														}
														line = reader.readLine();
													}
													reader.close();
													if(latitude == null || longitude == null){
														log.error("could not read coordinates out of file " + data.getPath());
														continue;
													}
												}
												else if(data.getName().endsWith("xls") && !excelRead){
													cntName += readExcel(new FileInputStream(data));
												}
											}
										} catch (IOException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										Coord coord = new Coord(longitude,latitude);
										Coord transformedCoord = transformer.transform(coord);
										
										this.countLocations.put(unzippedFolder.getName(),  cntName + ";" + transformedCoord.getX() + ";" + transformedCoord.getY());
										this.countLocationsOccurences.put(unzippedFolder.getName(), 1);
										count2++;
									}
			    			}
			    			else{
			    				log.info("KNEW station " + countID + " before ....");
			    				this.countLocationsOccurences.put(countID, this.countLocationsOccurences.get(countID) + 1);
			    			}
		    			}	
		    		}
		    	}
		    }
		  } else {
			  log.error("something is wrong with the input directory .... please look here: " + root.getAbsolutePath());
		  }
		  log.info("looked at " + count + " stations...");
		  log.info("station dir's with less than 2 files:" + (count - count2));
	}
	
	private String readExcel(FileInputStream fileInputStream){
		log.info("reading excel file ");
		POIFSFileSystem fs;
		String directionString = "";
		try {
			fs = new POIFSFileSystem(fileInputStream);
			HSSFWorkbook wb = new HSSFWorkbook(fs);
			HSSFSheet sheet = wb.getSheetAt(0);
			HSSFRow row;
			HSSFCell cell;
			
			row = sheet.getRow(2);
			
			for(int i = 6; i< row.getLastCellNum(); i++){
				cell = row.getCell(i);
				if(cell != null){

					try{
						if(cell.getStringCellValue().length() >= 1);
						directionString += row.getCell(i).getStringCellValue().replace(" ", "_");
					}
					catch(IllegalStateException e){
						directionString += cell.getNumericCellValue();
					}
					catch (Exception ex){
						ex.printStackTrace();
					}
				}
			}

			directionString += "_";
			row = sheet.getRow(3);
			
			for(int i = 6; i< row.getLastCellNum(); i++){
				cell = row.getCell(i);
				if(cell != null){

					try{
						if(cell.getStringCellValue().length() >= 1);
						directionString += row.getCell(i).getStringCellValue().replace(" ", "_");
					}
					catch(IllegalStateException e){
						directionString += cell.getNumericCellValue();
					}
					catch (Exception ex){
						ex.printStackTrace();
					}
				}
			}
			
			log.info("directionString= " + directionString);
		
			wb.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return directionString;
	}
	
	private void write(String xyFilePath, String listFilePath){
		
		log.info("....start writing output");
		String xyHead = "CountID;CountName;GSP_long;GPS_lat";
		String listHead = "CountID;OSMFromNode;OSMToNode;Occurences;comment";
		BufferedWriter xyFileWriter = IOUtils.getBufferedWriter(xyFilePath);
		BufferedWriter listFileWriter = IOUtils.getBufferedWriter(listFilePath);
		try {
			xyFileWriter.write(xyHead);
			listFileWriter.write(listHead);
			for (String count : this.countLocations.keySet()){
				xyFileWriter.newLine();
				xyFileWriter.write(count + ";" + this.countLocations.get(count));
				listFileWriter.newLine();
				listFileWriter.write(count + "_R1;;;" + this.countLocationsOccurences.get(count) + ";");
				listFileWriter.newLine();
				listFileWriter.write(count + "_R2;;;" + this.countLocationsOccurences.get(count) + ";");
			}
			xyFileWriter.flush();
			xyFileWriter.close();
			listFileWriter.flush();
			listFileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("finshed writing output...");
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PrepareCountLocalisation prep = 
			new PrepareCountLocalisation("C:/Users/Work/VSP/Nemo/Verkehrszaehlung_2015/nemo-master-ebd8d0a92610eb3fde0fd80bfe89fa12f1a68c31/data/input/counts/verkehrszaehlung_2015"
					, "C:/Users/Work/VSP/Nemo/Verkehrszaehlung_2015/nemo-master-ebd8d0a92610eb3fde0fd80bfe89fa12f1a68c31/data/input/counts/countLocations_output/");
		prep.run();
	}

}
