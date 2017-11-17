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

package playground.agarwalamit.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.common.collect.Table;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * Created by amit on 16.11.17.
 */

public final class TableUtils {

    public static Map<String, Double> sumValues(Table<?, ?, Map<String, Double>> table) {
        return table.values()
                    .stream()
                    .flatMap(m -> m.entrySet().stream())
                    .collect(Collectors.groupingBy(Map.Entry::getKey,
                            Collectors.summingDouble(Map.Entry::getValue)));
//             .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Double::sum));

    }

    public static Map<Double, Map<String, Double>> sumForAllLinks(Table<Double, Id<Link>, Map<String, Double>> table){
        Set<Double> times = table.rowKeySet();
        Map<Double, Map<String, Double>> time2Emiss = new HashMap<>();
        for (double d : times) {
            time2Emiss.put(d,
                    table.row(d)
                         .values()
                         .stream()
                         .flatMap(m -> m.entrySet().stream())
                         .collect(Collectors.groupingBy(Map.Entry::getKey,
                                 Collectors.summingDouble(Map.Entry::getValue))));
        }
        return time2Emiss;
    }
}

