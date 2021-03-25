package playground.lu.freight;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.core.tools.picocli.CommandLine;
import org.geotools.data.FeatureReader;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.analysis.DefaultAnalysisMainModeIdentifier;
import org.matsim.analysis.modalSplitUserType.ModeAnalysis;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * This code generates German wide long distance freight trip.
 * Input:
 * Author: Chengqi Lu
 */

public class GenerateGermanWideFreightTrips{
    // INPUT: Freight data; German major road network; NUTS3 shape file; Look up
    // table between NUTS zone ID and BWVM zone ID system
    private static final String SHAPEFILE_PATH = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries"
            + "/de/freight/original_data/NUTS3/NUTS3_2010_DE.shp";
    private static final String NETWORK_FILE = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries"
            + "/de/freight/original_data/german-primary-road.network.xml.gz";
    private static final String FREIGHT_DATA = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries"
            + "/de/freight/original_data/ketten-2010.csv";
    private static final String LOOKUP_TABLE = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries"
            + "/de/freight/original_data/lookup-table.csv";

    private static final double AVERAGE_CAPACITY_OF_TRUCK = 16 * 365; // 16 ton , 365 days per year
    private static final Random RND = new Random(4711);

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            args = new String[]{
                    "C:\\Users\\cluac\\MATSimScenarios\\Freight-Germany\\testing-german-long-distance-freight.xml.gz",
                    "test"};
        }
        String outputPath = args[0];
        String scale = args[1];
        double scalingFactor;

        switch (scale) {
            case "1pct":
                scalingFactor = 100;
                break;
            case "10pct":
                scalingFactor = 10;
                break;
            case "25pct":
                scalingFactor = 4;
                break;
            case "100pct":
                scalingFactor = 1;
                break;
            case "test":
                scalingFactor = 1000;
                break;
            default:
                throw new IllegalArgumentException("Please input scaling factor correctly: 1pct, 10pct, 25pct or test");
        }
        double adjustedTrucksLoad = AVERAGE_CAPACITY_OF_TRUCK * scalingFactor;

        // Load config, scenario and network
        Config config = ConfigUtils.createConfig();
        config.global().setCoordinateSystem("EPSG:5677");
        config.network().setInputFile(NETWORK_FILE);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        Population population = scenario.getPopulation();
        PopulationFactory populationFactory = population.getFactory();

        // Extracting relevant zones and associate them with the all the links inside
        Map<String, List<Id<Link>>> regionLinksMap = new HashMap<>();
        List<Link> links = network.getLinks().values().stream().filter(l -> l.getAllowedModes().contains("car"))
                .collect(Collectors.toList());

        System.out.println("Reading Shape File now...");
        ShapefileDataStore ds = (ShapefileDataStore) FileDataStoreFinder.getDataStore(new URL(SHAPEFILE_PATH));
        ds.setCharset(StandardCharsets.UTF_8);
        FeatureReader<SimpleFeatureType, SimpleFeature> it = ds.getFeatureReader();

        Map<String, Geometry> regions = new HashMap<>();
        while (it.hasNext()) {
            SimpleFeature feature = it.next();
            Geometry region = (Geometry) feature.getDefaultGeometry();
            String nutsId = feature.getAttribute("NUTS_ID").toString();
            regions.put(nutsId, region);
        }
        it.close();
        System.out.println("Shape file loaded. There are in total " + regions.keySet().size() + " regions");

        System.out.println("Start processing the region");
        int processed = 0;
        for (String nutsId : regions.keySet()) {
            Geometry region = regions.get(nutsId);
            boolean regionIsRelevant = false;
            List<Id<Link>> linksInsideRegion = new ArrayList<>();
            for (Link link : links) {
                if (isCoordWithinGeometry(link.getToNode().getCoord(), region)) {
                    regionIsRelevant = true;
                    linksInsideRegion.add(link.getId());
                }
            }
            if (regionIsRelevant) {
                regionLinksMap.put(nutsId, linksInsideRegion);
            }
            processed += 1;
            if (processed % 10 == 0) {
                System.out.println("Analysis in progress: " + processed + " regions have been processed");
            }
        }

        // Reading the look up table (RegionID-RegionName-Table.csv)
        Map<String, String> lookUpTable = new HashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(LOOKUP_TABLE).openStream()));
            reader.readLine(); // Skip first line
            String line = reader.readLine();
            while (line != null) {
                String nutsId = line.split(";")[3];
                String zoneId = line.split(";")[0];
                lookUpTable.put(zoneId, nutsId);
                line = reader.readLine();
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Region analysis complete!");
        System.out.println("There are " + regionLinksMap.keySet().size() + " relevant regions");

        Set<String> relevantRegionNutsIds = regionLinksMap.keySet();
        Map<String, String> lookUpTableCore = new HashMap<>();
        for (String regionId : lookUpTable.keySet()) {
            if (relevantRegionNutsIds.contains(lookUpTable.get(regionId))) {
                lookUpTableCore.put(regionId, lookUpTable.get(regionId));
            }
        }
        Set<String> relevantRegionIds = lookUpTableCore.keySet();

        // Read freight data and generate freight population
        try {
            MutableInt totalGeneratedPerson = new MutableInt();
            BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(FREIGHT_DATA).openStream()));
            reader.readLine(); // Skip first line
            String line = reader.readLine();
            while (line != null) {
                String[] dataEntry = line.split(";");
                String goodType = dataEntry[10];

                // Vorlauf
                String modeVL = dataEntry[6];
                String originVL = dataEntry[0];
                String destinationVL = dataEntry[2];
                String tonVL = dataEntry[15];

                // Hauptlauf
                String modeHL = dataEntry[7];
                String originHL = dataEntry[2];
                String destinationHL = dataEntry[3];
                String tonHL = dataEntry[16];

                // Nachlauf
                String modeNL = dataEntry[8];
                String originNL = dataEntry[3];
                String destinationNL = dataEntry[1];

                String tonNL = dataEntry[17];

                if (relevantRegionIds.contains(originVL) && relevantRegionIds.contains(destinationVL)
                        && modeVL.equals("2") && !tonVL.equals("0")) {
                    double trucks = Double.parseDouble(tonVL) / adjustedTrucksLoad;
                    int numOfTrucks = 0;
                    if (trucks < 1) {
                        if (RND.nextDouble() < trucks) {
                            numOfTrucks = 1;
                        }
                    } else {
                        numOfTrucks = (int) (Math.floor(trucks) + 1);
                    }
                    List<Id<Link>> linksInOrigin = regionLinksMap.get(lookUpTableCore.get(originVL));
                    Id<Link> fromLinkId = linksInOrigin.get(RND.nextInt(linksInOrigin.size()));

                    List<Id<Link>> linksInDestination = regionLinksMap.get(lookUpTableCore.get(destinationVL));
                    Id<Link> toLinkId = linksInDestination.get(RND.nextInt(linksInDestination.size()));

                    generateFreightPlan(network, fromLinkId, toLinkId, numOfTrucks, goodType, population,
                            populationFactory, totalGeneratedPerson);
                }

                if (relevantRegionIds.contains(originHL) && relevantRegionIds.contains(destinationHL)
                        && modeHL.equals("2") && !tonHL.equals("0")) {
                    double trucks = Double.parseDouble(tonHL) / adjustedTrucksLoad;
                    int numOfTrucks = 0;
                    if (trucks < 1) {
                        if (RND.nextDouble() < trucks) {
                            numOfTrucks = 1;
                        }
                    } else {
                        numOfTrucks = (int) (Math.floor(trucks) + 1);
                    }

                    List<Id<Link>> linksInOrigin = regionLinksMap.get(lookUpTableCore.get(originHL));
                    Id<Link> fromLinkId = linksInOrigin.get(RND.nextInt(linksInOrigin.size()));

                    List<Id<Link>> linksInDestination = regionLinksMap.get(lookUpTableCore.get(destinationHL));
                    Id<Link> toLinkId = linksInDestination.get(RND.nextInt(linksInDestination.size()));

                    generateFreightPlan(network, fromLinkId, toLinkId, numOfTrucks, goodType, population,
                            populationFactory, totalGeneratedPerson);
                }

                if (relevantRegionIds.contains(originNL) && relevantRegionIds.contains(destinationNL)
                        && modeNL.equals("2") && !tonNL.equals("0")) {
                    double trucks = Double.parseDouble(tonNL) / adjustedTrucksLoad;
                    int numOfTrucks = 0;
                    if (trucks < 1) {
                        if (RND.nextDouble() < trucks) {
                            numOfTrucks = 1;
                        }
                    } else {
                        numOfTrucks = (int) (Math.floor(trucks) + 1);
                    }

                    List<Id<Link>> linksInOrigin = regionLinksMap.get(lookUpTableCore.get(originNL));
                    Id<Link> fromLinkId = linksInOrigin.get(RND.nextInt(linksInOrigin.size()));

                    List<Id<Link>> linksInDestination = regionLinksMap.get(lookUpTableCore.get(destinationNL));
                    Id<Link> toLinkId = linksInDestination.get(RND.nextInt(linksInDestination.size()));

                    generateFreightPlan(network, fromLinkId, toLinkId, numOfTrucks, goodType, population,
                            populationFactory, totalGeneratedPerson);
                }
                line = reader.readLine();
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Write population
        System.out.println("Writing population file...");
        System.out.println("There are in total " + population.getPersons().keySet().size() + " freight trips");
        PopulationWriter pw = new PopulationWriter(population);
        pw.write(outputPath);
    }


    private static boolean isCoordWithinGeometry(Coord coord, Geometry geometry) {
        Point point = MGC.coord2Point(coord);
        return point.within(geometry);
    }

    private static void generateFreightPlan(Network network, Id<Link> fromLinkId, Id<Link> toLinkId, int numOfTrucks,
                                            String goodType, Population population, PopulationFactory populationFactory,
                                            MutableInt totalGeneratedPersons) {
        if (fromLinkId.toString().equals(toLinkId.toString())) {
            return; // We don't have further information on the trips within the same region
        }

        int generated = 0;
        while (generated < numOfTrucks) {
            Person freightPerson = populationFactory.createPerson(
                    Id.create("freight_" + totalGeneratedPersons.intValue(), Person.class));
            freightPerson.getAttributes().putAttribute("subpopulation", "freight");
            freightPerson.getAttributes().putAttribute("type_of_good", goodType);

            Plan plan = populationFactory.createPlan();
            Activity act0 = populationFactory.createActivityFromLinkId("freight_start", fromLinkId);
            act0.setCoord(network.getLinks().get(fromLinkId).getCoord());
            act0.setEndTime(RND.nextInt(86400));
            Leg leg = populationFactory.createLeg("freight");
            Activity act1 = populationFactory.createActivityFromLinkId("freight_end", toLinkId);
            act1.setCoord(network.getLinks().get(toLinkId).getCoord());

            plan.addActivity(act0);
            plan.addLeg(leg);
            plan.addActivity(act1);
            freightPerson.addPlan(plan);
            population.addPerson(freightPerson);

            generated += 1;
            totalGeneratedPersons.increment();
        }
    }
}
