package searchacceleration.examples.matsimdummy;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;

import searchacceleration.LinkUsageAnalyzer;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class DummyPlanStrategy implements PlanStrategy {

	private final LinkUsageAnalyzer linkUsageAnalyzer;

	public DummyPlanStrategy(final LinkUsageAnalyzer linkUsageAnalyzer) {
		this.linkUsageAnalyzer = linkUsageAnalyzer;
	}

	@Override
	public void run(HasPlansAndId<Plan, Person> hasPlanAndId) {
		if (this.linkUsageAnalyzer.isAllowedToReplan(hasPlanAndId.getId())) {
			final Plan newPlan = this.linkUsageAnalyzer.getNewPlan(hasPlanAndId.getId());
			/*
			 * TODO Add newPlan to the choice set (if it is not yet already in
			 * there), make sure that the choice set does not exceed its maximum
			 * size, set newPlan as selected.
			 */
		}
	}

	@Override
	public void init(ReplanningContext replanningContext) {
	}

	@Override
	public void finish() {
	}

}
