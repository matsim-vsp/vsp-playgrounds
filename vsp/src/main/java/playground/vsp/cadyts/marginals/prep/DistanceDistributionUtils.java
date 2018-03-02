/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package playground.vsp.cadyts.marginals.prep;

import java.util.Set;
import org.matsim.api.core.v01.Id;

/**
 * Created by amit on 21.02.18.
 */

public class DistanceDistributionUtils {

    private static final String ID_SEPERATOR = "_&_";

    public enum DistanceUnit {meter, kilometer}

    public enum DistanceDistributionFileLabels {mode, distanceLowerLimit, distanceUpperLimit, measuredCount}

    public static DistanceBin.DistanceRange getDistanceRange(double distance, Set<DistanceBin.DistanceRange> distanceRanges){
//        if(distanceRanges.isEmpty()) throw new RuntimeException("Distance range set is empty.");

        for(DistanceBin.DistanceRange distanceRange : distanceRanges) {
            if (distance >= distanceRange.getLowerLimit() && distance < distanceRange.getUpperLimit())
                return distanceRange;
        }
        throw new RuntimeException("No distance range found for "+ distance);
    }

    public static Id<ModalDistanceBinIdentifier> getModalBinId(String mode, DistanceBin.DistanceRange distanceRange){
        return Id.create( mode.concat(ID_SEPERATOR).concat( String.valueOf(distanceRange) ), ModalDistanceBinIdentifier.class);
    }

}
