package searchacceleration.datastructures;

import java.util.ArrayList;
import java.util.List;

import floetteroed.utilities.EmptyIterable;

/**
 * Stores 0/1 indicator data ("visits") that are indexed by (space, time)
 * tuples. The same key may be set multiple times, meaning that one can store
 * multiple ones per key. This class constructed to minimize memory usage for
 * very sparse data. "Space" is represented by the generic class L (e.g. a
 * network link).
 * 
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <L>
 *            the space coordinate type
 */
public class SpaceTimeIndicators<L> {

	// -------------------- MEMBERS --------------------

	private final List<List<L>> data;

	// -------------------- CONSTRUCTION --------------------

	public SpaceTimeIndicators(final int timeBinCnt) {
		this.data = new ArrayList<List<L>>(timeBinCnt);
		for (int bin = 0; bin < timeBinCnt; bin++) {
			this.data.add(null);
		}
	}

	// -------------------- PACKAGE INTERNALS --------------------

	int getTimeBinCnt() {
		return this.data.size();
	}

	Iterable<L> getVisitedSpaceObjects(final int timeBin) {
		if (this.data.get(timeBin) != null) {
			return this.data.get(timeBin);
		} else {
			return new EmptyIterable<L>();
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	public void visit(final L spaceObj, final int timeBin) {
		List<L> spaceObjList = this.data.get(timeBin);
		if (spaceObjList == null) {
			spaceObjList = new ArrayList<L>(1);
			this.data.set(timeBin, spaceObjList);
		}
		spaceObjList.add(spaceObj);
	}
}
