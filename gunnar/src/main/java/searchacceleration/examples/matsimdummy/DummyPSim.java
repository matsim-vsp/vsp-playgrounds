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

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.events.handler.EventHandler;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class DummyPSim {

	public DummyPSim() {
	}

	public Map<Id<Person>, Plan> getNewPlanForAllAgents() {
		/*
		 * TODO Run the same plan strategies as defined in the original MATSim
		 * configuration.
		 */
		return null;
	}

	public void executePlans(final Map<Id<Person>, Plan> plans, final EventHandler eventHandler) {
		/*
		 * TODO Execute the plans in the PSim, feed the event handler.
		 */
	}

}
