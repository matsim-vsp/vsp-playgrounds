package signals.laemmer.model;

import java.util.ArrayList;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.conflicts.Direction;
import org.matsim.contrib.signals.data.conflicts.IntersectionDirections;
import org.matsim.contrib.signals.model.SignalSystem;

class LaemmerUtils {
	
	public static ArrayList<SignalPhase> removeRedundantSubPhases(ArrayList<SignalPhase> signalPhases) {
		ArrayList<SignalPhase> phasesToRemove = new ArrayList<>();
		for (SignalPhase signalPhase : signalPhases) {
			for (SignalPhase otherSignalPhase : signalPhases) {
				if (!signalPhase.equals(otherSignalPhase) && signalPhase.getGreenSignalGroups().containsAll(otherSignalPhase.getGreenSignalGroups())){
					phasesToRemove.add(otherSignalPhase);
				}
			}
		}
		signalPhases.removeAll(phasesToRemove);
		return signalPhases;
	}

	/**
	 * Estimates the number of minimal needed phases in a signalSystem. Will not return accurate results...
	 *  - if two links, but not opposite links, have separate signalized left-turning lanes
	 *  - probably for crossings with more than four links
	 *  - and probably other non-common crossing layouts.
	 */
	public static int estimateNumberOfPhases(SignalSystem signalSystem, Network network, SignalsData signalsData) {
		
		Map<Id<SignalSystem>, IntersectionDirections> conflicts = signalsData.getConflictingDirectionsData().getConflictsPerSignalSystem();
		if (!conflicts.containsKey(signalSystem.getId())) {
			throw new RuntimeException("The adaptive signals implemented in FullyAdaptiveLaemmerSignalController do not work when no conflicting information is given for the signalized intersection.");
		}
		
		double numOfPhases = 2.;
		IntersectionDirections directions = conflicts.get(signalSystem.getId());
		Node intersectionNode = network.getLinks().get(signalSystem.getSignals().values().iterator().next().getLinkId()).getToNode();
		OUTERLOOP:
		for (Id<Link> inLink : intersectionNode.getInLinks().keySet()) {
			for (Id<Link> outLink : intersectionNode.getOutLinks().keySet()) {
				Direction dir = directions.getDirection(inLink, outLink);
				if (dir == null) {
					// direction does not exist. continue with next outLink
					continue;
				}
				if ((dir.getDirectionsWhichMustYield() != null && !dir.getDirectionsWhichMustYield().isEmpty()) || 
						(dir.getDirectionsWithRightOfWay() != null && !dir.getDirectionsWithRightOfWay().isEmpty())) {
					/* the inlink has at least one direction that is specified as 'must yiel' or 'right of way'. 
					 * we assume this means that we do not need an additional phase for this inlinks signals, 
					 * because they show green together with traffic with other priorities 
					 */
					continue OUTERLOOP;
				}
			}
			/* no direction of this link has 'must yield' or 'right of way' conflicts. 
			 * we assume this means that left-turning traffic needs an additional phase 
			 * (probably together with oncoming traffic). 
			 */
			numOfPhases += 0.5;
		}
		return (int) Math.ceil(numOfPhases);
	}
}
