package playground.mdziakowski;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.geom.*;

import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import playground.mdziakowski.activityReLocation.RunReLocationPlansSauber;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ReLocationPlansTest {

    private static final String shapeFile = "testShapeFile.";
    private static List<Polygon> polygonList = new ArrayList<>();

    @BeforeClass
    public static void setUpContext() {
        createShapeFile();
    }

    @Test
    public void test() {
        Assert.assertTrue(true);
    }

    @Test
    public void readeShapeFileTest() {
        Map<String, Geometry> zones = RunReLocationPlansSauber.readShapeFile(shapeFile + "shp");
        String[] nameOfZones = {"testShapeFile.1", "testShapeFile.2", "testShapeFile.3", "testShapeFile.4" };
        Assert.assertEquals(4, zones.size());
        for (String str : nameOfZones) {
            Assert.assertTrue(zones.containsKey(str));
        }
    }

    @Test
    public void inDistrictTest() {
    }

    private static void createShapeFile() {

//        Collection<SimpleFeature> features = new ArrayList<>();

        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

        PolygonFeatureFactory polygonFeatureFactory = new PolygonFeatureFactory.Builder()
                .create();

        Coordinate[] coordZone1 = new Coordinate[5];
        coordZone1[0] = new Coordinate(0, 0);
        coordZone1[1] = new Coordinate(0, 50);
        coordZone1[2] = new Coordinate(50, 50);
        coordZone1[3] = new Coordinate(50, 0);
        coordZone1[4] = new Coordinate(0, 0);

        Coordinate[] coordZone2 = new Coordinate[5];
        coordZone2[0] = new Coordinate(0, 0);
        coordZone2[1] = new Coordinate(0, 50);
        coordZone2[2] = new Coordinate(-50, 50);
        coordZone2[3] = new Coordinate(-50, 0);
        coordZone1[4] = new Coordinate(0, 0);

        Coordinate[] coordZone3 = new Coordinate[5];
        coordZone3[0] = new Coordinate(0, 0);
        coordZone3[1] = new Coordinate(0, -50);
        coordZone3[2] = new Coordinate(50, -50);
        coordZone3[3] = new Coordinate(50, 0);
        coordZone1[4] = new Coordinate(0, 0);

        Coordinate[] coordZone4 = new Coordinate[5];
        coordZone4[0] = new Coordinate(0, 0);
        coordZone4[1] = new Coordinate(0, -50);
        coordZone4[2] = new Coordinate(-50, -50);
        coordZone4[3] = new Coordinate(-50, 0);
        coordZone1[4] = new Coordinate(0, 0);

        LinearRing ring1 = geometryFactory.createLinearRing(coordZone1);
        Polygon polygon1 = geometryFactory.createPolygon(ring1);
        polygonList.add(polygon1);

        LinearRing ring2 = geometryFactory.createLinearRing(coordZone2);
        Polygon polygon2 = geometryFactory.createPolygon(ring2);
        polygonList.add(polygon2);

        LinearRing ring3 = geometryFactory.createLinearRing(coordZone3);
        Polygon polygon3 = geometryFactory.createPolygon(ring3);
        polygonList.add(polygon3);

        LinearRing ring4 = geometryFactory.createLinearRing(coordZone4);
        Polygon polygon4 = geometryFactory.createPolygon(ring4);
        polygonList.add(polygon4);


//        features.add(polygonFeatureFactory.createPolygon(coordZone1, new HashMap<String, Object>() {{put("Id", new String("zone1"));}}, null));
//        features.add(polygonFeatureFactory.createPolygon(coordZone2, new HashMap<String, Object>() {{put("Id", new String("zone2"));}}, null));
//        features.add(polygonFeatureFactory.createPolygon(coordZone3, new HashMap<String, Object>() {{put("Id", new String("zone3"));}}, null));
//        features.add(polygonFeatureFactory.createPolygon(coordZone4, new HashMap<String, Object>() {{put("Id", new String("zone4"));}}, null));

//        ShapeFileWriter.writeGeometries(features, shapeFile + "shp");
    }

}
