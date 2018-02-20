package signals.laemmer.model.stabilizationStrategies;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.Lanes;

import signals.laemmer.model.FullyAdaptiveLaemmerSignalController;
import signals.laemmer.model.LaemmerLane;
import signals.laemmer.model.LaemmerPhase;

public class MaxLaneCountStrategy extends AbstractStabilizationStrategy {

	public MaxLaneCountStrategy(FullyAdaptiveLaemmerSignalController fullyAdaptiveLaemmerSignalController, Network network, Lanes lanes) {
		super(fullyAdaptiveLaemmerSignalController, network, lanes);
	}

	@Override
	public LaemmerPhase determinePhase(Queue<LaemmerLane> lanesForStabilization, List<LaemmerPhase> laemmerPhases, boolean debug) {
		LaemmerPhase max = null;
		Map<LaemmerPhase, java.lang.Integer> stabilizationCandidatePhases = new HashMap<>();			
//		Stream<LaemmerPhase> candidatePhases = laemmerPhases.stream().sequential()
//				.filter(e -> e.phase.getGreenLanes().contains(stabilizeLane));
		
		List<LaemmerPhase> candidatePhases = new LinkedList<>();
		for (LaemmerPhase laemmerPhase : laemmerPhases) {
			if (laemmerPhase.getPhase().containsGreenLane(lanesForStabilization.peek().getLink().getId(), (lanesForStabilization.peek().getLane() == null ? null :lanesForStabilization.peek().getLane().getId()))) {
				candidatePhases.add(laemmerPhase);
			}
		}
		//calculate scores of each phase and memorize the highest score
		//TODO Decide what to do with two phases with equal score, pschade Jan'18
		int maxSelectionScore = 0;
		for (LaemmerPhase candPhase : candidatePhases) {
			//Phase get one scorepoint for each lane they set green
			int selectionScore = candPhase.getPhase().getNumberOfGreenLanes();
			for (Entry<Id<Link>, List<Id<Lane>>>  lanesToLink : candPhase.getPhase().getGreenLanesToLinks().entrySet())
				for (LaemmerLane stabilizationLaneCandidate : lanesForStabilization) {
					//TODO probabry nullcheck for lanesToLink.getValue() is needed, but unclear, what to do, is stabilizationLaneCandidate is not null but signalgroups lanes are
					if (stabilizationLaneCandidate.getLink().getId().equals(lanesToLink.getKey()) &&
							(stabilizationLaneCandidate.getLane() == null || lanesToLink.getValue().contains(stabilizationLaneCandidate.getLane().getId())))
						/*
						 * for lanes which can stabilized phases get 1000 scorepoints (1 above for
						 * containing the lane, 999 here. So it's always better to containing a lane
						 * which has to be stabilized, or an additional lane which should be stabilized,
						 * than containing any (realistic) number of (green signaled) lanes which should
						 * not. Only if two competing phases containing the same number of lanes the
						 * lane with more lanes in total should be selected. Since above the lanes are
						 * filtered to only that lanes containing the peek of stabilization queue, it
						 * isn't needed to add 999 scorepoints for it. Alternatively, the stabilization
						 * lanes can be ranked according to their position in the queue, so that its
						 * always better to select the phase containing a higher positioning lane,
						 * regardless of the number of lower-position lanes which also could be
						 * stabilized in a competing phase. pschade, Dec'17
						 */
						selectionScore += 999;
				}
			stabilizationCandidatePhases.put(candPhase, new java.lang.Integer(selectionScore));
			if (selectionScore > maxSelectionScore) {
				max = candPhase;
				maxSelectionScore = selectionScore;
			}
		}
		return max;
	}

}
