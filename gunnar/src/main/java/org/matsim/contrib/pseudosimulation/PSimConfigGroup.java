package org.matsim.contrib.pseudosimulation;

import org.matsim.core.config.ReflectiveConfigGroup;

public class PSimConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "psim";

	public static final String ITERATIONS_PER_CYCLE = "iterationsPerCycle";
	private int iterationsPerCycle = 10;

	public PSimConfigGroup() {
		super(GROUP_NAME);
	}

	@StringGetter(ITERATIONS_PER_CYCLE)
	public int getIterationsPerCycle() {
		return iterationsPerCycle;
	}

	@StringSetter(ITERATIONS_PER_CYCLE)
	public  void setIterationsPerCycle(int iterationsPerCycle) {
		this.iterationsPerCycle = iterationsPerCycle;
	}

}
