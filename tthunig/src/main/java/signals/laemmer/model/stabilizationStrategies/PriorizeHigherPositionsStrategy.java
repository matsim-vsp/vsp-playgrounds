package signals.laemmer.model.stabilizationStrategies;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.management.RuntimeErrorException;

import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.Lanes;

import signals.laemmer.model.FullyAdaptiveLaemmerSignalController;
import signals.laemmer.model.LaemmerLane;
import signals.laemmer.model.LaemmerPhase;

public class PriorizeHigherPositionsStrategy extends AbstractStabilizationStrategy {

	public PriorizeHigherPositionsStrategy(FullyAdaptiveLaemmerSignalController fullyAdaptiveLaemmerSignalController, Network network, Lanes lanes) {
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
			if (selectionScore > 25) {
				throw new IllegalStateException("There is no guarantee that PriorizeHigherPositionsStrategy strategy will work with more than 25 aproaches to one single crossing. Revisit the code to make sure, that proirizing stabilizing lanes will always higher ranked than not-stabilizing lanes or choose another stabilisation strategy.");
			}
			for (Entry<Id<Link>, List<Id<Lane>>>  lanesToLink : candPhase.getPhase().getGreenLanesToLinks().entrySet())
				for (LaemmerLane stabilizationLaneCandidate : lanesForStabilization) {
					//TODO probably nullcheck for lanesToLink.getValue() is needed, but unclear, what to do, is stabilizationLaneCandidate is not null but signalgroups lanes are
					if (stabilizationLaneCandidate.getLink().getId().equals(lanesToLink.getKey()) &&
							(stabilizationLaneCandidate.getLane() == null || lanesToLink.getValue().contains(stabilizationLaneCandidate.getLane().getId())))
						/*
						 * for lanes which can stabilized phases get scorepoints according to the
						 * position of the lane in the queue. First position will get 1000 points,
						 * second position 500, third 250 and so on... So never a phase with lower queue
						 * positions will be selected if its possible to select a phase with more higher
						 * positions. For every Lane in the phase (regardless if its needs stabilization
						 * or not) the phase will get 1 point, so phases with more lanes will be
						 * preferred over phases with same lanes for stabilization but with lower number
						 * of lanes in total. Should safely work for crossings with ~ max. 25 lanes and
						 * probably much more, as long as a large number of them are not getting green
						 * together. pschade, Dec'17
						 */
						selectionScore += (8192*8192*16)/Math.pow(2, ((List<LaemmerLane>)lanesForStabilization).indexOf(stabilizationLaneCandidate));
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
