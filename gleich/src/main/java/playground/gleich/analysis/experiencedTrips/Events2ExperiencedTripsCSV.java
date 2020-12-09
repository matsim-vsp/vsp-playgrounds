/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.gleich.analysis.experiencedTrips;

import com.google.inject.Injector;

import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.analysis.TripsAndLegsCSVWriter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.ReplayEvents;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.*;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import playground.gleich.analysis.DefaultAnalysisModeIdentifier;

/**
 * 
 * @author vsp-gleich
 *
 */
public final class Events2ExperiencedTripsCSV {

	private Scenario scenario;
    private final String eventsFile;
    private ExperiencedPlansService experiencedPlansService;
	// second level separator
	private final String sep2 = ",";
	private static String pathInclRunIdAndDot = "";

	private static final Logger log = Logger.getLogger(Events2ExperiencedTripsCSV.class);
    
    public static void main(String[] args) {
//    	String pathInclRunIdAndDot = "/home/gregor/git/runs-svn/avoev/snz-gladbeck/output-snzDrtO442x/snzDrtO442x.";
//		pathInclRunIdAndDot = "/home/gregor/git/runs-svn/avoev/snz-vulkaneifel/u19-runs/output_Vu-DRT-u19-pt-6/Vu-DRT-u19-pt-6.";
		pathInclRunIdAndDot = "/home/gregor/git/runs-svn/avoev/snz-vulkaneifel/output-Vu-DRT-9/Vu-DRT-9.";
//		String pathInclRunIdAndDot = "/home/gregor/git/runs-svn/avoev/snz-vulkaneifel/output-snzDrtO321g/snzDrtO321g.";
//		String pathTripFilterShapeFile = "/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v1/gladbeck.shp";
		String pathTripFilterShapeFile = "/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/vulkaneifel.shp";
		String coordinateReferenceSystem = "EPSG:25832";

//		String pathInclRunIdAndDot = "/home/gregor/tmp/open-berlin-intermodal/Z155e/Z155e.";
		if (args.length==3) {
			pathInclRunIdAndDot = args[0];
			pathTripFilterShapeFile = args[1];
			coordinateReferenceSystem = args[2];
		} else if (args.length>3) {
			throw new RuntimeException(">3 args.length not implemented yet.");
		}

//        Config config = ConfigUtils.loadConfig(pathInclRunIdAndDot + "output_config.xml"); / the proper way
		Config config = ConfigUtils.createConfig(); // snz backport
		config.global().setCoordinateSystem(coordinateReferenceSystem); // snz backport

        config.network().setInputFile(pathInclRunIdAndDot + "output_network.xml.gz");
        config.transit().setTransitScheduleFile(pathInclRunIdAndDot + "output_transitSchedule.xml.gz");
        config.plans().setInputFile(pathInclRunIdAndDot + "output_plans.xml.gz");
		config.facilities().setInputFile(pathInclRunIdAndDot + "output_facilities.xml.gz");
		config.transit().setVehiclesFile(pathInclRunIdAndDot + "output_transitVehicles.xml.gz");
		config.vehicles().setVehiclesFile(pathInclRunIdAndDot + "output_vehicles.xml.gz");
        config.plans().setInputPersonAttributeFile(null);

        AnalysisMainModeIdentifier mainModeIdentifier = new DefaultAnalysisModeIdentifier();

        Events2ExperiencedTripsCSV runner = new Events2ExperiencedTripsCSV(config,
				pathInclRunIdAndDot + "output_events.xml.gz");
        runner.runAnalysisAndWriteResult(pathInclRunIdAndDot + "output_experiencedTrips.csv.gz",
				pathInclRunIdAndDot + "output_experiencedLegs.csv.gz", mainModeIdentifier,
				pathTripFilterShapeFile);
    }

    public Events2ExperiencedTripsCSV(Config config, String eventsFile) {
		this.eventsFile = eventsFile;
        
        readEventsAndPrepareExperiencedPlansService(config);
    }

    private void readEventsAndPrepareExperiencedPlansService(Config config) {
        scenario = ScenarioUtils.loadScenario(config);
        Injector injector = org.matsim.core.controler.Injector.createInjector(config,
				new ExperiencedPlansModule(),
				new ExperiencedPlanElementsModule(),
				new EventsManagerModule(),
				new ScenarioByInstanceModule(scenario),
				new ReplayEvents.Module());
        injector.getInstance(EventsToLegs.class).setTransitSchedule(scenario.getTransitSchedule());
        ReplayEvents replayEvents = (ReplayEvents)injector.getInstance(ReplayEvents.class);
        replayEvents.playEventsFile(eventsFile, 0, true);
        
        experiencedPlansService = ((ExperiencedPlansService)injector.getInstance(ExperiencedPlansService.class));
        experiencedPlansService.writeExperiencedPlans(pathInclRunIdAndDot + "output_experienced_plans.xml.gz");
    }

    public void runAnalysisAndWriteResult(String outputExperiencedTripsFile, String outputExperiencedLegsFile,
										  AnalysisMainModeIdentifier mainModeIdentifier,
										  String shpFile) {
    	TripsAndLegsCSVWriter.CustomTripsWriterExtension customTripsWriterExtension = new ExperiencedTripsExtension(shpFile);
		TripsAndLegsCSVWriter.CustomLegsWriterExtension customLegsWriterExtension = new ExperiencedLegsExtension();
    	 new TripsAndLegsCSVWriter(scenario, customTripsWriterExtension, customLegsWriterExtension, mainModeIdentifier).write(experiencedPlansService.getExperiencedPlans(), outputExperiencedTripsFile, outputExperiencedLegsFile);
    	 log.info("Done writing " + outputExperiencedTripsFile + " and " + outputExperiencedLegsFile);
    }
    
    private class ExperiencedTripsExtension implements TripsAndLegsCSVWriter.CustomTripsWriterExtension {

		List<Geometry> geometries;

    	ExperiencedTripsExtension(String shpFile) {
    		if (shpFile!=null && !shpFile.equals("")) {
				try {
					geometries = ShpGeometryUtils.loadGeometries(Paths.get(shpFile).toUri().toURL());
				} catch (MalformedURLException e) {
					log.error(e + "\nInput shape file string was: " + shpFile);
					e.printStackTrace();
				}
			}
		}

		@Override
		public String[] getAdditionalTripHeader() {
			String[] header = {"transit_stops_visited", "start_dist_to_shape", "end_dist_to_shape"};
			return header;
		}

		@Override
		public List<String> getAdditionalTripColumns(Trip trip) {
			List<String> values = new ArrayList<>();
			// TODO: add real values
			StringBuilder transitStopsVisited = new StringBuilder();
			for (Leg leg: trip.getLegsOnly()) {
				if (leg.getRoute() instanceof TransitPassengerRoute) {
					TransitPassengerRoute transitPassengerRoute = (TransitPassengerRoute) leg.getRoute();
					transitStopsVisited.append(transitPassengerRoute.getAccessStopId().toString())
							.append(sep2)
							.append(transitPassengerRoute.getEgressStopId().toString())
							.append(sep2);
				}
			}
			values.add(transitStopsVisited.toString());
			Coord fromCoord = getCoordFromActivity(trip.getOriginActivity());
			Coord toCoord = getCoordFromActivity(trip.getDestinationActivity());
			values.add(getMinDistanceFromGeometries(fromCoord, geometries));
			values.add(getMinDistanceFromGeometries(toCoord, geometries));
			return values;
		}
    }

    // copy from TripsAndLegsCSVWriter
	private Coord getCoordFromActivity(Activity activity) {
		if (activity.getCoord() != null) {
			return activity.getCoord();
		} else if (activity.getFacilityId() != null && this.scenario.getActivityFacilities().getFacilities().containsKey(activity.getFacilityId())) {
			Coord coord = this.scenario.getActivityFacilities().getFacilities().get(activity.getFacilityId()).getCoord();
			return coord != null ? coord : this.getCoordFromLink(activity.getLinkId());
		} else {
			return this.getCoordFromLink(activity.getLinkId());
		}
	}

	// copy from TripsAndLegsCSVWriter
	private Coord getCoordFromLink(Id<Link> linkId) {
		return this.scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord();
	}

	static class ExperiencedLegsExtension implements TripsAndLegsCSVWriter.CustomLegsWriterExtension {
		@Override
		public String[] getAdditionalLegHeader() {
			String[] legHeader = new String[]{"isIntermodalDrtPt", "intermodalMode"};
			return legHeader;
		}

		@Override
		public List<String> getAdditionalLegColumns(TripStructureUtils.Trip experiencedTrip, Leg experiencedLeg) {
			List<String> legColumn = new ArrayList<>();

			boolean containsDrt = false;
			boolean containsPt = false;

			for (Leg leg: experiencedTrip.getLegsOnly()) {
				if (leg.getMode().equals(TransportMode.drt) || leg.getMode().equals("drt_teleportation")) {
					containsDrt = true;
				} else if (leg.getMode().equals(TransportMode.pt)) {
					containsPt = true;
				}
			}
			String isIntermodalDrtPt = (containsDrt && containsPt) ? "TRUE" : "FALSE";
			legColumn.add(isIntermodalDrtPt);
			legColumn.add((containsDrt && containsPt) ? "inter"+experiencedLeg.getMode() : "mono"+experiencedLeg.getMode());
			return legColumn;
		}
	}

	private String getMinDistanceFromGeometries(Coord coord, List<Geometry> geometries) {
    	// distance method unavailable for PreparedGeometry, only available for Geometry
		Point point = MGC.coord2Point(coord);
		Optional<Double> minimumDistance = geometries.stream().map(g -> g.distance(point)).min(Double::compareTo);
		return minimumDistance.isPresent() ? minimumDistance.get().toString() : "NA";
	}
}
