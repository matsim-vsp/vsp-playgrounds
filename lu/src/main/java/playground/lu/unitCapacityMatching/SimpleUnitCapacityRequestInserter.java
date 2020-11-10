package playground.lu.unitCapacityMatching;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.scheduler.DrtScheduleInquiry;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.network.NetworkUtils;

import playground.lu.vehicleAssignment.VehicleAssignmentTools;

public class SimpleUnitCapacityRequestInserter implements UnplannedRequestInserter {
	private static final Logger log = Logger.getLogger(SimpleUnitCapacityRequestInserter.class);

	private final DrtConfigGroup drtCfg;
	private final Fleet fleet;
	private final EventsManager eventsManager;
	private final MobsimTimer mobsimTimer;
	private final DrtZonalSystem zonalSystem;
	private final double maxWaitTime;
	private final double maxEuclideanDistance;
	private final DrtScheduleInquiry scheduleInquiry;
	private final VehicleAssignmentTools vehicleAssignmentTools;

	private static final String NO_SUITABLE_VEHICLE_FOUND_CAUSE = "no_suitable_vehicle_found";

	public SimpleUnitCapacityRequestInserter(DrtConfigGroup drtCfg, Fleet fleet, EventsManager eventsManager,
			MobsimTimer mobsimTimer, DrtZonalSystem zonalSystem, DrtScheduleInquiry drtScheduleInquiry,
			VehicleAssignmentTools vehicleAssignmentTools) {
		this.drtCfg = drtCfg;
		this.fleet = fleet;
		this.eventsManager = eventsManager;
		this.mobsimTimer = mobsimTimer;
		this.zonalSystem = zonalSystem;
		this.scheduleInquiry = drtScheduleInquiry;
		this.vehicleAssignmentTools = vehicleAssignmentTools;

		maxWaitTime = drtCfg.getMaxWaitTime();
		maxEuclideanDistance = 3000;

	}

	@Override
	public void scheduleUnplannedRequests(Collection<DrtRequest> unplannedRequests) {
		if (unplannedRequests.isEmpty()) {
			return;
		}

		List<? extends DvrpVehicle> idleVehicles = fleet.getVehicles().values().stream().filter(scheduleInquiry::isIdle)
				.collect(Collectors.toList());

		Iterator<DrtRequest> reqIter = unplannedRequests.iterator();

		while (reqIter.hasNext() && !idleVehicles.isEmpty()) {
			double shortestDistance = maxEuclideanDistance;
			DvrpVehicle selectedVehicle = null;

			DrtRequest request = reqIter.next();
			Link requestLink = request.getFromLink();
			for (DvrpVehicle vehicle : idleVehicles) {
				Link vehicleLink = ((DrtStayTask) vehicle.getSchedule().getCurrentTask()).getLink();
				double euclideanDistance = NetworkUtils.getEuclideanDistance(requestLink.getCoord(),
						vehicleLink.getCoord());
				if (euclideanDistance < shortestDistance) {
					shortestDistance = euclideanDistance;
					selectedVehicle = vehicle;
				}
			}

			if (selectedVehicle != null) {
				// Assign the vehicle to the request
				vehicleAssignmentTools.assignIdlingVehicleToRequest(selectedVehicle, request,
						mobsimTimer.getTimeOfDay());
				// Notify MATSim that a request is scheduled
				eventsManager.processEvent(new PassengerRequestScheduledEvent(mobsimTimer.getTimeOfDay(),
						drtCfg.getMode(), request.getId(), request.getPassengerId(), selectedVehicle.getId(),
						request.getPickupTask().getEndTime(), request.getDropoffTask().getBeginTime()));

				// Remove the request and the vehicle from their respective collections
				idleVehicles.remove(selectedVehicle);
				reqIter.remove();

			} else {
				eventsManager.processEvent(new PassengerRequestRejectedEvent(mobsimTimer.getTimeOfDay(),
						drtCfg.getMode(), request.getId(), request.getPassengerId(), NO_SUITABLE_VEHICLE_FOUND_CAUSE));
				log.debug("No suitable vehicle found for drt request " + request + " from passenger id="
						+ request.getPassengerId() + " fromLinkId=" + request.getFromLink().getId()
						+ ". Therefore the reuest is rejected!");
				reqIter.remove();
			}

		}

	}

}
