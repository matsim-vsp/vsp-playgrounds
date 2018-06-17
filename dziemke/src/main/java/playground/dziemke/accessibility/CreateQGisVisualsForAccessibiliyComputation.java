package playground.dziemke.accessibility;

import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import com.vividsolutions.jts.geom.Envelope;

/**
 * This is a starter class to create visual QGis-based output from a readily-done accessibility computation
 * where the accessibilities.csv file already exists
 * 
 * @author dziemke
 */
public class CreateQGisVisualsForAccessibiliyComputation {

	public static void main(String[] args) {
//		String workingDirectory = "../../../shared-svn/projects/maxess/data/nmb/output/17neuRestrictedFile/";
//		String workingDirectory = "/Users/dominik/Workspace/matsim/contribs/integration/test/output/org/matsim/integration/daily/accessibility/AccessibilityComputationNairobiTest_-7.0-3.5_new/runAccessibilityComputation/";
//		String workingDirectory = "/Users/dominik/Workspace/matsim/contribs/integration/test/output/org/matsim/integration/daily/accessibility/AccessibilityComputationNMBTest_-5.5--2.0/runAccessibilityComputation/";
		String workingDirectory = "../../runs-svn/patnaIndia/run108/jointDemand/policies/0.15pcu/accessibilities/BT-b_BT-mb_50/";
//		String workingDirectory = "../../upretoria/data/nmb/output_500_6_work/";
//		String workingDirectory = "../../upretoria/data/nmb/output_500_6_work_motherwell_road/";
//		String workingDirectory = "../../shared-svn/projects/maxess/data/capetown/output/02/";
//		String workingDirectory = "../../upretoria/data/capetown/output_500/";
//		String workingDirectory = "../../shared-svn/projects/accessibility_berlin/output/pt_200/";
		
//		int cellSize = 200;
//		int cellSize = 1000;
//		int cellSize = 500;
		int cellSize = 50;
		
//		final List<String> activityTypes = Arrays.asList(new String[]{"composite"});
//		final List<String> activityTypes = Arrays.asList(new String[]{"work"});
//		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.EDUCATION, FacilityTypes.SHOPPING});
		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.EDUCATION});
		
//		final List<String> modes = Arrays.asList(new String[]{TransportMode.car, TransportMode.bike, TransportMode.walk, "freespeed"});
//		final List<String> modes = Arrays.asList(new String[]{"pt-walk"});
//		final List<String> modes = Arrays.asList(new String[]{Modes4Accessibility.car.toString()});
//		final List<String> modes = Arrays.asList(new String[]{Modes4Accessibility.pt.toString()});
		final List<String> modes = Arrays.asList(new String[]{Modes4Accessibility.bike.toString()});
//		final List<String> modes = Arrays.asList(new String[]{Modes4Accessibility.freespeed.toString(), Modes4Accessibility.car.toString(),
//				Modes4Accessibility.bike.toString(), Modes4Accessibility.walk.toString()});
		
		Envelope envelope = new Envelope(307000,324000,2829000,2837000); // Patna
		String scenarioCRS = "EPSG:24345"; // EPSG:24345 = Kalianpur 1975 / UTM zone 45N
//		Envelope envelope = new Envelope(100000,180000,-3720000,-3675000); // Notation: minX, maxX, minY, maxY
//		String scenarioCRS = TransformationFactory.WGS84_SA_Albers; // used for NMB
//		Envelope envelope = new Envelope(-302000, -245000, 6160000, 6261000); // deliberately chosen slightly too high output_1000_ptnw_6_motherwell_railto improve picture
//		String scenarioCRS = "EPSG:22235"; // used for Cape Town
//		final Envelope envelope = new Envelope(4574000, 4620000, 5802000, 5839000); // Berlin; notation: minX, maxX, minY, maxY
//		String scenarioCRS = "EPSG:31468"; // EPSG:31468 = DHDN GK4
		
//		final boolean includeDensityLayer = true;
		final boolean includeDensityLayer = false;
		final Integer range = 9; // In the current implementation, this must always be 9
		
//		final Double lowerBound = 2.; // (upperBound - lowerBound) ideally nicely divisible by (range - 2)
//		final Double upperBound = 5.5;
//		final Double lowerBound = -3.5; // (upperBound - lowerBound) ideally nicely divisible by (range - 2)
//		final Double upperBound = 3.5;
//		final Double lowerBound = -1.75; // (upperBound - lowerBound) ideally nicely divisible by (range - 2)
//		final Double upperBound = 1.75;
//		final Double lowerBound = -7.; // (upperBound - lowerBound) ideally nicely divisible by (range - 2)
//		final Double upperBound = 0.;
//		final Double lowerBound = -14.; // (upperBound - lowerBound) ideally nicely divisible by (range - 2)
//		final Double upperBound = 0.;
		final Double lowerBound = -0.35; // (upperBound - lowerBound) ideally nicely divisible by (range - 2)
		final Double upperBound = 0.35;
		
		final int populationThreshold = (int) (0 / (1000/cellSize * 1000/cellSize));
//		final int populationThreshold = (int) (50 / (1000/cellSize * 1000/cellSize));

		String osName = System.getProperty("os.name");
		for (String actType : activityTypes) {
			String actSpecificWorkingDirectory = workingDirectory + actType + "/";
			for (String mode : modes) {
				VisualizationUtils.createQGisOutputGraduatedStandardColorRange(actType, mode, envelope, workingDirectory, scenarioCRS, includeDensityLayer,
						lowerBound, upperBound, range, cellSize, populationThreshold);
//				VisualizationUtils.createQGisOutputRuleBasedStandardColorRange(actType, mode, envelope, workingDirectory, scenarioCRS, includeDensityLayer,
//						lowerBound, upperBound, range, cellSize, populationThreshold);
//				VisualizationUtils.createQGisOutputGraduatedStandardColorRange(actType, mode, new Envelope(100000,180000,-3720000,-3675000), workingDirectory, TransformationFactory.WGS84_SA_Albers, includeDensityLayer,
//						lowerBound, upperBound, range, cellSize, populationThreshold);
//				VisualizationUtils.createQGisOutput(actType, mode, new Envelope(251800.0, 258300.0, 9854300.0, 9858700.0), workingDirectory, "EPSG:21037", includeDensityLayer,
//						lowerBound, upperBound, range, cellSize, populationThreshold);
				VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode, osName);
			}
		}  
	}
}