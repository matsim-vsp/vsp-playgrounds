package playground.vsp.demandde.counts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.filter.expression.ThisPropertyAccessorFactory;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.opengis.referencing.FactoryException;

import playground.vsp.andreas.fcd.ReadFcdNetwork;
import playground.vsp.demandde.counts.BastHourlyCountData.Day;

/** 
 * schlenther, may 18:
 * added some functionality:
 * you can now add a csv file containing node osm id's of countsand a corresponding network
 * so counts are automatically mapped on the corresponding link.
 * i have not imported so far the logic of finding a link id when the given from and to nodes are not 
 * directly connected in the given network. this might not be wished anyway.
 * in addition, you can now choose which column you want to convert by setting VEH_CLASS_COLUMN_HEADER field
 * 
 * 
 *<pre>
 *this class reads hourly traffic data from BASt counting stations out of a .txt file,
 *averages it per station and converts it into one MATSim Counts file, written out to <b>OUTPUTDIR</b>.
 *(see <link> http://www.bast.de/DE/FB-V/Fachthemen/v2-verkehrszaehlung/Verkehrszaehlung.html to download data </link>)
 *
 *The resulting MATSim counts have station names set by the following pattern:
 *<b><i>BASt_ID-DIRECTION</i></b>
 *Their linkID is currently set to the same. ====> partly deprecated, see comment above (schlenther, may 18)
 *Every original BASt counting station is converted into two Count objects, one for each direction.
 *The interval of the weekdays to be considered goes from <b>BEGINNING_WEEKDAY </b> to
 *<b>ENDING_WEEKDAY</b>, 1 representing Monday... 5 Friday.
 *<b><i>CALC_WEEKENDS</i></b> defines whether weekends should be considered. Weekends are defined from Saturday to Sunday. In addition, every public holiday is considered.
 *
 *<b>NOTE: </b> only correct values and corrected (estimated) values in the BASt file are considered. (see BASt datensatzbeschreibung).
 * </pre>
 * @author Tilmann Schlenther
 */
public class TSBASt2Count {
	
	//-------------------------  SETTINGS
	private final static int BEGINNING_WEEKDAY = 2;
	private final static int ENDING_WEEKDAY = 4;
	private final static boolean CALC_WEEKENDS = false;
	private final static boolean USE_UNLOCATED_STATIONS= false;
	private final static boolean USE_UNVALID_HOURS = false;
	private final static String VEH_CLASS_COLUMN_HEADER = "Pkw";
	
	
	//-------------------------  INPUT
	private final static String bastInputFile = "../../../svn/shared-svn/studies/de/open_berlin_scenario/be_5/counts/BASt_rawData/2016_B_S.txt";
	//should contain the following columns: BAST-ID;fromNodeDir1;toNodeDir1;fromNodeDir2;toNodeDir2
	private final static String countsLocationCSV = "../../../svn/shared-svn/studies/de/open_berlin_scenario/be_5/counts/BASt_mapMatching/mapMatchFile_BASt_berlin.csv";
	private final static String networkFile = 
			"../../../svn/shared-svn/studies/de/open_berlin_scenario/be_5/network/old/network_with-PT-GTFS_500.xml.gz";
	
	
	//-------------------------  OUTPUT       ../../../svn/shared-svn/studies/de/open_berlin_scenario/be_5
	private final static String OUTPUTDIR = "C:/Users/Work/VSP/BASt/2016_B_S_TUE_THU_" + VEH_CLASS_COLUMN_HEADER + "_Berlin.xml.gz";
	
	
	//-------------------------  VARIABLES
//									dir1   from    to        dir2   from    to 
	private static Map<String,Tuple<Tuple<Id<Node>,Id<Node>>,Tuple<Id<Node>,Id<Node>>>> bastID2NodeIDs = new HashMap<String, Tuple<Tuple<Id<Node>, Id<Node>>, Tuple<Id<Node>, Id<Node>>>>();
	private static Network network;
	static Logger log = Logger.getLogger(TSBASt2Count.class);
	
	public static void main(String[] args) throws IOException, ParseException, FactoryException {
		
		Map<String,BastHourlyCountData> allCountData = new HashMap<String,BastHourlyCountData>();
		
		boolean hasLocations = inputFilesExists(countsLocationCSV, networkFile);
		if(!USE_UNLOCATED_STATIONS && !hasLocations) {
			throw new RuntimeException("you don't want to use unlocated stations but location input could not be found");
		}
		if(hasLocations ) {
			readNetwork(networkFile);
			readCountLocations(countsLocationCSV);
		}
		readBAStInputFile(allCountData);
		convertDataToMATSimCounts(allCountData,hasLocations);
		
		if(hasLocations && bastID2NodeIDs.size() > 0) {
			log.warn("not all of the map matched bast counting stations were found in the database. here is a list:");
			String list = "\n";
			for(String bastID : bastID2NodeIDs.keySet()) {
				list += bastID + "\n";
			}
			log.warn(list);
		}
		
	}

	/**
	 * @param allCountData
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ParseException
	 */
	private static void readBAStInputFile(Map<String, BastHourlyCountData> allCountData)
			throws FileNotFoundException, IOException, ParseException {
		log.info("Loading bast counts...");
		File file = new File(bastInputFile);
		BufferedReader reader = new BufferedReader(new FileReader(file));
		
		String line = reader.readLine();
		String[] header = line.split(";");
		Map<String, Integer> colIndices = new HashMap<String, Integer>();
		for(int i = 0; i < header.length; i++) {
			header[i].trim();
			colIndices.put(header[i], i);
		}
		
		NumberFormat format = NumberFormat.getInstance(Locale.US);
		
		int indexCountNr = colIndices.get("Zst");
		int indexVolumeD1 = colIndices.get(VEH_CLASS_COLUMN_HEADER + "_R1");
		int indexValidityOfD1 = colIndices.get("K_" + VEH_CLASS_COLUMN_HEADER + "_R1");
		int indexVolumeD2 = colIndices.get(VEH_CLASS_COLUMN_HEADER + "_R2");
		int indexValidityOfD2 = colIndices.get("K_" + VEH_CLASS_COLUMN_HEADER + "_R2");
		int indexHour = colIndices.get("Stunde");
		int indexDayOfWeek = colIndices.get("Wotag");
		int indexPurpose = colIndices.get("Fahrtzw");
		
		int lineNr = 1;
		
		while((line = reader.readLine()) != null) {
			String tokens[] = line.split(";", -1);
			int weekDay = format.parse(tokens[indexDayOfWeek].trim()).intValue();
			
			boolean isConsideredWeekDay = (tokens[indexPurpose].equals("w") && (weekDay >= BEGINNING_WEEKDAY && weekDay <= ENDING_WEEKDAY)); 
			boolean isConsideredWeekEnd = (CALC_WEEKENDS && (tokens[indexPurpose].equals("s") || weekDay == 6));
			
			if(isConsideredWeekDay || isConsideredWeekEnd ){
				int stationNumber = format.parse(tokens[indexCountNr]).intValue();
				
				Day day = (isConsideredWeekDay) ? Day.WEEKDAY : Day.WEEKEND;
				String identifier = "" + stationNumber;
				if(CALC_WEEKENDS) identifier += "-" + day;
				BastHourlyCountData data = allCountData.get(identifier);
				if(data == null){
					data = new BastHourlyCountData(identifier, day);
				}
				
				int hour = format.parse(tokens[indexHour]).intValue();
				switch(tokens[indexValidityOfD1]){
				case "-":
				case "s":
				case "k":	
					data.computeAndSetVolume(true, hour, format.parse(tokens[indexVolumeD1].trim()).doubleValue());
				}
				switch(tokens[indexValidityOfD2]){
				case "-":
				case "s":
				case "k":	
					data.computeAndSetVolume(false, hour, format.parse(tokens[indexVolumeD2].trim()).doubleValue());
				}
				allCountData.put(identifier, data);
			}
			if(lineNr % 100000 == 0){
				log.info("read line " + lineNr);
			}
			lineNr ++;
		}
		reader.close();
	}

	/**
	 * @param allCounts
	 * @param hasLocations 
	 */
	private static void convertDataToMATSimCounts(Map<String, BastHourlyCountData> allCounts, boolean hasLocations) {
		log.info("start converting to MATSim counts..");
		
		Counts result = new Counts();
		result.setName("BASt Zählstellen - Jahresdurchschnitt");
		result.setYear(2017);
		
		for(BastHourlyCountData data : allCounts.values()){
			Id<Link> id;
			String str = data.getId() + "-D1";
			String bastID = data.getId();
			if(CALC_WEEKENDS) {
				bastID = data.getId().substring(0, data.getId().indexOf("-"));
			}
			Tuple<Tuple<Id<Node>, Id<Node>>, Tuple<Id<Node>, Id<Node>>> nodeTuple = bastID2NodeIDs.remove(bastID);
			if(nodeTuple == null && !USE_UNLOCATED_STATIONS) {
				continue;
			}
			
			if(hasLocations) {
				Node from = network.getNodes().get(nodeTuple.getFirst().getFirst());
				Node to = network.getNodes().get(nodeTuple.getFirst().getSecond());
				id = NetworkUtils.getConnectingLink(from, to).getId();
			} else {
				id = Id.createLinkId(data.getId() + "-D1");
			}
			convertData(result, data, id, str);
			
			str = data.getId() + "-D2";
			if(hasLocations) {
				Node from = network.getNodes().get(nodeTuple.getSecond().getFirst());
				Node to = network.getNodes().get(nodeTuple.getSecond().getSecond());
				id = NetworkUtils.getConnectingLink(from, to).getId();
			} else {
				id = Id.createLinkId(data.getId() + "-D2");
			}		
			convertData(result, data, id, str);
		}
		
		log.info("writing counts to " + OUTPUTDIR);
		CountsWriter writer = new CountsWriter(result);
//		File file = new File(OUTPUTDIR);
//		if(file.exists()) {
//			log.warn("overwriting File : " + file.getAbsolutePath());
//			file.delete();
//		} else {
//			log.info("trying to create path : " + file.getAbsolutePath());
//			file.mkdirs();
//			log.info("passed");
//			file.cre
//		}
		writer.write(OUTPUTDIR);
		
		log.info("conversion FINISHED...");
	}

	/**
	 * @param result
	 * @param data
	 * @param id 
	 * @param str
	 */
	private static void convertData(Counts result, BastHourlyCountData data, Id<Link> id, String str) {
		Count currentCount = result.createAndAddCount(id, str);
		if(currentCount != null) {
			for(int i = 1; i < 25; i++) {
				Double value = data.getR1Values().get(i);
				if(value == null){
					if(USE_UNVALID_HOURS) {
						log.error("station " + str + " has no valid entry for hour " + i +". Please check this. Instead, a negative volume for this hour is written into the counts file.");
						value = -1.0;
					} else {
						log.error("station " + str + " has no valid entry for hour " + i +". At the moment, we will not generate the matsim count!!!!");
						result.getCounts().remove(currentCount);
						return;
					}
				}
				currentCount.createVolume(i, value);
			}
		} else {
			log.warn("Cannot add two counts for one link (" + str+").");
		}
	}
	
	private static boolean inputFilesExists(String localisationFile, String networkFile) {
		File locFile = new File(localisationFile);
		File netFile = new File(networkFile);
		File bastFile = new File(bastInputFile);
		if(!bastFile.exists()){
			throw new RuntimeException("THE BAST DATA INPUT FILE COULD NOT BE FOUND AT:\n " + bastFile.getAbsolutePath());
		}
		if ( !locFile.exists()) {
			log.warn("THE COUNT LOCALISATION FILE COULD NOT BE FOUND AT:\n " + locFile.getAbsolutePath());
			log.warn("LINK ID'S WILL BE GENERIC");
			return false;
		} else if(!netFile.exists()){
			log.warn("THE NETWORK FILE COULD NOT BE FOUND AT:\n " + netFile.getAbsolutePath()) ;  
			log.warn("LINK ID'S WILL BE GENERIC");
			return false;
		}
		return true;
	}
	
	private static void readNetwork(String path) {
		network = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		Log.info("start reading network file");
		MatsimNetworkReader reader = new MatsimNetworkReader(network);
		reader.readFile(path);
		Log.info("finished reading network file");
	}
	
	private static void readCountLocations(String path) throws IOException {
	
		log.info("starting to read count localisation file");
		BufferedReader reader = new BufferedReader(new FileReader(path));
		
		String line = reader.readLine();
		String[] lineTokens = line.split(";");
//		Map<String, Integer> colIndices = new HashMap<String, Integer>();
		ArrayList<String> headers = new ArrayList<>();
		for(int i = 0; i < lineTokens.length; i++) {
			lineTokens[i].trim();
//			colIndices.put(lineTokens[i], i);
			headers.add(lineTokens[i]);
		}
		

//		int indexFromNodeD1 = colIndices.get("fromNodeDir1");
//		int indexToNodeD1 = colIndices.get("toNodeDir1");
//		int indexFromNodeD2 = colIndices.get("fromNodeDir2");
//		int indexToNodeD2 = colIndices.get("toNodeDir2");
//		int indexBAStID = colIndices.get("BASTNr");
//		int comment = Integer.MAX_VALUE;
//		if(colIndices.containsKey("comment")) {
//			comment = colIndices.get("comment");
//		}
		int indexFromNodeD1 = headers.indexOf("fromNodeDir1");
		int indexToNodeD1 = headers.indexOf("toNodeDir1");
		int indexFromNodeD2 = headers.indexOf("fromNodeDir2");
		int indexToNodeD2 = headers.indexOf("toNodeDir2");
		int indexBAStID = headers.indexOf("BASTNr");
		int comment = Integer.MAX_VALUE;
		if(headers.contains("comment")) {
			comment = headers.indexOf("comment");
		}
		
/*
 * @TODO:
 * account for comments
 */
		
		int lineNr = 1;
		while((line = reader.readLine()) != null) {
			lineTokens = line.split(";");
			Tuple<Id<Node>,Id<Node>> nodesDir1 = new Tuple<Id<Node>, Id<Node>>(Id.createNodeId(lineTokens[indexFromNodeD1]), Id.createNodeId(lineTokens[indexToNodeD1]));
			Tuple<Id<Node>,Id<Node>> nodesDir2 = new Tuple<Id<Node>, Id<Node>>(Id.createNodeId(lineTokens[indexFromNodeD2]), Id.createNodeId(lineTokens[indexToNodeD2]));
			bastID2NodeIDs.put(lineTokens[indexBAStID], new Tuple<Tuple<Id<Node>, Id<Node>>, Tuple<Id<Node>, Id<Node>>>(nodesDir1,nodesDir2));
		}
		
		reader.close();
		log.info("finished reading count localisation file");
	
	}
	
	
}








