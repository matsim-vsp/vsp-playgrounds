package unitCapacityMatching;

import java.util.Collection;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.schedule.Task.TaskType;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

public class ShortestPickupTimeVehicleSelector implements VehicleSelector {
	private final static double LARGE_NUMBER = 100000;
	private final LeastCostPathCalculator leastCostPathCalculator;
	private final TravelTime travelTime;

	public ShortestPickupTimeVehicleSelector(LeastCostPathCalculator leastCostPathCalculator, TravelTime travelTime) {
		this.leastCostPathCalculator = leastCostPathCalculator;
		this.travelTime = travelTime;
	}

	@Override
	public DvrpVehicle selectVehicleForRequest(DrtRequest request, Collection<Entry> vehicleEntries,
			double timeOfTheDay) {
		Link requestLink = request.getFromLink();
		DvrpVehicle selectedVehicle = null;

		double shortestTimeDistance = LARGE_NUMBER;
		for (Entry vEntry : vehicleEntries) {
			DvrpVehicle vehicle = vEntry.vehicle;
			double timeDistance = LARGE_NUMBER;
			Task currentTask = vehicle.getSchedule().getCurrentTask();

			// Case 1: idling vehicle (STAY)
			if (currentTask instanceof DrtStayTask) {
				Link vehicleLink = ((DrtStayTask) currentTask).getLink();
				timeDistance = calculateTravelTime(vehicleLink, requestLink, timeOfTheDay);
			}

			// Case 2: Driving vehicle (DRIVE)
			if (currentTask instanceof DrtDriveTask) {
				TaskType currentTaskType = currentTask.getTaskType();
				if (currentTaskType.equals(EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE)) {
					// Case 2.1: Rebalancing Vehicle
					Link vehicleLink = vEntry.start.link;
					timeDistance = calculateTravelTime(vehicleLink, requestLink, timeOfTheDay);
				} else {
					// Case 2.2: Other driving vehicle (driving with/towards passenger)
					timeDistance = calculateTimeDistanceForBusyVehicle(vehicle, requestLink, timeOfTheDay);
				}
			}

			// Case 3: Stopping vehicle (STOP)
			if (currentTask instanceof DrtStopTask) {
				timeDistance = calculateTimeDistanceForBusyVehicle(vehicle, requestLink, timeOfTheDay);
			}

			if (timeDistance < shortestTimeDistance) {
				shortestTimeDistance = timeDistance;
				selectedVehicle = vehicle;
			}
		}
		return selectedVehicle;
	}

	private double calculateTimeDistanceForBusyVehicle(DvrpVehicle vehicle, Link requestLink, double timeOfTheDay) {
		int finalTaskIndex = vehicle.getSchedule().getTaskCount() - 1;
		DrtStayTask finalStayTask = (DrtStayTask) vehicle.getSchedule().getTasks().get(finalTaskIndex);
		Link vehicleLink = finalStayTask.getLink();
		double additionalTIme = finalStayTask.getBeginTime() - timeOfTheDay;
		return calculateTravelTime(vehicleLink, requestLink, finalStayTask.getBeginTime()) + additionalTIme;
	}

	private double calculateTravelTime(Link fromLink, Link toLink, double timeOfTheDay) {
		VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(fromLink, toLink, timeOfTheDay, leastCostPathCalculator,
				travelTime);
		return path.getTravelTime();
	}

}
