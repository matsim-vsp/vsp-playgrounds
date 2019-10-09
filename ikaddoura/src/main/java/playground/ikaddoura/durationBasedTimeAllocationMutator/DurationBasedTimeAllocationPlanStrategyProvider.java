package playground.ikaddoura.durationBasedTimeAllocationMutator;


import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.TimeAllocationMutatorConfigGroup;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

public class DurationBasedTimeAllocationPlanStrategyProvider implements Provider<PlanStrategy> {

	@Inject private GlobalConfigGroup globalConfigGroup;
	@Inject private TimeAllocationMutatorConfigGroup timeAllocationMutatorConfigGroup;
	@Inject private PlansConfigGroup plansConfigGroup;
	@Inject private Population population;

	@Override
	public PlanStrategy get() {
		PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<>());

		DurationBasedTimeAllocationMutator mod = new DurationBasedTimeAllocationMutator(
				this.plansConfigGroup, this.timeAllocationMutatorConfigGroup, this.globalConfigGroup, population);
		builder.addStrategyModule(mod);

		return builder.build();
	}

}
