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

package playground.agarwalamit.templates;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import com.google.common.base.Charsets;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import playground.agarwalamit.utils.FileUtils;

/**
 * Created by amit on 21.01.18.
 */

public class SparkRDDExample {


    public static void main(String[] args) {

        String file = FileUtils.SHARED_SVN+"/projects/detailedEval/matsim-input-files/hbefa-files/v3.2/EFA_HOT_Subsegm_2005detailed.txt";

        //spark RDD
        long time = System.currentTimeMillis();

        SparkConf sparkConf = new SparkConf()
                .setAppName("Hello Spark!")
                .setMaster("local");

        JavaSparkContext sc = new JavaSparkContext(sparkConf);
        JavaRDD<String> lines = sc.textFile(file);
        System.out.println("\n\n\n\n");
        System.out.println(lines.count());
        System.out.println(lines.first());
        System.out.println(lines.filter(l -> l.contains("CO")).count());
        System.out.println("\n\n\n\n");
        sc.close();
        sc.stop();

        long timeTaken = System.currentTimeMillis() - time;
        System.out.println("Total time taken : " + timeTaken);

        //without spark
        time = System.currentTimeMillis();
        List<String> rows = null;
        try {
            rows = Files.readAllLines(Paths.get(file), Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
        System.out.println("\n\n\n\n");
        System.out.println(rows.size());
        System.out.println( rows.get(0) );
        System.out.println(rows.stream().filter(l -> l.contains("CO")).count());
        System.out.println("\n\n\n\n");

        timeTaken = System.currentTimeMillis() - time;
        System.out.println("Total time taken : " + timeTaken);

    }

}
