/**
 * 
 */
package optimize.cten.convert.Matsim2cten;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.SignalSystemsConfigGroup.IntersectionLogic;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Time;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import optimize.cten.convert.Matsim2cten.demand.DgZones;
import optimize.cten.convert.Matsim2cten.demand.PopulationToOd;
import optimize.cten.convert.Matsim2cten.demand.ZoneBuilder;
import optimize.cten.convert.Matsim2cten.network.NetLanesSignalsShrinker;

/**
 * Class to convert a MATSim scenario into KS format.
 * 
 * @author tthunig
 */
public final class TtMatsim2KS2015 {

	private static final Logger log = Logger.getLogger(RunCottbus2Cten.class);

	private static final String shapeFileDirectoryName = "shapes/";
	
	private static final CoordinateReferenceSystem CRS = MGC
			.getCRS(TransformationFactory.WGS84_UTM33N);	
	
	/**
	 * Runs the conversion process and writes the BTU output file.
	 * 
	 * @param signalSystemsFilename
	 * @param signalGroupsFilename
	 * @param signalControlFilename
	 * @param networkFilename
	 * @param lanesFilename
	 * @param populationFilename
	 * @param startTime
	 * @param endTime
	 * @param signalsBoundingBoxOffset outside this envelope all crossings stay unexpanded
	 * @param cuttingBoundingBoxOffset
	 * @param freeSpeedFilter the minimal free speed value for the interior link filter in m/s
	 * @param useFreeSpeedTravelTime a flag for dijkstras cost function: if true, dijkstra will use the free speed travel time, if false, dijkstra will use the travel distance as cost function
	 * @param maximalLinkLength restricts the NetworkSimplifier. Double.MAX_VALUE is no restriction.
	 * @param matsimPopSampleSize 1.0 means a 100% sample
	 * @param ksModelCommoditySampleSize 1.0 means that 1 vehicle is equivalent to 1 unit of flow
	 * @param minCommodityFlow only commodities with at least this demand will be optimized in the BTU model
	 * @param simplifyNetwork use network simplifier if true
	 * @param cellsX number of cells in x direction
	 * @param cellsY number of cells in y direction
	 * @param scenarioDescription
	 * @param dateFormat
	 * @param outputDirectory
	 * @throws Exception
	 */
	public static void convertMatsim2KS(String signalSystemsFilename,
			String signalGroupsFilename, String signalControlFilename,
			String signalConflictsFilename, String networkFilename, String lanesFilename,
			String populationFilename, double startTime, double endTime,
			double signalsBoundingBoxOffset, double cuttingBoundingBoxOffset,
			double freeSpeedFilter, boolean useFreeSpeedTravelTime,
			double maximalLinkLength, double matsimPopSampleSize,
			double ksModelCommoditySampleSize, double minCommodityFlow,
			boolean simplifyNetwork,
			int cellsX, int cellsY, String scenarioDescription,
			String dateFormat, String outputDirectory) throws Exception{
		
		// init some variables
		String spCost = "tt";
		if (!useFreeSpeedTravelTime) spCost = "dist";
		outputDirectory += dateFormat + "_minflow_" + minCommodityFlow + "_time" 
				+ startTime + "-" + endTime + "_speedFilter" + freeSpeedFilter + "_SP_" + spCost 
				+ "_cBB" + cuttingBoundingBoxOffset + "_sBB" + signalsBoundingBoxOffset + "/";
		String ksModelOutputFilename = "ks2010_model_" + Double.toString(minCommodityFlow) + "_"
				+ Double.toString(startTime) + "_" + Double.toString(cuttingBoundingBoxOffset) + ".xml";
		
		// run
		OutputDirectoryLogging.initLoggingWithOutputDirectory(outputDirectory);
		String shapeFileDirectory = createShapeFileDirectory(outputDirectory);
		Scenario fullScenario = loadScenario(networkFilename,
				populationFilename, lanesFilename, signalSystemsFilename,
				signalGroupsFilename, signalControlFilename, signalConflictsFilename);

		// reduce the size of the scenario
		NetLanesSignalsShrinker scenarioShrinker = new NetLanesSignalsShrinker(
				fullScenario, CRS);
		scenarioShrinker.shrinkScenario(outputDirectory, shapeFileDirectory,
				cuttingBoundingBoxOffset, freeSpeedFilter,
				useFreeSpeedTravelTime, maximalLinkLength, simplifyNetwork);

		// create the geometry for zones. The geometry itself is not used, but
		// the object serves as container for the link -> link OD pairs
		ZoneBuilder zoneBuilder = new ZoneBuilder(CRS);
		DgZones zones = zoneBuilder.createAndWriteZones(
				scenarioShrinker.getShrinkedNetwork(),
				scenarioShrinker.getCuttingBoundingBox(), cellsX, cellsY,
				shapeFileDirectory);

		// match population to the small network and convert to od, results are
		// stored in the DgZones object
		PopulationToOd pop2od = new PopulationToOd();
		pop2od.setMatsimPopSampleSize(matsimPopSampleSize);
		pop2od.setOriginalToSimplifiedLinkMapping(scenarioShrinker
				.getOriginalToSimplifiedLinkIdMatching());
		pop2od.convertPopulation2OdPairs(zones, fullScenario.getNetwork(),
				fullScenario.getPopulation(), CRS,
				scenarioShrinker.getShrinkedNetwork(),
				scenarioShrinker.getCuttingBoundingBox(), startTime, endTime,
				shapeFileDirectory);

		// convert to KoehlerStrehler2010 file format
		M2KS2010Converter converter = new M2KS2010Converter(
				scenarioShrinker.getShrinkedNetwork(),
				scenarioShrinker.getShrinkedLanes(),
				scenarioShrinker.getShrinkedSignals(),
				signalsBoundingBoxOffset, CRS);
		String description = createDescription(cellsX, cellsY, startTime,
				endTime, cuttingBoundingBoxOffset, matsimPopSampleSize,
				ksModelCommoditySampleSize, minCommodityFlow);
		converter.setKsModelCommoditySampleSize(ksModelCommoditySampleSize);
		converter.setMinCommodityFlow(minCommodityFlow);
		converter.convertAndWrite(outputDirectory, shapeFileDirectory,
				ksModelOutputFilename, scenarioDescription, description, zones, startTime,
				endTime);		
		
		printStatistics(cellsX, cellsY, cuttingBoundingBoxOffset, startTime,
				endTime);
		log.info("output ist written to " + outputDirectory);
		OutputDirectoryLogging.closeOutputDirLogging();
	}

	private static Scenario loadScenario(String networkFilename, String populationFilename, String lanesFilename,
			String signalSystemsFilename, String signalGroupsFilename, String signalControlFilename,
			String signalConflictsFilename) {

		Config c2 = ConfigUtils.createConfig();
		c2.qsim().setUseLanes(true);

		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(c2,
				SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
		signalsConfigGroup.setUseSignalSystems(true);

		c2.network().setInputFile(networkFilename);
		c2.plans().setInputFile(populationFilename);
		c2.network().setLaneDefinitionsFile(lanesFilename);

		signalsConfigGroup.setSignalSystemFile(signalSystemsFilename);
		signalsConfigGroup.setSignalGroupsFile(signalGroupsFilename);
		signalsConfigGroup.setSignalControlFile(signalControlFilename);

		if (signalConflictsFilename != null && !signalConflictsFilename.equals("")) {
			signalsConfigGroup.setIntersectionLogic(IntersectionLogic.CONFLICTING_DIRECTIONS_NO_TURN_RESTRICTIONS);
			signalsConfigGroup.setConflictingDirectionsFile(signalConflictsFilename);
		}

		Scenario scenario = ScenarioUtils.loadScenario(c2);

		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(c2).loadSignalsData());

		return scenario;
	}

	private static String createDescription(int cellsX, int cellsY,
			double startTime, double endTime, double boundingBoxOffset,
			double matsimPopSampleSize, double ksModelCommoditySampleSize,
			double minCommodityFlow) {
		String description = "offset: " + boundingBoxOffset + " cellsX: "
				+ cellsX + " cellsY: " + cellsY + " startTimeSec: " + startTime
				+ " endTimeSec: " + endTime;
		description += " matsimPopsampleSize: " + matsimPopSampleSize
				+ " ksModelCommoditySampleSize: " + ksModelCommoditySampleSize;
		description += " minimum flow of commodities to be included in conversion: "
				+ minCommodityFlow;
		return description;
	}

	private static void printStatistics(int cellsX, int cellsY,
			double boundingBoxOffset, double startTime, double endTime) {
		log.info("Number of Cells:");
		log.info("  X " + cellsX + " Y " + cellsY);
		log.info("Bounding Box: ");
		log.info("  Offset: " + boundingBoxOffset);
		log.info("Time: ");
		log.info("  startTime: " + startTime + " " + Time.writeTime(startTime));
		log.info("  endTime: " + endTime + " " + Time.writeTime(endTime));
	}

	private static String createShapeFileDirectory(String outputDirectory) {
		String shapeDir = outputDirectory + shapeFileDirectoryName;
		File outdir = new File(shapeDir);
		outdir.mkdir();
		return shapeDir;
	}
}
