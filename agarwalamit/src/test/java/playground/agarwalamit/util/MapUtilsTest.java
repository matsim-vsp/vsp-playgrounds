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

package playground.agarwalamit.util;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;
import playground.agarwalamit.utils.MapUtils;

/**
 * Created by amit on 19/09/16.
 */


public class MapUtilsTest {

    private final MatsimTestUtils helper = new MatsimTestUtils();

    @Test
    public void testDoubleValueSum(){

        Map<String, Integer> str2Int = new HashMap<>();
        str2Int.put("A",4);
        str2Int.put("B",8);
        str2Int.put("C",12);
        str2Int.put("D",16);

        //value sum
        int sumFromUtil = MapUtils.intValueSum(str2Int);
        int sum = 40;
        Assert.assertEquals("Sum is wrong",sum,sumFromUtil,MatsimTestUtils.EPSILON);

        Map<String, Double> str2Double = new HashMap<>();
        str2Double.put("A",4.);
        str2Double.put("B",8.);
        str2Double.put("C",12.);
        str2Double.put("D",16.);

        //value sum
        Assert.assertEquals("Sum is wrong",40.0, MapUtils.doubleValueSum(str2Double),MatsimTestUtils.EPSILON);

    }

    @Test
    public void testAddMaps(){ //merge

        Map<String, Double> map1 = new HashMap<>();
        map1.put("A",4.);
        map1.put("B",8.);
        map1.put("C",12.);
        map1.put("D",16.);

        Map<String, Double> map2 = new HashMap<>();
        map2.put("A",4.);
        map2.put("B",8.);
        map2.put("C",12.);
        map2.put("D",16.);

        //value sum
        Assert.assertEquals("Sum is wrong",80.0, MapUtils.doubleValueSum(MapUtils.addMaps(map1,map2)),MatsimTestUtils.EPSILON);

        // remove something from map1
        map1.remove("A");
        Assert.assertEquals("Sum is wrong",76.0, MapUtils.doubleValueSum(MapUtils.addMaps(map1,map2)),MatsimTestUtils.EPSILON);

        map2.remove("B");
        Assert.assertEquals("Sum is wrong",68.0, MapUtils.doubleValueSum(MapUtils.addMaps(map1,map2)),MatsimTestUtils.EPSILON);

        map2.clear();
        Assert.assertEquals("Sum is wrong",36.0, MapUtils.doubleValueSum(MapUtils.addMaps(map1,map2)),MatsimTestUtils.EPSILON);

        map1.clear();
        map2.put("B",4.0);
        Assert.assertEquals("Sum is wrong",4.0, MapUtils.doubleValueSum(MapUtils.addMaps(map1,map2)),MatsimTestUtils.EPSILON);

        map2.clear();
        Assert.assertEquals("Sum is wrong",0.0, MapUtils.doubleValueSum(MapUtils.addMaps(map1,map2)),MatsimTestUtils.EPSILON);
    }

    @Test
    public void testValueMapSum(){

        Map<String, Double> map1 = new HashMap<>();
        map1.put("A",4.);
        map1.put("B",8.);
        map1.put("C",12.);
        map1.put("D",16.);

        Map<String, Double> map2 = new HashMap<>();
        map2.put("A",4.);
        map2.put("B",8.);
        map2.put("C",12.);
        map2.put("D",16.);

        Map<Double, Map<String, Double>> inMap = new HashMap<>();
        inMap.put(3600., map1);
        inMap.put(7200., map2);

        Map<String, Double> outMap = MapUtils.valueMapSum(inMap);
        Map<String, Double> manualCheck = new HashMap<>();
        manualCheck.put("A",8.);
        manualCheck.put("B",16.);
        manualCheck.put("C",24.);
        manualCheck.put("D",32.);

        outMap.entrySet()
              .forEach(e -> Assert.assertEquals("wrong value",
                      e.getValue(),
                      manualCheck.get(e.getKey()),
                      MatsimTestUtils.EPSILON));

    }

}
