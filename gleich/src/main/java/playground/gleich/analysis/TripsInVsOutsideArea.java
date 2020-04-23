package playground.gleich.analysis;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TripsInVsOutsideArea {

    private final Scenario scenario;
    private static final Logger log = Logger.getLogger(TripsInVsOutsideArea.class);

    public static void main (String[] args) throws MalformedURLException {
//        String pathInclRunId = "/home/gregor/git/runs-svn/avoev/snz-gladbeck/output-snzDrt441/snzDrt441";
        String pathInclRunId = "/home/gregor/git/runs-svn/avoev/snz-vulkaneifel/output-snzDrt341/snzDrt341";
        Config config = ConfigUtils.loadConfig(pathInclRunId + ".output_config.xml");
        config.network().setInputFile(pathInclRunId + ".output_network.xml.gz");
        config.transit().setTransitScheduleFile(pathInclRunId + ".output_transitSchedule.xml.gz");
        config.plans().setInputFile(pathInclRunId + ".output_plans.xml.gz");
        config.facilities().setInputFile(pathInclRunId + ".output_facilities.xml.gz");

        String shapeFile = "file:///home/gregor/git/shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/vulkaneifel.shp";
        List<PreparedGeometry> geometries = ShpGeometryUtils.loadPreparedGeometries(new URL(shapeFile));

        TripsInVsOutsideArea runner = new TripsInVsOutsideArea(config);
        runner.run(geometries);
    }

    public TripsInVsOutsideArea(Config config) {
        scenario = ScenarioUtils.loadScenario(config);
    }

    public void run(List<PreparedGeometry> geometries) {
        List<Map.Entry<Long, Integer>> tripsPerAgent = scenario.getPopulation().getPersons().values().parallelStream().
                map(person -> {
                    List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());
                    long tripsStartOrEndInZone = trips.stream().filter(trip ->
                            ShpGeometryUtils.isCoordInPreparedGeometries(getCoordFromActivity(trip.getOriginActivity()), geometries) ||
                                    ShpGeometryUtils.isCoordInPreparedGeometries(getCoordFromActivity(trip.getDestinationActivity()), geometries)).count();
                    log.warn("person id: " + person.getId().toString() + ". trips in shape: " + tripsStartOrEndInZone + ". trips total: " + trips.size());
                    return Map.entry(tripsStartOrEndInZone, trips.size());
                }).collect(Collectors.toList());

        long startEndInsideTrips = tripsPerAgent.parallelStream().mapToLong(entry -> entry.getKey()).sum();
        long totalTrips = tripsPerAgent.parallelStream().mapToInt(Map.Entry::getValue).sum();

        log.info("trips staring or ending in shp: " + startEndInsideTrips + ". total number of trips: " + totalTrips + ". Share: " + ((double) startEndInsideTrips) / totalTrips);
    }

    /* copied from TripsAndLegsCSVWriter */
    private Coord getCoordFromActivity(Activity activity) {
        if (activity.getCoord() != null) {
            return activity.getCoord();
        } else if (activity.getFacilityId() != null && scenario.getActivityFacilities().getFacilities().containsKey(activity.getFacilityId())) {
            Coord coord = scenario.getActivityFacilities().getFacilities().get(activity.getFacilityId()).getCoord();
            return coord != null ? coord : getCoordFromLink(activity.getLinkId());
        } else return getCoordFromLink(activity.getLinkId());
    }

    /* copied from TripsAndLegsCSVWriter */
    //this is the least desirable way
    private Coord getCoordFromLink(Id<Link> linkId) {
        return scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord();
    }
}
