package searchacceleration.examples.matsimdummy;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;

import searchacceleration.LinkUsageAnalyzer;

/**
 * This plan strategy does not perform actual re-planning. It only asks, for
 * every re-planning person, the {@link LinkUsageAnalyzer} (i) if the person is
 * allowed to re-plan and (ii) what new plan to assign to a re-planning person.
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
	public void run(final HasPlansAndId<Plan, Person> hasPlanAndId) {
		if (this.linkUsageAnalyzer.isAllowedToReplan(hasPlanAndId.getId())) {
			final Plan newPlan = this.linkUsageAnalyzer.getNewPlan(hasPlanAndId.getId());
			/*
			 * TODO Do everything that happens in the "regular" MATSim
			 * re-planning: Add newPlan to the choice set (if it is not yet
			 * already in there), make sure that the choice set does not exceed
			 * its maximum size, set newPlan as selected, ...
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
