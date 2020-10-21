package zoneBasedMatching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.optimizer.insertion.DetourPathCalculator;
import org.matsim.contrib.drt.optimizer.insertion.ExtensiveInsertionSearch;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator.PenaltyCalculator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.mobsim.framework.MobsimTimer;

public class ExtensiveInsertionSerachWithZonalConstraints extends ExtensiveInsertionSearch {

	private final DrtZonalSystem drtZonalSystem;

	public ExtensiveInsertionSerachWithZonalConstraints(DetourPathCalculator detourPathCalculator,
			DrtConfigGroup drtCfg, MobsimTimer timer, ForkJoinPool forkJoinPool, PenaltyCalculator penaltyCalculator,
			DrtZonalSystem drtZonalSystem) {
		super(detourPathCalculator, drtCfg, timer, forkJoinPool, penaltyCalculator);
		this.drtZonalSystem = drtZonalSystem;
	}

	@Override
	public Optional<InsertionWithDetourData<PathData>> findBestInsertion(DrtRequest drtRequest,
			Collection<Entry> vEntries) {
		List<Entry> filteredVEntries = new ArrayList<>();
		for (Entry entry : vEntries) {
			Task currentTask = entry.vehicle.getSchedule().getCurrentTask();
			if (currentTask instanceof DrtStayTask) {
				// For idling vehicle (STAY), we will consider it if it is in the same zone of
				// the request
				if (considerVehicleOrNot(((DrtStayTask) currentTask).getLink(), drtRequest.getFromLink())) {
					filteredVEntries.add(entry);
				}
			} else if (currentTask instanceof DrtDriveTask) {
				DrtTaskType taskType = (DrtTaskType) currentTask.getTaskType();
				if (taskType.equals(EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE)) {
					// For rebalancing vehicle (RELOCATE), we will consider it if it is rebalancing
					// to the same zone of the request
					Link destinationLink = ((DrtDriveTask) currentTask).getPath().getToLink();
					if (considerVehicleOrNot(destinationLink, drtRequest.getFromLink())) {
						filteredVEntries.add(entry);
					}
				} else {
					// For vehicle driving with passenger(s) or driving towards passenger (DRIVE),
					// we will always consider it
					filteredVEntries.add(entry);
				}
			} else {
				// For vehicle picking up/dropping off passenger (STOP), we will always consider
				// it
				filteredVEntries.add(entry);
			}

		}
		return calculate(drtRequest, filteredVEntries);
	}

	private boolean considerVehicleOrNot(Link vehicleLink, Link requestLink) {
		DrtZone vehicleZone = drtZonalSystem.getZoneForLinkId(vehicleLink.getId());
		DrtZone requestZone = drtZonalSystem.getZoneForLinkId(requestLink.getId());
		if (vehicleZone.equals(requestZone)) {
			return true;
		}
		return false;
	}

}
