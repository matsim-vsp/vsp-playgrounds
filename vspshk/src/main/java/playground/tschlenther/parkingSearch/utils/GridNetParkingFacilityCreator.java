package playground.tschlenther.parkingSearch.utils;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.tabularFileParser.*;
import org.matsim.facilities.*;

public class GridNetParkingFacilityCreator {

	private final static String pathToZoneOne = "C:/Users/Work/VSP/WiMi/TeachParking/input/parkingSlots_oben_1perLink.csv";
	private final static String pathToZoneTwo = "";
	private static final String output = "C:/Users/Work/VSP/WiMi/TeachParking/input/parkingSlots_oben_1perLink.xml";
	private static final String pathToNetFile = "C:/Users/Work/VSP/WiMi/TeachParking/input/grid/network.xml";
	
	public static void main(String[] args){
		Map<Id<Link>,Double> zoneOneFacilities = mapLinkIDsOfZoneToParkingCapacity(pathToZoneOne);
//		List<Id<Link>> zoneTwoLinks = mapLinkIDsOfZoneToParkingCapacity(pathToZoneTwo);
		
		Network network = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		MatsimNetworkReader netReader = new MatsimNetworkReader(network);
		netReader.readFile(pathToNetFile);
		
		ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();
		ActivityFacilitiesFactory factory = facilities.getFactory();

		for(Id<Link> link : zoneOneFacilities.keySet()){
			ActivityOption option = factory.createActivityOption(ParkingUtils.PARKACTIVITYTYPE);
			Coord coord = network.getLinks().get(link).getCoord();
			ActivityFacility facility = factory.createActivityFacility(Id.create("parking_" + link, ActivityFacility.class), link);
			option.setCapacity(zoneOneFacilities.get(link));
			facility.addActivityOption(option);
			facility.setCoord(coord);
			facilities.addActivityFacility(facility);
		}
		
//		for(Id<Link> link : zoneTwoLinks){
//			Coord coord = network.getLinks().get(link).getCoord();
//			ActivityFacility facility = factory.createActivityFacility(Id.create("parking_" + link, ActivityFacility.class), link);
//			facility.addActivityOption(option);
//			facility.setCoord(coord);
//			facilities.addActivityFacility(facility);
//		}
		
		new FacilitiesWriter(facilities).write(output);
	}

	private static Map<Id<Link>,Double> mapLinkIDsOfZoneToParkingCapacity(String pathToZoneFile){
		
		Map<Id<Link>,Double> parkingSlots = new HashMap<>();
		
		TabularFileParserConfig config = new TabularFileParserConfig();
        config.setDelimiterTags(new String[] {"\t",";"});
        config.setFileName(pathToZoneFile);
        config.setCommentTags(new String[] { "#" });
        new TabularFileParser().parse(config, new TabularFileHandler() {
			boolean header = true;

        	@Override
			public void startRow(String[] row) {
				if(!header){
					Id<Link> linkId = Id.createLinkId(row[0]);
					parkingSlots.put(linkId,Double.parseDouble(row[1]));
				}
				header = false;
			}
		
        });
		return parkingSlots;
	}

}

