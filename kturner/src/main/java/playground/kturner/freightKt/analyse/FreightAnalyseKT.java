package playground.kturner.freightKt.analyse;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.UncheckedIOException;

public class FreightAnalyseKT {

	/**
	 *  Calculates and writes some analysis for the defined Runs.
	 *  
	 *  @author kturner
	 */
	
//	private static final String RUN_DIR = "../../OutputKMT/projects/freight/studies/reAnalysing_MA/MATSim/Berlin-MultipleToursSingleTimeWindow/I-Base/Run_1/" ;
//	private static final String RUN_DIR = "../../OutputKMT/projects/freight/studies/reAnalysing_MA/MATSim/Berlin-MultipleTours/Ia-BaseE/Run_1/" ; 		// Base mit Electro ab Depot
//	private static final String RUN_DIR = "../../OutputKMT/projects/freight/studies/reAnalysing_MA/MATSim/Berlin-MultipleTours/II-CityMaut/Run_1/" ;		//City-Maut
//	private static final String RUN_DIR = "../../OutputKMT/projects/freight/studies/reAnalysing_MA/MATSim/Berlin-MultipleTours/IIa-CityMautE/Run_1/" ;; 	//City-Maut mit electro ab Depot
//	private static final String RUN_DIR = "../../OutputKMT/projects/freight/studies/reAnalysing_MA/MATSim/Berlin-MultipleTours/IIIa-LEZE/Run_1/" ; //CO2-free City mit Elektro ab Depot
//	private static final String RUN_DIR = "../../OutputKMT/projects/freight/studies/reAnalysing_MA/MATSim/Berlin/IV-UCC/Run_1/" ;	//CO2-freie city mit UCC
	private static final String RUN_DIR = "../../OutputKMT/projects/freight/FoodOpenBerlin/I-Base/"; 	//CO2-freie city mit UCC mit Elektro ab Depot

//	private static final String RUN_DIR = "../../OutputKMT/projects/freight/studies/reAnalysing_MA/MATSim/Berlin/I-Base/Run_1/" ;
	
	private static final String OUTPUT_DIR = RUN_DIR + "Analysis/" ;
		
	private static final Logger log = Logger.getLogger(FreightAnalyseKT.class);
	
	public static void main(String[] args) throws UncheckedIOException, IOException {
		OutputDirectoryLogging.initLoggingWithOutputDirectory(OUTPUT_DIR);
		
		FreightAnalyseKT analysis = new FreightAnalyseKT();
		analysis.run();
		log.info("### Finished ###");
		OutputDirectoryLogging.closeOutputDirLogging();
	}
	
		private void run() throws UncheckedIOException, IOException {
			
			//TODO: Why is the configfile not used as .gz?
			File configFile = new File(RUN_DIR + "output_config.xml");
//			File configFile = new File(RUN_DIR + "output_config.xml.gz");
			File populationFile = new File(RUN_DIR + "output_plans.xml.gz");
			File networkFile = new File(RUN_DIR+ "output_network.xml.gz");
			File carrierFile = new File(RUN_DIR+ "output_carriers.xml.gz");
			File vehicleTypefile = new File(RUN_DIR+ "output_vehicleTypes.xml.gz");
			
			Config config = ConfigUtils.loadConfig(configFile.getAbsolutePath());
			config.plans().setInputFile(populationFile.getAbsolutePath());
			config.network().setInputFile(networkFile.getAbsolutePath());
			
			MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
			EventsManager eventsManager = EventsUtils.createEventsManager();
			
			CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
			new CarrierVehicleTypeReader(vehicleTypes).readFile(vehicleTypefile.getAbsolutePath()) ;
			
			Carriers carriers = new Carriers() ;
			new CarrierPlanXmlReader(carriers).readFile(carrierFile.getAbsolutePath() ) ;

			TripEventHandler tripHandler = new TripEventHandler(scenario, vehicleTypes);
			eventsManager.addHandler(tripHandler);
					
			int iteration = config.controler().getLastIteration();
			String eventsFile = RUN_DIR + "ITERS/it." + iteration + "/" + iteration + ".events.xml.gz";
			
			log.info("Reading the event file...");
			MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
			reader.readFile(eventsFile);
			log.info("Reading the event file... Done.");
			
			TripWriter tripWriter = new TripWriter(tripHandler, OUTPUT_DIR);
			for (Carrier carrier : carriers.getCarriers().values()){
				tripWriter.writeDetailedResultsSingleCarrier(carrier.getId().toString());
				tripWriter.writeTourResultsSingleCarrier(carrier.getId().toString());
			}

			tripWriter.writeResultsPerVehicleTypes();
			tripWriter.writeTourResultsAllCarrier();
			
			
			log.info("### Analysis DONE");
			
	}

}
