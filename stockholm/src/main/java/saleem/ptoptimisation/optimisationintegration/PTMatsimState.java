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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.opdyts.microstate.MATSimState;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

import floetteroed.opdyts.SimulatorState;
import floetteroed.utilities.math.Vector;
import saleem.ptoptimisation.utils.ScenarioHelper;

/**
 * Considering the "day-to-day" iterations of MATSim as the stages of a
 * discrete-time stochastic process, this class represents the state of this
 * process. This state consists of the plan choice sets of all agents, including
 * scores and information about the selected plan.
 * 
 * @author Mohammad Saleem
 * 
 * @see SimulatorState
 */
public class PTMatsimState extends MATSimState {

	private final TransitSchedule schedule;
	
	private final Vehicles vehicles;
	
	private final Scenario scenario;
	
	private final double occupancyScale;
	
	
	// -------------------- CONSTRUCTION --------------------

	/**
	 * Takes over a <em>deep copy</em> of the population's plans and a
	 * <em>reference</em> to the vector representation.
	 * 
	 * @param population
	 *            the current MATSim population
	 * @param vectorRepresentation
	 *            a real-valued vector representation of the current MATSim
	 *            state.
	 * @param scenario
	 *     The simulation scenario being executed, containing all relevant details.
	 	 * @param ptscehedule
	 *            The decision variable representing a variation of transit schedule.
	  * @param occupancyScale
	 *            Generally kept at 1.
	 */
	public PTMatsimState(final Population population,
			final Vector vectorRepresentation, final Scenario scenario, final PTSchedule ptscehedule, double occupancyScale) {
		super(population,vectorRepresentation);
		this.scenario=scenario;
		ScenarioHelper helper = new ScenarioHelper();
		this.schedule=helper.deepCopyTransitSchedule(ptscehedule.getPreSchedule());
		this.vehicles=helper.deepCopyVehicles(ptscehedule.getPreVehicles());
		this.occupancyScale=occupancyScale;
	}
	@Override
	public Vector getReferenceToVectorRepresentation() {
		final Vector occupancies = super.getReferenceToVectorRepresentation()
				.copy();
		occupancies.mult(this.occupancyScale);
		return occupancies;
	}

	// -------------------- HELPERS AND INTERNALS --------------------

	public void implementInSimulation() {
		ScenarioHelper helper = new ScenarioHelper();
		//The following code enables switching among decision variables and states.
		helper.removeEntireScheduleAndVehicles(scenario);//Removes all vehicle types, vehicles, stop facilities and transit lines from a transit schedule
		helper.addVehicles(scenario, vehicles);//Adds all vehicle types and vehicles from an updated stand alone vehicles object into the current scenario vehicles object
		helper.addTransitSchedule(scenario, schedule);//Add all stop facilities and transit lines from a stand alone updated transit schedule into the current scenario transit schedule
		super.implementInSimulation();
	
	}
}

