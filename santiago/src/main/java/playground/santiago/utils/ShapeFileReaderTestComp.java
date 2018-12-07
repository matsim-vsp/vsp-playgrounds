package playground.santiago.utils;

import org.matsim.core.utils.gis.ShapeFileReader;

public class ShapeFileReaderTestComp {
	
	private static String inputDirectory = "../../shared-svn/santiago/scenario/inputForMATSim/";
	private static String cordonShapeFile = inputDirectory + "policies/cordon_triangle/modifiedCordon/modifiedTriangleEPSG32719.shp";

	public static void main(String[] args) {
		ShapeFileReader shapeReader = new ShapeFileReader();
		shapeReader.readFileAndInitialize(cordonShapeFile);

	}

}
