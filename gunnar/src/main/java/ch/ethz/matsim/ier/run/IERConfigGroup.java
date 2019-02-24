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
package ch.ethz.matsim.ier.run;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class IERConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "ier";

	public IERConfigGroup() {
		super(GROUP_NAME);
	}

	// -------------------- iterationsPerCycle --------------------

	private int iterationsPerCycle = 10;

	@StringGetter("iterationsPerCycle")
	public int getIterationsPerCycle() {
		return this.iterationsPerCycle;
	}

	@StringSetter("iterationsPerCycle")
	public void setIterationsPerCycle(int iterationsPerCycle) {
		this.iterationsPerCycle = iterationsPerCycle;
	}

	// -------------------- batchSize --------------------

	private int batchSize = 200;

	@StringGetter("batchSize")
	public int getBatchSize() {
		return this.batchSize;
	}

	@StringSetter("batchSize")
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

}
