/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.cadyts.marginals.prep;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

/**
 * A class to create a object which contains mode and distance-bin-index information.
 * 
 * @author amit
 */

public final class ModalBinIdentifier implements Identifiable<ModalBinIdentifier> {
	
	private final String mode;
	private final DistanceBin.DistanceRange distanceRange;
	private final Id<ModalBinIdentifier> id;
	

	
	public String getMode() {
		return mode;
	}

	public DistanceBin.DistanceRange getDistanceRange() {
		return distanceRange;
	}

	// don't make it public: Amit Feb'18
	ModalBinIdentifier(final String mode, final DistanceBin.DistanceRange distanceRange) {
		this.mode = mode;
		this.distanceRange = distanceRange;
		this.id = DistanceDistributionUtils.getModalBinId(mode, distanceRange);
	}

	@Override
	public Id<ModalBinIdentifier> getId() {
		return this.id;
	}

	@Override
	public String toString() {
		return "ModalDistanceBin[" +
				"mode='" + mode + '\'' +
				", distanceRange=" + distanceRange +
				']';
	}
}
