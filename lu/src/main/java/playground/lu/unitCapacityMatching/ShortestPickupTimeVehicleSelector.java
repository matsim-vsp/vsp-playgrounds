package playground.lu.unitCapacityMatching;

import java.util.Collection;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.schedule.Task.TaskType;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import playground.lu.vehicleScheduling.VehicleAssignmentTools;

public class ShortestPickupTimeVehicleSelector implements VehicleSelector {
	private final static double LARGE_NUMBER = 100000;
	private final VehicleAssignmentTools vehicleAssignmentTools;

	public ShortestPickupTimeVehicleSelector(Network network, TravelTime travelTime, TravelDisutility travelDisutility,
			DrtTaskFactory taskFactory, DrtConfigGroup drtCfg) {
		vehicleAssignmentTools = new VehicleAssignmentTools(travelTime, taskFactory, drtCfg, network, travelDisutility);
	}

	@Override
	public Entry selectVehicleEntryForRequest(DrtRequest request, Collection<Entry> vehicleEntries,
			double timeOfTheDay) {
		if (vehicleEntries == null) {
			// No disposable vehicle in the zone at all
			return null;
		}

		if (vehicleEntries.isEmpty()) {
			// All disposable vehicles in the zone are used by other requests
			return null;
		}

		Link requestLink = request.getFromLink();
		double shortestTimeDistance = LARGE_NUMBER;
		Entry selectedVehicleEntry = null;
		boolean diversionRequired = false;

		for (Entry vEntry : vehicleEntries) {
			DvrpVehicle vehicle = vEntry.vehicle;
			double timeDistance = LARGE_NUMBER;
			boolean diversionRequiredForThisVehicle = false;

			Task currentTask = vehicle.getSchedule().getCurrentTask();

			// Case 1: idling vehicle (STAY)
			if (currentTask instanceof DrtStayTask) {
				Link vehicleLink = ((DrtStayTask) currentTask).getLink();
				timeDistance = vehicleAssignmentTools.calculateTravelTime(vehicleLink, requestLink, timeOfTheDay);
			}

			// Driving vehicle (DRIVE)
			if (currentTask instanceof DrtDriveTask) {
				TaskType currentTaskType = currentTask.getTaskType();
				if (currentTaskType.equals(EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE)
						&& currentTask.getTaskIdx() == vehicle.getSchedule().getTaskCount() - 2) {
					// Case 2: Rebalancing Vehicle
					Link vehicleLink = vEntry.start.link;
					timeDistance = vehicleAssignmentTools.calculateTravelTime(vehicleLink, requestLink, timeOfTheDay);
					diversionRequiredForThisVehicle = true;
				} else {
					// Case 3: Other driving vehicle (driving with/towards passenger)
					timeDistance = vehicleAssignmentTools.calculateTimeDistanceForBusyVehicle(vehicle, requestLink,
							timeOfTheDay);
				}
			}

			// Case 4: Stopping vehicle (STOP)
			if (currentTask instanceof DrtStopTask) {
				timeDistance = vehicleAssignmentTools.calculateTimeDistanceForBusyVehicle(vehicle, requestLink,
						timeOfTheDay);
			}

			if (timeDistance < shortestTimeDistance) {
				shortestTimeDistance = timeDistance;
				selectedVehicleEntry = vEntry;
				diversionRequired = diversionRequiredForThisVehicle;
			}
		}
		vehicleAssignmentTools.assignRequestToVehicle(selectedVehicleEntry, request, diversionRequired, timeOfTheDay);

		return selectedVehicleEntry;
	}

}
