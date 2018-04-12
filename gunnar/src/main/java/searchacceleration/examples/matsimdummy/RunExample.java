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

		TimeDiscretization timeDiscr = new TimeDiscretization(0, 3600, 24);
		LinkUsageListener linkUsageListener = new LinkUsageListener(timeDiscr);
		LinkUsageAnalyzer linkUsageAnalyzer = new LinkUsageAnalyzer(linkUsageListener);

		/*
		 * Insert this into the controller and run the simulation.
		 * 
		 * TODO: One needs to somehow take over the re-planning logic here (as
		 * explained further in DummyController.run().
		 */

		DummyController controler = new DummyController();
		controler.addEventHandler(linkUsageListener);
		controler.addIterationStartsListener(linkUsageAnalyzer);
		controler.run();

	}

}
