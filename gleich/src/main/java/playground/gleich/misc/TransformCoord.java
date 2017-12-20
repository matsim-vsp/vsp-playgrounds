package playground.gleich.misc;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class TransformCoord {

	public static void main(String[] args) {
		String epsg = args[0];
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(
				epsg, TransformationFactory.WGS84);
		Coord coord = CoordUtils.createCoord(Double.parseDouble(args[1]), Double.parseDouble(args[2]));
		System.out.println(transformation.transform(coord));
	}
}
