package scenarios.illustrative.singleCrossing;

import org.apache.log4j.Logger;
import org.matsim.contrib.signals.SignalSystemsConfigGroup.IntersectionLogic;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerConfigGroup.Regime;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerConfigGroup.StabilizationStrategy;

import scenarios.illustrative.braess.run.RunBraessSimulation;
import scenarios.illustrative.singleCrossing.SingleCrossingScenario.SignalControl;

/**
 * @author nkuehnel, tthunig
 */
public class RunSingleCrossingScenario {
	
	private static final Logger log = Logger.getLogger(RunBraessSimulation.class);

    private static final SingleCrossingScenario.SignalControl SIGNAL_CONTROL = SignalControl.FIXED;
    private static final Regime LAEMMER_REGIME = Regime.OPTIMIZING;
    
    private static final boolean VISUALIZE_WITH_OTFVIS = true;
    private static final boolean LIVE_ARRIVAL_RATES = true;
    private static final boolean STOCHASTIC_DEMAND = true;
    private static final boolean USE_LANES = true;
    private static final boolean GROUPED = true;
    private static final boolean TEMPORAL_CROWD = false;
    
    private static final StabilizationStrategy STABILIZATION_STRATEGY = StabilizationStrategy.USE_MAX_LANECOUNT;
    private static final IntersectionLogic INTERSECTION_LOGIC = IntersectionLogic.NONE;
    
    /** minimal green time in seconds */
    private static final int MIN_G = 5;
    
    public static void main(String[] args) {
    		log.info("Running single crossing scenario with signalcontrol "+SIGNAL_CONTROL.toString());
        double flowNS = 360;
        double flowWE = 1440;
//	        for (int i = 0; i <= 2520; i += 120) {
//	        	flowWE = i;
		SingleCrossingScenario scenarioBuilder = new SingleCrossingScenario(flowNS, flowWE, SIGNAL_CONTROL,
				LAEMMER_REGIME, STABILIZATION_STRATEGY, VISUALIZE_WITH_OTFVIS, STOCHASTIC_DEMAND,
				USE_LANES, LIVE_ARRIVAL_RATES, GROUPED, MIN_G, TEMPORAL_CROWD);
		scenarioBuilder.setIntersectionLogic(INTERSECTION_LOGIC);
		scenarioBuilder.defineControler().run();
//	        }
    }
}
