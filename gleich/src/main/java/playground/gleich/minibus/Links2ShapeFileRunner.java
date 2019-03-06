package playground.gleich.minibus;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.opengis.feature.simple.SimpleFeature;

public class Links2ShapeFileRunner {
	
	private final static Logger log = Logger.getLogger(Links2ShapeFileRunner.class);

	public static void main(String[] args) {
		String networkFile = "/home/gregor/git/capetown/output-minibus-wo-transit/lastGoodRun/output_network.xml.gz";
		String scheduleFile = "/home/gregor/tmpDumpCluster/capetown/output-minibus-wo-transit/2018-12-10_100pct_withoutRouteDesignScoring/ITERS/it.1500/1500.transitScheduleScored.xml.gz";
		String idTransitLineToPrint = "para_0_6";
		String idTransitRouteToPrint = "para_0_6-131_356";
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		config.transit().setTransitScheduleFile(scheduleFile);
		config.global().setCoordinateSystem("SA_Lo19");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork(); 
		
		NetworkFilterManager nfmCar = new NetworkFilterManager(network);
		nfmCar.addLinkFilter(new NetworkLinkFilter() {
			
			@Override
			public boolean judgeLink(Link l) {
				if (l.getAllowedModes().contains("car")) return true;
				else return false;
			}
		});
		Network roadNetwork = nfmCar.applyFilters();
		
		Set<Id<Link>> link2HasTransitStop = new HashSet<>();
		HashMap<Id<Link>, Integer> link2PositionOnTransitRouteX = new HashMap<>();
		
		for (TransitStopFacility stop: scenario.getTransitSchedule().getFacilities().values()) {
			link2HasTransitStop.add(stop.getLinkId());
		}

		TransitRoute transitRouteToPrint = scenario.getTransitSchedule().getTransitLines().get(Id.create(idTransitLineToPrint, TransitLine.class)).getRoutes().get(Id.create(idTransitRouteToPrint, TransitRoute.class));
		NetworkRoute netRoute = transitRouteToPrint.getRoute();
		
		int linkCounter = 1;
		link2PositionOnTransitRouteX.put(netRoute.getStartLinkId(), linkCounter);
		for (Id<Link> link: netRoute.getLinkIds()) {
					linkCounter++;
					link2PositionOnTransitRouteX.put(link, linkCounter);
				}
		linkCounter++;
		link2PositionOnTransitRouteX.put(netRoute.getEndLinkId(), linkCounter);
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(config.global().getCoordinateSystem(), config.global().getCoordinateSystem());
		String coordinateSystem = "EPSG:2048";
		exportNetwork2Shp(roadNetwork, "/home/gregor/tmpDumpCluster/capetown/output-minibus-wo-transit/2018-12-10_100pct_withoutRouteDesignScoring/", coordinateSystem, ct, link2HasTransitStop, link2PositionOnTransitRouteX);
	}

// from playground.ikaddoura.analysis.shapes.Network2Shape;
		public static void exportNetwork2Shp(Network network, String outputDirectory, String crs, CoordinateTransformation ct, Set<Id<Link>> link2HasTransitStop, HashMap<Id<Link>, Integer> link2PositionOnTransitRouteX){
			
			String outputPath = outputDirectory + "network-shp/";
			File file = new File(outputPath);
			file.mkdirs();
			
			PolylineFeatureFactory factory = new PolylineFeatureFactory.Builder()
			.setCrs(MGC.getCRS(crs))
			.setName("Link")
			.addAttribute("Id", String.class)
			.addAttribute("Length", Double.class)
			.addAttribute("capacity", Double.class)
			.addAttribute("lanes", Double.class)
			.addAttribute("Freespeed", Double.class)
			.addAttribute("Modes", String.class)
			.addAttribute("HasStop", Integer.class)
			.addAttribute("PosRoute", Integer.class)
			.create();
			
			Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
							
			for (Link link : network.getLinks().values()){
				if (link.getAllowedModes().contains("car")) {
					int hasStop = 0;
					if (link2HasTransitStop.contains(link.getId())) {
						hasStop = 1;
					}
					
					int posRoute = -1;
					if (link2PositionOnTransitRouteX.containsKey(link.getId())) {
						posRoute = link2PositionOnTransitRouteX.get(link.getId());
					}
					
					SimpleFeature feature = factory.createPolyline(
							new Coordinate[]{
									new Coordinate(MGC.coord2Coordinate(ct.transform(link.getFromNode().getCoord()))),
									new Coordinate(MGC.coord2Coordinate(ct.transform(link.getToNode().getCoord())))
							}, new Object[] {link.getId(), link.getLength(), link.getCapacity(), link.getNumberOfLanes(), link.getFreespeed(), link.getAllowedModes(), hasStop, posRoute
							}, null
					);
					features.add(feature);
				}
			}
			
			log.info("Writing network to shapefile... ");
			ShapeFileWriter.writeGeometries(features, outputPath + "network.shp");
			log.info("Writing network to shapefile... Done.");
		}

}
