package playground.lu.drtAnalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;

public class ZonalAvailabilityHandler implements ActivityStartEventHandler, ActivityEndEventHandler,
		MobsimAfterSimStepListener, AvailabilityAnalysisHandler {
	private final DrtZonalSystem zonalSystem;
	private final NeighbouringZoneIdentifier neighbouringZoneIdentifier;

	private final static double FACTOR_OF_NEIGHTBOURING_ZONES = 0.5; // value in range [0, 1]
	private final Map<DrtZone, MutableDouble> allDayAvailableTimeCount = new HashMap<>(); // 01:00 - 25:00 (24 hours)
	private final Map<DrtZone, MutableDouble> peakHourAvailableTimeCount = new HashMap<>(); // 06:00 - 09:00 (3 hours)
	private final Map<DrtZone, MutableInt> numVehiclesInZone = new HashMap<>();
	private final Map<DrtZone, Boolean> vehicleInNeighbouringZones = new HashMap<>();

	public ZonalAvailabilityHandler(DrtZonalSystem zonalSystem, NeighbouringZoneIdentifier neighbouringZoneIdentifier) {
		this.zonalSystem = zonalSystem;
		this.neighbouringZoneIdentifier = neighbouringZoneIdentifier;
		reset(0);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().equals("DrtStay")) {
			DrtZone zone = zonalSystem.getZoneForLinkId(event.getLinkId());
			numVehiclesInZone.get(zone).increment();
		}

	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals("DrtStay")) {
			DrtZone zone = zonalSystem.getZoneForLinkId(event.getLinkId());
			numVehiclesInZone.get(zone).decrement();
		}

	}

	@Override
	public void notifyMobsimAfterSimStep(@SuppressWarnings("rawtypes") MobsimAfterSimStepEvent event) {
		double time = event.getSimulationTime();
		if (time >= 3600 && time < 90000) {
			List<DrtZone> zonesWithGoodAvailability = new ArrayList<>();
			for (DrtZone zone : numVehiclesInZone.keySet()) {
				if (numVehiclesInZone.get(zone).intValue() > 0) {
					zonesWithGoodAvailability.add(zone);
				}
			}
			Set<DrtZone> zoneWithPartialAvailability = neighbouringZoneIdentifier
					.getZonesWithPartialAvailability(zonesWithGoodAvailability);

			for (DrtZone zone : zonesWithGoodAvailability) {
				allDayAvailableTimeCount.get(zone).increment();
				if (time >= 21600 && time < 32400) {
					peakHourAvailableTimeCount.get(zone).increment();
				}
			}

			for (DrtZone zone : zoneWithPartialAvailability) {
				allDayAvailableTimeCount.get(zone).add(FACTOR_OF_NEIGHTBOURING_ZONES);
				if (time >= 21600 && time < 32400) {
					peakHourAvailableTimeCount.get(zone).add(FACTOR_OF_NEIGHTBOURING_ZONES);
				}
			}
		}
	}

	@Override
	public void reset(int iteration) {
		for (DrtZone zone : zonalSystem.getZones().values()) {
			allDayAvailableTimeCount.put(zone, new MutableDouble());
			peakHourAvailableTimeCount.put(zone, new MutableDouble());
			numVehiclesInZone.put(zone, new MutableInt());
			vehicleInNeighbouringZones.put(zone, false);
		}
	}

	public Map<DrtZone, Double> getAllDayAvailabilityRate() {
		Map<DrtZone, Double> allDayAvailabilityRate = new HashMap<>();
		double denominator = 86400;
		for (DrtZone zone : zonalSystem.getZones().values()) {
			allDayAvailabilityRate.put(zone, allDayAvailableTimeCount.get(zone).doubleValue() / denominator);
		}
		return allDayAvailabilityRate;
	}

	public Map<DrtZone, Double> getPeakHourAvailabilityRate() {
		Map<DrtZone, Double> peakHourAvailabilityRate = new HashMap<>();
		double denominator = 10800;
		for (DrtZone zone : zonalSystem.getZones().values()) {
			peakHourAvailabilityRate.put(zone, peakHourAvailableTimeCount.get(zone).doubleValue() / denominator);
		}
		return peakHourAvailabilityRate;
	}
}
