package zoneBasedMatching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearch;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.contrib.dvrp.schedule.Task;

public class InsertionSerachWithZonalConstraints implements DrtInsertionSearch<PathData> {

	private final DrtZonalSystem drtZonalSystem;
	private final DrtInsertionSearch<PathData> drtInsertionSearch;

	public InsertionSerachWithZonalConstraints(DrtInsertionSearch<PathData> drtInsertionSearch,
			DrtZonalSystem drtZonalSystem) {
		this.drtZonalSystem = drtZonalSystem;
		this.drtInsertionSearch = drtInsertionSearch;
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
		return drtInsertionSearch.findBestInsertion(drtRequest, filteredVEntries);
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
