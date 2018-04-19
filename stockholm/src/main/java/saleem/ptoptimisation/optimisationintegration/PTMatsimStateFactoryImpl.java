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

import floetteroed.opdyts.DecisionVariable;
import floetteroed.utilities.math.Vector;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.opdyts.MATSimState;
import org.matsim.contrib.opdyts.MATSimStateFactory;
import org.matsim.core.controler.Controler;

/**
 * A factory class to create PTMatSimState objects.
 * 
 * @author Mohammad Saleem
 *
 */
public class PTMatsimStateFactoryImpl<U extends DecisionVariable> implements
		MATSimStateFactory<U> {
	private Scenario scenario;
	final double occupancyScale;
	public PTMatsimStateFactoryImpl(Scenario scenario, final double occupancyScale) {
		 this.scenario = scenario;
		 this.occupancyScale=occupancyScale;
	}

	@Override
	public MATSimState newState(final Population population,
								final Vector stateVector, final U decisionVariable) {
		return new PTMatsimState(population, stateVector, scenario, (PTSchedule)decisionVariable, occupancyScale);
		
	}

	@Override
	public void registerControler(Controler controler) {
		// TODO Auto-generated method stub
		
	}

}
