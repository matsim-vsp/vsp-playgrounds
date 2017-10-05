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
package playground.vsp.nemo.ScenarioCreation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.matsim.api.core.v01.network.Network;

import playground.vsp.demandde.counts.BastHourlyCountData;

/**
 * @author tschlenther
 *
 */
public class NemoShortTermCountsCreator extends NemoCountsCreator {

	/**
	 * @param network
	 * @param pathToCountDataRootDirectory
	 * @param pathToCountStationToOSMNodesMappingFile
	 * @param outputPath
	 */
	public NemoShortTermCountsCreator(Network network, String pathToCountDataRootDirectory,
			String pathToCountStationToOSMNodesMappingFile, String outputPath, int firstYear, int lastYear) {
		super(network, pathToCountDataRootDirectory, pathToCountStationToOSMNodesMappingFile, outputPath);

		this.setFirstDayOfAnalysis(LocalDate.of(firstYear,1,1));
		this.setLastDayOfAnalysis(LocalDate.of(lastYear,12,31));
	}

	@Override
	public void run() {
		super.init();
		super.readData();
		  
		if(network!=null){
			readNodeIDsOfCountingStationsAndGetLinkIDs();
		}

		String description = "--Nemo short period count data--";
		SimpleDateFormat format = new SimpleDateFormat("YY_MM_dd_HHmmss");
		String now = format.format(Calendar.getInstance().getTime());
		description += "\n created: " + now;
		  
	  	createAndWriteMatsimCounts(description,"Nemo_ShortTermCounts_"+now);
		  
		finish();
		
	}
	
	@Override
	protected void analyzeYearDir(File rootDirOfYear, int currentYear) {
		log.info("Start analysis of directory " + rootDirOfYear.getPath());
		
		 File[] filesInRoot = rootDirOfYear.listFiles();
		 if (filesInRoot != null) {
		    for (File fileInRootDir : filesInRoot) {
		    	if(fileInRootDir.isDirectory()){
		    		analyzeCountDirectory(fileInRootDir, currentYear);
		    	}
		   	}
		  } else {
			  log.severe("something is wrong with the year directory .... please look here: " + rootDirOfYear.getAbsolutePath());
			  this.finish();
		  }
		
	}

	private void analyzeCountDirectory(File countDir, int currentYear) {
		File[] countData = countDir.listFiles();
		if(countData != null){
			try {
				for(File data : countData){
					if(data.getName().endsWith("xls")){
						readExcel(new FileInputStream(data), data.getName().substring(0, 8),currentYear);
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			log.severe("something is wrong with the count directory .... please look here: " + countDir.getAbsolutePath());
			this.finish();
		}
	}

	private void readExcel(FileInputStream fileInputStream, String countID, int year) {
		log.info("reading excel file of count " + countID);
		POIFSFileSystem fs;
		String directionString = "";
		
		try {
			fs = new POIFSFileSystem(fileInputStream);
			HSSFWorkbook wb = new HSSFWorkbook(fs);
			HSSFSheet sheet = wb.getSheetAt(0);
			HSSFRow row;
			HSSFCell cell;
			
			String streetID = sheet.getRow(3).getCell(1).getStringCellValue();
			BastHourlyCountData kfzCountData = this.kfzCountingStationsData.get(countID);
			BastHourlyCountData svCountData = this.kfzCountingStationsData.get(countID);
			
			if(kfzCountData == null){
				kfzCountData = new BastHourlyCountData(countID + "_" + streetID, null);	//ID = countID_countName_streetID
			}
			
			if(svCountData == null){
				svCountData = new BastHourlyCountData(countID +"_" + streetID, null);		//ID = countID_countName_streetID
			}
			
			
			for(int currentRow = 21; currentRow <= sheet.getLastRowNum(); currentRow ++){
				row = sheet.getRow(currentRow);
				
				int dayOfMonth = Integer.parseInt(row.getCell(0).getStringCellValue().substring(0, 2));
				int month = Integer.parseInt(row.getCell(0).getStringCellValue().substring(3, 5));
				
				LocalDate currentDate = LocalDate.of(year, month, dayOfMonth);
				
				cell = row.getCell(4);
				boolean isValidData = false;
				if(cell.getCellType() == 0){
					System.out.println("cell type is numeric: value=" + cell.getNumericCellValue());
					
				} else if(cell.getCellType() == 1){
					if(cell.getStringCellValue().equals("-")){
						isValidData = true;
					}
				} else{
					System.out.println("cell type = " + cell.getCellType());
				}
				
				if(this.weekRange_min <= currentDate.getDayOfWeek().getValue() && currentDate.getDayOfWeek().getValue() <= this.weekRange_max
						&& isValidData){
					
//					int hour = Integer.parseInt(row.getCell(1).getStringCellValue().substring(0,2));
//					int kfzValueDir1 = Integer.parseInt(row.getCell(5).getStringCellValue().substring(0,2));
//					int svValueDir1 = Integer.parseInt(row.getCell(6).getStringCellValue().substring(0,2));
//					int kfzValueDir2 = Integer.parseInt(row.getCell(7).getStringCellValue().substring(0,2));
//					int svValueDir2 = Integer.parseInt(row.getCell(8).getStringCellValue().substring(0,2));
					
					int hour = Integer.parseInt(row.getCell(1).getStringCellValue().substring(0,2));
					int kfzValueDir1 = getIntegerValue(row.getCell(5));
					int svValueDir1 = getIntegerValue(row.getCell(6));
					int kfzValueDir2 = getIntegerValue(row.getCell(7));
					int svValueDir2 = getIntegerValue(row.getCell(8));
					
					kfzCountData.computeAndSetVolume(true, hour, kfzValueDir1 - svValueDir1);				// we want proper distinction between heavy and light traffic vehicles
					kfzCountData.computeAndSetVolume(false, hour, kfzValueDir2 - svValueDir2);				// bicycle also is considered as 'KFZ'
	
					svCountData.computeAndSetVolume(true, hour, svValueDir1);
					svCountData.computeAndSetVolume(false, hour, svValueDir2);

				}
			}
			wb.close();
			
			this.kfzCountingStationsData.put(countID, kfzCountData);
			this.svCountingStationsData.put(countID, svCountData);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	int getIntegerValue (HSSFCell cell){
		if(cell.getCellType() == 0){
			return (int) cell.getNumericCellValue();
		}else{
			return Integer.parseInt(cell.getStringCellValue());
		}
	}

}
