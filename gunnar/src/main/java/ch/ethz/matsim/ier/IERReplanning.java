package ch.ethz.matsim.ier;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import ch.ethz.matsim.ier.emulator.AgentEmulator;
import ch.ethz.matsim.ier.replannerselection.ReplannerSelector;
import ch.ethz.matsim.ier.run.IERConfigGroup;

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

	/**
	 * Number of replanning + scoring iterations.
	 */
	private final int iterationsPerCycle;

	/**
	 * Number of threads on which the emulation of plans is happening. Currently, we
	 * get this from global.numberOfThreads
	 */
	private final int numberOfThreads;

	/**
	 * This is the number of agents that are simulated in a chunk on each thread. A
	 * value that is too small will slow down the emulation because of the overhead
	 * of creating the scoring functions. A value that is too high will lead to the
	 * situation where some threads may have a lot of "heavy" agents that take a lot
	 * of run time and some may have only "light" ones. This would also effectively
	 * increase runtime.
	 */
	private final int batchSize;

	private final Provider<ReplanningContext> replanningContextProvider;
	private final StrategyManager strategyManager;
	private final Population population;
	private final Provider<AgentEmulator> agentEmulatorProvider;
	private final ReplannerSelector replannerSelector;

	@Inject
	IERReplanning(StrategyManager strategyManager, Population population,
			Provider<ReplanningContext> replanningContextProvider, Config config,
			Provider<AgentEmulator> agentEmulatorProvider, ReplannerSelector replannerSelector) {
		this.population = population;
		this.strategyManager = strategyManager;
		this.replanningContextProvider = replanningContextProvider;
		this.numberOfThreads = config.global().getNumberOfThreads();
		{
			final IERConfigGroup ierConfig = ConfigUtils.addOrGetModule(config, IERConfigGroup.class);
			this.batchSize = ierConfig.getBatchSize();
			this.iterationsPerCycle = ierConfig.getIterationsPerCycle();
		}
		this.agentEmulatorProvider = agentEmulatorProvider;
		this.replannerSelector = replannerSelector;
	}

	public void notifyReplanning(ReplanningEvent event) {
		try {

			/*
			 * This handler observes the network experience of all agents with their new
			 * proposed plans (i.e. the result of the iterationsPerCycle re-planning
			 * iterations *prior* to the call to this.replannerSelector.afterReplanning().
			 * 
			 * Necessary events for car traffic: LinkEnterEvent, VehicleEntersTrafficEvent,
			 * VehicleLeavesTrafficEvent.
			 * 
			 * Necessary events for public transport: PersonEntersVehicleEvent.
			 * 
			 */
			final EventHandler handlerForLastReplanningIteration = this.replannerSelector
					.getHandlerForHypotheticalNetworkExperience();

			final EventHandler handlerForOtherReplanningIterations = new EventHandler() {
			};

			/*
			 * This memorizes the plans and network experiences of the population prior to
			 * the replanning.
			 */
			this.replannerSelector.beforeReplanning();

			final ReplanningContext replanningContext = this.replanningContextProvider.get();

			for (int i = 0; i < this.iterationsPerCycle; i++) {
				logger.info(String.format("Started replanning iteration %d/%d", i + 1, iterationsPerCycle));

				// We run replanning on all agents (exactly as it is defined in the config)
				this.strategyManager.run(this.population, replanningContext);

				final EventHandler currentEventHandler;
				if (i == this.iterationsPerCycle - 1) {
					currentEventHandler = handlerForLastReplanningIteration;
				} else {
					currentEventHandler = handlerForOtherReplanningIterations;
				}

				// We emulate the whole population in parallel
				emulateInParallel(this.population, event.getIteration(), currentEventHandler);

				logger.info(String.format("Finished replanning iteration %d/%d", i + 1, iterationsPerCycle));
			}

			/*
			 * Before the following call, feed the handlerForLastReplanningIteration.
			 * 
			 * afterReplanning() compares the plans and (hypothetical) network experiences
			 * of the population before and after the replanning and resets the plans of
			 * agents that are not allowed to replan to what was memorized in the call to
			 * beforeReplanning().
			 */
			this.replannerSelector.afterReplanning();

		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private void emulateInParallel(Population population, int iteration, EventHandler eventHandler)
			throws InterruptedException {
		Iterator<? extends Person> personIterator = population.getPersons().values().iterator();
		List<Thread> threads = new LinkedList<>();

		long totalNumberOfPersons = population.getPersons().size();
		AtomicLong processedNumberOfPersons = new AtomicLong(0);
		AtomicBoolean finished = new AtomicBoolean(false);

		// Maybe there is a better way to do this... Turn logging off.
		Logger.getLogger("org.matsim").setLevel(Level.WARN);

		// Here we set up all the runner threads and start them
		for (int i = 0; i < numberOfThreads; i++) {
			Thread thread = new Thread(() -> {
				AgentEmulator agentEmulator = agentEmulatorProvider.get();

				List<Person> batch = new LinkedList<>();

				do {
					batch.clear();

					// Here we create our batch
					synchronized (personIterator) {
						while (personIterator.hasNext() && batch.size() < batchSize) {
							batch.add(personIterator.next());
						}
					}

					// And here we send all the agents to the emulator. The score will be written to
					// the plan directly.
					for (Person person : batch) {
						agentEmulator.emulate(person, person.getSelectedPlan(), eventHandler);
					}

					processedNumberOfPersons.addAndGet(batch.size());
				} while (batch.size() > 0);

				agentEmulator.writeScores();
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
