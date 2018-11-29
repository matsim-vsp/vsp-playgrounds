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
package org.matsim.contrib.opdyts.buildingblocks.decisionvariables.capacityscaling;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.scalar.ScalarDecisionVariable;
import org.matsim.core.config.Config;

import floetteroed.utilities.Units;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class NetworkCapacityScaling implements ScalarDecisionVariable<NetworkCapacityScaling> {

	// -------------------- STATIC HELPERS --------------------

	public static double factorFromAbsoluteChange_veh_h(final double absAvgChange_veh_h, final Network network) {
		int linkCnt = 0;
		double linkCapSum_veh_h = 0.0;		
		for (Link link : network.getLinks().values()) {
			final double linkCap_veh_h = link.getFlowCapacityPerSec() * Units.VEH_H_PER_VEH_S;
			if (linkCap_veh_h > 0) {
				linkCnt++;
				linkCapSum_veh_h += linkCap_veh_h;
			}
		}
		if (linkCnt == 0) {
			throw new RuntimeException("There are no links with a positive capacity.");
		}
		return (absAvgChange_veh_h * linkCnt)  / linkCapSum_veh_h;
	}

	// -------------------- CONSTANTS --------------------

	private final Config config;

	// -------------------- MEMBERS --------------------

	private double factor;

	// -------------------- CONSTRUCTION --------------------

	public NetworkCapacityScaling(final Config config, final double factor) {
		this.config = config;
		this.factor = factor;
	}

	// --------------- IMPLEMENTATION OF ScalarDecisionVariable ---------------

	@Override
	public void implementInSimulation() {
		this.config.qsim().setFlowCapFactor(this.factor);
		this.config.qsim().setStorageCapFactor(this.factor);
	}

	@Override
	public void setValue(final double val) {
		this.factor = val;
	}

	@Override
	public double getValue() {
		return this.factor;
	}

	@Override
	public NetworkCapacityScaling newDeepCopy() {
		return new NetworkCapacityScaling(this.config, this.factor);
	}

}
