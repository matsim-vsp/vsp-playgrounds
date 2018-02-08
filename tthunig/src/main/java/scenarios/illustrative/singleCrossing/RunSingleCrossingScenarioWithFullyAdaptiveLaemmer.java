package scenarios.illustrative.singleCrossing;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;

import signals.laemmer.model.LaemmerConfig.Regime;
import signals.laemmer.run.LaemmerMain;

/**
 * @author nkuehnel, tthunig, pschade
 */
public class RunSingleCrossingScenarioWithFullyAdaptiveLaemmer {
    private static final Logger log = Logger.getLogger(LaemmerMain.class);

    private static final boolean USE_LAEMMER = true;
    private static final Regime LAEMMER_REGIME = Regime.STABILIZING;
    
    private static final boolean VISUALIZE_WITH_OTFVIS = true;
    private static final boolean LOG_ENABLED = true;
    private static final boolean LIVE_ARRIVAL_RATES = true;
    private static final boolean STOCHASTIC_DEMAND = false;
    private static final boolean USE_LANES = true;
    private static final boolean GROUPED = true;
    private static final boolean TEMPORAL_CROWD = false;
    /** minimal green time in seconds */
    private static final int MIN_G = 5;
    
    public static void main(String[] args) {
    	Logger.getRootLogger().setLevel(Level.WARN);
    	if (USE_LAEMMER){    	
    		log.info("Running single crossing scenario with laemmer signals...");
    	} else {
    		log.info("Running single crossing scenario with fixed-time signals...");
    	}
        
        double flowNS = 180;
        double flowWE = 1450;
//        for (int i = 0; i <= 3600; i += 600) {
//        for (int i = 0; i <= 10; i++) {
//        	flowWE = i;
            ComplexSingleCrossingScenario complexSingleCrossingSc = new ComplexSingleCrossingScenario(flowNS, 0.0, flowWE, 0.15, USE_LAEMMER, LAEMMER_REGIME, VISUALIZE_WITH_OTFVIS, LOG_ENABLED, STOCHASTIC_DEMAND, USE_LANES, LIVE_ARRIVAL_RATES, GROUPED, MIN_G, TEMPORAL_CROWD);
			Controler singleCrossingScenario2Controler = complexSingleCrossingSc.defineControler();
			singleCrossingScenario2Controler.run();
//		 }
    }

}
