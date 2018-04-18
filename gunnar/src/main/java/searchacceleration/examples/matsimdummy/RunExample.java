/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */ 
package searchacceleration.examples.matsimdummy;

import floetteroed.utilities.TimeDiscretization;
import searchacceleration.ConstantReplanningParameters;
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
		LinkUsageAnalyzer linkUsageAnalyzer = new LinkUsageAnalyzer(linkUsageListener,
				new ConstantReplanningParameters(0.1, 1.0));

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
