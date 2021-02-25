package playground.lu.congestionAwareDrt;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.core.router.FastAStarEuclideanFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class ReroutingStrategy {
	private final double proportionToReroute = 0.3;
	private final Random rnd = new Random(1234);
	private final VehicleEntry.EntryFactory vehicleEntryFactory;
	private final ForkJoinPool forkJoinPool;

	private final LeastCostPathCalculator leastCostPathCalculator;
	private final TravelTime travelTime;
	private final double stopDuration;

	private final TravelDisutility travelDisutility;
	private final Network network;

	public ReroutingStrategy(TravelTime travelTime, DrtConfigGroup drtCfg, Network network,
			TravelDisutility travelDisutility, VehicleEntry.EntryFactory vehicleEntryFactory,
			ForkJoinPool forkJoinPool) {
		this.travelTime = travelTime;
		this.stopDuration = drtCfg.getStopDuration();
		this.leastCostPathCalculator = new FastAStarEuclideanFactory().createPathCalculator(network, travelDisutility,
				travelTime);
		this.vehicleEntryFactory = vehicleEntryFactory;
		this.forkJoinPool = forkJoinPool;
		this.travelDisutility = travelDisutility;
		this.network = network;

	}

	public void rerouteVehicles(double now, Fleet fleet) {
		// TODO maybe there is other better way to do this
		if (travelDisutility instanceof CongestionAvertingTravelDisutility) {
			((CongestionAvertingTravelDisutility) travelDisutility).updateInFlow();
		}

		// TODO consider move this to the DRT optimizer
		var vehicleEntries = forkJoinPool.submit(() -> fleet.getVehicles()
				.values()
				.parallelStream()
				.map(v -> vehicleEntryFactory.create(v, now))
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(e -> e.vehicle.getId(), e -> e))).join();
		
		for (VehicleEntry vEntry : vehicleEntries.values()) {
			if (rnd.nextDouble() < proportionToReroute) {
				rerouteVehicle(vEntry);
			}
		}
		
		// Debug part
//		if (now <= 28800 && now >= 21600) {
//			Id<Link> linkId = Id.create(147, Link.class);
//			double cost = travelDisutility.getLinkTravelDisutility(network.getLinks().get(linkId), now, null, null);
//			System.out.println("Disutility for traveling on link 147 at " + now + " is " + cost);
//		}
	}

	private void rerouteVehicle(VehicleEntry vEntry) {
		DvrpVehicle vehicle = vEntry.vehicle;
		Schedule schedule = vehicle.getSchedule();
		if (schedule.getStatus() == ScheduleStatus.STARTED) {
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
				VrpPathWithTravelData updatedPath = VrpPaths.calcAndCreatePath(divertableLink, destination,
						divertableTime, leastCostPathCalculator, travelTime);
				((OnlineDriveTaskTracker) currentDriveTask.getTaskTracker()).divertPath(updatedPath);
//				currentDriveTask.setEndTime(updatedPath.getArrivalTime()); // Probably already changed in the divert path function

				double timePoint = currentDriveTask.getEndTime();

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
	}

}
