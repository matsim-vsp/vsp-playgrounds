package playground.mdziakowski.activityReLocation;

import org.matsim.contrib.accessibility.osm.CombinedOsmReader;
import org.matsim.contrib.accessibility.utils.AccessibilityFacilityUtils;

import java.io.FileNotFoundException;

public class RunOSMBerlinActivities {

    public static void main(String[] args) {

        String root = "D:/Arbeit/Berlin/ReLocation/Test/";

        //        String inputOSMFile = root + "brandenburg-berlin-latest.osm_01.osm";
        String inputOSMFile = root + "berlin-latest-test.osm_01.osm";
//        String outputFacilityFile = root + "facilities.xml.gz";
        String outputFacilityFile = root + "combinedFacilities-test.xml";
        String attributeFile = root + "attributeFile-test.xml";

        String newCoord = "DHDN_GK4";

        CombinedOsmReader activitiesReader = new CombinedOsmReader(newCoord,
                AccessibilityFacilityUtils.buildOsmLandUseToMatsimTypeMap(),
                AccessibilityFacilityUtils.buildOsmBuildingToMatsimTypeMap(),
                AccessibilityFacilityUtils.buildOsmAmenityToMatsimTypeMap(),
                AccessibilityFacilityUtils.buildOsmLeisureToMatsimTypeMap(),
                AccessibilityFacilityUtils.buildOsmTourismToMatsimTypeMap(),
                AccessibilityFacilityUtils.buildUnmannedEntitiesList(),
                0);

        try {
            activitiesReader.parseFile(inputOSMFile);
            activitiesReader.writeFacilities(outputFacilityFile);
            activitiesReader.writeFacilityAttributes(attributeFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("Done");

    }

}
