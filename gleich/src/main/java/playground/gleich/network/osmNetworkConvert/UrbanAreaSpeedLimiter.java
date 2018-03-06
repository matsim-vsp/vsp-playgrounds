package playground.gleich.network.osmNetworkConvert;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.QuadTree;

/*
 * TODO: Ideas
 * - exclude motorway junctions in counting of neighbourimg intersections using link attributes
 * - very high number of intersections -> urban area
 * - small number of intersections, but many residential roads -> urban area
 * - ........
 */
public class UrbanAreaSpeedLimiter {
	
	private Map<Id<Node>, String> node2AreaType = new HashMap<>();
	private Map<String, Double> areaType2maxSpeed = new HashMap<>();
	private Set<String> roadTypesToExclude = new HashSet<>();
	private String urbanAreaType = "urban";
	private String ruralAreaType = "rural";
	
	public void findAndSetSpeedLimitsInUrbanAreas(Network network) {
		areaType2maxSpeed.put(urbanAreaType, 50.0/3.6);
		areaType2maxSpeed.put(ruralAreaType, 90.0/3.6);
		
		// Create a QuadTree to find neighbouring nodes later
		QuadTree<Node> quadTree = prepareQuadTree(network);
		
		for (Node node: network.getNodes().values()) {
			Collection<Node> neighbouringNodes = quadTree.getDisk(node.getCoord().getX(), node.getCoord().getY(), 100);
			
			for (Node node2: neighbouringNodes) {
				for (Link link: node2.getOutLinks().values()) {
					if (link.getAttributes().getAttribute("type").equals("residential")) {
						// This neighbouring node has an residential link going out -> residential area? -> urban area?
					}
				}
			}
			if (neighbouringNodes.size() > 10) {
				node2AreaType.put(node.getId(), urbanAreaType);
			} else {
				node2AreaType.put(node.getId(), ruralAreaType);
			}
		}
		
		for (Link link: network.getLinks().values()) {
			// Access road type copied from OSM data: link.getAttributes().getAttribute("type")
			if (roadTypesToExclude.contains(link.getAttributes().getAttribute("type"))) {
				// this is a major road (e.g. a motorway) where inner city speed limits do not apply
				continue;
			}
			double maxHigherSpeed = areaType2maxSpeed.get(node2AreaType.get(link.getFromNode().getId()));
			if (areaType2maxSpeed.get(node2AreaType.get(link.getToNode().getId())) > maxHigherSpeed) {
				maxHigherSpeed = areaType2maxSpeed.get(node2AreaType.get(link.getToNode().getId()));
			}
			if (link.getFreespeed() > maxHigherSpeed) {
				link.setFreespeed(maxHigherSpeed);
			}
		}
		
	}

	private QuadTree<Node> prepareQuadTree(Network network) {
		// Find min x, min y, max x, max y coordinates to set up a QuadTree
		double minx = 0;
		double miny = 0;
		double maxx = 0;
		double maxy = 0;
		
		for (Node node: network.getNodes().values()) {
			minx = node.getCoord().getX();
			miny = node.getCoord().getY();
			maxx = node.getCoord().getX();
			maxy = node.getCoord().getY();
			break;
		}

		for(Node node: network.getNodes().values()) {
			if (node.getCoord().getX() < minx) {
				minx = node.getCoord().getX();
			}
			if (node.getCoord().getY() < miny) {
				miny = node.getCoord().getY();
			}
			if (node.getCoord().getX() > maxx) {
				maxx = node.getCoord().getX();
		}
			if (node.getCoord().getY() > maxy) {
				maxy = node.getCoord().getY();

			}
		}
		
		QuadTree<Node> quadTree = new QuadTree<>(minx, miny, maxx, maxy); 
		for (Node node: network.getNodes().values()) {
			quadTree.put(node.getCoord().getX(), node.getCoord().getY(), node);
		}
		
		return quadTree;
	}
}

