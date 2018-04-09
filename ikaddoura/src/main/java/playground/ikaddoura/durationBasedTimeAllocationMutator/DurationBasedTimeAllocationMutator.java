package playground.ikaddoura.durationBasedTimeAllocationMutator;

import javax.inject.Provider;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.TimeAllocationMutatorConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripRouter;

class DurationBasedTimeAllocationMutator extends AbstractMultithreadedModule {  	
	private final Provider<TripRouter> tripRouterProvider;
	private final boolean affectingDuration;
	private final PlansConfigGroup.ActivityDurationInterpretation activityDurationInterpretation;
	private final double mutationRange;
  	
	public DurationBasedTimeAllocationMutator(Provider<TripRouter> tripRouterProvider, PlansConfigGroup plansConfigGroup, TimeAllocationMutatorConfigGroup timeAllocationMutatorConfigGroup, GlobalConfigGroup globalConfigGroup,
			final Population population) {
		super(globalConfigGroup);
		
		this.tripRouterProvider = tripRouterProvider;
		this.affectingDuration = timeAllocationMutatorConfigGroup.isAffectingDuration();
		this.activityDurationInterpretation = plansConfigGroup.getActivityDurationInterpretation();
		this.mutationRange = timeAllocationMutatorConfigGroup.getMutationRange();
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		PlanAlgorithm pmta;
		switch (this.activityDurationInterpretation) {
		case minOfDurationAndEndTime:
			throw new RuntimeException("Not yet implemented. Aborting...");
		default:
			pmta = new DurationBasedPlanMutateTimeAllocationSimplified(
					this.tripRouterProvider.get().getStageActivityTypes(), this.mutationRange, this.affectingDuration, MatsimRandom.getLocalInstance());
		}
		return pmta;	}

	
}
