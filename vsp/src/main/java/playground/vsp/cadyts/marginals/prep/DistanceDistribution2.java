package playground.vsp.cadyts.marginals.prep;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import java.util.*;
import java.util.stream.Collectors;

public class DistanceDistribution2 {

	private static final Logger logger = Logger.getLogger(DistanceDistribution2.class);

	private Map<Id<DistanceBin>, DistanceBin> distanceBins = new HashMap<>();

	public void add(String mode, DistanceBin.DistanceRange distanceRange, double standardDeviation, double value) {

		DistanceBin bin = new DistanceBin(mode, distanceRange, standardDeviation, value);
		distanceBins.put(bin.getId(), bin);
	}

	public void increaseCountByOne(String mode, double distance) {
		DistanceBin distanceBin = distanceBins.values().stream()
				.filter(bin -> bin.getMode().equals(mode))
				.filter(bin -> bin.getDistanceRange().isWithinRange(distance))
				.findAny()
				.orElseThrow(() -> new RuntimeException("Could not find distance bin for: " + distance));

		distanceBin.addToCount(1);
	}

	public Collection<DistanceBin> getDistanceBins() {
		return distanceBins.values();
	}

	public DistanceBin getBin(Id<DistanceBin> id) {
		return distanceBins.get(id);
	}

	public DistanceBin getBin(String mode, double distance) {
		return distanceBins.values().stream()
				.filter(bin -> bin.getMode().equals(mode))
				.filter(bin -> bin.getDistanceRange().isWithinRange(distance))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Could not find distance bin for: " + distance));
	}

	public DistanceDistribution2 copyWithEmptyBins() {
		DistanceDistribution2 result = new DistanceDistribution2();
		for (DistanceBin bin : distanceBins.values()) {
			result.add(bin.getMode(), bin.getDistanceRange(), bin.getStandardDeviation(), 0);
		}
		return result;
	}
}
