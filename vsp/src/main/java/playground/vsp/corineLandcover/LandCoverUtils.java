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

package playground.vsp.corineLandcover;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Based on the classification of the CORINE land cover data, zones are categorized in two parts: one for home activities and other for rest of the activities.
 * Created by amit on 31.07.17.
 * 
 * tschlenther, 12.04.2018:
 * added another type of data source: Urban Atlas, which differs between more types of activities
 */

public class LandCoverUtils {


    private String landCover_ShapeFile_AttributeID; 

    private final Map<LandCoverActivityType, List<Integer>> activityType2LandCoverId = new HashMap<>();
    
    DataSource dataSource;
    
    public enum LandCoverActivityType {home, leisure, shopping, work, other}
    public enum DataSource {Corine, UrbanAtlas}

    LandCoverUtils(DataSource dataSource) {
    	this.dataSource = dataSource;
    	
    	if(dataSource.equals(DataSource.Corine)){
    		landCover_ShapeFile_AttributeID = "CODE_12";
    		{
    			List<Integer> landCoverIds = new ArrayList<>();
    			landCoverIds.add(111); // continuous urban fabric
    			landCoverIds.add(112); // Discontinuous urban fabric
    			activityType2LandCoverId.put(LandCoverActivityType.home,landCoverIds);
    		}
    		{
    			List<Integer> landCoverIds = new ArrayList<>();
    			landCoverIds.add(111); // continuous urban fabric
    			landCoverIds.add(112); // Discontinuous urban fabric
    			landCoverIds.add(121); //Industrial or commercial use
    			landCoverIds.add(123); //Port areas
    			landCoverIds.add(124); //Airports
    			landCoverIds.add(133); //Construction sites
    			landCoverIds.add(142); //Sport and leisure facilities
    			activityType2LandCoverId.put(LandCoverActivityType.other, landCoverIds);
    		}
    	}
    	else if(dataSource.equals(DataSource.UrbanAtlas)){
    		landCover_ShapeFile_AttributeID = "CODE2012";
    		List<Integer> basicLandUseIds = new ArrayList<>();

    		basicLandUseIds.add(11100); // continuous urban fabric S.L.>80%
			
			basicLandUseIds.add(11210); // Discontinuous urban fabric S.L. 50%-80%
			basicLandUseIds.add(11220); // Discontinuous urban fabric S.L. 30%-50%
			basicLandUseIds.add(11230); // Discontinuous urban fabric S.L. 10%-30%
			basicLandUseIds.add(11240); // Discontinuous urban fabric S.L.<10%
			
			basicLandUseIds.add(12100); //Industrial, commercial, public, military and private units

    		{	//land use types assigned to home and shopping activity
    			List<Integer> landCoverIds = new ArrayList<>();
    			landCoverIds.addAll(basicLandUseIds);
    			activityType2LandCoverId.put(LandCoverActivityType.home,landCoverIds);
    			activityType2LandCoverId.put(LandCoverActivityType.shopping,landCoverIds);
    		}
    		{	//land use types assigned to work activity
    			List<Integer> landCoverIds = new ArrayList<>();
    			landCoverIds.addAll(basicLandUseIds);
    			landCoverIds.add(12300); //Port areas
    			landCoverIds.add(12400); //Airports
    			landCoverIds.add(13300); //Construction sites

    			activityType2LandCoverId.put(LandCoverActivityType.work,landCoverIds);
    		}
    		{	//land use types assigned to leisure activity
    			List<Integer> landCoverIds = new ArrayList<>();
    			landCoverIds.addAll(basicLandUseIds);
    			landCoverIds.add(14100); //Green Urban Areas
    			landCoverIds.add(14200); //Sport and leisure facilities
//    			landCoverIds.add(31000); //Forests
    			activityType2LandCoverId.put(LandCoverActivityType.leisure,landCoverIds);
    		}
    		{	//all above mentioned id's are assigned to other
    			List<Integer> landCoverIds = new ArrayList<>();
    			landCoverIds.addAll(basicLandUseIds);
    			landCoverIds.add(12300); //Port areas
    			landCoverIds.add(12400); //Airports
    			landCoverIds.add(13300); //Construction sites
    			landCoverIds.add(14100); //Green Urban Areas
    			landCoverIds.add(14200); //Sport and leisure facilities
//    			landCoverIds.add(31000); //Forests
    			activityType2LandCoverId.put(LandCoverActivityType.other,landCoverIds);
    		}
    	}
    	else {
    		throw new IllegalArgumentException("no lookup list implented yet for type of landcover data source = " + dataSource.toString());
    	}
    	
    }

    public List<LandCoverActivityType> getActivityTypesFromZone(final int landCoverId){
        List<LandCoverActivityType> output = new ArrayList<>();
        for(LandCoverActivityType activityTypeFromLandCover : activityType2LandCoverId.keySet() ) {
            if (activityType2LandCoverId.get(activityTypeFromLandCover).contains(landCoverId)) {
                output.add(activityTypeFromLandCover);
            }
        }
        return output;
    }
    
    public DataSource getDataSource() {
    	return this.dataSource;
    }
    
    public String getLandCoverShapeFileAttributeID() {
    	return this.landCover_ShapeFile_AttributeID;
    }
}
