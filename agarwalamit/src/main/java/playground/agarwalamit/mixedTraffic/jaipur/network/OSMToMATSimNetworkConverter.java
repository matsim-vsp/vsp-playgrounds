package playground.agarwalamit.mixedTraffic.jaipur.network;

import java.util.Collection;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.utils.io.OsmNetworkReader.OsmFilter;
import org.opengis.feature.simple.SimpleFeature;

import playground.agarwalamit.mixedTraffic.jaipur.JaipurUtils;

/**
 * 
 * @author Amit
 *
 */
public class OSMToMATSimNetworkConverter {
	
	/**
	 * following files are available at https://github.com/amit2011/jaipur-data
	 */
	private static final String boundaryShapeFile = "..\\..\\jaipur-data\\shapeFile\\arcGIS\\jaipur_boundary\\District_Boundary.shp";
	private static final String inputOSMFile = "..\\..\\jaipur-data\\osm\\extracted\\jaipur_bb-box.osm";
	
	private static final String matsimNetworkFile = "..\\..\\jaipur-data\\matsimFiles\\jaipur_net_insideDistrictBoundary.xml.gz";
	
	public static void main(String[] args) {
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Network network = scenario.getNetwork();
		
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, JaipurUtils.EPSG);
		
		//TODO use bicycle OSM reader instead.
		OsmNetworkReader reader = new OsmNetworkReader(network, transformation);
		reader.addOsmFilter(new JaipurOSMFilter(boundaryShapeFile));
		reader.parse(inputOSMFile);
		
		new NetworkWriter(network).write(matsimNetworkFile);
		
	}

	static class JaipurOSMFilter implements OsmFilter {
		
		private final Geometry geometry;
		
		JaipurOSMFilter (String shapeFile){
			Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
			geometry = playground.agarwalamit.utils.geometry.GeometryUtils.getGeometryFromListOfFeatures(features);
		}

		@Override
		public boolean coordInFilter(Coord coord, int hierarchyLevel) {
			if (hierarchyLevel<=4) return true; //keep all
			else if (geometry.contains(MGC.coord2Point(coord)) && hierarchyLevel<=6) return true;
			return false;
		}
		
	}
	
	
}
