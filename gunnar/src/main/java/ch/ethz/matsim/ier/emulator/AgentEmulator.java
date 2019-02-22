package ch.ethz.matsim.ier.emulator;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.scoring.EventsToScore;
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
	private final EventsManager eventsManager;
	private final EventsToScore eventsToScore;
	private final SimulationEmulator simulationEmulator;
	private final int iteration;

	@Inject
	public AgentEmulator(Scenario scenario, ScoringFunctionFactory scoringFunctionFactory,
			SimulationEmulator simulationEmulator, ReplanningContext context) {
		this.eventsManager = EventsUtils.createEventsManager();

		// We do want to keep track of the (hypothetical) scores obtained in the
		// emulation. They form part of the performance expectation based on which the
		// decision who gets to replan or not is based.
		// this.eventsToScore = EventsToScore.createWithoutScoreUpdating(scenario,
		// scoringFunctionFactory, eventsManager);
		this.eventsToScore = EventsToScore.createWithScoreUpdating(scenario, scoringFunctionFactory, eventsManager);

		this.simulationEmulator = simulationEmulator;
		this.iteration = context.getIteration();

		this.eventsToScore.beginIteration(iteration);
	}

	/**
	 * Emulates an agent's plan given the SimulationEmulator that has been defined
	 * previously. In particular, the whole class encapsulates one iteration of
	 * scoring while events are generated per agent. The reason this works is that
	 * the scoring doesn't care about the timing of events of independent agents.
	 */
	public void emulate(Person person, Plan plan, EventHandler eventHandler) {
		eventsManager.resetHandlers(iteration);
		eventsManager.addHandler(eventHandler);
		simulationEmulator.emulate(person, plan, eventsManager);
		eventsManager.removeHandler(eventHandler);
		eventsManager.finishProcessing();
	}

	public void writeScores() {
		eventsToScore.finish();
	}
}
