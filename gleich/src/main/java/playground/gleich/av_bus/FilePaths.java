package playground.gleich.av_bus;

public class FilePaths {
	
//	public final static String PATH_BASE_DIRECTORY = "../../../../"; // 3 Levels to Workspace -> runs-svn (partly checked-out -> looks up local copy)
//	public final static String PATH_BASE_DIRECTORY = "../../../../../shared-svn/studies/gleich/av-bus_berlinNW/"; // 3 Levels to Workspace -> runs-svn (partly checked-out -> looks up local copy)
	public final static String PATH_BASE_DIRECTORY = "../../../../Documents/EclipseWorkspace/av-bus_berlinNW/"; // 3 Levels to Workspace -> runs-svn (partly checked-out -> looks up local copy)
//	public final static String PATH_BASE_DIRECTORY = "C:/Users/gleich/av_bus berlin/";
//	public final static String PATH_BASE_DIRECTORY = "dat/Uni/av_bus berlin/";
	
	/** 10pct Scenario Unmodified */
	public final static String PATH_NETWORK_BERLIN__10PCT = "data/input/Berlin10pct/network.final10pct.xml.gz";
	public final static String PATH_NETWORK_CHANGE_EVENTS_BERLIN__10PCT = "data/input/Berlin10pct/changeevents10.xml.gz";
	public final static String PATH_POPULATION_BERLIN__10PCT_UNFILTERED = "data/input/Berlin10pct/population.10pct.unfiltered.base.xml.gz";
	public final static String PATH_TRANSIT_SCHEDULE_BERLIN__10PCT = "data/input/Berlin10pct/transitSchedule.xml.gz";
	public final static String PATH_TRANSIT_VEHICLES_BERLIN__10PCT = "data/input/Berlin10pct/transitVehicles.final.xml";
	/** 10pct Scenario Modified */
	public final static String PATH_NETWORK_BERLIN__10PCT_DRT_ACCESS_LOOPS = "data/input/Berlin10pct/mod/network.10pct.drtAccessLoops.xml";
	public final static String PATH_POPULATION_BERLIN__10PCT_FILTERED = "data/input/Berlin10pct/mod/population.10pct.filtered.xml.gz";
	public final static String PATH_POPULATION_BERLIN__10PCT_FILTERED_1000 = "data/input/Berlin10pct/mod/population.10pct.filtered_about1000.xml";
	public final static String PATH_LINKS_ENCLOSED_IN_AREA_BERLIN__10PCT = "data/input/Berlin10pct/mod/linksInArea.csv";
	public final static String PATH_SHP_LINKS_ENCLOSED_IN_AREA_BERLIN__10PCT = "data/input/Berlin10pct/mod/linksInArea.shp";
	// base transit schedules are identical (Berlin10pct/transitSchedule.xml.gz == Berlin100pct/transitSchedule.xml.gz)
	public final static String PATH_TRANSIT_SCHEDULE_BERLIN__10PCT_WITHOUT_BUSES_IN_STUDY_AREA = "data/input/Berlin100pct/mod/transitSchedule.100pct.withoutBusesInArea.xml";
	/* 
	 * Berlin10pct/transitVehicles.final.xml has no maximumVelocity set. Most transit lines can more or less stick to their schedule
	 * in the simulation, so apparently there is no need to set a maximum velocity to spped them up (in respect to whatever 
	 * default maximum velocity is used in absence of any values set in the vehicles file). 
	 * "RE" and "IC" long distance pt is seriously delayed but that is probably caused by the speeds set for the links in the network file
	 */
//	public final static String PATH_TRANSIT_VEHICLES_BERLIN__10PCT_45MPS = PATH_BASE_DIRECTORY + "data/input/Berlin10pct/mod/transitVehicles.10pct.45mps.xml";
	/** 10pct Scenario Configs and corresponding input files */
	public final static String PATH_CONFIG_BERLIN__10PCT_NULLFALL = "data/input/Berlin10pct/Nullfall/config.10pct.Nullfall.xml";
	public final static String PATH_COORDS2TIME_SURCHARGE_FILE = "data/input/Berlin10pct/mod/surchargeTransitStops.txt";
	
	public final static String PATH_CONFIG_BERLIN__10PCT_DRT = "data/input/Berlin10pct/DRT/config.10pct.DRT.xml";
	public final static String PATH_DRT_VEHICLES_15_CAP1_BERLIN__10PCT = "data/input/Berlin10pct/DRT/DRTVehicles.10pct.DRT_15_Cap1.xml";
	public final static String PATH_DRT_VEHICLES_20_CAP1_BERLIN__10PCT = "data/input/Berlin10pct/DRT/DRTVehicles.10pct.DRT_20_Cap1.xml";
	public final static String PATH_DRT_VEHICLES_25_CAP1_BERLIN__10PCT = "data/input/Berlin10pct/DRT/DRTVehicles.10pct.DRT_25_Cap1.xml";
	
	public final static String PATH_DRT_VEHICLES_10_CAP4_BERLIN__10PCT = "data/input/Berlin10pct/DRT/DRTVehicles.10pct.DRT_10_Cap4.xml";
	public final static String PATH_DRT_VEHICLES_15_CAP4_BERLIN__10PCT = "data/input/Berlin10pct/DRT/DRTVehicles.10pct.DRT_15_Cap4.xml";
	public final static String PATH_DRT_VEHICLES_20_CAP4_BERLIN__10PCT = "data/input/Berlin10pct/DRT/DRTVehicles.10pct.DRT_20_Cap4.xml";
	public final static String PATH_DRT_VEHICLES_50_CAP4_BERLIN__10PCT = "data/input/Berlin10pct/DRT/DRTVehicles.10pct.DRT_50_Cap4.xml";
	
	/** 10pct Scenario Output */
	public final static String PATH_OUTPUT_BERLIN__10PCT_NULLFALL = "data/output/Berlin10pct/Nullfall";
	
	public final static String PATH_OUTPUT_BERLIN__10PCT_DRT_15_CAP1 = "data/output/Berlin10pct/DRT_15_Cap1";
	public final static String PATH_OUTPUT_BERLIN__10PCT_DRT_20_CAP1 = "data/output/Berlin10pct/DRT_20_Cap1";
	public final static String PATH_OUTPUT_BERLIN__10PCT_DRT_25_CAP1 = "data/output/Berlin10pct/DRT_25_Cap1";

	public final static String PATH_OUTPUT_BERLIN__10PCT_DRT_10_CAP4 = "data/output/Berlin10pct/DRT_10_Cap4";
	public final static String PATH_OUTPUT_BERLIN__10PCT_DRT_15_CAP4 = "data/output/Berlin10pct/DRT_15_Cap4";
	public final static String PATH_OUTPUT_BERLIN__10PCT_DRT_20_CAP4 = "data/output/Berlin10pct/DRT_20_Cap4";
	public final static String PATH_OUTPUT_BERLIN__10PCT_DRT_50_CAP4 = "data/output/Berlin10pct/DRT_50_Cap4";
	
	/** 100pct Scenario */
	public final static String PATH_NETWORK_BERLIN_100PCT = "data/input/Berlin100pct/network/network.10pct.base.xml.gz";
	public final static String PATH_NETWORK_BERLIN_100PCT_ACCESS_LOOPS = "data/input/Berlin100pct/network/network.100pct.drtAccessLoops.xml";
	public final static String PATH_NETWORK_CHANGE_EVENTS_BERLIN_100PCT = "data/input/Berlin100pct/network/networkChangeEvents.100pct.base.xml.gz";
	public final static String PATH_NETWORK_CHANGE_EVENTS_BERLIN_100PCT_WITHOUT_STUDY_AREA = "data/input/Berlin100pct/network/networkChangeEvents.100pct.withoutStudyArea.xml.gz";
	public final static String PATH_POPULATION_BERLIN_100PCT_UNFILTERED = "data/input/Berlin100pct/population/population.100pct.unfiltered_base.xml.gz";
	public final static String PATH_TRANSIT_SCHEDULE_BERLIN_100PCT = "data/input/Berlin100pct/pt/transitSchedule.100pct.base.xml.gz";
	public final static String PATH_TRANSIT_VEHICLES_BERLIN_100PCT = "data/input/Berlin100pct/pt/transitVehicles.100pct.base.xml.gz";
	/** 100pct Scenario */
	public final static String PATH_POPULATION_BERLIN_100PCT_FILTERED = "data/input/Berlin100pct/population/population.100pct.filtered.xml.gz";
	public final static String PATH_LINKS_ENCLOSED_IN_AREA_BERLIN_100PCT = "data/input/Berlin100pct/network/linksInArea.csv";
	public final static String PATH_SHP_LINKS_ENCLOSED_IN_AREA_BERLIN_100PCT = "data/input/Berlin100pct/network/linksInArea.shp";
	public final static String PATH_TRANSIT_SCHEDULE_BERLIN_100PCT_WITHOUT_BUSES_IN_STUDY_AREA = "data/input/Berlin100pct/pt/transitSchedule.100pct.withoutBusesInArea.xml";
	public final static String PATH_TRANSIT_VEHICLES_BERLIN_100PCT_45MPS = "data/input/Berlin100pct/pt/transitVehicles.100pct.45mps.xml";
	/** 100pct Scenario Output */
	public final static String PATH_OUTPUT_BERLIN_100PCT_MODIFIED_TRANSIT_SCHEDULE_TEST = "data/output/test/modified_transitSchedule";
	
	/** Study area */
	public final static String PATH_STUDY_AREA_SHP = "data/input/Untersuchungsraum shp/study_area.shp";
	public final static String STUDY_AREA_SHP_KEY = "id"; // name of the key column
	public final static String STUDY_AREA_SHP_ELEMENT = "1"; // key for the element containing the study area
	/** AV Operation Area */
	public final static String PATH_AV_OPERATION_AREA_SHP = "data/input/Untersuchungsraum shp/av_operation_area.shp";
	public final static String PATH_AV_OPERATION_AREA_SIMPLIFIED_SHP = "data/input/Untersuchungsraum shp/av_operation_area_simplified.shp";
	public final static String AV_OPERATION_AREA_SHP_KEY = "id"; // name of the key column
	public final static String AV_OPERATION_AREA_SHP_ELEMENT = "1"; // key for the element containing the study area

}
