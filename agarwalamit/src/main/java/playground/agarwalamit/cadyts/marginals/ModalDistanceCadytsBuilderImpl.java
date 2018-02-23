/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.agarwalamit.cadyts.marginals;

import java.util.Map;
import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.measurements.SingleLinkMeasurement;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.cadyts.general.CadytsBuilderImpl;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.LookUpItemFromId;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

/**
 * @author nagel
 * @author mrieser
 */
public final class ModalDistanceCadytsBuilderImpl {

	public static final String MARGINALS = "_marginals";

	private static Logger log = Logger.getLogger( ModalDistanceCadytsBuilderImpl.class ) ;

	public static <T> AnalyticalCalibrator<T> buildCalibratorAndAddMeasurements(final Config config, final Map<Id<T>, DistanceBin> inputDistanceDistribution,
																				LookUpItemFromId<T> lookUp, Class<T> idType) {

		if (inputDistanceDistribution.isEmpty()) {
			log.warn("Distance distribution container is empty.");
		}

		CadytsConfigGroup cadytsConfig = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);

		AnalyticalCalibrator<T> matsimCalibrator = CadytsBuilderImpl.buildCalibrator(config);

		//add counts data into calibrator
		int numberOfAddedMeasurements = 0 ;
		for(Map.Entry< Id<T>, DistanceBin> entry : inputDistanceDistribution.entrySet()){
			// (loop over all counting "items" (usually locations/stations)

			T item = lookUp.getItem(Id.create(entry.getKey(), idType)) ;

			if ( item==null ) {
				throw new RuntimeException("item is null; entry=" + entry + " idType=" + idType ) ;
			}
			DistanceBin bin = entry.getValue();
			matsimCalibrator.addMeasurement(item, (int) bin.getDistanceRange().getLowerLimit(), (int) bin.getDistanceRange().getUpperLimit(), bin.getCount(), SingleLinkMeasurement.TYPE.COUNT_VEH);
			numberOfAddedMeasurements++;
		}

        if ( numberOfAddedMeasurements==0 ) {
			log.warn("No measurements were added.");
        }
		
        if ( matsimCalibrator.getProportionalAssignment() ) {
        	throw new RuntimeException("Gunnar says that this may not work so do not set to true. kai, sep'14") ;
        }
		return matsimCalibrator;
	}
}