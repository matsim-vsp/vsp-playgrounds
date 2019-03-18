package ch.ethz.matsim.ier.emulator;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;

import com.google.inject.Inject;

/**
 * This is an example implementation of SimulationEmulator. See the interface
 * for more information and comments inline for an explanation what is done
 * here.
 * 
 * @author shoerl
 */
public class FirstSimpleSimulationEmulator implements SimulationEmulator {

	private final MatsimServices services;

	@Inject
	public FirstSimpleSimulationEmulator(MatsimServices services) {
		this.services = services;
	}

	@Override
	public void emulate(Person person, Plan plan, EventsManager eventsManager) {
		List<? extends PlanElement> elements = plan.getPlanElements();

		double time_s = 0.0;

		for (int i = 0; i < elements.size(); i++) {
			final PlanElement element = elements.get(i);

			final boolean isFirstElement = (i == 0);
			final boolean isLastElement = (i == (elements.size() - 1));

			if (element instanceof Activity) {
				
				/*
				 * Simulating activities is quite straight-forward. Either they have an end time
				 * set or they have a maximum duration. It only gets a bit more complicated for
				 * public transit. In that case we would need to check if the next leg is a
				 * public transit trip and adjust the end time here according to the schedule.
				 */

				final Activity activity = (Activity) element;
				time_s = (new ActivityEmulatorImpl(eventsManager)).emulateActivityAndReturnEndTime_s(activity, person,
						time_s, isFirstElement, isLastElement);

			} else if (element instanceof Leg) {
				
				/*
				 * Same is true for legs. We need to generate departure and arrival events and
				 * everything in between.
				 */

				final Leg leg = (Leg) element;
				final Activity previousActivity = (Activity) elements.get(i - 1);
				final Activity followingActivity = (Activity) elements.get(i + 1);
				time_s = (new CarLegEmulatorImpl(eventsManager, this.services.getScenario().getNetwork(), this.services.getLinkTravelTimes(), this.services.getScenario().getActivityFacilities()))
						.emulateLegAndReturnEndTime_s(leg, person, previousActivity, followingActivity, time_s);
			}
		}
	}
}
