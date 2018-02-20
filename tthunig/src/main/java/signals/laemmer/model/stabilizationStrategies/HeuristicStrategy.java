package signals.laemmer.model.stabilizationStrategies;

import java.util.List;
import java.util.Queue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.lanes.data.Lanes;

import signals.laemmer.model.FullyAdaptiveLaemmerSignalController;
import signals.laemmer.model.LaemmerLane;
import signals.laemmer.model.LaemmerPhase;
import signals.laemmer.model.SignalPhase;
import signals.laemmer.model.util.SignalUtils;

public class HeuristicStrategy extends AbstractStabilizationStrategy {

	public HeuristicStrategy(FullyAdaptiveLaemmerSignalController fullyAdaptiveLaemmerSignalController, Network network, Lanes lanes) {
		super(fullyAdaptiveLaemmerSignalController, network, lanes);
	}

	@Override
	public LaemmerPhase determinePhase(Queue<LaemmerLane> lanesForStabilization, List<LaemmerPhase> laemmerPhases, boolean debug) {
		SignalPhase generatedPhase = new SignalPhase();
		SignalSystem signalSystem = getFullyAdaptiveLaemmerSignalController().getSystem();
		generatedPhase.addGreenSignalGroup(lanesForStabilization.peek().getSignalGroup());
		if (lanesForStabilization.size() > 1) {
			stabilisationLaneLoop:
			for (LaemmerLane laemmerLane : ((List<LaemmerLane>) lanesForStabilization).subList(1, lanesForStabilization.size())) {
				boolean addLane = true;
				for (Id<SignalGroup> presentSignalGroup : generatedPhase.getGreenSignalGroups()) {
					if (!SignalUtils.isConflictFreeCombination(signalSystem.getSignalGroups().get(presentSignalGroup), laemmerLane.getSignalGroup(), getNetwork(), getLanes())) {
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
				if(!SignalUtils.isConflictFreeCombination(systemSignalGroup, signalSystem.getSignalGroups().get(presentSignalGroupId), getNetwork(), getLanes())){
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
}
