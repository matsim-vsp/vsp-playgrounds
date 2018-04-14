package searchacceleration.datastructures;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import floetteroed.utilities.Tuple;

/**
 * Stores integer (counting) data in a map with keys consisting of (space, time)
 * tuples. "Space" is represented by the generic class L (e.g. a network link).
 * 
 * This minimal class exists only to speed up numerical operations in
 * {@link ScoreUpdater} that require iterating over all map entries. For a
 * less memory-intensive implementation, see
 * {@link SpaceTimeIndicatorVectorListBase}.
 * 
 * @author Gunnar Flötteröd
 * 
 * @param L
 *            the space coordinate type
 *
 */
class SpaceTimeVectorMapBased<L> {

	// -------------------- MEMBERS --------------------

	// all values are non-null
	private final Map<Tuple<L, Integer>, Integer> data = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	SpaceTimeVectorMapBased(final SpaceTimeIndicatorVectorListBased<L> parent) {
		if (parent != null) {
			for (int timeBin = 0; timeBin < parent.getTimeBinCnt(); timeBin++) {
				for (L spaceObj : parent.getVisitedSpaceObjects(timeBin)) {
					this.add(this.newKey(spaceObj, timeBin), 1);
				}
			}
		}
	}

	// -------------------- INTERNALS --------------------

	private Tuple<L, Integer> newKey(final L spaceObj, final Integer timeBin) {
		return new Tuple<>(spaceObj, timeBin);
	}

	private Integer get(final Tuple<L, Integer> key) {
		if (this.data.containsKey(key)) {
			return this.data.get(key);
		} else {
			return 0;
		}
	}

	private void set(final Tuple<L, Integer> key, final Integer value) {
		if ((value == null) || (value.intValue() == 0)) {
			this.data.remove(key);
		} else {
			this.data.put(key, value);
		}
	}

	private void add(final Tuple<L, Integer> key, final Integer addend) {
		if (addend != null) {
			this.set(key, this.get(key) + addend);
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	Set<Map.Entry<Tuple<L, Integer>, Integer>> getEntryView() {
		return Collections.unmodifiableSet(this.data.entrySet());
	}

	void subtract(final SpaceTimeVectorMapBased<L> other) {
		for (Map.Entry<Tuple<L, Integer>, Integer> entry : other.data.entrySet()) {
			this.set(entry.getKey(), this.get(entry.getKey()) - entry.getValue());
		}
	}
}
