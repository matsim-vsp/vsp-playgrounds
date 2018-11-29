/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.opdyts;

import org.matsim.core.config.ReflectiveConfigGroup;

import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.opdyts.searchalgorithms.SelfTuner;

/**
 * 
 * @author Amit, created this on 03.06.17.
 * @author Gunnar, as of Sep 2018.
 */

public class OpdytsConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "opdyts";

	public OpdytsConfigGroup() {
		super(GROUP_NAME);
	}

	// ==================== TIME DISCRETIZATION ====================

	public static final int DEFAULT_STARTTIME_S = 0;

	private int startTime_s = DEFAULT_STARTTIME_S;

	@StringGetter("startTime_s")
	public int getStartTime_s() {
		return this.startTime_s;
	}

	@StringSetter("startTime_s")
	public void setStartTime(int startTime_s) {
		this.startTime_s = startTime_s;
	}

	public static final int DEFAULT_BINSIZE_S = 3600;

	private int binSize_s = DEFAULT_BINSIZE_S;

	@StringGetter("binSize")
	public int getBinSize_s() {
		return this.binSize_s;
	}

	@StringSetter("binSize")
	public void setBinSize(int binSize) {
		this.binSize_s = binSize;
	}

	public static final int DEFAULT_BINCNT = 24;

	private int binCount = DEFAULT_BINCNT;

	@StringGetter("binCount")
	public int getBinCount() {
		return this.binCount;
	}

	@StringSetter("binCount")
	public void setBinCount(int binCount) {
		this.binCount = binCount;
	}

	// ==================== SELF-TUNING ====================

	private boolean noisySystem = SelfTuner.DEFAULT_NOISYSYSTEM;

	@StringGetter("noisySystem")
	public boolean isNoisySystem() {
		return noisySystem;
	}

	@StringSetter("noisySystem")
	public void setNoisySystem(boolean noisySystem) {
		this.noisySystem = noisySystem;
	}

	private double selfTuningInertia = SelfTuner.DEFAULT_SELFTUNINGINERTIA;

	@StringGetter("selfTuningInertia")
	public double getInertia() {
		return selfTuningInertia;
	}

	@StringSetter("selfTuningInertia")
	public void setInertia(double inertia) {
		this.selfTuningInertia = inertia;
	}

	private double selfTuningWeightScale = SelfTuner.DEFAULT_SELFTUNINGSCALE;

	@StringGetter("selfTuningWeightScale")
	public double getSelfTuningWeightScale() {
		return this.selfTuningWeightScale;
	}

	@StringSetter("selfTuningWeightScale")
	public void setSelfTuningWeightScale(double selfTuningWeightScale) {
		this.selfTuningWeightScale = selfTuningWeightScale;
	}

	private double initialEquilibriumGapWeight = SelfTuner.DEFAULT_INITIALEQUILIBRIUMGAPWEIGHT;

	@StringGetter("initialEquilibriumGapWeight")
	public double getInitialEquilibriumGapWeight() {
		return initialEquilibriumGapWeight;
	}

	@StringSetter("initialEquilibriumGapWeight")
	public void setInitialEquilibriumGapWeight(double initialEquilibriumGapWeight) {
		this.initialEquilibriumGapWeight = initialEquilibriumGapWeight;
	}

	private double initialUniformityGapWeight = SelfTuner.DEFAULT_INITIALUNIFORMITYGAPWEIGHT;

	@StringGetter("initialUniformityGapWeight")
	public double getInitialUniformityGapWeight() {
		return initialUniformityGapWeight;
	}

	@StringSetter("initialUniformityGapWeight")
	public void setInitialUniformityGapWeight(double initialUniformityGapWeight) {
		this.initialUniformityGapWeight = initialUniformityGapWeight;
	}

	// ==================== TRAJECTORY MEMORY CONSTRAINTS ====================

	private int maxMemoryPerTrajectory = RandomSearch.DEFAULT_MAXMEMORYPERTRAJECTORY;

	@StringGetter("maxMemoryPerTrajectory")
	public int getMaxMemoryPerTrajectory() {
		return maxMemoryPerTrajectory;
	}

	@StringSetter("maxMemoryPerTrajectory")
	public void setMaxMemoryPerTrajectory(int maxMemoryPerTrajectory) {
		this.maxMemoryPerTrajectory = maxMemoryPerTrajectory;
	}

	private int maxTotalMemory = RandomSearch.DEFAULT_MAXTOTALMEMORY;

	@StringGetter("maxTotalMemory")
	public int getMaxTotalMemory() {
		return maxTotalMemory;
	}

	@StringSetter("maxTotalMemory")
	public void setMaxTotalMemory(int maxTotalMemory) {
		this.maxTotalMemory = maxTotalMemory;
	}

	// ==================== STAGE/ITERATION LIMIT ====================

	private static final String MAX_ITERATION = "maxIteration";
	private int maxIteration = 10;

	@StringGetter(MAX_ITERATION)
	public int getMaxIteration() {
		return this.maxIteration;
	}

	@StringSetter(MAX_ITERATION)
	public void setMaxIteration(int maxIteration) {
		this.maxIteration = maxIteration;
	}

	private static final String MAX_TRANSITION = "maxTransition";

	private int maxTransition = Integer.MAX_VALUE;

	@StringGetter(MAX_TRANSITION)
	public int getMaxTransition() {
		return this.maxTransition;
	}

	@StringSetter(MAX_TRANSITION)
	public void setMaxTransition(int maxTransition) {
		this.maxTransition = maxTransition;
	}

	// ==================== WARM-UP ITERATIONS ====================

	private int warmUpIterations = RandomSearch.DEFAULT_WARMUPITERATIONS;

	@StringGetter("warmUpIterations")
	public int getWarmUpIterations() {
		return this.warmUpIterations;
	}

	@StringSetter("warmUpIterations")
	public void setWarmUpIterations(int warmUpIterations) {
		this.warmUpIterations = warmUpIterations;
	}

	private boolean useAllWarmUpIterations = RandomSearch.DEFAULT_USEALLWARMUPITERATIONS;

	@StringGetter("useAllWarmUpIterations")
	public boolean getUseAllWarmUpIterations() {
		return this.useAllWarmUpIterations;
	}

	@StringSetter("useAllWarmUpIterations")
	public void setUseAllWarmUpIterations(boolean useAllWarmUpIterations) {
		this.useAllWarmUpIterations = useAllWarmUpIterations;
	}

	// ==================== EN BLOCK SIMULATION ITERATIONS ====================

	public static final int DEFAULT_ENBLOCKSIMULATIONITERATIONS = 1;
	
	private int enBlockSimulationIterations = DEFAULT_ENBLOCKSIMULATIONITERATIONS;

	@StringGetter("enBlockSimulationIterations")
	public int getEnBlockSimulationIterations() {
		return this.enBlockSimulationIterations;
	}

	@StringSetter("enBlockSimulationIterations")
	public void setEnBlockSimulationIterations(int enBlockSimulationIterations) {
		this.enBlockSimulationIterations = enBlockSimulationIterations;
	}

	// =============== FIXED-ITERATION-NUMBER CONVERGENCE CRITERION ===============

	private Integer numberOfIterationsForAveraging = null;

	@StringGetter("numberOfIterationsForAveraging")
	public Integer getNumberOfIterationsForAveraging() {
		return numberOfIterationsForAveraging;
	}

	@StringSetter("numberOfIterationsForAveraging")
	public void setNumberOfIterationsForAveraging(int numberOfIterationsForAveraging) {
		this.numberOfIterationsForAveraging = numberOfIterationsForAveraging;
	}

	private Integer numberOfIterationsForConvergence = null;

	@StringGetter("numberOfIterationsForConvergence")
	public Integer getNumberOfIterationsForConvergence() {
		return numberOfIterationsForConvergence;
	}

	@StringSetter("numberOfIterationsForConvergence")
	public void setNumberOfIterationsForConvergence(int numberOfIterationsForConvergence) {
		this.numberOfIterationsForConvergence = numberOfIterationsForConvergence;
	}
}