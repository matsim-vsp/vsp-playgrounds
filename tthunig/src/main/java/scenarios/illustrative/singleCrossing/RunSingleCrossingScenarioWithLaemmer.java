package scenarios.illustrative.singleCrossing;

import org.apache.log4j.Logger;

import signals.laemmer.model.LaemmerConfig.Regime;
import signals.laemmer.run.LaemmerMain;

/**
 * @author nkuehnel, tthunig
 */
public class RunSingleCrossingScenarioWithLaemmer {
    private static final Logger log = Logger.getLogger(LaemmerMain.class);

    private static final boolean USE_LAEMMER = false;
    private static final Regime LAEMMER_REGIME = Regime.COMBINED;
    
    private static final boolean VIS = false;
    private static final boolean LOG_ENABLED = false;
    private static final boolean LIVE_ARRIVAL_RATES = true;
    private static final boolean STOCHASTIC_DEMAND = false;
    private static final boolean USE_LANES = true;
    private static final boolean GROUPED = true;
    private static final boolean TEMPORAL_CROWD = false;
    /** minimal green time in seconds */
    private static final int MIN_G = 0;
    
    public static void main(String[] args) {
    	if (USE_LAEMMER){    	
    		log.info("Running single crossing scenario with laemmer signals...");
    	} else {
    		log.info("Running single crossing scenario with fixed-time signals...");
    	}
        
        double flowNS = 360;
        double flowWE = 1800;
//        for (int i = 0; i <= 2520; i += 120) {
//        	flowWE = i;
            new SingleCrossingScenario(flowNS, flowWE, USE_LAEMMER, LAEMMER_REGIME, VIS, LOG_ENABLED, STOCHASTIC_DEMAND, USE_LANES, LIVE_ARRIVAL_RATES, GROUPED, MIN_G, TEMPORAL_CROWD).defineControler().run();
//        }
    }

}
