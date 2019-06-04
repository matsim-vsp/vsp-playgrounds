package org.matsim.contrib.ier.emulator;

import java.util.List;

import org.matsim.api.core.v01.TransportMode;
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
 * @author Gunnar Flötteröd
 */
public class FirstSimpleSimulationEmulator implements SimulationEmulator {

	private final MatsimServices services;

	private final FifoTransitPerformance transitPerformance;

	@Inject
	public FirstSimpleSimulationEmulator(final MatsimServices services,
			final FifoTransitPerformance transitPerformance) {
		this.services = services;
		this.transitPerformance = transitPerformance;
	}

	@Override
	public void emulate(final Person person, final Plan plan, final EventsManager eventsManager) {
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
				time_s = (new RegularActivityEmulator(eventsManager)).emulateActivityAndReturnEndTime_s(activity,
						person, time_s, isFirstElement, isLastElement);

			} else if (element instanceof Leg) {

				/*
				 * Same is true for legs. We need to generate departure and arrival events and
				 * everything in between.
				 */

				final Leg leg = (Leg) element;
				final Activity previousActivity = (Activity) elements.get(i - 1);
				final Activity followingActivity = (Activity) elements.get(i + 1);

				final LegEmulator legEmulator;
				if (TransportMode.car.equals(leg.getMode())) {
					legEmulator = new CarLegEmulator(eventsManager, this.services.getScenario().getNetwork(),
							this.services.getLinkTravelTimes(), this.services.getScenario().getActivityFacilities());
				} else if (TransportMode.pt.equals(leg.getMode())) {
					legEmulator = new FifoTransitLegEmulator(eventsManager, this.transitPerformance,
							this.services.getScenario());
				} else {
					legEmulator = new OnlyDepartureArrivalLegEmulator(eventsManager,
							this.services.getScenario().getActivityFacilities());
				}
				time_s = legEmulator.emulateLegAndReturnEndTime_s(leg, person, previousActivity, followingActivity,
						time_s);
			}
		}
	}
}
