package playground.lu.unitCapacityMatching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

public class ZonalVehicleSelectionUtils {
	public static Map<DrtZone, List<Entry>> groupDisposableVehicleEntriesPerZone(DrtZonalSystem zonalSystem,
			Collection<Entry> vEntries, double time, double maxWaitTime) {
		Map<DrtZone, List<Entry>> disposableVehicleEntriesPerZone = new HashMap<>();
		for (Entry vEntry : vEntries) {
			DvrpVehicle vehicle = vEntry.vehicle;
			int finalTaskIndex = vehicle.getSchedule().getTaskCount() - 1;
			DrtStayTask finalStayTask = (DrtStayTask) vehicle.getSchedule().getTasks().get(finalTaskIndex);
			DrtZone zone = zonalSystem.getZoneForLinkId(finalStayTask.getLink().getId());
			if (finalStayTask.getBeginTime() < time + maxWaitTime) {
				disposableVehicleEntriesPerZone.computeIfAbsent(zone, z -> new ArrayList<>()).add(vEntry);
			}
		}
		return disposableVehicleEntriesPerZone;
	}
}
