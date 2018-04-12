package searchacceleration.datastructures;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import floetteroed.utilities.Tuple;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class SpaceTimeVectorMapbased<L> {

	// -------------------- MEMBERS --------------------

	final Map<Tuple<L, Integer>, Double> data = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public SpaceTimeVectorMapbased() {
	}

	// TODO NEW
	public SpaceTimeVectorMapbased(final SpaceTimeIndicatorVectorListbased<L> parent) {
		if (parent != null) {
			for (int bin = 0; bin < parent.data.size(); bin++) {
				if (parent.data.get(bin) != null) {
					for (L spaceObj : parent.data.get(bin)) {
						this.add(spaceObj, bin, 1.0);
					}
				}
			}
		}
	}

	public SpaceTimeVectorMapbased<L> newDeepCopy() {
		final SpaceTimeVectorMapbased<L> result = new SpaceTimeVectorMapbased<>();
		result.data.putAll(this.data);
		return result;
	}

	// -------------------- INTERNALS --------------------

	static <L2> Tuple<L2, Integer> newKey(final L2 spaceObj, final Integer timeBin) {
		return new Tuple<>(spaceObj, timeBin);
	}

	// TODO do the value check before the key is created
	void set(final Tuple<L, Integer> key, final Double value) {
		if ((value == null) || (value.doubleValue() == 0.0)) {
			this.data.remove(key);
		} else {
			this.data.put(key, value);
		}
	}

	// TODO do the value check before the key is created
	// TODO NEW
	void add(final Tuple<L, Integer> key, final Double addend) {
		if (addend != null) {
			if (this.data.containsKey(key)) {
				this.set(key, this.data.get(key) + addend);
			} else {
				this.set(key, addend);
			}
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	public void set(final L spaceObj, final int timeBin, final double value) {
		this.set(newKey(spaceObj, timeBin), value);
	}

	public void add(final L spaceObj, final int timeBin, final double value) {
		this.add(newKey(spaceObj, timeBin), value);
	}

	public double sum() {
		double result = 0.0;
		for (Double val : this.data.values()) {
			result += val;
		}
		return result;
	}

	public double ell1Norm() {
		double result = 0.0;
		for (Double val : this.data.values()) {
			result += Math.abs(val);
		}
		return result;
	}

	public SpaceTimeVectorMapbased<L> subtract(final SpaceTimeVectorMapbased<L> other) {
		for (Map.Entry<Tuple<L, Integer>, Double> entry : other.data.entrySet()) {
			final Double myVal = this.data.get(entry.getKey());
			final double diff = ((myVal == null) ? -entry.getValue() : myVal - entry.getValue());
			this.set(entry.getKey(), diff);
		}
		return this;
	}

	public double innerProduct(final SpaceTimeVectorMapbased<L> other) {

		final SpaceTimeVectorMapbased<L> smaller, larger;
		if (this.data.size() < other.data.size()) {
			smaller = this;
			larger = other;
		} else {
			smaller = other;
			larger = this;
		}

		double result = 0.0;
		for (Map.Entry<Tuple<L, Integer>, Double> entry1 : smaller.data.entrySet()) {
			final Double value2 = larger.data.get(entry1.getKey());
			if (value2 != null) {
				result += entry1.getValue() * value2;
			}
		}
		return result;
	}

	// TESTING

	public static <K> void printComparison(SpaceTimeVectorMapbased<K> x, SpaceTimeVectorMapbased<K> y, SpaceTimeVectorMapbased<K> z) {

		final Set<Tuple<K, Integer>> allKeys = new LinkedHashSet<>(x.data.keySet());
		allKeys.addAll(y.data.keySet());
		allKeys.addAll(z.data.keySet());

		for (Tuple<K, Integer> key : allKeys) {

			System.out.print(key.getA() + "\t" + key.getB() + "\t");

			if (x.data.containsKey(key)) {
				System.out.print(x.data.get(key));
			} else {
				System.out.print("0");
			}
			System.out.print("\t");

			if (y.data.containsKey(key)) {
				System.out.print(y.data.get(key));
			} else {
				System.out.print("0");
			}
			System.out.print("\t");

			if (z.data.containsKey(key)) {
				System.out.print(z.data.get(key));
			} else {
				System.out.print("0");
			}
			System.out.println();

		}
	}

}
