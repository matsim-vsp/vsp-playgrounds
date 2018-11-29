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
package gunnar.ihop4;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class IhopConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "ihop";

	public IhopConfigGroup() {
		super(GROUP_NAME);
	}

	// -------------------- simulatedPopulationShare --------------------

	private Double simulatedPopulationShare = null;

	@StringSetter("simulatedPopulationShare")
	public void setSimulatedPopulationShare(final Double simulatedPopulationShare) {
		this.simulatedPopulationShare = simulatedPopulationShare;
	}

	@StringGetter("simulatedPopulationShare")
	public Double getSimulatedPopulationShare() {
		return this.simulatedPopulationShare;
	}

}
