package playground.mdziakowski.activityReLocation;

import org.matsim.contrib.accessibility.osm.CombinedOsmReader;
import org.matsim.contrib.accessibility.utils.AccessibilityFacilityUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.io.FileNotFoundException;
import java.io.IOException;

public class RunOSMBerlinActivities {

    public static void main(String[] args) {
    	

//        String root = "D:/Arbeit/Berlin/ReLocation/Test/";
//    	 String root = "D:/Arbeit/Berlin/ReLocation/exception/";

//                String inputOSMFile = root + "bremen-latest.osm_01.osm";
        String inputOSMFile = args[0];
//        String outputFacilityFile = root + "facilities.xml.gz";
//        String outputFacilityFile = root + "combinedFacilities-test1.xml";
//        String attributeFile = root + "attributeFile-test1.xml";
        
        String outputFacilityFile = "./combinedFacilities-test1.xml";
        String attributeFile = "./attributeFile-test1.xml";
        
        OutputDirectoryLogging.catchLogEntries();
        try {
        OutputDirectoryLogging.initLoggingWithOutputDirectory("./log");
        } catch (IOException e1) {
        e1.printStackTrace();
        }
        
        String newCoord = args[1];
//        String newCoord = "EPSG:31468";
        
        CombinedOsmReader activitiesReader = new CombinedOsmReader(newCoord,
                AccessibilityFacilityUtils.buildOsmLandUseToMatsimTypeMap(),
                AccessibilityFacilityUtils.buildOsmBuildingToMatsimTypeMap(),
                AccessibilityFacilityUtils.buildOsmAmenityToMatsimTypeMapV2(),
                AccessibilityFacilityUtils.buildOsmLeisureToMatsimTypeMapV2(),
                AccessibilityFacilityUtils.buildOsmTourismToMatsimTypeMapV2(),
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
