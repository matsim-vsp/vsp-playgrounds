package searchacceleration.examples.matsimdummy;

import org.matsim.contrib.opdyts.utils.TimeDiscretization;

import searchacceleration.LinkUsageAnalyzer;
import searchacceleration.LinkUsageListener;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RunExample {

	public static void main(String[] args) {

		/*
		 * Set up the convergence acceleration infrastructure.
		 * 
		 * All PSEUDOSIM-related functionality is in the LinkUsageAnalyzer.
		 */

		LinkUsageListener linkUsageListener = new LinkUsageListener(new TimeDiscretization(0, 3600, 24));
		LinkUsageAnalyzer linkUsageAnalyzer = new LinkUsageAnalyzer(linkUsageListener);

		/*
		 * Insert this into the controller and run the simulation. The
		 * DummyPlanStrategy makes (somehow...) sure that the re-planning
		 * decided in the LinkUsageAnalyzer is taken over into the MATSim
		 * re-planning.
		 */

		DummyController controler = new DummyController();
		controler.addEventHandler(linkUsageListener);
		controler.addIterationStartsListener(linkUsageAnalyzer);
		controler.setPlanStrategyThatOverridesAllOthers(new DummyPlanStrategy(linkUsageAnalyzer));
		controler.run();

	}

}
