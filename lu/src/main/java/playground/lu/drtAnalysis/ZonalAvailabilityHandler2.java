package playground.lu.drtAnalysis;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.analysis.zonal.DrtZoneTargetLinkSelector;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;

public class ZonalAvailabilityHandler2 implements ActivityStartEventHandler, ActivityEndEventHandler,
		MobsimAfterSimStepListener, AvailabilityAnalysisHandler {
	private final DrtZonalSystem zonalSystem;
	private final DrtZoneTargetLinkSelector drtZoneTargetLinkSelector;
	private final Network network;

	private static final double MAX_SQUARED_EUCLIDEAN_DISTANCE = 9000000; // max euclidean distance = 3km (3 km * 3 km = 9 km^2)

	private final Map<DrtZone, MutableInt> allDayAvailableTimeCount = new HashMap<>(); // 01:00 - 25:00 (24 hours)
	private final Map<DrtZone, MutableInt> peakHourAvailableTimeCount = new HashMap<>(); // 06:00 - 09:00 (3 hours)
	private final Map<String, Coord> currentStatyTasks = new HashMap<>();
	private final Map<DrtZone, Coord> zoneCoordMap = new HashMap<>();

	public ZonalAvailabilityHandler2(DrtZonalSystem zonalSystem, DrtZoneTargetLinkSelector drtZoneTargetLinkSelector,
			Network network) {
		this.zonalSystem = zonalSystem;
		this.drtZoneTargetLinkSelector = drtZoneTargetLinkSelector;
		this.network = network;
		reset(0);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().equals("DrtStay")) {
			currentStatyTasks.put(event.getPersonId().toString(),
					network.getLinks().get(event.getLinkId()).getToNode().getCoord());
		}

	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals("DrtStay")) {
			currentStatyTasks.remove(event.getPersonId().toString());
		}

	}

	@Override
	public void notifyMobsimAfterSimStep(@SuppressWarnings("rawtypes") MobsimAfterSimStepEvent event) {
		double time = event.getSimulationTime();
		if (time >= 3600 && time < 90000) {
			for (DrtZone zone : zoneCoordMap.keySet()) {
				Coord zoneCoord = zoneCoordMap.get(zone);
				for (Coord stayTaskCoord : currentStatyTasks.values()) {
					double squareDistance = DistanceUtils.calculateSquaredDistance(zoneCoord, stayTaskCoord);
					if (squareDistance <= MAX_SQUARED_EUCLIDEAN_DISTANCE) {
						allDayAvailableTimeCount.get(zone).increment();
						if (time >= 21600 && time < 32400) {
							peakHourAvailableTimeCount.get(zone).increment();
						}
						break;
					}
				}
			}
		}

	}

	@Override
	public void reset(int iteration) {
		for (DrtZone zone : zonalSystem.getZones().values()) {
			allDayAvailableTimeCount.put(zone, new MutableInt());
			peakHourAvailableTimeCount.put(zone, new MutableInt());
			zoneCoordMap.put(zone, drtZoneTargetLinkSelector.selectTargetLink(zone).getToNode().getCoord());
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
