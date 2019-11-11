package playground.dziemke.examples;

import org.locationtech.jts.geom.Envelope;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class TestCoordinateTransformation {

	public static void main(String[] args) {
		// WGS84 = EPSG:4326
		// Arc 1960 / UTM zone 37S = "EPSG:21037"
		// DHDN / 3-degree Gauss-Kruger zone 4 = "EPSG:31468"
//		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(
//				TransformationFactory.DHDN_GK4, TransformationFactory.WGS84);
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation
//				(TransformationFactory.GK4, TransformationFactory.WGS84);
//				("EPSG:31468", "EPSG:4326");
//				("EPSG:4326", "EPSG:21037");
//				("EPSG:4326", "EPSG:26911");
//				("EPSG:4326", "EPSG:25832"); // WGS84 -> Essen
//				("EPSG:3006", "EPSG:4326");
//				(TransformationFactory.WGS84_SA_Albers, "EPSG:4326");
//				("EPSG:32734", "EPSG:4326"); // EPSG:32734 = WGS 84 / UTM zone 34S, Cape Town
				("EPSG:4326", "EPSG:31464");

//		Coord originalCoord1 = new Coord(36.82829619497265, -1.291087691581653); // near Nairobi, Kenya
//		Coord originalCoord1 = new Coord(13.124627, 52.361485); // Berlin lower left
//		Coord originalCoord2 = new Coord(13.718465, 52.648131); // Berlin upper right
//		Coord originalCoord1 = new Coord(150583.9441831379,-3699678.99131796); // somewhere in NMB in SA_Albers
//		Coord originalCoord2 = new Coord(171583.944, y);
//		Coord coordNE = new Coord(41.88, 5.000); // Kenya northeast corner
//		Coord coordSW = new Coord(33.88, -4.75); // Kenya southwest corner 
//		Coord coordNE = new Coord(36.8014, -1.3055); // Kibera northeast corner 
//		Coord coordSW = new Coord(36.7715, -1.3198); // Kibera southwest corner 
//		Coord coordNE = new Coord(-119.55, 34.45); // Santa Barbara northeast corner
//		Coord coordSW = new Coord(-119.90, 34.38); // Santa Barbara southwest corner
//		Coord coordNE = new Coord(13.3643, 52.5526); // Berlin-GG northeast corner
//		Coord coordSW = new Coord(13.3275, 52.5384); // Berlin-GG southwest corner
//		Coord coordNE = new Coord(13.3436, 52.5268); // Berlin-SG northeast corner
//		Coord coordSW = new Coord(13.3121, 52.5119); // Berlin-SG southwest corner
//		Coord coordNE = new Coord(4434888.563,5416627.515); // Eichstaett
//		Coord coordSW = new Coord(4468750,5331600); // München
//		Coord coordNE = new Coord(11.110749318005148, 48.88366550799898); // Eichstaett
//		Coord coordSW = new Coord(11.57879257275828, 48.12168842748246); // München
//		Coord coordNE = new Coord(7.137, 51.533); // Essen NE
//		Coord coordSW = new Coord(6.895, 51.347); // Essen SW
//		Coord coordNE = new Coord(290000, 6251000); // Cape Town City Bowl NE
//		Coord coordSW = new Coord(256000, 6222000); // Cape Town City Bowl SW
		Coord coordNE = new Coord(7.994, 51.826); // Ruhrgebiet NE
//		Coord coordSW = new Coord(6.283, 51.212); // Ruhrgebiet SW
		Coord coordSW = new Coord(13.091243, 52.413506); // Königsweg Bln



		
		Coord convertedCoordNE = transformation.transform(coordNE);
		Coord convertedCoordSW = transformation.transform(coordSW);
		
		System.out.println("###########################################################################");
		System.out.println("coordNE: " + coordNE);
		System.out.println("coordSW: " + coordSW);
		System.out.println("convertedCoordNE: " + convertedCoordNE);
		System.out.println("convertedCoordSW: " + convertedCoordSW);
		System.out.println("###########################################################################");
	}
}