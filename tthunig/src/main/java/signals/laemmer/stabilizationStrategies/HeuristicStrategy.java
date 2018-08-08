package signals.laemmer.stabilizationStrategies;

import java.util.List;
import java.util.Queue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.lanes.Lanes;

import signals.laemmer.FullyAdaptiveLaemmerSignalController;
import signals.laemmer.LaemmerApproach;
import signals.laemmer.LaemmerPhase;
import signals.laemmer.SignalCombinationBasedOnConflicts;
import signals.laemmer.SignalPhase;

public class HeuristicStrategy extends AbstractStabilizationStrategy {
	
	private SignalCombinationBasedOnConflicts signalCombinationConflicts;

	public HeuristicStrategy(FullyAdaptiveLaemmerSignalController fullyAdaptiveLaemmerSignalController, Network network, Lanes lanes) {
		super(fullyAdaptiveLaemmerSignalController, network, lanes);
	}

	@Override
	public LaemmerPhase determinePhase(Queue<LaemmerApproach> lanesForStabilization, List<LaemmerPhase> laemmerPhases, boolean debug) {
		SignalPhase generatedPhase = new SignalPhase();
		SignalSystem signalSystem = getFullyAdaptiveLaemmerSignalController().getSystem();
		generatedPhase.addGreenSignalGroup(lanesForStabilization.peek().getSignalGroup());
		if (lanesForStabilization.size() > 1) {
			stabilisationLaneLoop:
			for (LaemmerApproach laemmerLane : ((List<LaemmerApproach>) lanesForStabilization).subList(1, lanesForStabilization.size())) {
				boolean addLane = true;
				for (Id<SignalGroup> presentSignalGroup : generatedPhase.getGreenSignalGroups()) {
					if (!signalCombinationConflicts.isConflictFreeCombination(presentSignalGroup, laemmerLane.getSignalGroup().getId())) {
						addLane = false;
						continue stabilisationLaneLoop;
					}
				}
				if (addLane) {
					generatedPhase.addGreenSignalGroup(laemmerLane.getSignalGroup());
				}
			}
		}
		//add non-stabilizing signal groups if non-conflicting
		//TODO add option to add nonPriorityAllowedConflicts
		allSignalGroups: for (SignalGroup systemSignalGroup : signalSystem.getSignalGroups().values()) {
			boolean addSg = true;
			for (Id<SignalGroup> presentSignalGroupId : generatedPhase.getGreenSignalGroups()) {
				if (systemSignalGroup.getId().equals(presentSignalGroupId))
					continue allSignalGroups;
				if(!signalCombinationConflicts.isConflictFreeCombination(systemSignalGroup.getId(), presentSignalGroupId)){
					addSg=false;
					continue allSignalGroups;
				}
			}
			if(addSg)
				generatedPhase.addGreenSignalGroup(systemSignalGroup);
		}
		if (debug) {
			//TODO remove debug output
			System.out.print("generated Phase: ");
			for (Id<SignalGroup> sg : generatedPhase.getGreenSignalGroups()) {
				System.out.print(sg + ", ");
			}
			System.out.print("\n");
		}
		return new LaemmerPhase(getFullyAdaptiveLaemmerSignalController(), generatedPhase);
	}
	
	public void setSignalCombinationTool(SignalCombinationBasedOnConflicts signalCombinationConflicts) {
		this.signalCombinationConflicts = signalCombinationConflicts;
	}
}
