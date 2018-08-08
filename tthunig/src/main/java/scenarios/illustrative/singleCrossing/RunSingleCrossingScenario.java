package scenarios.illustrative.singleCrossing;

import org.matsim.contrib.signals.SignalSystemsConfigGroup.IntersectionLogic;

import scenarios.illustrative.singleCrossing.SingleCrossingScenario.SignalControl;
import signals.laemmer.LaemmerConfig.Regime;
import signals.laemmer.LaemmerConfig.StabilizationStrategy;

/**
 * @author nkuehnel, tthunig
 */
public class RunSingleCrossingScenario {

    private static final SingleCrossingScenario.SignalControl SIGNAL_CONTROL = SignalControl.LAEMMER_FULLY_ADAPTIVE;
    private static final Regime LAEMMER_REGIME = Regime.OPTIMIZING;
    
    private static final boolean VISUALIZE_WITH_OTFVIS = true;
    private static final boolean LOG_ENABLED = true;
    private static final boolean LIVE_ARRIVAL_RATES = true;
    private static final boolean STOCHASTIC_DEMAND = true;
    private static final boolean USE_LANES = true;
    private static final boolean GROUPED = true;
    private static final boolean TEMPORAL_CROWD = false;
    
    private static final StabilizationStrategy STABILIZATION_STRATEGY = StabilizationStrategy.USE_MAX_LANECOUNT;
    private static final IntersectionLogic INTERSECTION_LOGIC = IntersectionLogic.CONFLICTING_DIRECTIONS_NO_TURN_RESTRICTIONS;
    
    /** minimal green time in seconds */
    private static final int MIN_G = 5;
    
    public static void main(String[] args) {
    	log.info("Running single crossing scenario with signalcontrol "+SIGNAL_CONTROL.toString());
        double flowNS = 360;
        double flowWE = 1440;
//	        for (int i = 0; i <= 2520; i += 120) {
//	        	flowWE = i;
		SingleCrossingScenario scenarioBuilder = new SingleCrossingScenario(flowNS, flowWE, SIGNAL_CONTROL,
				LAEMMER_REGIME, STABILIZATION_STRATEGY, VISUALIZE_WITH_OTFVIS, LOG_ENABLED, STOCHASTIC_DEMAND,
				USE_LANES, LIVE_ARRIVAL_RATES, GROUPED, MIN_G, TEMPORAL_CROWD);
		scenarioBuilder.setIntersectionLogic(INTERSECTION_LOGIC);
		scenarioBuilder.defineControler().run();
//	        }
    }
}
