package scenarios.illustrative.singleCrossing;

import org.matsim.contrib.signals.SignalSystemsConfigGroup.IntersectionLogic;
import org.matsim.core.controler.Controler;

import scenarios.illustrative.singleCrossing.ComplexSingleCrossingScenario.SignalControl;
import signals.laemmerFix.LaemmerConfig.Regime;
import signals.laemmerFix.LaemmerConfig.StabilizationStrategy;

/**
 * @author nkuehnel, tthunig, pschade
 */
public class RunComplexSingleCrossingScenario {

    private static final ComplexSingleCrossingScenario.SignalControl SIGNAL_CONTROL = SignalControl.LAEMMER_NICO;
    private static final Regime LAEMMER_REGIME = Regime.COMBINED;
    private static final StabilizationStrategy STABILIZATION_STRATEGY = StabilizationStrategy.USE_MAX_LANECOUNT;
    private static final IntersectionLogic INTERSECTION_LOGIC = IntersectionLogic.CONFLICTING_DIRECTIONS_NO_TURN_RESTRICTIONS;
    private static final boolean VISUALIZE_WITH_OTFVIS = true;
    private static final boolean LOG_ENABLED = false;
    private static final boolean LIVE_ARRIVAL_RATES = true;
    private static final boolean STOCHASTIC_DEMAND = true;
    private static final boolean USE_LANES = true;
    private static final boolean GROUPED = true;
    private static final boolean TEMPORAL_CROWD = false;
    /** minimal green time in seconds */
    private static final int MIN_G = 5;
    
    public static void main(String[] args) {
        
        double flowNS = 360;
        double flowWE = 1800;
//	        for (int i = 0; i <= 2100; i += 60) {
//	        	flowWE = i;
	            ComplexSingleCrossingScenario complexSingleCrossingSc = new ComplexSingleCrossingScenario(flowNS, 0.0, flowWE, 0.16, SIGNAL_CONTROL, LAEMMER_REGIME, STABILIZATION_STRATEGY, VISUALIZE_WITH_OTFVIS, LOG_ENABLED, STOCHASTIC_DEMAND, USE_LANES, LIVE_ARRIVAL_RATES, GROUPED, MIN_G, TEMPORAL_CROWD);
				complexSingleCrossingSc.setIntersectionLogic(INTERSECTION_LOGIC);
	            Controler singleCrossingScenarioControler = complexSingleCrossingSc.defineControler();
				singleCrossingScenarioControler.run();
//			 }
    }
}
