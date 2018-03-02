package signals.laemmer.model.stabilizationStrategies;

import java.util.LinkedList;
import java.util.List;
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

public class CombineSimilarRegulationTime extends AbstractStabilizationStrategy {

	public CombineSimilarRegulationTime(FullyAdaptiveLaemmerSignalController fullyAdaptiveLaemmerSignalController, Network network, Lanes lanes) {
		super(fullyAdaptiveLaemmerSignalController, network, lanes);
	}

	@Override
	public LaemmerPhase determinePhase(Queue<LaemmerLane> lanesForStabilization, List<LaemmerPhase> laemmerPhases, boolean debug) {
		LaemmerPhase max = null;
		double minRegulationTimeDifference = Double.POSITIVE_INFINITY;
//		Stream<LaemmerPhase> candidatePhases = laemmerPhases.stream().sequential()
//				.filter(e -> e.phase.getGreenLanes().contains(peekStabilizeLane));
		
		List<LaemmerPhase> candidatePhases = new LinkedList<>();
		for (LaemmerPhase laemmerPhase : laemmerPhases) {
			if (laemmerPhase.getPhase().containsGreenLane(lanesForStabilization.peek().getLink().getId(),
					(lanesForStabilization.peek().getLane() == null ? null : lanesForStabilization.peek().getLane().getId()))) {
				candidatePhases.add(laemmerPhase);
			}
		}
		for (LaemmerPhase candPhase : candidatePhases) {
			//Phase get one scorepoint for each lane they set green
			double regulationTimeDifferenceSum = 0;
			int laneCount = 0;
			for (Entry<Id<Link>, List<Id<Lane>>> laneToLink : candPhase.getPhase().getGreenLanesToLinks().entrySet())
				for (LaemmerLane stabilizationLaneCandidate : lanesForStabilization) {
					if (stabilizationLaneCandidate.equals(lanesForStabilization.peek())) {
						continue;
					}
					if (stabilizationLaneCandidate.getLink().getId().equals(laneToLink.getKey()) &&
							(stabilizationLaneCandidate.getLane() == null || laneToLink.getValue().contains(stabilizationLaneCandidate.getLane().getId()) )) {
						regulationTimeDifferenceSum += Math.abs(lanesForStabilization.peek().getRegulationTime()-stabilizationLaneCandidate.getRegulationTime());
						laneCount++;
					}
				}
			//determine the phase with the lowest average divergence
			if (regulationTimeDifferenceSum/laneCount < minRegulationTimeDifference) {
				minRegulationTimeDifference = regulationTimeDifferenceSum/laneCount;
				max = candPhase;
			}
		}
		if (max==null) {
			max = candidatePhases.get(0);
		}
		return max;
	}

}
