/*
 * Copyright 2018 Mohammad Saleem
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
 * contact: salee@kth.se
 *
 */ 
package saleem.ptoptimisation.optimisationintegration;

import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.opdyts.MATSimState;
import saleem.ptoptimisation.utils.ScenarioHelper;

/**
 * Returns the negative sum of the scores of the selected plans of all agents, 
 * while balancing for the cost of added vehicles or removed vehicles.
 * 
 * @author Mohammad Saleem
 *
 */
public class PTObjectiveFunction implements ObjectiveFunction {
	private String str = "";
	private int totalVeh = 36813;
	private Scenario scenario;
	public PTObjectiveFunction(Scenario scenario){
		this.scenario=scenario;
	}
	@Override
	public double value(SimulatorState state) {//Simple summation of selected plan scores
		double result = 0;
		// TODO Auto-generated method stub
		final MATSimState ptstate = (MATSimState) state;
		for (Id<Person> personId : ptstate.getPersonIdView()) {
			final Plan selectedPlan = ptstate
				.getSelectedPlan(personId);
			result -= selectedPlan.getScore();
		}
		int currenttotal = scenario.getTransitVehicles().getVehicles().size();
		ScenarioHelper helper = new ScenarioHelper();
		int unusedvehs = helper.getUnusedVehs(scenario.getTransitSchedule());//Vehicles that are not in use, alloted to routes which have been deleted.
		int added = currenttotal-totalVeh-unusedvehs;
		result /= ptstate.getPersonIdView().size();
		if(added>0){
			for(int i=0; i<added; i++){
				/*0.00045 is 4500 SEK per day divided by entire population of Stockholm, 4500 calculated by keeping cost of bus, 
				 * maintenance cost, parking cost, driver salaries, fuel cost etc. in mind.
				 */
				result = result + 0.00045;//Added vehicles have an adverse effect on total neg score
			}
		}
		else if (added<0){
			added=Math.abs(added);
			for(int i=0; i<added;i++){
				result = result - 0.00045;//Removed vehicles have a positive (neg score minimising) effect.
			}
		}
		return result;
	}
}
