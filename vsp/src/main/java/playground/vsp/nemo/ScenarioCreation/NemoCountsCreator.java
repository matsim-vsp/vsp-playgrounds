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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

import playground.vsp.demandde.counts.BastHourlyCountData;

/**
 * @author tschlenther
 *
 */
public class NemoCountsCreator {

	private Logger log = Logger.getLogger("NemoCountsCreatorLogger");

	private String outputPath;
	private String pathToCountData;
	private String pathToOSMMappingFile;
	
	private LocalDate firstDayOfAnalysis = null;
	private LocalDate lastDayOfAnalysis = null;
	private List<LocalDate> datesToIgnore = new ArrayList<LocalDate>();
	
	private int monthRange_min = 1;
	private int monthRange_max = 12;
	
	private int weekRange_min = 1;
	private int weekRange_max = 5;
	
	private Map<String,BastHourlyCountData> kfzCountingStationsData = new HashMap<String,BastHourlyCountData>();
	private Map<String,BastHourlyCountData> svCountingStationsData = new HashMap<String,BastHourlyCountData>();
	
	private Map<String,String> countingStationNames = new HashMap<String,String>();
	private Map<String,String> problemsPerCountingStation = new HashMap<String,String>();
	private List<String> notLocatedCountingStations = new ArrayList<String>();
	
	private Map<String,Id<Link>> linkIDsOfCountingStations = new HashMap<String,Id<Link>>();
	private List<Long> countingStationsToOmit = new ArrayList<Long>();
	private Network network;

	private final String kfzColumnHeader = "KFZ";
	private final String svColumnHeader = "SV";
	
	public NemoCountsCreator(Network network, String pathToCountDataRootDirectory, String pathToCountStationToOSMNodesMappingFile, String outputPath) {
		this.network = network;
		this.outputPath = outputPath;
		this.pathToCountData = pathToCountDataRootDirectory;
		this.pathToOSMMappingFile = pathToCountStationToOSMNodesMappingFile;
	}

	//--------------------------------------------------------------------------------------------------------------------------------------
	/**
	 * Runs through the data directory and aggregates data that lies in between the specified start and end date.
	 * Then runs through the given network and looks for the links that connect the corresponding fromNode and toNode of each counting station,
	 * specified in a given csv file. Finally, the aggregated data is converted into two matsim-counts-files, one each for normal and heavy traffic,
	 * which get written out to the specified output directory. With <i>specified</i> i mean given as a parameter to the constructor<br>
	 * tschlenther jul'17
	 */
	public void run(){
		
		//list counting stations that might lead to some calibration problems or where data/localization is not clear
		countingStationsToOmit.add(5002l);
		countingStationsToOmit.add(5025l);		//not clear where exactly the counting station is located (hauptfahrbahn?)
		
		File outPutDir = new File(this.outputPath.substring(0, this.outputPath.lastIndexOf("/")));
		if (!outPutDir.exists()){
			outPutDir.mkdirs();
		}
		initializeLogger();
		
		 File rootDirecoty = new File(this.pathToCountData);
		 File[] filesInRoot = rootDirecoty.listFiles();
		  if (filesInRoot != null) {
		    for (File fileInRootDir : filesInRoot) {
		    	if(fileInRootDir.isDirectory()){
		    		int currentYear = Integer.parseInt(fileInRootDir.getName().substring(fileInRootDir.getName().length() - 4));
		    		if(checkIfYearIsToBeAnalyzed(currentYear)){
		    			analyzeYearDir(fileInRootDir,currentYear);
		    		}
		    	}
		    }
		  } else {
			  log.severe("the given root directory of input count data could not be accessed... aborting");
			  this.finish();
		  }
		  
		  writeOutListOfAnalyzedCountStations();
		  if(network!=null) readNodeIDsOfCountingStationsAndGetLinkIDs();
		  createAndWriteMatsimCounts();
		  finish();
	}
	
	private void createAndWriteMatsimCounts() {
		Counts svCountContainer = new Counts();
		Counts kfzCountContainer = new Counts();
		SimpleDateFormat format = new SimpleDateFormat("YY_MM_dd_HHmmss");
		
		String description = "--Nemo count data-- start date: " + this.firstDayOfAnalysis.toString() + " end date:" + this.lastDayOfAnalysis.toString();
		String now = format.format(Calendar.getInstance().getTime());
		
		description += "\n created: " + now;
		svCountContainer.setDescription(description);
		kfzCountContainer.setDescription(description);

		svCountContainer.setYear(this.lastDayOfAnalysis.getYear());
		kfzCountContainer.setYear(this.lastDayOfAnalysis.getYear());
		
		log.info("start conversion of sv data into matsim counts..");
		convertDataToMatSimCounts(svCountContainer, svCountingStationsData);
		log.info("...finished with conversion of sv data");
		
		log.info("start conversion of kfz data into matsim counts..");
		convertDataToMatSimCounts(kfzCountContainer, kfzCountingStationsData);
		log.info("...finished with conversion of kfz data");
		
		String out = "NemoCounts_data_";
		log.info("writing sv counts to " + this.outputPath + out + "SV" + now);
		CountsWriter svWriter = new CountsWriter(svCountContainer);
		svWriter.write(this.outputPath + out + "SV_" + now + ".xml");
		
		log.info("writing sv counts to " + this.outputPath + out + "KFZ" + now);
		CountsWriter kfzWriter = new CountsWriter(kfzCountContainer);
		kfzWriter.write(this.outputPath + out + "KFZ_" + now + ".xml");
		
		log.info("...finished writing...");
	}


	private void convertDataToMatSimCounts(Counts<Link> container, Map<String,BastHourlyCountData> dataMap){
		int cnt = 0;
		for(String countNrString : dataMap.keySet()){
			String stationID = "NW_" + countNrString;
			cnt ++;
			if(cnt % 10 == 0){
				log.info("converting station nr " + cnt);
			}
			BastHourlyCountData data = dataMap.get(countNrString);
			
			Id<Link> linkIDDirectionOne = this.linkIDsOfCountingStations.get(stationID + "_R1");
			Id<Link> linkIDDirectionTwo = this.linkIDsOfCountingStations.get(stationID + "_R2");
			if(container == null){
				log.severe("container is null..cannot convert..");
			}
			if(data == null){
				log.severe("can not access the basthourlycountdata... the countNrString was " + countNrString );
			}
			if(linkIDDirectionOne == null){
				String problem = "direction 1 of the counting station NW_" + countNrString + " was not localised in the given csv file, thus it is matched to generic link id: directionNotSpecifiedInCSV_" + stationID + "R1";
				log.severe(problem);
				this.problemsPerCountingStation.put(stationID + "_R1", problem);
				linkIDDirectionOne = Id.createLinkId("directionNotSpecifiedInCSV_" + stationID + "_R1");
				this.notLocatedCountingStations.add(stationID + "_R1");
			}
			if(linkIDDirectionTwo == null){
				String problem = "direction 2 of the counting station NW_" + countNrString + " was not localised in the given csv file, thus it is matched to generic link id: directionNotSpecifiedInCSV_" + stationID + "R2";
				log.severe(problem);
				this.problemsPerCountingStation.put(stationID + "_R2", problem);
				linkIDDirectionTwo = Id.createLinkId("directionNotSpecifiedInCSV_" + stationID + "_R2");
				this.notLocatedCountingStations.add(stationID + "_R2");
			}
				
				Count<Link> countDirOne = null;
				Count<Link> countDirTwo = null;
			try{
				String name = data.getId().substring(0,5);
				countDirOne = container.createAndAddCount(linkIDDirectionOne, name + "R1_" + data.getId().substring(5));
				countDirTwo = container.createAndAddCount(linkIDDirectionTwo, name + "R2_" + data.getId().substring(5));
			} catch(Exception e){
				String str = "\n current station = " + stationID + ". the other station that is already on the link is station " + container.getCount(linkIDDirectionTwo) + " for R1 or station " + container.getCount(linkIDDirectionOne) + " for R2";
				System.out.println(e.getMessage() + str);
				e.printStackTrace();
				
				this.problemsPerCountingStation.put(stationID, e.getMessage() + str);
				continue;
			}
			
			for(int i = 1; i < 25; i++) {
				Double valueDirOne = data.getR1Values().get(i);
				Double valueDirTwo = data.getR2Values().get(i);

				if(valueDirOne == null){
					String problem = "station " + stationID + " has a non-valid entry for hour " + i +" in direction one. Please check this. The value in the count file is set to -1."; 
					log.severe( problem + " Error occured at creation nr " + cnt);
					if(this.problemsPerCountingStation.containsKey(stationID)){
						problem = this.problemsPerCountingStation.get(stationID) + problem;
					}
					this.problemsPerCountingStation.put(stationID, problem);
					valueDirOne = -1.;
				}
				if(valueDirTwo == null){
					String problem = "station " + countNrString + " has a non-valid entry for hour " + i +" in direction two. Please check this. The value in the count file is set to -1."; 
					log.severe( problem + " Error occured at creation nr " + cnt);
					if(this.problemsPerCountingStation.containsKey(stationID)){
						problem = this.problemsPerCountingStation.get(stationID) + problem;
					}
					this.problemsPerCountingStation.put(stationID, problem);
					valueDirTwo = -1.0;
				}
					countDirOne.createVolume(i, valueDirOne);
					countDirTwo.createVolume(i, valueDirTwo);
			}
		}
	}

	private void readNodeIDsOfCountingStationsAndGetLinkIDs() {		
		log.info("...start reading OSM-nodeID's from " + this.pathToOSMMappingFile);
		
		Map<String,Id<Link>> linkIDsOfCounts = new HashMap<String,Id<Link>>();
		TabularFileParserConfig config = new TabularFileParserConfig();
        config.setDelimiterTags(new String[] {";"});
        config.setFileName(pathToOSMMappingFile);
        
        CountLinkFinder linkFinder = new CountLinkFinder(network);
        
        new TabularFileParser().parse(config, new TabularFileHandler() {
        	private boolean header = true;
        	
			@Override
			public void startRow(String[] row) {
				if(!header){
					Node fromNode = network.getNodes().get(Id.createNodeId(Long.parseLong(row[1])));
					if(fromNode == null){
						String problem = "could not find fromNode " + Id.createNodeId(Long.parseLong(row[1]));
						log.severe(problem);
						log.severe("this means something went wrong in network creation");
						problemsPerCountingStation.put(row[0], problem);
						log.severe("setting a non-valid link id: " + "noFromNode"  + row[0]);
						linkIDsOfCounts.put(row[0], Id.createLinkId("noFromNode_" + row[0]));
						header = false;
						notLocatedCountingStations.add(row[0]);
						return;
					}
					Id<Node> toNodeID = Id.createNodeId(Long.parseLong(row[2]));
					Node toNode = network.getNodes().get(toNodeID);
					if(toNode == null){
						String problem = "could not find toNode " + Id.createNodeId(Long.parseLong(row[1]));
						log.severe(problem);
						log.severe("this means something went wrong in network creation");
						problemsPerCountingStation.put(row[0], problem);
						log.severe("setting a non-valid link id: " + "noToNode"  + row[0]);
						linkIDsOfCounts.put(row[0], Id.createLinkId("noToNode_" + row[0]));
						header = false;
						notLocatedCountingStations.add(row[0]);
						return;
					}
				
					Id<Link> countLinkID = null;
					for(Link outlink : fromNode.getOutLinks().values()){
						if(outlink.getToNode().getId().equals(toNodeID)){
							countLinkID = outlink.getId();
						}
					}
					if(countLinkID == null){
						String problem = "could not find a link directly leading from node " + fromNode.getId() + " to node " + toNodeID;
//						log.severe(problem);
						problemsPerCountingStation.put(row[0], problem);

						countLinkID = linkFinder.getFirstLinkOnTheWayFromNodeToNode(fromNode, toNode);
						if(countLinkID == null){
							problem = "COULD FIND NO PATH LEADING FROM NODE " + fromNode.getId() + " TO NODE " + toNodeID;
							log.severe(problem);
							countLinkID = Id.createLinkId("pathCouldNotBeCreated_" + row[0]);
							problemsPerCountingStation.put(row[0], problem);
							notLocatedCountingStations.add(row[0]);
						}
						
					}
//					log.info("adding key-value-pair: k=" + row[0] + " v=" + countLinkID);
					linkIDsOfCounts.put(row[0], countLinkID);
				}
				header = false;
			}
		
        });
        log.info("-----------------------------------------------------");
        log.info("read in " + linkIDsOfCounts.size() + " link-id's");
        this.linkIDsOfCountingStations = linkIDsOfCounts;
        
        log.info("number of OSM-Node-ID-mappings that were not directly connected by a link, but a path could be calculated: " + linkFinder.getNrOfFoundPaths());
        
        if(linkFinder.getNrOfFoundPaths() >= 1){
        	log.info("writing out a network file for visualisation that contains all reconstructed paths between the corresponding OSM-fromNodes and OSM-toNodes. The first link of a path has capacity of 0, the second capacity of 1 ....");
    		SimpleDateFormat format = new SimpleDateFormat("YY_MM_dd_HH_mm");
        	linkFinder.writeNetworkThatShowsAllFoundPaths(outputPath + "visualisationNetworkOfReconstrucetPaths_" + format.format(Calendar.getInstance().getTime()) + ".xml");
        }
	}



	private void analyzeYearDir(File rootDirOfYear, int currentYear) {
		log.info("Start analysis of directory " + rootDirOfYear.getPath());
		
		 File[] filesInRoot = rootDirOfYear.listFiles();
		  if (filesInRoot != null) {
		    for (File fileInRootDir : filesInRoot) {
		    	if(fileInRootDir.isDirectory() && checkIfMonthIsToBeAnalyzed(fileInRootDir.getName())){
		    		analyzeMonth(fileInRootDir, currentYear);
		    	}
		    }
		  } else {
			  log.severe("something is wrong with the input directory .... please look here: " + rootDirOfYear.getAbsolutePath());
			  this.finish();
		  }
		
	}

	/**
	 * goes through the input data file that contains traffic data of one counting station for one month and aggregates the data in the previously defined way.
	 * the first three rows of the input file define the layout of the file, for more information see the documentation file at
	 * shared-svn\projects\nemo_mercator\40_Data\counts\LandesbetriebStrassenbauNRW_Verkehrszentrale\BASt-Bestandsbandformat_Version2004.pdf
	 * 
	 */
	private void analyzeMonth(File monthDir, int currentYear) {
		log.info("Start to analyze month " + monthDir.getName());
		
		File[] countFiles = monthDir.listFiles();
		  if (countFiles != null) {
			for(int ff = 0; ff<countFiles.length; ff++){
				File countFile = countFiles[ff];
				String name = countFile.getName();
				this.countingStationNames.put(name.substring(0, name.lastIndexOf(".")) , monthDir.getName());
				try {
					BufferedReader reader = new BufferedReader(new FileReader(countFile));
					
					String headerOne = reader.readLine();
					String headerTwo = reader.readLine();
					String headerThree = reader.readLine();
					
					String countID = headerOne.substring(5, 9);
					
					Long id = Long.parseLong(countID); 
					if(countingStationsToOmit.contains(id)){
						log.info("skipping station " + id);
						continue;
					}
					
					String streetID = headerOne.substring(13,20);
					streetID = streetID.replaceAll("\\s", "");
					String countName = headerOne.substring(21,46);
					countName = countName.replaceAll("\\s", "");

					int nrOfLanesDir1 = Integer.parseInt(headerTwo.substring(1, 3));
					int nrOfLanesDir2 = Integer.parseInt(headerTwo.substring(4, 6));
					
					int nrOfVehicleGroups = Integer.parseInt(headerThree.substring(1, 3));	// either 1 => all vehicles in one class or 2 => distinction of heavy traffic

					int kfzBaseColumn = Integer.MAX_VALUE;
					int svBaseColumn = Integer.MAX_VALUE;
					
					String[] headerThreeArray = headerThree.split("\\s+");
					for(int i = 2; i < headerThreeArray.length ; i++){
						String vehicleClass = headerThreeArray[i];
						if(vehicleClass.equals(kfzColumnHeader)){
							kfzBaseColumn = i;
						} else if(vehicleClass.equals(svColumnHeader)){
							svBaseColumn = i;
						}
					}
					if(svBaseColumn == Integer.MAX_VALUE){
						log.severe("could not find SV column" + countFile.getAbsolutePath());
						log.severe("at the moment, this can not be processed and leads to an abort..");
						reader.close();
						throw new RuntimeException("could not find SV column" + countFile.getAbsolutePath());
					}
					
										
					BastHourlyCountData kfzData = this.kfzCountingStationsData.get(countID);
					if(kfzData == null){
						kfzData = new BastHourlyCountData(countID + "_" + countName + "_" + streetID, null);	//ID = countID_countName_streetID
					}
					
					BastHourlyCountData svData = this.kfzCountingStationsData.get(countID);
					if(svData == null){
						svData = new BastHourlyCountData(countID +"_" + countName + "_" + streetID, null);		//ID = countID_countName_streetID
					}
					
					String line;
					String[] rowData;
					while((line = reader.readLine()) != null) {
						if(line.charAt(6) != 'i'){				//letter i stands for data that was somehow edited after investigation. we'll skip the row
							rowData = line.split("\\s+");
							
							Integer currentMonth = Integer.parseInt(line.substring(2, 4));
							Integer currentDay = Integer.parseInt(line.substring(4, 6));
							
							LocalDate currentDate = LocalDate.of(currentYear, currentMonth, currentDay);
							if (currentDate.isAfter(lastDayOfAnalysis)){
								break;
							}
							if( currentDate.isAfter(firstDayOfAnalysis.minusDays(1))
									&& currentDay >= this.weekRange_min && currentDay <= this.weekRange_max
									&& !this.datesToIgnore.contains(currentDate) ){
								
								int hour = Integer.parseInt(rowData[1].substring(0, 2));
								double svValueOfDirection = 0;
								double kfzValueOfDirection = 0;
								
								//direction1
								for(int lane = 1; lane <= nrOfLanesDir1; lane ++){
									String svValueOfLaneString = rowData[svBaseColumn + (lane-1) * nrOfVehicleGroups];
									String kfzValueOfLaneString = rowData[kfzBaseColumn + (lane-1) * nrOfVehicleGroups];
									double svValueOfLane = 0;
									if(svValueOfLaneString.endsWith("-")){
										svValueOfLane = Double.parseDouble(svValueOfLaneString.substring(0, svValueOfLaneString.length() - 1)); 
										svValueOfDirection += svValueOfLane;
									}
									if(kfzValueOfLaneString.endsWith("-")){
										//in the underlying data the kfz volume includes all heavy traffic (sv)... so we need to calculate the difference
										double kfzSvDifferenceOfLane = Double.parseDouble(kfzValueOfLaneString.substring(0, kfzValueOfLaneString.length() - 1)) - svValueOfLane; 
										kfzValueOfDirection += kfzSvDifferenceOfLane;
									}
								}
								
								kfzData.computeAndSetVolume(true, hour, kfzValueOfDirection);
								svData.computeAndSetVolume(true, hour, svValueOfDirection);
								
								//direction 2
								int svBaseCloumnDir2 = svBaseColumn + nrOfLanesDir1 * nrOfVehicleGroups;
								int kfzBaseColumnDir2 = kfzBaseColumn + nrOfLanesDir1 * nrOfVehicleGroups;
								svValueOfDirection = 0;
								kfzValueOfDirection = 0;
								
								for(int lane = 1; lane <= nrOfLanesDir2; lane ++){
									String svValueOfLaneString = rowData[svBaseCloumnDir2 + (lane-1) * nrOfVehicleGroups];
									String kfzValueOfLaneString = rowData[kfzBaseColumnDir2 + (lane-1) * nrOfVehicleGroups];
									double svValueOfLane = 0;
									if(svValueOfLaneString.endsWith("-")){
										svValueOfLane = Double.parseDouble(svValueOfLaneString.substring(0, svValueOfLaneString.length() - 1)); 
										svValueOfDirection += svValueOfLane;
									}
									if(kfzValueOfLaneString.endsWith("-")){
										//in the underlying data the kfz volume includes all heavy traffic (sv)... so we need to calculate the difference
										double kfzSvDifferenceOfLane = Double.parseDouble(kfzValueOfLaneString.substring(0, kfzValueOfLaneString.length() - 1)) - svValueOfLane; 
										kfzValueOfDirection += kfzSvDifferenceOfLane;
									}
								}
								kfzData.computeAndSetVolume(false, hour, kfzValueOfDirection);
								svData.computeAndSetVolume(false, hour, svValueOfDirection);
							}
						}
					}
					
					this.kfzCountingStationsData.put(countID, kfzData);
					this.svCountingStationsData.put(countID, svData);
					
					
					reader.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					log.severe("could not access " + countFile.getAbsolutePath() + "\n the corresponding data is not taken into account");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    	
		    }
		  } else {
			  log.severe("the following directory is empty or cannot be accessed. Thus, it is skipped. Path = " + monthDir.getAbsolutePath());
			  this.finish();
		  }
	}

	private boolean checkIfMonthIsToBeAnalyzed(String name) {
		int month = Integer.parseInt(name.substring(name.length() - 2));
		return (month >= this.monthRange_min && month <= monthRange_max && (month <= lastDayOfAnalysis.getMonthValue()) );
	}

	private boolean checkIfYearIsToBeAnalyzed(Integer dirYear) {
		int startYear = this.firstDayOfAnalysis.getYear();
		int endYear = this.lastDayOfAnalysis.getYear();
		return (dirYear >= startYear && dirYear <= endYear);
	}

	private void finish() {
			log.info("Aggregated data sets: \n kfz-data : " + this.kfzCountingStationsData.size() + "\n sv-data: " + this.svCountingStationsData.size());
			log.info("number of problems that occured while creating matsim counts: " + this.problemsPerCountingStation.size());
			String allProblems = "";
			List<String> keySet = new ArrayList<String>();
			keySet.addAll(this.problemsPerCountingStation.keySet());
			Collections.sort(keySet);
			for(String station : keySet){
				allProblems += "\n" + station + ": \t" + this.problemsPerCountingStation.get(station);
			}
			log.info("list of these problems per station: \n" + allProblems);
			
			log.info("-----------------");
			if(!this.notLocatedCountingStations.isEmpty()){
				log.info("List of not located counting stations: \n");
				String list = "~ ";
				for(String station : notLocatedCountingStations){
					list += station + "\n~ "; 
				}
				log.info(list);
			}
			log.info("...closing NemoCountsCreator...");
	}

	
	private void writeOutListOfAnalyzedCountStations() {
		try {
			  log.info("writing out all counting station names to " + outputPath);
			  File f = new File(outputPath + "allCountingStations_from_" + this.firstDayOfAnalysis.toString() + "_to" + this.lastDayOfAnalysis.toString() + ".txt");
			  if(f.exists()) f.delete();
			  f.createNewFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter( f ) );
			List<String> allNames = new ArrayList<String>(); 
			allNames.addAll(this.countingStationNames.keySet());
			Collections.sort(allNames);
			
			for(String countName : allNames){
				writer.write(countName + "\t letzter Monat : " + countingStationNames.get(countName) + "\n");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	
	private void initializeLogger(){
		 SimpleDateFormat format = new SimpleDateFormat("YY_MM_dd_HHmmss");
		 FileHandler fh = null;
		 ConsoleHandler ch = null;
	        try {
	        	fh = new FileHandler(outputPath + "Log_"
	                + format.format(Calendar.getInstance().getTime()) + ".log");
	        	ch = new ConsoleHandler();
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        Formatter formatter = new Formatter() {
	        	
	            @Override
	            public String format(LogRecord record) {
	                SimpleDateFormat logTime = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
	                Calendar cal = new GregorianCalendar();
	                cal.setTimeInMillis(record.getMillis());
	                return ( record.getLevel() + " "
	                        + logTime.format(cal.getTime())
	                        + " || "
	                        + record.getSourceClassName().substring(
	                                record.getSourceClassName().lastIndexOf(".")+1,
	                                record.getSourceClassName().length())
	                        + "."
	                        + record.getSourceMethodName()
	                        + "() : "
	                        + record.getMessage() + "\n");
	            }
	        };
	        log.setUseParentHandlers(false);
	        fh.setFormatter(formatter);
	        ch.setFormatter(formatter);
	        log.addHandler(fh);
	        log.addHandler(ch);
	        
	}
	
	public static void main(String[] args){
		final String INPUT_COUNT_NODES_MAPPING_CSV= "C:/Users/Work/svn/shared-svn/projects/nemo_mercator/40_Data/counts/OSMNodeIDs_Testlauf.csv";
		final String INPUT_COUNT_DATA_ROOT_DIR = "C:/Users/Work/svn/shared-svn/projects/nemo_mercator/40_Data/counts/LandesbetriebStrassenbauNRW_Verkehrszentrale";
		final String OUTPUT_COUNTS_DIR = "C:/Users/Work/VSP/Nemo/";
		
		NemoCountsCreator countCreator = new NemoCountsCreator(null, INPUT_COUNT_DATA_ROOT_DIR, INPUT_COUNT_NODES_MAPPING_CSV, OUTPUT_COUNTS_DIR);
		
		countCreator.setFirstDayOfAnalysis(LocalDate.of(2014, 1, 1));
		countCreator.setLastDayOfAnalysis(LocalDate.of(2014, 1, 2));
		countCreator.run();
	}
	
//-----------------------------------------------------------------  SETTERS  ----------------------------------------------------------------------------------------
	public void setOutputPath (String newOutputPath){
		this.outputPath = newOutputPath;
	}
	
	public void setNetwork(Network network){
		this.network = network;
	}
	
	public void setFirstDayOfAnalysis(LocalDate day){
		this.firstDayOfAnalysis = day;
	}
	
	public void setLastDayOfAnalysis(LocalDate day){
		this.lastDayOfAnalysis = day;
	}

	public void setDatesToIgnore(List<LocalDate> datesToIgnore) {
		this.datesToIgnore = datesToIgnore;
	}

	public void addToStationsToOmit(Long stationID){
		this.countingStationsToOmit.add(stationID);
	}
	
	public void setStationsToOmit(List<Long> listOfStationsToOmit){
		this.countingStationsToOmit = listOfStationsToOmit;
	}
}
