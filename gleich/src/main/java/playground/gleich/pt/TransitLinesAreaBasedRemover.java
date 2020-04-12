package playground.gleich.pt;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.pt.utils.TransitScheduleValidator.ValidationResult;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import playground.vsp.andreas.utils.pt.TransitLineRemover;
import playground.vsp.andreas.utils.pt.TransitScheduleCleaner;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class TransitLinesAreaBasedRemover {

	private static final Logger log = Logger.getLogger(TransitLinesAreaBasedRemover.class);

	public static void main(String[] args) throws MalformedURLException {
		final String inScheduleFile = "../../shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/optimizedSchedule.xml.gz";
		final String inNetworkFile = "../../shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/optimizedNetwork.xml.gz";
		final String outScheduleFile = "../../shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v1/optimizedScheduleWoBusTouchingZone.xml.gz";
		final String zoneShpFile = "file://../../shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/vulkaneifel.shp";

		//load shp file
		List<PreparedGeometry> geometries = ShpGeometryUtils.loadPreparedGeometries(new URL(zoneShpFile));

		Config config = ConfigUtils.createConfig();
		config.transit().setTransitScheduleFile(inScheduleFile);
		config.network().setInputFile(inNetworkFile);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		TransitSchedule inTransitSchedule = scenario.getTransitSchedule();

		Set<Id<TransitLine>> linesToRemove = inTransitSchedule.getTransitLines().values().stream().
				filter(line -> line.getRoutes().values().stream().allMatch(route -> route.getTransportMode().equals("bus"))).
				filter(line -> touchesZone(line, geometries)).
				map(line -> line.getId()).
				collect(Collectors.toSet());

		linesToRemove.stream().sorted().forEach(l -> log.info(l.toString()));

		TransitSchedule outTransitSchedule = TransitLineRemover.removeTransitLinesFromTransitSchedule(inTransitSchedule, linesToRemove);

		TransitSchedule outTransitScheduleCleaned = TransitScheduleCleaner.removeStopsNotUsed(outTransitSchedule);

		ValidationResult validationResult = TransitScheduleValidator.validateAll(outTransitScheduleCleaned, scenario.getNetwork());
		log.warn(validationResult.getErrors());

		new TransitScheduleWriter(outTransitScheduleCleaned).writeFile(outScheduleFile);
	}

	private static boolean completelyInZone(TransitLine line, List<PreparedGeometry> zones) {
		Map<Id<TransitStopFacility>, Boolean> stop2LocationInZone = new HashMap<>();

		line.getRoutes().values().forEach(route -> checkAndWriteLocationPerStop(stop2LocationInZone, route, zones));
		return stop2LocationInZone.values().stream().allMatch(b -> b == true);
	}

	private static boolean touchesZone (TransitLine line, List<PreparedGeometry> zones) {
		Map<Id<TransitStopFacility>, Boolean> stop2LocationInZone = new HashMap<>();

		line.getRoutes().values().forEach(route -> checkAndWriteLocationPerStop(stop2LocationInZone, route, zones));
		return stop2LocationInZone.values().stream().anyMatch(b -> b == true);
	}

	private static void checkAndWriteLocationPerStop(Map<Id<TransitStopFacility>, Boolean> stop2LocationInZone, TransitRoute route, List<PreparedGeometry> zones) {
		route.getStops().forEach(stop -> stop2LocationInZone.put(stop.getStopFacility().getId(), ShpGeometryUtils.isCoordInPreparedGeometries(stop.getStopFacility().getCoord(), zones)));
	}

}