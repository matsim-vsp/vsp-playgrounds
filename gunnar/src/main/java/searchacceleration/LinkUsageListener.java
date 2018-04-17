package searchacceleration;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.TimeDiscretization;
import searchacceleration.datastructures.SpaceTimeIndicators;

/**
 * Keeps track of when every single vehicle enters which link.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LinkUsageListener implements LinkEnterEventHandler, VehicleEntersTrafficEventHandler {

	// -------------------- MEMBERS --------------------

	private final TimeDiscretization timeDiscretization;

	private final Map<Id<Vehicle>, SpaceTimeIndicators<Id<Link>>> veh2indicators = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public LinkUsageListener(final TimeDiscretization timeDiscretization) {
		this.timeDiscretization = timeDiscretization;
	}

	// -------------------- INTERNALS --------------------

	private void registerLinkEntry(final Id<Link> linkId, final Id<Vehicle> vehicleId, final double time_s) {
		if ((time_s >= this.timeDiscretization.getStartTime_s()) && (time_s < this.timeDiscretization.getEndTime_s())) {
			SpaceTimeIndicators<Id<Link>> indicators = this.veh2indicators.get(vehicleId);
			if (indicators == null) {
				indicators = new SpaceTimeIndicators<Id<Link>>(this.timeDiscretization.getBinCnt());
				this.veh2indicators.put(vehicleId, indicators);
			}
			indicators.visit(linkId, this.timeDiscretization.getBin(time_s));
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	public TimeDiscretization getTimeDiscretization() {
		return this.timeDiscretization;
	}

	Map<Id<Vehicle>, SpaceTimeIndicators<Id<Link>>> getAndClearIndicators() {
		final Map<Id<Vehicle>, SpaceTimeIndicators<Id<Link>>> result = new LinkedHashMap<>(this.veh2indicators);
		this.veh2indicators.clear();
		return result;
	}

	// --------------- IMPLEMENTATION OF EventHandler INTERFACES ---------------

	@Override
	public void reset(int iteration) {
		if (this.veh2indicators.size() > 0) {
			throw new RuntimeException("veh2indicators should be empty");
		}
	}

	@Override
	public void handleEvent(final VehicleEntersTrafficEvent event) {
		this.registerLinkEntry(event.getLinkId(), event.getVehicleId(), event.getTime());
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		this.registerLinkEntry(event.getLinkId(), event.getVehicleId(), event.getTime());
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		System.out.println("Started ...");

		final Config config = ConfigUtils.loadConfig("./testdata/berlin_2014-08-01_car_1pct/config.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final Controler controler = new Controler(scenario);
		final TimeDiscretization timeDiscr = new TimeDiscretization(0, 3600, 24);
		final LinkUsageListener loa = new LinkUsageListener(timeDiscr);
		controler.getEvents().addHandler(loa);

		controler.run();

		System.out.println("... done.");
	}

}
