package org.matsim.contrib.ier;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.ier.emulator.AgentEmulator;
import org.matsim.contrib.ier.emulator.SimulationEmulator;
import org.matsim.contrib.ier.replannerselection.ReplannerSelector;
import org.matsim.contrib.ier.replannerselection.ReplannerSelector.IEREventHandlerProvider;
import org.matsim.contrib.ier.run.IERConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunctionFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * This class replaces the standard MATSim replanning. It fullfills a number of
 * tasks:
 * 
 * <ul>
 * <li>For each agent, perform the MATSIm-configured replanning multiple times
 * to arrive at a "best-response" for the agent plan at the momentary system
 * state.</li>
 * <li>Select whether each agent should switch from the currently used plan to
 * the new optimized plan.</li>
 * </ul>
 * 
 * @author shoerl
 */
@Singleton
public final class IERReplanning implements PlansReplanning, ReplanningListener {
	private final static Logger logger = Logger.getLogger(IERReplanning.class);

	private final int numberOfEmulationThreads;

	private final IERConfigGroup ierConfig;

	private final Provider<ReplanningContext> replanningContextProvider;
	private final StrategyManager strategyManager;
	private final Scenario scenario;
	private final Provider<AgentEmulator> agentEmulatorProvider;
	private final Provider<SimulationEmulator> simulationEmulatorProvider;
	private final ReplannerSelector replannerSelector;
	private final ScoringFunctionFactory scoringFunctionFactory;

	@Inject
	IERReplanning(StrategyManager strategyManager, Scenario scenario,
			Provider<ReplanningContext> replanningContextProvider, Config config,
			Provider<AgentEmulator> agentEmulatorProvider, Provider<SimulationEmulator> simulationEmulatorProvider,
			ReplannerSelector replannerSelector, ScoringFunctionFactory scoringFunctionFactory) {
		this.strategyManager = strategyManager;
		this.scenario = scenario;
		this.replanningContextProvider = replanningContextProvider;
		this.numberOfEmulationThreads = config.global().getNumberOfThreads();
		this.ierConfig = ConfigUtils.addOrGetModule(config, IERConfigGroup.class);
		this.agentEmulatorProvider = agentEmulatorProvider;
		this.simulationEmulatorProvider = simulationEmulatorProvider;
		this.replannerSelector = replannerSelector;
		this.scoringFunctionFactory = scoringFunctionFactory;
	}

	public void notifyReplanning(ReplanningEvent event) {
		try {

			// this.replannerSelector.beforeReplanning();

			final IEREventHandlerProvider handlerForLastReplanningIterationProvider = this.replannerSelector
					.beforeReplanningAndGetEventHandlerProvider();
			final IEREventHandlerProvider handlerForOtherReplanningIterationsProvider = new IEREventHandlerProvider() {
				@Override
				public EventHandler get(Set<Id<Person>> personIds) {
					return new EventHandler() {
					};
				}
			};

			final ReplanningContext replanningContext = this.replanningContextProvider.get();

			for (int i = 0; i < this.ierConfig.getIterationsPerCycle(); i++) {
				logger.info(String.format("Started replanning iteration %d/%d", i + 1,
						this.ierConfig.getIterationsPerCycle()));

				// We run replanning on all agents (exactly as it is defined in the config)
				this.strategyManager.run(this.scenario.getPopulation(), replanningContext);

				final IEREventHandlerProvider currentEventHandlerProvider;
				if (i == this.ierConfig.getIterationsPerCycle() - 1) {
					currentEventHandlerProvider = handlerForLastReplanningIterationProvider;
				} else {
					currentEventHandlerProvider = handlerForOtherReplanningIterationsProvider;
				}

				if (this.ierConfig.isParallel()) {
					emulateInParallel(this.scenario.getPopulation(), event.getIteration(), currentEventHandlerProvider);
				} else {
					emulateSequentially(this.scenario.getPopulation(), event.getIteration(),
							currentEventHandlerProvider.get(this.scenario.getPopulation().getPersons().keySet()));
				}

				logger.info(String.format("Finished replanning iteration %d/%d", i + 1,
						this.ierConfig.getIterationsPerCycle()));
			}

			this.replannerSelector.afterReplanning();

		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private void emulateSequentially(Population population, int iteration, EventHandler eventHandler)
			throws InterruptedException {

		final EventsManager eventsManager = EventsUtils.createEventsManager();
		final EventsToScore eventsToScore = EventsToScore.createWithScoreUpdating(this.scenario,
				this.scoringFunctionFactory, eventsManager);
		eventsToScore.beginIteration(iteration);

		eventsManager.addHandler(eventHandler);
		eventsManager.resetHandlers(iteration);

		for (Person person : population.getPersons().values()) {
			this.simulationEmulatorProvider.get().emulate(person, person.getSelectedPlan(), eventsManager);
		}

		eventsManager.finishProcessing();
		eventsToScore.finish();
	}

	private void emulateInParallel(Population population, int iteration,
			final IEREventHandlerProvider eventHandlerProvider) throws InterruptedException {
		Iterator<? extends Person> personIterator = population.getPersons().values().iterator();
		List<Thread> threads = new LinkedList<>();

		long totalNumberOfPersons = population.getPersons().size();
		AtomicLong processedNumberOfPersons = new AtomicLong(0);
		AtomicBoolean finished = new AtomicBoolean(false);

		// Maybe there is a better way to do this... Turn logging off.
		Logger.getLogger("org.matsim").setLevel(Level.WARN);

		// Here we set up all the runner threads and start them
		for (int i = 0; i < this.numberOfEmulationThreads; i++) {
			Thread thread = new Thread(() -> {
				AgentEmulator agentEmulator = this.agentEmulatorProvider.get();

				Map<Id<Person>, Person> batch = new LinkedHashMap<>();
				// List<Person> batch = new LinkedList<>();
				// Set<Id<Person>> batchIds = new LinkedHashSet<>();

				do {
					batch.clear();

					// Here we create our batch
					synchronized (personIterator) {
						while (personIterator.hasNext() && batch.size() < this.ierConfig.getBatchSize()) {
							final Person person = personIterator.next();
							batch.put(person.getId(), person);
						}
					}

					final EventHandler eventHandler = eventHandlerProvider.get(batch.keySet());

					// And here we send all the agents to the emulator. The score will be written to
					// the plan directly.
					for (Person person : batch.values()) {
						agentEmulator.emulate(person, person.getSelectedPlan(), eventHandler);
					}

					processedNumberOfPersons.addAndGet(batch.size());
				} while (batch.size() > 0);
			});

			threads.add(thread);
			thread.start();
		}

		// We want one additional thread to track progress and output some information
		Thread progressThread = new Thread(() -> {
			long currentProcessedNumberOfPersons = 0;
			long lastProcessedNumberOfPersons = -1;

			while (!finished.get()) {
				try {
					currentProcessedNumberOfPersons = processedNumberOfPersons.get();

					if (currentProcessedNumberOfPersons > lastProcessedNumberOfPersons) {
						logger.info(String.format("Emulating... %d / %d (%.2f%%)", currentProcessedNumberOfPersons,
								totalNumberOfPersons, 100.0 * currentProcessedNumberOfPersons / totalNumberOfPersons));
					}

					lastProcessedNumberOfPersons = currentProcessedNumberOfPersons;

					Thread.sleep(10);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});

		progressThread.start();

		// Wait for all the runners to finish
		for (Thread thread : threads) {
			thread.join();
		}

		// Wait for the progress thread to finish
		finished.set(true);
		progressThread.join();

		logger.info("Emulation finished.");

		// Maybe there is a better way to do this... Turn logging on.
		Logger.getLogger("org.matsim").setLevel(null);
	}
}
