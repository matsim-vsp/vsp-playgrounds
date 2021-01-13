package playground.lu.drtAnalysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;

public class NeighbouringZoneIdentifier {
	private static final Logger log = Logger.getLogger(NeighbouringZoneIdentifier.class);
	private final Map<DrtZone, Set<DrtZone>> neighbouringMap;;

	public NeighbouringZoneIdentifier(DrtZonalSystem zonalSystem) {
		neighbouringMap = getNeibouringMap(zonalSystem);
	}

	public Set<DrtZone> getZonesWithPartialAvailability(List<DrtZone> zonesWithVehicle) {
		Set<DrtZone> zonesWithPartialAvailability = new HashSet<>();
		for (DrtZone zone : zonesWithVehicle) {
			zonesWithPartialAvailability.addAll(neighbouringMap.get(zone));
		}
		zonesWithPartialAvailability.removeAll(zonesWithVehicle);
		return zonesWithPartialAvailability;
	}

	private Map<DrtZone, Set<DrtZone>> getNeibouringMap(DrtZonalSystem zonalSystem) {
		log.info("Generating neighbouring maps for zones...");
		Map<DrtZone, Set<DrtZone>> neighbouringMap = new HashMap<>();
		Collection<DrtZone> drtZones = zonalSystem.getZones().values();
		for (DrtZone zone1 : drtZones) {
			neighbouringMap.put(zone1, new HashSet<>());
			for (DrtZone zone2 : drtZones) {
				Set<Node> nodesInZone1 = new HashSet<>();
				Set<Node> nodesInZone2 = new HashSet<>();
				for (Link link : zone1.getLinks()) {
					nodesInZone1.add(link.getFromNode());
					nodesInZone1.add(link.getToNode());
				}
				for (Link link : zone2.getLinks()) {
					nodesInZone2.add(link.getFromNode());
					nodesInZone2.add(link.getToNode());
				}
				nodesInZone1.retainAll(nodesInZone2);
				if (!nodesInZone1.isEmpty() && zone1 != zone2) {
					neighbouringMap.get(zone1).add(zone2);
				}
			}
		}
		log.info("Neighbouring map generation complete!");
		return neighbouringMap;
	}
}
