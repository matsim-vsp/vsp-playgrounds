/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.dziemke.utils;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.vsp.openberlinscenario.planmodification.PlanFileModifier;

/**
 * @author dziemke
 */
public class DZPlanFileModifier {
	
	public static void main(String[] args) {
//		String inputPlansFile = "../../upretoria/data/capetown/scenario_2017/original/population.xml.gz";
//		String outputPlansFile = "../../upretoria/data/capetown/scenario_2017/population_32734.xml.gz";
//		String inputPlansFile = "../../capetown/data/scenario_2017/population_32734.xml.gz";
//		String outputPlansFile = "../../capetown/data/scenario_2017/population_32734_1pct.xml.gz";
		
//		String inputPlansFile = "../../runs-svn/open_berlin_scenario/b5_22/b5_22.output_plans.xml.gz";
//		String outputPlansFile = "../../runs-svn/open_berlin_scenario/b5_22/b5_22.output_plans_no_links.xml.gz";
		
//		String inputPlansFile = "../../imob/data/scenarios/limburg_frac2/matsim/population/population_50pct_no-children.xml.gz";
//		String outputPlansFile = "../../imob/data/scenarios/limburg_frac2/matsim/population/population_10pct_no-children.xml.gz";
		
//		String inputPlansFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_6/population/population_census.xml.gz";
//		String outputPlansFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_6/population/population_1pct.xml.gz";
		
//		String inputPlansFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_5/cemdap_input/502/plans1.xml.gz";
//		String outputPlansFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_6/population/plans_10000.xml.gz";

		String inputPlansFile = "../../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-10pct.plans.xml.gz";
		String outputPlansFile = "../../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-10pct.plans-no-links-routes.xml.gz";

//		double selectionProbability = 0.2;
//		double selectionProbability = 0.01;
		double selectionProbability = 1;
//		boolean onlyTransferSelectedPlan = true;
		boolean onlyTransferSelectedPlan = false;
		boolean considerHomeStayingAgents = true;
		boolean includeNonSelectedStayHomePlans = true;
		boolean onlyConsiderPeopleAlwaysGoingByCar = false;
		int maxNumberOfAgentsConsidered = 10000000;

		boolean removeLinksAndRoutes = true;
//		String inputCRS = TransformationFactory.HARTEBEESTHOEK94_LO19;
//		String outputCRS = "EPSG:32734";
		String inputCRS = null;
		String outputCRS = null;
		
		CoordinateTransformation ct;
		if (inputCRS == null && outputCRS == null) {
			ct = new IdentityTransformation();
		} else {
			ct = TransformationFactory.getCoordinateTransformation(inputCRS, outputCRS);
		}

		PlanFileModifier planFileModifier = new PlanFileModifier(inputPlansFile, outputPlansFile, selectionProbability, onlyTransferSelectedPlan,
				considerHomeStayingAgents, includeNonSelectedStayHomePlans, onlyConsiderPeopleAlwaysGoingByCar,
				maxNumberOfAgentsConsidered, removeLinksAndRoutes, ct);
		
		planFileModifier.modifyPlans();
	}
}