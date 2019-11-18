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

package playground.ikaddoura.savPricing.noiseSAV;

import org.apache.log4j.Logger;
import org.matsim.contrib.noise.NoiseComputationModule;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;

/**
* @author ikaddoura
*/

public class NoiseComputationModuleSAV extends AbstractModule {
	private static final Logger log = Logger.getLogger(NoiseComputationModuleSAV.class);

	@Override
	public void install() {
		
		NoiseConfigGroup noiseParameters = ConfigUtils.addOrGetModule(this.getConfig(), NoiseConfigGroup.class);

		this.install(new NoiseComputationModule());
				
		if (noiseParameters.isInternalizeNoiseDamages()) {
			
			this.addEventHandlerBinding().to(NoisePricingHandlerSAV.class).asEagerSingleton();
			
			log.info("Internalizing noise damages. This requires that the default travel disutility is replaced by a travel distuility which accounts for noise tolls.");
		}		
	}

}

