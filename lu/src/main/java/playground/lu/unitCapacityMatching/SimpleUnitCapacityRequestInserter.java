package playground.lu.unitCapacityMatching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
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
	private final double maxEuclideanDistance;
	private final double patientienceTime;
	private final DrtScheduleInquiry scheduleInquiry;
	private final VehicleAssignmentTools vehicleAssignmentTools;
	private final DrtZonalSystem zonalSystem;
	private final double largeNumber = 10000000;

	private static final String NO_SUITABLE_VEHICLE_FOUND_CAUSE = "no_suitable_vehicle_found";

	public SimpleUnitCapacityRequestInserter(DrtConfigGroup drtCfg, Fleet fleet, EventsManager eventsManager,
			MobsimTimer mobsimTimer, DrtScheduleInquiry drtScheduleInquiry,
			VehicleAssignmentTools vehicleAssignmentTools, DrtZonalSystem zonalSystem) {
		this.drtCfg = drtCfg;
		this.fleet = fleet;
		this.eventsManager = eventsManager;
		this.mobsimTimer = mobsimTimer;
		this.scheduleInquiry = drtScheduleInquiry;
		this.vehicleAssignmentTools = vehicleAssignmentTools;
		this.zonalSystem = zonalSystem;

		// TODO if this matching algorithm is implemented, read these parameters from
		// the config file
		maxEuclideanDistance = 3000;
//		maxEuclideanDistance = 1000000;  // i.e. no max distance restriction for matching
		patientienceTime = 108000;
	}

	@Override
	public void scheduleUnplannedRequests(Collection<DrtRequest> unplannedRequests) {
		if (unplannedRequests.isEmpty()) {
			return;
		}

		double timeOfDay = mobsimTimer.getTimeOfDay();
		List<? extends DvrpVehicle> idleVehicles = fleet.getVehicles().values().stream().filter(scheduleInquiry::isIdle)
				.collect(Collectors.toList());

		Iterator<DrtRequest> reqIter = unplannedRequests.iterator();

		while (reqIter.hasNext() && !idleVehicles.isEmpty()) {
			DrtRequest request = reqIter.next();
			Link requestLink = request.getFromLink();
			DrtZone requestZone = zonalSystem.getZoneForLinkId(requestLink.getId());

			// Vehicle within the same zone or vehicle within certain distance are feasible
			// for the request (this setting avoid matching a request to a very far away
			// vehicle. This may reduce the overall waiting time, as it is likely that a
			// rebalancing vehicle will arrive soon)
			// TODO perhaps rewrite this into a shorter format?
			List<DvrpVehicle> feasibleVehicles = new ArrayList<>();
			for (DvrpVehicle vehicle : idleVehicles) {
				Link vehicleLink = ((DrtStayTask) vehicle.getSchedule().getCurrentTask()).getLink();
				DrtZone vehicleZone = zonalSystem.getZoneForLinkId(vehicleLink.getId());
				double euclideanDistance = NetworkUtils.getEuclideanDistance(requestLink.getCoord(),
						vehicleLink.getCoord());
				if (euclideanDistance <= maxEuclideanDistance || requestZone == vehicleZone) {
					feasibleVehicles.add(vehicle);
				}
			}

			// Find the closest vehicles from the feasible vehicle list
			double shortestDistance = largeNumber;
			DvrpVehicle selectedVehicle = null;
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
				vehicleAssignmentTools.assignIdlingVehicleToRequest(selectedVehicle, request, timeOfDay);
				// Notify MATSim that a request is scheduled
				eventsManager.processEvent(new PassengerRequestScheduledEvent(timeOfDay, drtCfg.getMode(),
						request.getId(), request.getPassengerId(), selectedVehicle.getId(),
						request.getPickupTask().getEndTime(), request.getDropoffTask().getBeginTime()));

				// Remove the request and the vehicle from their respective collections
				idleVehicles.remove(selectedVehicle);
				reqIter.remove();
			} else if (timeOfDay > request.getSubmissionTime() + patientienceTime || timeOfDay >= 108000) {
				eventsManager.processEvent(new PassengerRequestRejectedEvent(timeOfDay, drtCfg.getMode(),
						request.getId(), request.getPassengerId(), NO_SUITABLE_VEHICLE_FOUND_CAUSE));
				log.debug("No suitable vehicle found for drt request " + request + " from passenger id="
						+ request.getPassengerId() + " fromLinkId=" + request.getFromLink().getId()
						+ ". Therefore the request is rejected!");
				reqIter.remove();
			}

		}

	}

}
