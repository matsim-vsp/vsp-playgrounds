package scenarios.illustrative.singleCrossing;

import java.util.Arrays;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;

import scenarios.illustrative.singleCrossing.SingleCrossingScenario.SignalControl;
import signals.laemmer.model.LaemmerConfig.Regime;
import signals.laemmer.model.LaemmerConfig.StabilizationStrategy;
import signals.laemmer.run.LaemmerMain;

/**
 * @author nkuehnel, tthunig
 */
public class RunSingleCrossingScenario {
    private static final Logger log = Logger.getLogger(LaemmerMain.class);

    private static final SingleCrossingScenario.SignalControl SIGNAL_CONTROL = SignalControl.LAEMMER_NICO;
    private static final Regime LAEMMER_REGIME = Regime.OPTIMIZING;
    
    private static final boolean VISUALIZE_WITH_OTFVIS = false;
    private static final boolean LOG_ENABLED = true;
    private static final boolean LIVE_ARRIVAL_RATES = true;
    private static final boolean STOCHASTIC_DEMAND = true;
    private static final boolean USE_LANES = true;
    private static final boolean GROUPED = true;
    private static final boolean TEMPORAL_CROWD = false;
    private static final StabilizationStrategy STABILIZATION_STRATEGY = StabilizationStrategy.USE_MAX_LANECOUNT;
    
    /** minimal green time in seconds */
    private static final int MIN_G = 5;
    
    public static void main(String[] args) {
    	log.info("Running single crossing scenario with signalcontrol "+SIGNAL_CONTROL.toString());
        double flowNS = 360;
        double flowWE = 1440;
//        for (int strategyNum = 0; strategyNum < StabilizationStrategy.values().length; strategyNum++) {
//        	if (StabilizationStrategy.values()[strategyNum].equals(StabilizationStrategy.CUSTOM)) {
//        		continue;
//        	}
//        	StabilizationStrategy STABILIZATION_STRATEGY = StabilizationStrategy.values()[strategyNum];
	        for (int i = 0; i <= 2520; i += 120) {
	        	flowWE = i;
	            Controler singleCrossingScenario = new SingleCrossingScenario(flowNS, flowWE, SIGNAL_CONTROL, LAEMMER_REGIME, STABILIZATION_STRATEGY, VISUALIZE_WITH_OTFVIS, LOG_ENABLED, STOCHASTIC_DEMAND, USE_LANES, LIVE_ARRIVAL_RATES, GROUPED, MIN_G, TEMPORAL_CROWD).defineControler();
	            singleCrossingScenario.run();
	        }
//        }
    }
}
