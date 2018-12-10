package playground.tschlenther.plateauRouter;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree.NodeData;
import org.matsim.vehicles.Vehicle;

import java.util.*;

/**
 * @author tschlenther + gthunig
*
 */
public class PlateauRouter implements LeastCostPathCalculator {

	private final Network network;

	private LeastCostPathTree lcpTree;
	
	PlateauRouter(Network network, TravelDisutility travelCosts, TravelTime travelTimes) {
		this.network = network;
		this.lcpTree = new LeastCostPathTree(travelTimes, travelCosts);
	}
	
	@Override
	public Path calcLeastCostPath(Node fromNode, Node toNode, double starttime,
			Person person, Vehicle vehicle) {

		return chooseFinalPath(calculateBestPlateauPaths(fromNode, toNode, starttime, 10, 2));
	}

	public TreeSet<Path> calculateBestPlateauPaths(Node fromNode, Node toNode, double starttime, int choiceSetSize, int resultSetSize) {
        TreeSet<Path> finalBestPlateauPaths;
            lcpTree.calculate(network, fromNode, starttime);
            Map<Id<Node>, NodeData> fromTree = lcpTree.getTree();
            lcpTree.calculate(network, toNode, starttime);
            Map<Id<Node>, NodeData> toTree = lcpTree.getTree();
            Map<Id<Node>, AdvancedNodeData> plateauTree = calcPlateauTree(fromTree, toTree);

            //calculate the given number of plateaus (the x longest ones = the x best ones)
            TreeSet<Path> longestPlateaus = getLongestPlateaus(choiceSetSize, plateauTree);

            //choose the x best plateaus out of longestplateaus
            finalBestPlateauPaths = chooseXBestPaths(resultSetSize, calcFinalPlateauPaths(longestPlateaus, starttime, fromNode, toNode, fromTree));

            Path shortestPath = calcPath(fromNode.getId(), toNode.getId(), fromTree);
            int[] nodesToRemove = {0, shortestPath.nodes.size()-1};
            Path finalShortestPath = removeNodeFromPath(nodesToRemove, shortestPath);

            finalBestPlateauPaths.add(finalShortestPath);
	    return finalBestPlateauPaths;
    }

	private TreeSet<Path> getEmptyComparingPathTreeSet() {
		return new TreeSet<>(Comparator.comparingDouble(path -> path.travelCost));
	}

	private TreeSet<Path> chooseXBestPaths(int x, TreeSet<Path> finalPlateauPaths) {
		
		TreeSet<Path> finalBestPlateauPaths = getEmptyComparingPathTreeSet();
		
		for (int i = 0; i < x; i++) {
			finalBestPlateauPaths.add(finalPlateauPaths.pollFirst());
		}
		return finalBestPlateauPaths;
	}
	
	private TreeSet<Path> calcFinalPlateauPaths(TreeSet<Path> longestPlateaus, double starttime, Node fromNode, Node toNode, Map<Id<Node>, NodeData> fromTree) {
		
		TreeSet<Path> finalPlateauPaths = getEmptyComparingPathTreeSet();
		
		while(!longestPlateaus.isEmpty()) {
			Path currentPlateau = longestPlateaus.pollFirst();
			
			lcpTree.calculate(network, currentPlateau.nodes.get(currentPlateau.nodes.size()-1), starttime);
			Map<Id<Node>, NodeData> fromPlateauTree = lcpTree.getTree();
			Path currentFinalPath = getFinalPath(currentPlateau, fromTree, fromPlateauTree, fromNode.getId(), toNode.getId());
		
			finalPlateauPaths.add(currentFinalPath);
		}
		
		return finalPlateauPaths;
	}

	private Path getFinalPath(Path plateau, Map<Id<Node>, NodeData> fromTree,
			Map<Id<Node>, NodeData> fromPlateauTree, Id<Node> fromNodeId, Id<Node> toNodeId) {
		Path toPlateau = calcPath(fromNodeId, plateau.nodes.get(0).getId(), fromTree);
		Path originToPlateauEnd = concatPath(toPlateau, plateau);
		Path fromPlateau = calcPath(plateau.nodes.get(plateau.nodes.size()-1).getId(), toNodeId, fromPlateauTree);
		Path fullPath = concatPath(originToPlateauEnd, fromPlateau);
		int[] nodesToRemove = {0, fullPath.nodes.size()-1};
		Path finalPath = removeNodeFromPath(nodesToRemove, fullPath);
		return finalPath;
	}
	
	private Path removeNodeFromPath(int[] nodeIndices, Path path) {
		List<Node> nodes = path.nodes;
		for (int i = nodeIndices.length-1; i >= 0; i--) {
			nodes.remove(i);
		}
		return new Path(nodes, path.links, path.travelTime, path.travelCost);
	}
	
	private Path calcPath(Id<Node> fromNodeId, Id<Node> toNodeId, Map<Id<Node>, NodeData> tree) {
		
		List<Node> nodes = new ArrayList<>();
		Id<Node> currentNodeId = toNodeId;
		while (tree.get(currentNodeId).getPrevNodeId() != null) {
			nodes.add(network.getNodes().get(currentNodeId));
			currentNodeId = tree.get(currentNodeId).getPrevNodeId();
		}
		nodes.add(network.getNodes().get(currentNodeId));
		nodes = reverseList(nodes);
		
		List<Link> links = getLinksFromNodes(nodes);
		double totalTravelTime = calcTotalTravelTime(links);
		
		return new Path(nodes, links, totalTravelTime, totalTravelTime);
	}
	
	private List<Node> reverseList(List<Node> nodes) {
		List<Node> reverseList = new ArrayList<>();
		for(int i = nodes.size()-1; i >= 0; i--){
			reverseList.add(nodes.get(i));
		}
		return reverseList;
	}

	private Path concatPath(Path firstPath, Path secondPath) {
		List<Node> nodes = firstPath.nodes;
		nodes.remove(nodes.size()-1);
		List<Link> links = firstPath.links;
		double travelTime = firstPath.travelTime + secondPath.travelTime;
		double travelCost = firstPath.travelCost + secondPath.travelCost;
		nodes.addAll(secondPath.nodes);
		links.addAll(secondPath.links);
		return new Path(nodes, links, travelTime, travelCost);
	}

	private Path chooseFinalPath(TreeSet<Path> longestPlateaus) {
//		zufallswert von 1 bis size
		
		long random = Math.round((Math.random() * (longestPlateaus.size()-1))+1);
		Path choosenPath = null;
		for (int i = 0; i < random; i++) {
			choosenPath = longestPlateaus.pollFirst();
		}
		return choosenPath;
//		long summarizedCosts = 0;
//		long[] plateauWeights = new long[longestPlateaus.size()];
//		int i = 0;
//		for (Path path : longestPlateaus) {
//			plateauWeights[i] = Math.round(path.travelCost);
//			summarizedCosts += Math.round(plateauWeights[i]);
//			i++;
//		}
//		long summarizedWeights = 0;
//		i = 0;
//		for (Path path : longestPlateaus) {
//			plateauWeights[i] = Math.round(Math.pow(plateauWeights[i], (-1)));
//			plateauWeights[i] *= Math.round(summarizedCosts);
//			summarizedWeights += Math.round(plateauWeights[i]);
//			i++;
//		}
//		System.out.println("summarized weights: " + summarizedWeights);
//		for (long weight : plateauWeights) {
//			System.out.println("plateau weight: " + weight);
//		}
//		long random = Math.round((Math.random() * summarizedWeights));
//		System.out.println("random: " + random);
//	    summarizedWeights = 0;
//	    i = 0;
//	    for (Path path : longestPlateaus) {
//	    	System.out.println("i=" + i + " other shit: " + (plateauWeights[i] + summarizedWeights));
//	    	System.out.println("other shit: " + Math.round((plateauWeights[i] + summarizedWeights)));
//	    	if (random < Math.round((plateauWeights[i] + summarizedWeights))) return path;
//	    	i++;
//	    }
//	    System.out.println("nullish");
//		return null;
	}

	private Map<Id<Node>, AdvancedNodeData> calcPlateauTree(Map<Id<Node>, NodeData> fromTree, Map<Id<Node>, NodeData> toTree) {
		
		HashMap<Id<Node>, AdvancedNodeData> plateauTree = new HashMap<>();
		
		for (Map.Entry<Id<Node>, NodeData> node : fromTree.entrySet()) {
			for (Map.Entry<Id<Node>, NodeData> previousNode : toTree.entrySet()) {
				if (node.getValue().getPrevNodeId() == previousNode.getKey()) {
					if (node.getKey() == previousNode.getValue().getPrevNodeId()) {
						AdvancedNodeData nodeData = plateauTree.get(node.getKey());
						if (nodeData != null) {
							nodeData.setPrevNodeId(node.getValue().getPrevNodeId());
						} else {
							plateauTree.put(node.getKey(), new AdvancedNodeData(node.getValue().getPrevNodeId(), null, 0.0, 0.0));
						}
						nodeData = plateauTree.get(previousNode.getKey());
						if (nodeData != null) {
							nodeData.setNextNodeId(previousNode.getValue().getPrevNodeId());
						} else {
							plateauTree.put(previousNode.getKey(), 
											new AdvancedNodeData(null, previousNode.getValue().getPrevNodeId(), 0.0, 0.0));
						}
						break;
					}
				}
			}
		}
		
		return plateauTree;
	}
	
	private TreeSet<Path> getLongestPlateaus(int count, Map<Id<Node>, AdvancedNodeData> plateauTree) {
		
		TreeSet<Path> plateaus = getEmptyComparingPathTreeSet();
		while (!plateauTree.entrySet().isEmpty()) {
			List<Id<Node>> keys = new ArrayList<>(plateauTree.keySet());
			Id<Node> nodeId = keys.get(0);
			AdvancedNodeData nodeData = plateauTree.get(nodeId);
			
			Map<Id<Node>, AdvancedNodeData> currentPlateauNodes = identifyPlateauNodes(nodeId, nodeData, plateauTree);
			plateauTree = removePlateauNodes(currentPlateauNodes, plateauTree);
			List<Node> nodes = getNodeListFromMap(nodeId, currentPlateauNodes);
			List<Link> links = getLinksFromNodes(nodes);
			double totalTravelTime = calcTotalTravelTime(links);
			
			Path currentPlateau = new Path(nodes, links, totalTravelTime, totalTravelTime);
			
			plateaus.add(currentPlateau);
			if (plateaus.size() > count) plateaus.remove(plateaus.first());
		}
		
		return plateaus;
	}
	
	private List<Node> getNodeListFromMap(Id<Node> nodeId, Map<Id<Node>, AdvancedNodeData> plateauNodes) {
		Id<Node> currentNodeId = nodeId;
		while(plateauNodes.get(currentNodeId).getPrevNodeId() != null) {
			currentNodeId = plateauNodes.get(currentNodeId).getPrevNodeId();
		}
		
		List<Node> nodes = new ArrayList<>();
		do {
			nodes.add(network.getNodes().get(currentNodeId));
			currentNodeId = plateauNodes.get(currentNodeId).getNextNodeId();
		} while (plateauNodes.get(currentNodeId).getNextNodeId() != null);
			
		return nodes;
	}

	private List<Link> getLinksFromNodes(List<Node> nodes) {
		List<Link> links = new ArrayList<>();
		
		for (int i = 0; i < nodes.size() - 1; i++) {
			Link link = NetworkUtils.getConnectingLink(nodes.get(i), nodes.get(i + 1));
			links.add(link);
		}
		
		return links;
	}

	private Map<Id<Node>, AdvancedNodeData> removePlateauNodes(	Map<Id<Node>, AdvancedNodeData> plateauNodes,
																Map<Id<Node>, AdvancedNodeData> plateauTree) {
		for (Id<Node> currentNodeId : plateauNodes.keySet()) {
			plateauTree.remove(currentNodeId);
		}
		return plateauTree;
	}

	private Map<Id<Node>, AdvancedNodeData> identifyPlateauNodes(Id<Node> nodeId, AdvancedNodeData nodeData, Map<Id<Node>, AdvancedNodeData> plateauTree) {
		Map<Id<Node>, AdvancedNodeData> plateauNodes = new HashMap<>();
		
		if (nodeData.getNextNodeId() != null) {
			plateauNodes.putAll(identifyNextPlateauNodes(nodeData.getNextNodeId(), plateauTree.get(nodeData.getNextNodeId()), plateauTree));
		}
		plateauNodes.put(nodeId, nodeData);
		if (nodeData.getPrevNodeId() != null) {
			plateauNodes.putAll(identifyPreviousPlateauNodes(nodeData.getPrevNodeId(), plateauTree.get(nodeData.getPrevNodeId()), plateauTree));
		}
		return plateauNodes;
	}
	
	private Map<Id<Node>, AdvancedNodeData> identifyNextPlateauNodes(Id<Node> nodeId, AdvancedNodeData nodeData, Map<Id<Node>, AdvancedNodeData> plateauTree) {
		
		Map<Id<Node>, AdvancedNodeData> plateauNodes = new HashMap<>();
		
		if (nodeData.getNextNodeId() != null) {
			plateauNodes.putAll(identifyNextPlateauNodes(nodeData.getNextNodeId(), plateauTree.get(nodeData.getNextNodeId()), plateauTree));
		}
		plateauNodes.put(nodeId, nodeData);
		return plateauNodes;
	}
	
	private Map<Id<Node>, AdvancedNodeData> identifyPreviousPlateauNodes(Id<Node> nodeId, AdvancedNodeData nodeData, Map<Id<Node>, AdvancedNodeData> plateauTree) {
		
		Map<Id<Node>, AdvancedNodeData> plateauNodes = new HashMap<>();
		
		plateauNodes.put(nodeId, nodeData);
		if (nodeData.getPrevNodeId() != null) {
			plateauNodes.putAll(identifyPreviousPlateauNodes(nodeData.getPrevNodeId(), plateauTree.get(nodeData.getPrevNodeId()), plateauTree));
		}
		return plateauNodes;
	}

	private double calcTotalTravelTime(List<Link> links) {
		double totalTravelTime = 0.0;
		for (Link link : links) {
			double currentTravelTime = link.getLength() / link.getFreespeed();
			totalTravelTime += currentTravelTime;
		}
		return totalTravelTime;
	}

}
