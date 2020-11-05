package playground.lu.unitCapacityMatching;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import org.apache.log4j.Logger;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;

public class UnitCapcityZonalRequestInserter implements UnplannedRequestInserter {
	private static final Logger log = Logger.getLogger(UnitCapcityZonalRequestInserter.class);

	private final DrtConfigGroup drtCfg;
	private final Fleet fleet;
	private final EventsManager eventsManager;
	private final MobsimTimer mobsimTimer;
	private final DrtZonalSystem zonalSystem;
	private final double maxWaitTime;

	private static final String NO_SUITABLE_VEHICLE_FOUND_CAUSE = "no_suitable_vehicle_found";

	private final VehicleSelector vehicleSelector;
	private final VehicleData.EntryFactory vehicleDataEntryFactory;
	private final ForkJoinPool forkJoinPool;

	public UnitCapcityZonalRequestInserter(DrtConfigGroup drtCfg, Fleet fleet, EventsManager eventsManager,
			MobsimTimer mobsimTimer, DrtZonalSystem zonalSystem, VehicleSelector vehicleSelector,
			VehicleData.EntryFactory vehicleDataEntryFactory, ForkJoinPool forkJoinPool) {
		this.drtCfg = drtCfg;
		this.fleet = fleet;
		this.eventsManager = eventsManager;
		this.mobsimTimer = mobsimTimer;
		this.zonalSystem = zonalSystem;
		this.vehicleSelector = vehicleSelector;
		this.vehicleDataEntryFactory = vehicleDataEntryFactory;
		this.forkJoinPool = forkJoinPool;
		maxWaitTime = drtCfg.getMaxWaitTime();
	}

	@Override
	public void scheduleUnplannedRequests(Collection<DrtRequest> unplannedRequests) {
		if (unplannedRequests.isEmpty()) {
			return;
		}
		double timeOfTheDay = mobsimTimer.getTimeOfDay();

		VehicleData vData = new VehicleData(timeOfTheDay, fleet.getVehicles().values().stream(),
				vehicleDataEntryFactory, forkJoinPool);

		Map<DrtZone, List<Entry>> disposableVehicleEntriesPerZone = ZonalVehicleSelectionUtils
				.groupDisposableVehicleEntriesPerZone(zonalSystem, vData.getEntries(), timeOfTheDay, maxWaitTime);

		Iterator<DrtRequest> reqIter = unplannedRequests.iterator();
		while (reqIter.hasNext()) {
			DrtRequest request = reqIter.next();
			DrtZone requestZone = zonalSystem.getZoneForLinkId(request.getFromLink().getId());
			if (requestZone == null) {
				eventsManager.processEvent(
						new PassengerRequestRejectedEvent(mobsimTimer.getTimeOfDay(), drtCfg.getMode(),
								request.getId(), request.getPassengerId(), NO_SUITABLE_VEHICLE_FOUND_CAUSE));
				log.debug("No suitable vehicle found for drt request " + request + " from passenger id="
						+ request.getPassengerId() + " fromLinkId=" + request.getFromLink().getId()
						+ " because the request is outside the DRT zonal system");
				reqIter.remove();
				continue;
			}
			
			Entry selectedVehicleEntry = vehicleSelector.selectVehicleEntryForRequest(request,
					disposableVehicleEntriesPerZone.get(requestZone), mobsimTimer.getTimeOfDay());
			if (selectedVehicleEntry == null) {
				if (timeOfTheDay > request.getSubmissionTime() + maxWaitTime) {
					eventsManager.processEvent(
							new PassengerRequestRejectedEvent(mobsimTimer.getTimeOfDay(), drtCfg.getMode(),
									request.getId(), request.getPassengerId(), NO_SUITABLE_VEHICLE_FOUND_CAUSE));
					log.debug("No suitable vehicle found for drt request " + request + " from passenger id="
							+ request.getPassengerId() + " fromLinkId=" + request.getFromLink().getId()
							+ " after max wait time has passed. Therefore the reuest is rejected!");
					reqIter.remove();
				} else {
					log.debug("No suitable vehicle found for drt request " + request + " from passenger id="
							+ request.getPassengerId() + " at this moment. Will try agian at next time step");
				}
			} else {
				vData.updateEntry(selectedVehicleEntry.vehicle);
				disposableVehicleEntriesPerZone.get(requestZone).remove(selectedVehicleEntry);
				eventsManager
						.processEvent(new PassengerRequestScheduledEvent(mobsimTimer.getTimeOfDay(), drtCfg.getMode(),
								request.getId(), request.getPassengerId(), selectedVehicleEntry.vehicle.getId(),
								request.getPickupTask().getEndTime(), request.getDropoffTask().getBeginTime()));
				reqIter.remove();
			}

		}
	}

}
