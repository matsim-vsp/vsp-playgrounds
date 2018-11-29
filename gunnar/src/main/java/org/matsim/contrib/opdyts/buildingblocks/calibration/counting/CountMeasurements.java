/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.opdyts.buildingblocks.calibration.counting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.core.controler.AbstractModule;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class CountMeasurements {

	// -------------------- CONSTANTS --------------------

	private Map<CountMeasurementSpecification, double[]> measSpec2data = new LinkedHashMap<>();

	private final double simulatedPopulationShare;

	// -------------------- MEMBERS --------------------

	private List<AbstractModule> modules = null;

	private List<AbsoluteLinkEntryCountDeviationObjectiveFunction> objectiveFunctions = null;

	// -------------------- CONSTRUCTION --------------------

	public CountMeasurements(final double simulatedPopulationShare) {
		this.simulatedPopulationShare = simulatedPopulationShare;
	}

	// -------------------- COMPOSITION --------------------

	public void addMeasurement(final CountMeasurementSpecification spec, double[] data) {
		this.measSpec2data.put(spec, data);
	}

	// -------------------- BUILDING --------------------

	public void build() {

		this.modules = new ArrayList<>(this.measSpec2data.size());
		this.objectiveFunctions = new ArrayList<>(this.measSpec2data.size());
		final Map<CountMeasurementSpecification, LinkEntryCounter> measSpec2linkEntryCounter = new LinkedHashMap<>();

		for (Map.Entry<CountMeasurementSpecification, double[]> entry : this.measSpec2data.entrySet()) {
			final CountMeasurementSpecification spec = entry.getKey();

			final LinkEntryCounter simCounter;
			if (measSpec2linkEntryCounter.containsKey(spec)) {
				simCounter = measSpec2linkEntryCounter.get(spec);
			} else {
				simCounter = new LinkEntryCounter(spec);
				measSpec2linkEntryCounter.put(spec, simCounter);
				this.modules.add(new AbstractModule() {
					@Override
					public void install() {
						this.addEventHandlerBinding().toInstance(simCounter);
						this.addControlerListenerBinding().toInstance(simCounter);
					}
				});
			}

			final double[] data = entry.getValue();
			this.objectiveFunctions.add(new AbsoluteLinkEntryCountDeviationObjectiveFunction(data, simCounter,
					this.simulatedPopulationShare));
		}
	}

	// -------------------- GETTERS --------------------

	public List<AbstractModule> getModules() {
		return this.modules;
	}

	public List<AbsoluteLinkEntryCountDeviationObjectiveFunction> getObjectiveFunctions() {
		return this.objectiveFunctions;
	}

}
