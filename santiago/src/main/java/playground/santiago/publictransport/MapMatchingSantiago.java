package playground.santiago.publictransport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.SearchableNetwork;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;

import com.bmw.hmm_lib.Hmm;
import com.bmw.hmm_lib.MostLikelySequence;
import com.bmw.hmm_lib.TimeStep;


public class MapMatchingSantiago {
	
	
	private static class ClRoute {

		String id;

		public Coord coord;
	}
	
	

	static class MyTransitRouteStop {
		ClRoute stop;
		int index;
	}

	public static void main(String[] args) {
		
		double measurementErrorSigma = 50.0;
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile("../../../mapMatching/0_networks/toMATSim/fullNetwork_v3.xml");
		
		FreespeedTravelTimeAndDisutility travelCosts = new FreespeedTravelTimeAndDisutility(0.0, 0.0, -1.0);
		LeastCostPathCalculator router = new DijkstraFactory().createPathCalculator(scenario.getNetwork(), travelCosts, travelCosts);
		Map<String, List<ClRoute>> routes = new HashMap<String, List<ClRoute>>();
		
		TabularFileParserConfig config = new TabularFileParserConfig();
		config.setFileName("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/pt-gps/2015-04-ShapeRutas.csv");
		config.setDelimiterTags(new String[]{";"});
		
		
		new TabularFileParser().parse(config, new TabularFileHandler() {
			int nLines=0;
			@Override
			public void startRow(String[] strings) {
				if (nLines++ == 0) return;
				String id = strings[1];
				if (!routes.containsKey(id)) {
					routes.put(id, new ArrayList<>());
				}
				ClRoute clRoute = new ClRoute();
				clRoute.id = id;
				clRoute.coord = new Coord(Double.parseDouble(strings[2]), Double.parseDouble(strings[3]));
				routes.get(id).add(clRoute);
			}
		});

		double radius = 50.0;
		routes.entrySet().stream().limit(100).forEach(entry -> {
			System.out.println(entry.getKey());
			List<TimeStep<Link, MyTransitRouteStop>> timeSteps = new ArrayList<>();
			int i=0;
			ClRoute previous = null;
			for (ClRoute transitRouteStop : entry.getValue()) {
				if (previous == null || CoordUtils.calcEuclideanDistance(previous.coord, transitRouteStop.coord) > 2*measurementErrorSigma) {
					MyTransitRouteStop state = new MyTransitRouteStop();
					state.stop = transitRouteStop;
					state.index = i++;
					Collection<Node> nearestNodes;
					double myRadius = 0.0;
					do {
						myRadius += radius;
						nearestNodes = ((SearchableNetwork) scenario.getNetwork()).getNearestNodes(transitRouteStop.coord, myRadius);
					} while (nearestNodes.isEmpty());
					Collection<Link> nearestLinks = nearestNodes.stream().flatMap(node -> node.getInLinks().values().stream()).collect(Collectors.toList());
					timeSteps.add(new TimeStep<Link, MyTransitRouteStop>(state, nearestLinks));
					previous = transitRouteStop;
				}
			}
			TemporalMetrics<MyTransitRouteStop> temporalMetrics = new TemporalMetrics<MyTransitRouteStop>() {
				@Override
				public double timeDifference(MyTransitRouteStop o1, MyTransitRouteStop o2) {
					double v = (o2.index - o1.index) * 1.0;
					return v;
				}
			};
			SpatialMetrics<Link, MyTransitRouteStop> spatialMetrics = new SpatialMetrics<Link, MyTransitRouteStop>() {
				@Override
				public double measurementDistance(Link link, MyTransitRouteStop o) {
					return CoordUtils.distancePointLinesegment(link.getFromNode().getCoord(), link.getToNode().getCoord(), o.stop.coord);
				}

				@Override
				public double linearDistance(MyTransitRouteStop o, MyTransitRouteStop o1) {
					return CoordUtils.calcEuclideanDistance(o.stop.coord, o1.stop.coord);
				}

				@Override
				public Double routeLength(Link node1, Link node2) {
					LeastCostPathCalculator.Path path = router.calcLeastCostPath(node1.getToNode(), node2.getToNode(), 0.0, null, null);
					double dist = 0.0;
					for (Link link : path.links) {
						dist += link.getLength();
					}
					return dist;
				}
			};
			MapMatchingHmmProbabilities<Link, MyTransitRouteStop> probabilities =
					new MapMatchingHmmProbabilities<>(timeSteps, spatialMetrics, temporalMetrics, measurementErrorSigma, 0.01);
			MostLikelySequence<Link, MyTransitRouteStop> seq = Hmm.computeMostLikelySequence(probabilities, timeSteps.iterator());

			if (!seq.isBroken) {
				if (!seq.sequence.isEmpty()) {
					List<Id<Link>> linkIds = new ArrayList<>();
					Link link = seq.sequence.get(0);
					for (int j=1; j<seq.sequence.size(); j++) {
						linkIds.add(link.getId());
						Link nextLink = seq.sequence.get(j);
						LeastCostPathCalculator.Path path = router.calcLeastCostPath(link.getToNode(), nextLink.getFromNode(), 0.0, null, null);
						linkIds.addAll(path.links.stream().map(Link::getId).collect(Collectors.toList()));
						link = nextLink;
					}
					linkIds.add(link.getId());
					TransitLine transitLine = scenario.getTransitSchedule().getFactory().createTransitLine(Id.create(entry.getKey(), TransitLine.class));
					TransitRoute transitRoute = scenario.getTransitSchedule().getFactory().createTransitRoute(
							Id.create(entry.getKey(), TransitRoute.class),
							RouteUtils.createNetworkRoute(linkIds, scenario.getNetwork()),
							new ArrayList<>(),
							"mode");
					transitLine.addRoute(transitRoute);
					scenario.getTransitSchedule().addTransitLine(transitLine);
				}
			}
			System.out.println(seq.isBroken);
			System.out.println(seq.sequence);
			System.out.printf("%d -> %d\n", timeSteps.size(), seq.sequence.size());
		});
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile("../../../mapMatching/1_output/mapMatchedTransitSchedule.xml.gz");
	}

}