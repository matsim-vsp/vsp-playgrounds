package ch.ethz.matsim.ier.emulator;

import java.util.List;

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.utils.misc.Time;

import com.google.inject.Inject;

/**
 * This is an example implementation of SimulationEmulator. See the interface
 * for more information and comments inline for an explanation what is done
 * here.
 * 
 * @author shoerl
 */
public class FirstSimpleSimulationEmulator implements SimulationEmulator {
	@Inject
	public FirstSimpleSimulationEmulator() {
	}

	@Override
	public void emulate(Person person, Plan plan, EventsManager eventsManager) {
		List<? extends PlanElement> elements = plan.getPlanElements();

		double time = 0.0;

		for (int i = 0; i < elements.size(); i++) {
			PlanElement element = elements.get(i);

			boolean isFirstElement = i == 0;
			boolean isLastElement = i == elements.size() - 1;

			if (element instanceof Activity) {
				/*
				 * Simulating activities is quite straight-forward. Either they have an end time
				 * set or they have a maximum duration. It only gets a bit more complicated for
				 * public transit. In that case we would need to check if the next leg is a
				 * public transit trip and adjust the end time here according to the schedule.
				 */

				Activity activity = (Activity) element;

				if (!isFirstElement) {
					eventsManager.processEvent(new ActivityStartEvent(time, person.getId(), activity.getLinkId(),
							activity.getFacilityId(), activity.getType()));
				}

				if (!Time.isUndefinedTime(activity.getEndTime())) {
					time = Math.max(time, activity.getEndTime());
				} else {
					time += activity.getMaximumDuration();
				}

				if (!isLastElement) {
					eventsManager.processEvent(new ActivityEndEvent(time, person.getId(), activity.getLinkId(),
							activity.getFacilityId(), activity.getType()));
				}
			} else if (element instanceof Leg) {
				/*
				 * Same is true for legs. We need to generate departure and arrival events and
				 * everything in between.
				 */

				Leg leg = (Leg) element;

				Activity previousActivity = (Activity) elements.get(i - 1);
				Activity followingActivity = (Activity) elements.get(i + 1);

				eventsManager.processEvent(
						new PersonDepartureEvent(time, person.getId(), previousActivity.getLinkId(), leg.getMode()));

				// Here we currently only add a teleportaton event. For vehicular modes like car
				// or pt we would probably like to add more events here. (Like
				// PersonEntersVehicle). The important question is which events do we actually
				// need to create to make the scoring consistent.

				double travelTime = leg.getTravelTime();

				if (Time.isUndefinedTime(travelTime)) {
					// For some reason some legs don't have a proper travel time in equil scenario.
					// Not sure why, but I don't think it has to do with this whole setup here.
					travelTime = 3600.0;
				}

				time += travelTime;
				eventsManager.processEvent(
						new TeleportationArrivalEvent(time, person.getId(), leg.getRoute().getDistance()));

				eventsManager.processEvent(
						new PersonArrivalEvent(time, person.getId(), followingActivity.getLinkId(), leg.getMode()));
			}
		}
	}
}
