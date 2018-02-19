package signals.laemmer.model.stabilizationStrategies;

import java.util.List;
import java.util.Queue;

import org.matsim.api.core.v01.network.Network;
import org.matsim.lanes.data.Lanes;

import signals.laemmer.model.FullyAdaptiveLaemmerSignalController;
import signals.laemmer.model.LaemmerLane;
import signals.laemmer.model.LaemmerPhase;

public abstract class AbstractStabilizationStrategy {
	
	private Lanes lanes;
	private Network network;
	private FullyAdaptiveLaemmerSignalController fullyAdaptiveLaemmerSignalController;

	public AbstractStabilizationStrategy(FullyAdaptiveLaemmerSignalController fullyAdaptiveLaemmerSignalController, Network network, Lanes lanes) {
		this.fullyAdaptiveLaemmerSignalController = fullyAdaptiveLaemmerSignalController;
		this.network = network;
		this.lanes = lanes;
	}
	
	public LaemmerPhase determinePhase(Queue<LaemmerLane> lanesForStabilization, List<LaemmerPhase> laemmerPhases) {
		return determinePhase(lanesForStabilization, laemmerPhases, false);
	}

	public abstract LaemmerPhase determinePhase(Queue<LaemmerLane> lanesForStabilization, List<LaemmerPhase> laemmerPhases, boolean debug);

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