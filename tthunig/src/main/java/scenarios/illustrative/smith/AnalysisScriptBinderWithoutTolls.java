/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package scenarios.illustrative.smith;

import scenarios.illustrative.analysis.TtListenerToBindAndWriteAnalysis;

/**
 * @author tthunig
 */
class AnalysisScriptBinderWithoutTolls extends TtListenerToBindAndWriteAnalysis{

	public AnalysisScriptBinderWithoutTolls() {
		this.scriptNameRouteDistribution = "plot_routeDistribution_withoutTolls";
		this.scriptNameRoutesAndTTs = "plot_routesAndTTs_withoutTolls";
	}
	
	
}
