package playground.vsp.cadyts.marginals;

import cadyts.demand.PlanBuilder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import playground.vsp.cadyts.marginals.prep.DistanceBin;

import java.util.HashMap;
import java.util.Map;

public class ModalDistancePlansTranslator implements PlansTranslator<Id<DistanceBin>> {

	private Map<Id<Person>, PlanBuilder<Id<DistanceBin>>> planBuilders = new HashMap<>();

	@Override
	public cadyts.demand.Plan<Id<DistanceBin>> getCadytsPlan(Plan plan) {
		if (planBuilders.containsKey(plan.getPerson().getId())) {
			return planBuilders.get(plan.getPerson().getId()).getResult();
		}
		return null;
	}

	void addTurn(Id<DistanceBin> turn, Id<Person> forPerson) {

		PlanBuilder<Id<DistanceBin>> planBuilder = planBuilders.computeIfAbsent(forPerson, key -> new PlanBuilder<>());
		planBuilder.addTurn(turn, 1);
	}

	void reset() {
		planBuilders.clear();
	}
}
