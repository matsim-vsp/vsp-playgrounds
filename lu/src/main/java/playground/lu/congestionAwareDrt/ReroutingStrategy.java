package playground.lu.congestionAwareDrt;

import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.core.router.FastAStarEuclideanFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class ReroutingStrategy {
	private final double proportionToReroute = 0.3;
	private final Random rnd = new Random();

	private final LeastCostPathCalculator leastCostPathCalculator;
	private final TravelTime travelTime;
	private final double stopDuration;

	public ReroutingStrategy(TravelTime travelTime, DrtConfigGroup drtCfg, Network network,
			TravelDisutility travelDisutility) {
		this.travelTime = travelTime;
		this.stopDuration = drtCfg.getStopDuration();
		this.leastCostPathCalculator = new FastAStarEuclideanFactory().createPathCalculator(network, travelDisutility,
				travelTime);

	}

	public void rerouteVehicles() {
		System.out.println("reroute fleet now");
	}

	private void rerouteVehicle(Entry vEntry) {
		DvrpVehicle vehicle = vEntry.vehicle;
		Schedule schedule = vehicle.getSchedule();
		List<? extends Task> tasks = schedule.getTasks();
		Task currentTask = schedule.getCurrentTask();
		int currentTaskIdx = currentTask.getTaskIdx();

		// Current task should be the first one on the task list
		if (currentTask instanceof DrtDriveTask) {
			DrtDriveTask currentDriveTask = (DrtDriveTask) currentTask;
			Link destination = currentDriveTask.getPath().getToLink();
			Link divertableLink = vEntry.start.getLink();
			double divertableTime = vEntry.start.getDepartureTime();

			// Update curretn drive task with a new path
			VrpPathWithTravelData updatedPath = VrpPaths.calcAndCreatePath(divertableLink, destination, divertableTime,
					leastCostPathCalculator, travelTime);
			((OnlineDriveTaskTracker) currentDriveTask.getTaskTracker()).divertPath(updatedPath);
			currentDriveTask.setEndTime(updatedPath.getArrivalTime());
		}

		double timePoint = tasks.get(0).getEndTime();

		for (int i = currentTaskIdx + 1; i < tasks.size(); i++) {
			if (tasks.get(i) instanceof DrtDriveTask) {
				DrtDriveTask driveTask = (DrtDriveTask) tasks.get(i);
				double travelTime = ((VrpPathWithTravelData) driveTask.getPath()).getTravelTime();

				driveTask.setBeginTime(timePoint);
				timePoint += travelTime;
				driveTask.setEndTime(timePoint);
			}

			if (tasks.get(i) instanceof DrtStopTask) {
				DrtStopTask stopTask = (DrtStopTask) tasks.get(i);
				stopTask.setBeginTime(timePoint);
				timePoint += stopDuration;
				stopTask.setEndTime(timePoint);
			}

			if (tasks.get(i) instanceof DrtStayTask) {
				tasks.get(i).setBeginTime(timePoint);
			}
		}
	}

}
