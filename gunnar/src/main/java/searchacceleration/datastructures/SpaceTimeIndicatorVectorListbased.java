package searchacceleration.datastructures;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <L> the space object type
 */
public class SpaceTimeIndicatorVectorListbased<L> {

	// -------------------- MEMBERS --------------------

	final List<List<L>> data;

	// -------------------- CONSTRUCTION --------------------

	public SpaceTimeIndicatorVectorListbased(final int timeBinCnt) {
		this.data = new ArrayList<List<L>>(timeBinCnt);
		for (int bin = 0; bin < timeBinCnt; bin++) {
			this.data.add(null);
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	public void add(final L spaceObj, final int timeBin) {
		List<L> spaceObjList = this.data.get(timeBin);
		if (spaceObjList == null) {
			spaceObjList = new ArrayList<L>();
			this.data.set(timeBin, spaceObjList);
		}
		spaceObjList.add(spaceObj);
	}

	public int sum() {
		int result = 0;
		for (List<L> spaceObjList : this.data) {
			if (spaceObjList != null) {
				result += spaceObjList.size();
			}
		}
		return result;
	}
}
