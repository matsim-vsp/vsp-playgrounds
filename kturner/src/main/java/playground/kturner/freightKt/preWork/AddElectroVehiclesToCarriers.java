package playground.kturner.freightKt.preWork;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.vehicles.Vehicle;

/**
 * @author: Kturner
 */
class AddElectroVehiclesToCarriers {

	/**
	 * Hilfsklasse um Zeitfenster an den Depots eindeutig zu identifzieren
	 * @author kturner
	 *
	 */
	 static class DepotTimeWindow {
		private Id<Link> depotLocation;
		private TimeWindow tw;
		
		public static DepotTimeWindow newInstance(Id<Link> depotLocation, TimeWindow tw) {
			return new DepotTimeWindow(depotLocation, tw);
		}

		private DepotTimeWindow(Id<Link> depotLocation, TimeWindow tw) {
			this.depotLocation = depotLocation;
			this.tw = tw;
		}
		
		Id<Link> getDepot() {
			return depotLocation;
		}
		
		void setDepot(Id<Link> depotLocation) {
			this.depotLocation = depotLocation;
		}
		
		TimeWindow getTw() {
			return tw;
		}
		
		void setTw(TimeWindow tw) {
			this.tw = tw;
		}

		@Override
		public String toString() {
			return "[Depot=" + depotLocation.toString() + ", start=" + tw.getStart() +", end= " + tw.getEnd()+ "]";
		}
		
	}

	
	private static final Logger log = Logger.getLogger(AddElectroVehiclesToCarriers.class);		//TODO: Logging-Level ansehen und ggf anpassen.
	
	//Beginn Namesdefinition KT Für Berlin-Szenario 
	private static final String INPUT_DIR = "../../shared-svn/projects/freight/studies/MA_Turner-Kai/input/Berlin_Szenario/" ;
	private static final String OUTPUT_DIR = "../../shared-svn/projects/freight/studies/MA_Turner-Kai/input/Berlin_Szenario/new/" ;
//	private static final String TEMP_DIR = "../../shared-svn/projects/freight/studies/MA_Turner-Kai/input/Berlin_Szenario/Temp/";

	//Dateinamen ohne XML-Endung
	private static final String VEHTYPES_NAME = "vehicleTypes" ;
	private static final String CARRIERS_NAME = "carrierLEH_v2_withFleet" ;
	private static final ArrayList<String> CARRIERS_2EXTRACT = null ;			//Retailer Names, die herausselektiert werden soll; null wenn alle verbleiben soll (keine Selektion)
				//new ArrayList<String>(Arrays.asList("aldi", "rewe"));
	//Ende  Namesdefinition Berlin


	private static final String VEHTYPEFILE = INPUT_DIR + VEHTYPES_NAME + ".xml";
	private static final String CARRIERFILE = INPUT_DIR + CARRIERS_NAME + ".xml" ;
	private static final String CARRIEROUTFILE = OUTPUT_DIR + CARRIERS_NAME + "_withElectro.xml" ;

	
	static Carriers carriers = new Carriers() ;

	public static void main(String[] args) {
		createDir(new File(OUTPUT_DIR));

		new CarrierPlanXmlReaderV2(carriers).readFile(CARRIERFILE) ;
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		new CarrierVehicleTypeReader(vehicleTypes).readFile(VEHTYPEFILE) ;

		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicleTypes) ;

		Carriers extractedCarriers = extractCarriers(carriers, CARRIERS_2EXTRACT); //Step 2: Extrahieren einzelner Carrier (alle, die mit dem RetailerNamen beginnen)		

		addElectroVehiclesToCarriers(extractedCarriers, vehicleTypes);			//Step 3: Schauen welche Kombinationen aus Depot und Zeitfenster für jeden Carrier vorhanden sind und entsprechen Electrofzg in Flotte hinzufügen.
		
		//Ohne Aufteileung anhand UCCs
		extractedCarriers = renameVehId(extractedCarriers); 							//Step4: VehId je Carrier einzigartig machen, da sonst weitere Vorkommen ingnoriert werden (und somit nicht alle Depots genutzt werden).
		new CarrierPlanXmlWriterV2(extractedCarriers).write(CARRIEROUTFILE);
		System.out.println("### ENDE ###");
	}
	

	/** (Step2)
	 * Extrahieren einzelner Retailer (alle, die mit dem RetailerNamen beginnen)
	 * @param carriers
	 * @param retailerNamesSelection:  Array of all retailer/carrier to extract. (begin of CarrierId) Null, if all carriers shoud remain
	 * @return carriers with Id starting with retailerName or 
	 * 			if selectedRetailerNames == null the unmodified carriers.
	 */
	private static Carriers extractCarriers(Carriers carriers, ArrayList<String> retailerNamesSelection) {
		if (retailerNamesSelection == null) {
			return carriers;
		}
		Carriers tempCarriers = new Carriers();
		for (String retailerName : retailerNamesSelection) {
			for (Carrier carrier : carriers.getCarriers().values()){
				if (carrier.getId().toString().startsWith(retailerName)) {
					tempCarriers.addCarrier(carrier);
				}
			}
		}	
		return tempCarriers;
	}
	
	
	
	/**
	 * Step 3:
	 * Für jede Depot- / Zeitfensterkombination die Fahrzeugflotte um die Elektrofahrzeuge erweitern.
	 * @param carriers
	 * @param vehicleTypes
	 */
	private static Carriers addElectroVehiclesToCarriers(Carriers carriers, CarrierVehicleTypes vehicleTypes) {
		Carriers carriersWithElectro = new Carriers();
		for (Carrier carrier : carriers.getCarriers().values()) {
			List<DepotTimeWindow> depotTimeWindows = new ArrayList<DepotTimeWindow>();
			for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles()) { 	//Prüfe für jedes Fzg des Carriers ob das Zeitfenster ebreits bekannt: Ja: mache nichts, Nein-> Erweitere Fzg flotte.
				Id<Link> depotLocation = carrierVehicle.getLocation();
				TimeWindow tw_vehicle = TimeWindow.newInstance(carrierVehicle.getEarliestStartTime(), carrierVehicle.getLatestEndTime());
				boolean alreadyDone = false;
				for (DepotTimeWindow dtw : depotTimeWindows) {
					if (dtw.getDepot().equals(depotLocation) && dtw.getTw().getStart() == tw_vehicle.getStart() && dtw.getTw().getEnd() == tw_vehicle.getEnd()) {
						alreadyDone = true;
						log.debug("Carrier: " + carrier.getId().toString() + " --> Already exisiting DepotTimeWindow " + dtw.toString());
					} 
				}
				if (!alreadyDone) { //Combination of DepotLocation and TimeWindow not handled yet.
					DepotTimeWindow dtwToAdd = DepotTimeWindow.newInstance(depotLocation, tw_vehicle);
					depotTimeWindows.add(dtwToAdd);
					log.info("Carrier: " + carrier.getId().toString() + " --> Added DepotTimeWindow: " + dtwToAdd.toString());
				}
			}
			carriersWithElectro.addCarrier(carrier);
			for (DepotTimeWindow dtw : depotTimeWindows) {
				addElectroVehicles(carriersWithElectro.getCarriers().get(carrier.getId()), vehicleTypes, dtw.getDepot(), dtw.getTw());
			}
		}
		return carriersWithElectro;
	}

	//TODO: Absichern, dass zu erstellende VehicleType auch in VehicleTypes vorhanden sind! 
	/**
	 * Elektro-Fahrzeug-Typen den Carriern zuordnen
	 * Dabei gilt, dass frozen nur über den light8telectro_frozen verfügt  und alle anderen
	 * light8telectro und medium18telectro verfügen. Es werden Fahrzeuge für jedes Depot angelegt.
	 */
	private static void addElectroVehicles(Carrier carrier, CarrierVehicleTypes vehicleTypes, Id<Link> depotLocation,
			TimeWindow tw) {
		if (carrier.getId().toString().endsWith("TIEFKUEHL")){
				carrier.getCarrierCapabilities().getCarrierVehicles()
				.add(CarrierVehicle.Builder.newInstance(Id.create("light8telectro_frozen", Vehicle.class), depotLocation)
						.setType(vehicleTypes.getVehicleTypes().get(Id.create("light8telectro_frozen", VehicleType.class)))
						.setEarliestStart(tw.getStart()).setLatestEnd(tw.getEnd())
						.build());
			
		} else {
			
				carrier.getCarrierCapabilities().getCarrierVehicles()
				.add(CarrierVehicle.Builder.newInstance(Id.create("light8telectro", Vehicle.class), depotLocation)
						.setType(vehicleTypes.getVehicleTypes().get(Id.create("light8telectro", VehicleType.class)))
						.setEarliestStart(tw.getStart()).setLatestEnd(tw.getEnd())
						.build());

				carrier.getCarrierCapabilities().getCarrierVehicles()
				.add(CarrierVehicle.Builder.newInstance(Id.create("medium18telectro", Vehicle.class), depotLocation)
						.setType(vehicleTypes.getVehicleTypes().get(Id.create("medium18telectro", VehicleType.class)))
						.setEarliestStart(tw.getStart()).setLatestEnd(tw.getEnd())
						.build());
			}		
	}


	 /**  
	 * VehicleId je Carrier um Location erweitern, da sonst weitere Vorkommen auf Grund gleicher VehicleId ingnoriert werden 
	 * und somit nicht alle Depots genutzt werden.
	 */
	private static Carriers renameVehId(Carriers carriers) {
		//Alphabetsliste erstellen
		List<Character> alph = new ArrayList<Character>() ;
		for(char c='a'; c<='z'; c++) {
			alph.add(c);
		}

		for (Carrier carrier : carriers.getCarriers().values()){
			//zählt mit, wie oft Id für diesen Carrier vergeben wurde
			Map<String, Integer> nuOfVehPerId = new TreeMap<String, Integer>();

			//da Änderung der vorhandenen Fahrzeuge sonst nicht ging, Umweg über 
			//temporären neuen Carrier & setzen der Eigenschaften.
			CarrierCapabilities tempCc = CarrierCapabilities.newInstance();
			tempCc.setFleetSize(carrier.getCarrierCapabilities().getFleetSize());
			for (CarrierVehicle cv : carrier.getCarrierCapabilities().getCarrierVehicles()){
				String vehIdwLink = cv.getVehicleId().toString() + "_" + cv.getLocation().toString();
				String newVehId;
				if (!nuOfVehPerId.containsKey(vehIdwLink)){
					nuOfVehPerId.put(vehIdwLink, 1);
					newVehId = vehIdwLink;
				} else {
					nuOfVehPerId.put(vehIdwLink, nuOfVehPerId.get(vehIdwLink) + 1);
					//TODO: Abischerung gegen Leerlaufen des Alphabet-Arrays  überprüfen/ausprobieren (mehr als 26 mal verwendet erstellen.)
					Assert.assertTrue("No more chars to add to vehicleId",  nuOfVehPerId.get(vehIdwLink) <= alph.size());
					newVehId = vehIdwLink + alph.get(nuOfVehPerId.get(vehIdwLink)-1);
				}	

				//Vehicle neu erstellen, da setVehicleId nicht verfügbar.
				//Dabei eindeutigen Buchstaben für jede VehId-DepotLink-Kombination einfügen

				tempCc.getCarrierVehicles().add(CarrierVehicle.Builder
						.newInstance(Id.create(newVehId, Vehicle.class), cv.getLocation())
						.setType(cv.getVehicleType())
						.setEarliestStart(cv.getEarliestStartTime()).setLatestEnd(cv.getLatestEndTime())
						.build());
			}
			carrier.setCarrierCapabilities(tempCc); //Zurückschreiben des neuen Carriers

		}		
		return carriers;
	}

	
	private static void createDir(File file) {
		System.out.println("Verzeichnis " + file + " erstellt: "+ file.mkdirs());	
	}
}
