package playground.agarwalamit.fundamentalDiagrams.core;

import java.util.List;
import java.util.stream.IntStream;

/**
 * @author ssix
 *
 */
final class BinaryAdditionModule {

	private final List<Integer> maxValues;
	private final List<Integer> steps;
	private final Integer[] point;

	public BinaryAdditionModule(final List<Integer> maxValues, final List<Integer> steps, final Integer[] point){
		this.maxValues = maxValues;
		this.steps = steps;
		this.point = point;
	}

	private boolean furtherAdditionPossible() {
		return IntStream.range(0, point.length).anyMatch(i -> (point[i] + this.steps.get(i)) <= this.maxValues.get(i));
	}

	void addPoint(){
		addTo(point, point.length-1);
	}

	private void addTo(Integer[] point, int index){
		if (furtherAdditionPossible()){
			if ( ! ((point[index] + this.steps.get(index)) > this.maxValues.get(index))){
				Integer newIndexValue = point[index] + this.steps.get(index);
				point[index] = newIndexValue;
			} else {
				point[index] = 0;
				addTo(point, index-1);
			}
		} else {
			FundamentalDiagramDataGenerator.LOG.info("Already tried too many combinations!!!");
		}
	}

	public Integer[] getPoint() {
		return point;
	}

}
