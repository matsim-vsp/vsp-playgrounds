/**
 *
 */
package org.matsim.contrib.pseudosimulation.mobsim;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.pseudosimulation.mobsim.transitperformance.TransitEmulator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * @author fouriep
 */
@Singleton
public class PSimProvider implements Provider<Mobsim> {

	// -------------------- MEMBERS --------------------

	@Inject
	private TravelTime travelTime;

	@Inject
	private TransitEmulator transitEmulator;

	@Inject
	private Scenario scenario;

	@Inject
	private EventsManager eventsManager;

	private final Map<Id<Person>, Plan> plansForPSim = new LinkedHashMap<>();

	private final Map<Id<Person>, Double> selectedUnchangedPlanScores = new LinkedHashMap<>();

	// --------------- BOOK-KEEPING OF WHICH PLANS TO EXECUTE ---------------

	public void registerAllSelectedPlansForPSim() {
		this.plansForPSim.clear();
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			this.plansForPSim.put(person.getId(), person.getSelectedPlan());
		}
	}

	public void keepOnlyChangedPlansForPSim() {
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

	public void reconstructScoreOfPlansNotInPSim() {
		for (Map.Entry<Id<Person>, Double> entry : this.selectedUnchangedPlanScores.entrySet()) {
			this.scenario.getPopulation().getPersons().get(entry.getKey()).getSelectedPlan().setScore(entry.getValue());
		}
	}

	// -------------------- IMPLEMENTATION OF Provider<Mobsim> --------------------

	@Override
	public Mobsim get() {
		return new PSim(this.scenario, this.eventsManager, this.plansForPSim.values(), this.travelTime,
				this.transitEmulator);
	}
}
