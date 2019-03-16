package ch.ethz.matsim.ier.emulator;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.PersonExperiencedActivity;
import org.matsim.core.scoring.PersonExperiencedLeg;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

import com.google.inject.Inject;

/**
 * This class has the purpose to emulate MATSim for a single agent's plan. All
 * the relevant parts to perform the emulation and the scoring are completely
 * encapsulated, such that each agent can be simulated without interference on
 * one independent thread.
 * 
 * @author shoerl
 */
public final class AgentEmulator {
	private final SimulationEmulator simulationEmulator;
	private final int iteration;

	private final ScoringFunctionFactory scoringFunctionFactory;
	private final Scenario scenario;

	@Inject
	public AgentEmulator(Scenario scenario, ScoringFunctionFactory scoringFunctionFactory,
			SimulationEmulator simulationEmulator, ReplanningContext context) {
		this.simulationEmulator = simulationEmulator;
		this.iteration = context.getIteration();
		this.scenario = scenario;
		this.scoringFunctionFactory = scoringFunctionFactory;
	}

	/**
	 * Emulates an agent's plan given the SimulationEmulator that has been defined
	 * previously. In particular, the whole class encapsulates one iteration of
	 * scoring while events are generated per agent. The reason this works is that
	 * the scoring doesn't care about the timing of events of independent agents.
	 */
	public void emulate(Person person, Plan plan, EventHandler eventHandler) {
		EventsManager eventsManager = EventsUtils.createEventsManager();

		EventsToActivities eventsToActivities = new EventsToActivities();
		EventsToLegs eventsToLegs = new EventsToLegs(scenario);

		eventsManager.addHandler(eventsToActivities);
		eventsManager.addHandler(eventsToLegs);
		eventsManager.addHandler(eventHandler);

		ScoringFunction scoringFunction = scoringFunctionFactory.createNewScoringFunction(person);
		ScoringFunctionWrapper wrapper = new ScoringFunctionWrapper(scoringFunction);

		eventsToActivities.addActivityHandler(wrapper);
		eventsToLegs.addLegHandler(wrapper);

		eventsManager.resetHandlers(iteration);

		simulationEmulator.emulate(person, plan, eventsManager);

		eventsManager.finishProcessing();
		eventsToActivities.finish();

		plan.setScore(scoringFunction.getScore());
	}

	static private class ScoringFunctionWrapper
			implements EventsToActivities.ActivityHandler, EventsToLegs.LegHandler, BasicEventHandler {
		private final ScoringFunction scoringFunction;

		public ScoringFunctionWrapper(ScoringFunction scoringFunction) {
			this.scoringFunction = scoringFunction;
		}

		@Override
		public void handleLeg(PersonExperiencedLeg leg) {
			scoringFunction.handleLeg(leg.getLeg());
		}

		@Override
		public void handleActivity(PersonExperiencedActivity activity) {
			scoringFunction.handleActivity(activity.getActivity());
		}

		@Override
		public void handleEvent(Event event) {
			// TODO: ScoringFunctionsForPopulation defines more logic here, which we do not
			// replicate right now.
		}
	}
}
