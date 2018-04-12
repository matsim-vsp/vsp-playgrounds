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
		return null;
	}

	public void executeNewPlans(final EventHandler eventHandler) {
	}

}
