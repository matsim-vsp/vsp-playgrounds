/**
 *
 */
package org.matsim.contrib.pseudosimulation.mobsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.pseudosimulation.mobsim.transitperformance.TransitEmulator;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
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

	@Inject
	private PlanCatcher plans;
	@Inject
	private TravelTime travelTime;
	@Inject
	private TransitEmulator transitEmulator;
	@Inject
	private Scenario scenario;
	@Inject
	private EventsManager eventsManager;

	@Override
	public Mobsim get() {
		return new PSim(this.scenario, this.eventsManager, this.plans.getPlansForPSim(), this.travelTime,
				this.transitEmulator);
	}
}
