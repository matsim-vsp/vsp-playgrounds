package signals.laemmer.model.stabilizationStrategies;

import org.matsim.api.core.v01.network.Network;
import org.matsim.lanes.Lanes;
import signals.laemmer.model.FullyAdaptiveLaemmerSignalController;
import signals.laemmer.model.LaemmerApproach;
import signals.laemmer.model.LaemmerPhase;

import java.util.List;
import java.util.Queue;

public abstract class AbstractStabilizationStrategy {
	
	private Lanes lanes;
	private Network network;
	private FullyAdaptiveLaemmerSignalController fullyAdaptiveLaemmerSignalController;

	public AbstractStabilizationStrategy(FullyAdaptiveLaemmerSignalController fullyAdaptiveLaemmerSignalController, Network network, Lanes lanes) {
		this.fullyAdaptiveLaemmerSignalController = fullyAdaptiveLaemmerSignalController;
		this.network = network;
		this.lanes = lanes;
	}
	
	public LaemmerPhase determinePhase(Queue<LaemmerApproach> lanesForStabilization, List<LaemmerPhase> laemmerPhases) {
		return determinePhase(lanesForStabilization, laemmerPhases, false);
	}

	public abstract LaemmerPhase determinePhase(Queue<LaemmerApproach> lanesForStabilization, List<LaemmerPhase> laemmerPhases, boolean debug);

	public FullyAdaptiveLaemmerSignalController getFullyAdaptiveLaemmerSignalController() {
		return this.fullyAdaptiveLaemmerSignalController;
	}
	
	public Network getNetwork() {
		return network;
	}
	
	public Lanes getLanes() {
		return lanes;
	}
}
