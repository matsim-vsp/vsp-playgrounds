package playground.gleich.misc;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.util.Collection;

public class GetShapeFileAttributes {

    public static void main(String[] args) {
//        String shapeFile = "/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/vulkaneifel.shp";
        String shapeFile = "/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/berlin/v1/Bezirksregionen_zone_UTM32N/Bezirksregionen_zone_UTM32N_fixed.shp";


        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
        Collection<SimpleFeature> features2 = ShapeFileReader.getAllFeatures("/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/vulkaneifel.shp");

        for(SimpleFeature feature: features) {
            System.out.println(feature.getAttribute("NO"));
            feature.getAttributes().stream().forEach(att -> System.out.println(att.toString() + " " + att.getClass()));
        }

        for(SimpleFeature feature: features2) {
            System.out.println(" TATATA: " +feature.getAttribute("id"));
            feature.getAttributes().stream().forEach(att -> System.out.println(att.toString() + " " + att.getClass()));
        }
    }
}
