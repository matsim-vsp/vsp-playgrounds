package org.matsim.contrib.pseudosimulation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MobSimSwitcher implements IterationEndsListener, IterationStartsListener, BeforeMobsimListener {

	@Inject
	private Scenario scenario;

	@Inject
	private Config config;

	private final Map<Id<Person>, Plan> plansForPSim = new LinkedHashMap<>();

	private final Map<Id<Person>, Double> selectedUnchangedPlanScores = new LinkedHashMap<>();

	private boolean isQSimIteration = true;

	// ---------------------- GETTERS ----------------------

	public boolean isQSimIteration() {
		return isQSimIteration;
	}

	public Collection<Plan> getPlansForPSim() {
		return this.plansForPSim.values();
	}

	// ---------------------- LISTENER IMPLEMENTATIONS ----------------------
	
	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		this.isQSimIteration = (event.getIteration()
				% ConfigUtils.addOrGetModule(this.config, PSimConfigGroup.class).getIterationsPerCycle() == 0);
		if (this.isQSimIteration) {
			Logger.getLogger(this.getClass()).warn("Running full queue simulation");
		} else {
			Logger.getLogger(this.getClass()).info("Running PSim");
			this.plansForPSim.clear();
			for (Person person : this.scenario.getPopulation().getPersons().values()) {
				this.plansForPSim.put(person.getId(), person.getSelectedPlan());
			}
		}
	}

	@Override
	public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
		if (!this.isQSimIteration()) {
			this.selectedUnchangedPlanScores.clear();
			for (Person person : this.scenario.getPopulation().getPersons().values()) {
				final Plan plan = person.getSelectedPlan();
				if (this.plansForPSim.get(plan.getPerson().getId()) == plan) {
					// Same plan as before; does not need to be evaluated again in pSim.
					this.plansForPSim.remove(plan.getPerson().getId());
					// Memorize this plan's score because leaving it out will mess up the scoring.
					this.selectedUnchangedPlanScores.put(plan.getPerson().getId(), plan.getScore());
				} else
					// A different plan than before; needs to be evaluated in pSim.
					this.plansForPSim.put(plan.getPerson().getId(), plan);
			}
		}
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if (!this.isQSimIteration()) {
			for (Map.Entry<Id<Person>, Double> entry : this.selectedUnchangedPlanScores.entrySet()) {
				this.scenario.getPopulation().getPersons().get(entry.getKey()).getSelectedPlan().setScore(entry.getValue());
			}
		}
	}
}
