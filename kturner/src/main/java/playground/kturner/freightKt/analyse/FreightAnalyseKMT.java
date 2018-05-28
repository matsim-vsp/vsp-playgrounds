package playground.kturner.freightKt.analyse;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingSchemeImpl;

public class FreightAnalyseKMT {

	/**
	 *  Calculates and writes some analysis for the defined Runs.
	 *  
	 *  @author kturner
	 */

	private static final String RUN_DIR = "../../OutputKMT/projects/freight/studies/reAnalysing_MA/MATSim/Berlin/Base_Ia/Run_1/" ;
//	private static final String RUN_DIR = "../../OutputKMT/projects/freight/studies/reAnalysing_MA/MATSim/Berlin/CordonTollOnHeavy/Run_1/" ; //City-Maut
//	private static final String RUN_DIR = "../../OutputKMT/projects/freight/studies/reAnalysing_MA/MATSim/Berlin/ElectroWithoutUCC/Run_1/" ; //CO2-free City
//	private static final String RUN_DIR = "../../OutputKMT/projects/freight/studies/reAnalysing_MA/MATSim/Berlin/ElectroWithUCC/Run_1/" 	//CO2-freie city mit UCC

//	private static final String RUN_DIR = "../../OutputKMT/projects/freight/studies/reAnalysing_MA/MATSim/Grid/Base/Run_1/" ;
	
	private static final String OUTPUT_DIR = RUN_DIR + "Analysis/" ;
		
	private static final Logger log = Logger.getLogger(FreightAnalyseKMT.class);
	
	public static void main(String[] args) throws UncheckedIOException, IOException {
		OutputDirectoryLogging.initLoggingWithOutputDirectory(OUTPUT_DIR);
		
		FreightAnalyseKMT analysis = new FreightAnalyseKMT();
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
			File lezZonefile = new File("../../shared-svn/projects/freight/studies/MA_Turner-Kai/input/Berlin_Szenario/lez_area.xml"); 
			
			Config config = ConfigUtils.loadConfig(configFile.getAbsolutePath());		
			config.plans().setInputFile(populationFile.getAbsolutePath());
			config.network().setInputFile(networkFile.getAbsolutePath());
			
			MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
			EventsManager eventsManager = EventsUtils.createEventsManager();
			
			CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
			new CarrierVehicleTypeReader(vehicleTypes).readFile(vehicleTypefile.getAbsolutePath()) ;
			
			Carriers carriers = new Carriers() ;
			new CarrierPlanXmlReaderV2(carriers).readFile(carrierFile.getAbsolutePath()) ;
			
			//TODO: Add switch with/without LEZ 
			//reading in lowEmissionZone
			final RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
			RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(scheme);
			try {
				rpReader.readFile(lezZonefile.getAbsolutePath());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			Set<Id<Link>> lezLinkIds = scheme.getTolledLinkIds();  //Link-Ids der Umweltzone (LEZ)

			FreightAnalyseKmtEventHandler tripHandler = new FreightAnalyseKmtEventHandler(scenario, vehicleTypes, lezLinkIds);
			eventsManager.addHandler(tripHandler);
					
			int iteration = config.controler().getLastIteration();
			String eventsFile = RUN_DIR + "ITERS/it." + iteration + "/" + iteration + ".events.xml.gz";
			
			log.info("Reading the event file...");
			MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
			reader.readFile(eventsFile);
			log.info("Reading the event file... Done.");
			
			FreightAnalyseKmtWriter tripWriter = new FreightAnalyseKmtWriter(tripHandler, OUTPUT_DIR);
			//TODO: Writer hat Werte fuer innerhalb der LEZ bisher noch nicht fur die ganzen Carrier.
			for (Carrier carrier : carriers.getCarriers().values()){
				tripWriter.writeDetailedResultsSingleCarrier(carrier.getId().toString());
				tripWriter.writeVehicleResultsSingleCarrier(carrier.getId().toString());
			}
			
			tripWriter.writeResultsPerVehicleTypes();
			
			
			log.info("### Analysis DONE");
			
	}

}
