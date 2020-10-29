package playground.lu.vehicleAssignment;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

public class VehicleAssignmentTools {
	private final LeastCostPathCalculator leastCostPathCalculator;
	private final TravelTime travelTime;
	private final DrtTaskFactory taskFactory;
	private final double stopDuration;

	public VehicleAssignmentTools(LeastCostPathCalculator leastCostPathCalculator, TravelTime travelTime,
			DrtTaskFactory taskFactory, DrtConfigGroup drtCfg) {
		this.leastCostPathCalculator = leastCostPathCalculator;
		this.travelTime = travelTime;
		this.taskFactory = taskFactory;
		stopDuration = drtCfg.getStopDuration();
	}

	public void assignRequestToVehicle(Entry vehicleEntry, DrtRequest request, boolean diversionRequired,
			double timeOfTheDay) {
		DvrpVehicle vehicle = vehicleEntry.vehicle;
		if (diversionRequired) {
			// Stop current rebalance task and add request to the schedule
			// Step 1. Divert the vehicle to the request
			Schedule schedule = vehicle.getSchedule();
			Task currentTask = schedule.getStatus() == ScheduleStatus.PLANNED ? null : schedule.getCurrentTask();
			Task beforePickupTask;
			LinkTimePair diversion = ((OnlineDriveTaskTracker) currentTask.getTaskTracker()).getDiversionPoint();

			// Remove the final stay task
			schedule.removeLastTask();

			if (diversion != null) { // divert vehicle
				beforePickupTask = currentTask;
				VrpPathWithTravelData vrpPath = VrpPaths.calcAndCreatePath(vehicleEntry.start.link,
						request.getFromLink(), vehicleEntry.start.time, leastCostPathCalculator, travelTime);
				((OnlineDriveTaskTracker) beforePickupTask.getTaskTracker()).divertPath(vrpPath);
			} else { // too late for diversion
				if (request.getFromLink() != vehicleEntry.start.link) { // add a new drive task
					VrpPathWithTravelData vrpPath = VrpPaths.calcAndCreatePath(vehicleEntry.start.link,
							request.getFromLink(), vehicleEntry.start.time, leastCostPathCalculator, travelTime);
					beforePickupTask = taskFactory.createDriveTask(vehicleEntry.vehicle, vrpPath, DrtDriveTask.TYPE);
					schedule.addTask(currentTask.getTaskIdx() + 1, beforePickupTask);
				} else { // no need for a new drive task
					beforePickupTask = currentTask;
				}
			}

			// Step 2. Append Stop task (pick up) to the end
			double scheduledPickUpTime = beforePickupTask.getEndTime();
			DrtStopTask pickUpStopTask = taskFactory.createStopTask(vehicle, scheduledPickUpTime,
					Math.max(scheduledPickUpTime + stopDuration, request.getEarliestStartTime()),
					request.getFromLink());
			schedule.addTask(pickUpStopTask);
			pickUpStopTask.addPickupRequest(request);
			request.setPickupTask(pickUpStopTask);

			// Step 3. Append drive task to the end, if the departure link and arrival link
			// of the request is no the same
			double departureTime = pickUpStopTask.getEndTime();
			double scheduledArrivalTime = departureTime;
			if (request.getFromLink() != request.getToLink()) {
				VrpPathWithTravelData vrpPath = VrpPaths.calcAndCreatePath(request.getFromLink(), request.getToLink(),
						departureTime, leastCostPathCalculator, travelTime);
				DrtDriveTask driveCustomerToDestinationTask = taskFactory.createDriveTask(vehicle, vrpPath,
						DrtDriveTask.TYPE);
				schedule.addTask(driveCustomerToDestinationTask);
				scheduledArrivalTime = driveCustomerToDestinationTask.getEndTime();
			}

			// Step 4. Append Stop task (drop off) to the end
			DrtStopTask dropOffStopTask = taskFactory.createStopTask(vehicle, scheduledArrivalTime,
					scheduledArrivalTime + stopDuration, request.getToLink());
			schedule.addTask(dropOffStopTask);
			dropOffStopTask.addDropoffRequest(request);
			request.setDropoffTask(dropOffStopTask);

			// Step 5. Append new final stay task to the end
			double newStayTaskStartTime = dropOffStopTask.getEndTime();
			DrtStayTask newFinalStayTask = taskFactory.createStayTask(vehicle, newStayTaskStartTime,
					vehicle.getServiceEndTime(), dropOffStopTask.getLink());
			schedule.addTask(newFinalStayTask);

		} else {
			// Adding new request to the end of the current schedule
			// Step 1. Set end time to the final stay task to timeOfTheDay + 1 (i.e. next
			// second)
			Schedule schedule = vehicle.getSchedule();
			int finalTaskIndex = schedule.getTaskCount() - 1;
			DrtStayTask finalStayTask = (DrtStayTask) schedule.getTasks().get(finalTaskIndex);
			double stayTaskEndTime = timeOfTheDay + 1;
			finalStayTask.setEndTime(stayTaskEndTime);

			// Step 2. Append Drive Task to the end, if the final stay task is not the
			// departure link of the request
			double scheduledPickUpTime = stayTaskEndTime;
			if (finalStayTask.getLink() != request.getFromLink()) {
				VrpPathWithTravelData vrpPath = VrpPaths.calcAndCreatePath(finalStayTask.getLink(),
						request.getFromLink(), stayTaskEndTime, leastCostPathCalculator, travelTime);
				DrtDriveTask driveToPassengerTask = taskFactory.createDriveTask(vehicle, vrpPath, DrtDriveTask.TYPE);
				schedule.addTask(driveToPassengerTask);
				scheduledPickUpTime = driveToPassengerTask.getEndTime();
			}

			// Step 3. Append Stop task (pick up) to the end
			DrtStopTask pickUpStopTask = taskFactory.createStopTask(vehicle, scheduledPickUpTime,
					Math.max(scheduledPickUpTime + stopDuration, request.getEarliestStartTime()),
					request.getFromLink());
			schedule.addTask(pickUpStopTask);
			pickUpStopTask.addPickupRequest(request);
			request.setPickupTask(pickUpStopTask);

			// Step 4. Append drive task to the end, if the departure link and arrival link
			// of the request is no the same
			double departureTime = pickUpStopTask.getEndTime();
			double scheduledArrivalTime = departureTime;
			if (request.getFromLink() != request.getToLink()) {
				VrpPathWithTravelData vrpPath = VrpPaths.calcAndCreatePath(request.getFromLink(), request.getToLink(),
						departureTime, leastCostPathCalculator, travelTime);
				DrtDriveTask driveCustomerToDestinationTask = taskFactory.createDriveTask(vehicle, vrpPath,
						DrtDriveTask.TYPE);
				schedule.addTask(driveCustomerToDestinationTask);
				scheduledArrivalTime = driveCustomerToDestinationTask.getEndTime();
			}

			// Step 5. Append Stop task (drop off) to the end
			DrtStopTask dropOffStopTask = taskFactory.createStopTask(vehicle, scheduledArrivalTime,
					scheduledArrivalTime + stopDuration, request.getToLink());
			schedule.addTask(dropOffStopTask);
			dropOffStopTask.addDropoffRequest(request);
			request.setDropoffTask(dropOffStopTask);

			// Step 6. Append new final stay task to the end
			double newStayTaskStartTime = dropOffStopTask.getEndTime();
			DrtStayTask newFinalStayTask = taskFactory.createStayTask(vehicle, newStayTaskStartTime,
					vehicle.getServiceEndTime(), dropOffStopTask.getLink());
			schedule.addTask(newFinalStayTask);
		}
	}

	public double calculateTimeDistanceForBusyVehicle(DvrpVehicle vehicle, Link requestLink, double timeOfTheDay) {
		int finalTaskIndex = vehicle.getSchedule().getTaskCount() - 1;
		DrtStayTask finalStayTask = (DrtStayTask) vehicle.getSchedule().getTasks().get(finalTaskIndex);
		Link vehicleLink = finalStayTask.getLink();
		double additionalTIme = finalStayTask.getBeginTime() - timeOfTheDay;
		return calculateTravelTime(vehicleLink, requestLink, finalStayTask.getBeginTime()) + additionalTIme;
	}

	public double calculateTravelTime(Link fromLink, Link toLink, double timeOfTheDay) {
		VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(fromLink, toLink, timeOfTheDay, leastCostPathCalculator,
				travelTime);
		return path.getTravelTime();
	}

}
