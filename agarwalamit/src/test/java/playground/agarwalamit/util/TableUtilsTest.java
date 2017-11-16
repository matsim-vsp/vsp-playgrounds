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
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;
import playground.agarwalamit.utils.TableUtils;

/**
 * Created by amit on 19/09/16.
 */


public class TableUtilsTest {

    @Test
    public void test(){
        Table<String,String, Map<String, Double>> tab = HashBasedTable.create();
        {
            Map<String, Double> emiss = new HashMap<>();
            emiss.put("CO2", 2.0);
            emiss.put("NOx", 3.0);
            emiss.put("NO2", 4.0);
            tab.put("amit","cold",emiss);
        }
        {
            Map<String, Double> emiss = new HashMap<>();
            emiss.put("CO2", 5.5);
            emiss.put("NOx", 2.4);
            emiss.put("NO2", 14.1);
            tab.put("agarwal","warm",emiss);
        }

        Map<String, Double> sumFromUtils = TableUtils.sumValues(tab);

        Map<String, Double> manualSum = new HashMap<>();
        manualSum.put("CO2",7.5);
        manualSum.put("NOx",5.4);
        manualSum.put("NO2",18.1);

        Assert.assertEquals("Size of map is wrong.",manualSum.size(), sumFromUtils.size(),
                MatsimTestUtils.EPSILON);

        for(String str : sumFromUtils.keySet()){
            System.out.println("str :"+str+"\t value :"+sumFromUtils.get(str));
            Assert.assertEquals("value sum from table is wrong.",manualSum.get(str), sumFromUtils.get(str),
                    MatsimTestUtils.EPSILON);
        }
    }

}
