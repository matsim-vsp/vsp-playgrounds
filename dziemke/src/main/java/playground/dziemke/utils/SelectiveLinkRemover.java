package playground.dziemke.utils;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.util.*;

public class SelectiveLinkRemover {

    public static void main (String[] args) {
        String networkInputFile = "../../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-10pct/input/berlin-v5-network.xml.gz";
        String networkOutputFile = "../../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-10pct/input/berlin-v5-network-sec2-hundekopf.xml.gz";

        List<String> typesToBeRemoved = Arrays.asList("tertiary", "tertiary_link", "unclassified", "residential", "living_street");

        String areaShapeFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/shapefiles/berlin_hundekopf/berlin_hundekopf.shp";
        String attributeCaption = "SCHLUESSEL";
        // String distinctiveFeatureId = "";
        String distinctiveFeatureId = "Hundekopf";

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimNetworkReader reader = new MatsimNetworkReader(scenario.getNetwork());
        reader.readFile(networkInputFile);

        Collection<SimpleFeature> features = (new ShapeFileReader()).readFileAndInitialize(areaShapeFile);
        Geometry areaGeometry = ShapeFileUtils.getGeometryByValueOfAttribute(features, attributeCaption, distinctiveFeatureId);

        // Get pt subnetwork
        Scenario ptScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        TransportModeNetworkFilter transportModeNetworkFilterPt = new TransportModeNetworkFilter(scenario.getNetwork());
        transportModeNetworkFilterPt.filter(ptScenario.getNetwork(), new HashSet<>(Arrays.asList(TransportMode.pt)));

        List<Link> shortlistedLinks = new ArrayList<>();

        for (Link link : new ArrayList<>(scenario.getNetwork().getLinks().values())) {
            if (link.getAllowedModes().contains(TransportMode.car)) {
                String actualType = link.getAttributes().getAttribute("type").toString();
                for (String checkedType : typesToBeRemoved) {
                    if (actualType.equals(checkedType)) {
                        shortlistedLinks.add(link);

                    }
                }
            }
        }

        if (areaGeometry != null) {
            for (Link link : new ArrayList<>(shortlistedLinks)) {
                Point linkCenterAsPoint = MGC.xy2Point(link.getCoord().getX(), link.getCoord().getY());
                if (!areaGeometry.contains(linkCenterAsPoint)) {
                    shortlistedLinks.remove(link);
                }
            }
        }

        for (Link link : new ArrayList<>(shortlistedLinks)) {
            scenario.getNetwork().removeLink(link.getId());
        }

        NetworkCleaner networkCleaner = new NetworkCleaner();
        networkCleaner.run(scenario.getNetwork());

        // Add pt back into the other network
        // Note: Customized attribute are not considered here
        NetworkFactory factory = scenario.getNetwork().getFactory();
        for (Node node : ptScenario.getNetwork().getNodes().values()) {
            Node node2 = factory.createNode(node.getId(), node.getCoord());
            scenario.getNetwork().addNode(node2);
        }
        for (Link link : ptScenario.getNetwork().getLinks().values()) {
            Node fromNode = scenario.getNetwork().getNodes().get(link.getFromNode().getId());
            Node toNode = scenario.getNetwork().getNodes().get(link.getToNode().getId());
            Link link2 = factory.createLink(link.getId(), fromNode, toNode);
            link2.setAllowedModes(link.getAllowedModes());
            link2.setCapacity(link.getCapacity());
            link2.setFreespeed(link.getFreespeed());
            link2.setLength(link.getLength());
            link2.setNumberOfLanes(link.getNumberOfLanes());
            scenario.getNetwork().addLink(link2);
        }

        NetworkWriter writer = new NetworkWriter(scenario.getNetwork());
        writer.write(networkOutputFile);
    }
}