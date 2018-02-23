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

package playground.ikaddoura.berlin;

import java.util.HashMap;
import java.util.Map;

import playground.vsp.demandde.corineLandcover.CORINELandCoverCoordsModifier;

/**
* @author ikaddoura
*/

public class CorineForCemdapPlans {
	
	public static void main(String[] args) {
		String corineLandCoverFile = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/input/shapefiles/corine_landcover_berlin-brandenburg/corine_lancover_berlin-brandenburg_GK4.shp";

	    String zoneFile = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/input/shapefiles/2013/gemeindenLOR_DHDN_GK4.shp";
	    String zoneIdTag = "NR";
	
	    String matsimPlans = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/berlin_4.0/population/pop_300ik1/plans_10pct.xml.gz";
	    String outPlans = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/berlin_4.0/population/pop_300ik1/plans_10pct_corine-landcover.xml.gz";
	
	    boolean simplifyGeom = false;
	    boolean combiningGeoms = false;
	    boolean sameHomeActivity = true;
	    String homeActivityPrefix = "home";
	
	    Map<String, String> shapeFileToFeatureKey = new HashMap<>();
	    shapeFileToFeatureKey.put(zoneFile, zoneIdTag);
	
	    CORINELandCoverCoordsModifier plansFilterForCORINELandCover = new CORINELandCoverCoordsModifier(matsimPlans,
	            shapeFileToFeatureKey,
	            corineLandCoverFile,
	            simplifyGeom,
	            combiningGeoms,
	            sameHomeActivity,
	            homeActivityPrefix);
	    plansFilterForCORINELandCover.process();
	    plansFilterForCORINELandCover.writePlans(outPlans);
    }
}

