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

/**
 *  Calculates and writes some analysis for the defined Runs.
 *
 *  @author kturner
 */
public class FreightAnalyseKT {

	private static final String RUN_DIR = "../../tubCloud/Shared/vsp_zerocuts/scenarios/Fracht_LEH_OpenBln_oneTW/output/I-Base2000it/";
//  private static final String RUN_DIR = "../../tubCloud/Shared/vsp_zerocuts/scenarios/Fracht_LEH_OpenBln_oneTW/output/I-Base2000it_NwCE/";
//  private static final String RUN_DIR = "../../tubCloud/Shared/vsp_zerocuts/scenarios/Fracht_LEH_OpenBln_oneTW/output/I-Base_NwCE_BVWP_2000it/";
//	private static final String RUN_DIR = "../../tubCloud/Shared/vsp_zerocuts/scenarios/Fracht_LEH_OpenBln_oneTW/output/I-Base_NwCE_BVWP_Pickup_2000it/";

	
	private static final String OUTPUT_DIR = RUN_DIR + "Analysis/" ;
		
	private static final Logger log = Logger.getLogger(FreightAnalyseKT.class);
	
	public static void main(String[] args) throws UncheckedIOException, IOException {
		OutputDirectoryLogging.initLoggingWithOutputDirectory(OUTPUT_DIR);
		
		FreightAnalyseKT analysis = new FreightAnalyseKT();
		analysis.run();
		log.info("### Finished ###");
		OutputDirectoryLogging.closeOutputDirLogging();
	}
	
		private void run() throws UncheckedIOException {

			File populationFile = new File(RUN_DIR + "output_plans.xml.gz");
			File networkFile = new File(RUN_DIR+ "output_network.xml.gz");
			File eventsFile = new File(RUN_DIR + "output_events.xml.gz");
			File carrierFile = new File(RUN_DIR+ "output_carriers.xml.gz");
			File vehicleTypefile = new File(RUN_DIR+ "output_vehicleTypes.xml.gz");
			
			Config config = ConfigUtils.createConfig();
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

			log.info("Reading the event file...");
			MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
			reader.readFile(eventsFile.getAbsolutePath());
			log.info("Reading the event file... Done.");
			
			TripWriter tripWriter = new TripWriter(tripHandler, OUTPUT_DIR);
			for (Carrier carrier : carriers.getCarriers().values()){
				tripWriter.writeDetailedResultsSingleCarrier(carrier.getId().toString());
				tripWriter.writeTourResultsSingleCarrier(carrier.getId().toString());
			}

			tripWriter.writeResultsPerVehicleTypes();
			tripWriter.writeTourResultsAllCarrier();

			new WriteCarrierScoreInfos(carriers, new File(OUTPUT_DIR + "#CarrierScoreInformation.txt"));
			log.info("### Analysis DONE");
	}
}
