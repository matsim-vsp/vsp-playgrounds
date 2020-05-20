package playground.dziemke.moblaw;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * @author dziemke, cliesenjohann
 */
public class AddNodesAndLinksToNetwork_V2 {

	static void createNewBicycleNodesAndLinks(Network network) {

		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:4326", "EPSG:31464");

		// Trasse 1 - Y-Trasse

		Node node111 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_111"), transformation.transform(new Coord(13.540334, 52.435332)));
		Node node112 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_112"), transformation.transform(new Coord(13.526474, 52.426728)));
		Node node113 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_113"), transformation.transform(new Coord(13.511306, 52.427824)));
		Node node114 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_114"), transformation.transform(new Coord(13.457823, 52.458283)));
		Node node115 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_115"), transformation.transform(new Coord(13.460933, 52.468574)));
		Node node116 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_116"), transformation.transform(new Coord(13.460254, 52.484609)));
		Node node117 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_117"), transformation.transform(new Coord(13.443956, 52.494342)));
		Node node118 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_118"), transformation.transform(new Coord(13.426799, 52.499172)));
		Node node121 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_121"), transformation.transform(new Coord(13.441711, 52.458539)));
		Node node122 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_122"), transformation.transform(new Coord(13.435594, 52.469479)));
		Node node123 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_123"), transformation.transform(new Coord(13.420939, 52.467537)));
		Node node124 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_124"), transformation.transform(new Coord(13.417849, 52.480464)));
		Node node125 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_125"), transformation.transform(new Coord(13.406133, 52.482738)));
		Node node126 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_126"), transformation.transform(new Coord(13.407806, 52.488853)));
		
		// Trasse 2 - Tegel-Spandau 
		Node node211 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_211"), transformation.transform(new Coord(13.208553, 52.561455)));
		Node node212 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_212"), transformation.transform(new Coord(13.243014, 52.556564)));
		Node node213 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_213"), transformation.transform(new Coord(13.317921, 52.548542)));
		Node node214 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_214"), transformation.transform(new Coord(13.332571, 52.541571)));
		Node node215 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_215"), transformation.transform(new Coord(13.366000, 52.538190)));
		Node node216 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_216"), transformation.transform(new Coord(13.374095, 52.528755)));
		Node node221 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_221"), transformation.transform(new Coord(13.323781, 52.560932)));

		// Trasse 3 Königsweg
		Node node31 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_31"), transformation.transform(new Coord(13.091243, 52.413506)));
		Node node32 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_32"), transformation.transform(new Coord(13.135272, 52.396828)));
		Node node33 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_33"), transformation.transform(new Coord(13.130722, 52.391486)));
		Node node34 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_34"), transformation.transform(new Coord(13.196755, 52.415625)));
		Node node35 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_35"), transformation.transform(new Coord(13.179144, 52.421956)));
		Node node36 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_36"), transformation.transform(new Coord(13.189282, 52.433801)));
		Node node37 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_37"), transformation.transform(new Coord(13.270527, 52.498079)));

		// Trasse 4 - Panke-Trail
		Node node411 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_411"), transformation.transform(new Coord(13.386752, 52.531538 )));
		Node node412 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_412"), transformation.transform(new Coord(13.379252, 52.544204)));
		Node node413 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_413"), transformation.transform(new Coord(13.417620, 52.570229)));
		Node node414 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_414"), transformation.transform(new Coord(13.442371, 52.590746)));
		Node node415 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_415"), transformation.transform(new Coord(13.451556, 52.609713)));
		Node node416 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_416"), transformation.transform(new Coord(13.468089, 52.614573)));
		Node node421 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_421"), transformation.transform(new Coord(13.410311, 52.528444)));
		Node node422 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_422"), transformation.transform(new Coord(13.430438, 52.550693)));

		//Trasse 5 - West-Route
		Node node51 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_51"), transformation.transform(new Coord(13.119318, 52.528783)));
		Node node52 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_52"), transformation.transform(new Coord(13.178154, 52.516628)));
		Node node53 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_53"), transformation.transform(new Coord(13.21523, 52.509104)));
		Node node54 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_54"), transformation.transform(new Coord(13.228536, 52.506561)));
		Node node55 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_55"), transformation.transform(new Coord(13.259416, 52.508465)));
		Node node56 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_56"), transformation.transform(new Coord(13.30482, 52.511338)));
		Node node57 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_57"), transformation.transform(new Coord(13.337822, 52.513823)));

		//Trasse 6 - Teltowkanal-Route
		Node node61 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_61"), transformation.transform(new Coord(13.250979, 52.406020)));
		Node node62 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_62"), transformation.transform(new Coord(13.268574, 52.404710)));
		Node node63 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_63"), transformation.transform(new Coord(13.310245, 52.430623)));
		Node node64 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_64"), transformation.transform(new Coord(13.332821, 52.445797)));
		Node node65 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_65"), transformation.transform(new Coord(13.340202, 52.445666)));
		Node node66 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_66"), transformation.transform(new Coord(13.354879, 52.459031)));
		Node node67 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_67"), transformation.transform(new Coord(13.370071, 52.485173)));
		Node node68 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_68"), transformation.transform(new Coord(13.373333, 52.503097)));
		Node node69 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_69"), transformation.transform(new Coord(13.375265, 52.509210)));

		// Trasse 7 - Spandauer Damm - Freiheit
		Node node71 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_71"), transformation.transform(new Coord(13.199121, 52.535049)));
		Node node72 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_72"), transformation.transform(new Coord(13.204196, 52.536168)));
		Node node73 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_73"), transformation.transform(new Coord(13.210268, 52.532504)));
		Node node74 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_74"), transformation.transform(new Coord(13.246702, 52.527653)));
		Node node75 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_75"), transformation.transform(new Coord(13.247562, 52.524665)));
		Node node76 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_76"), transformation.transform(new Coord(13.284425, 52.519064)));

		// Trasse 8 - Nonnendammallee - Falkenseer Chaussee
		Node node81 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_81"), transformation.transform(new Coord(13.148180, 52.554237)));
		Node node82 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_82"), transformation.transform(new Coord(13.194871, 52.545678)));
		Node node83 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_83"), transformation.transform(new Coord(13.213325, 52.538291)));
		Node node84 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_84"), transformation.transform(new Coord(13.233894, 52.538838)));
		Node node85 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_85"), transformation.transform(new Coord(13.264246, 52.537053)));
		Node node86 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_86"), transformation.transform(new Coord(13.283642, 52.534297)));
		Node node87 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_87"), transformation.transform(new Coord(13.299605, 52.533249)));
		Node node88 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_88"), transformation.transform(new Coord(13.300990, 52.531030)));

		// Trasse 9 Landsberger Allee - Marzahn
		Node node91 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_91"), transformation.transform(new Coord(13.585235, 52.549332)));
		Node node92 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_92"), transformation.transform(new Coord(13.533895, 52.540273)));
		Node node93 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_93"), transformation.transform(new Coord(13.458395, 52.529840)));
		Node node94 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_94"), transformation.transform(new Coord(13.418211, 52.522535)));
		Node node95 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_95"), transformation.transform(new Coord(13.336485, 52.513829)));

		// Trasse 10 Heiligensee
		Node node1001 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_1001"), transformation.transform(new Coord(13.225422, 52.627326)));
		Node node1002 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_1002"), transformation.transform(new Coord(13.253250, 52.607936)));
		Node node1003 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_1003"), transformation.transform(new Coord(13.281659, 52.598631)));
		Node node1004 = NetworkUtils.createAndAddNode(network, Id.createNodeId(CombineNetworks.FAST_CYCLE_TRACK + "_1004"), transformation.transform(new Coord(13.292734, 52.581554)));


		/* Add the links */


		// Trasse 1 - Y-Trasse
		Link link101 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_101", node111, node112, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link102 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_102", node112, node113, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link103 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_103", node113, node114, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link104 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_104", node114, node115, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link105 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_105", node115, node116, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link106 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_106", node116, node117, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link107 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_107", node117, node118, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link108 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_108", node114, node121, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link109 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_109", node121, node122, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link1010 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_1010", node122, node123, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link1011 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_1011", node123, node124, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link1012 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_1012", node124, node125, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link1013 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_1013", node125, node126, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);

		// Trasse 2 - Tegel-Spandau
		Link link201 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_201", node211, node212, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link202 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_202", node212, node213, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link203 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_203", node213, node214, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link204 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_204", node214, node215, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link205 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_205", node215, node216, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link206 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_206", node213, node221, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);

		// Trasse 3 - Königsweg
		Link link301 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_301", node31, node35, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link302 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_302", node35, node36, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link303 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_303", node36, node37, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link304 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_304", node32, node35, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link305 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_305", node33, node34, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link306 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_306", node34, node35, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);

		// Trasse 4 - Panke-Trail
		Link link401 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_401", node411, node412, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link402 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_402", node412, node413, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link403 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_403", node413, node414, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link404 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_404", node414, node415, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link405 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_405", node415, node416, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link406 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_406", node421, node422, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link407 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_407", node422, node413, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);

		// Trasse 5 - West-Route
		Link link501 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_501", node51, node52, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link502 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_502", node52, node53, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link503 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_503", node53, node54, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link504 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_504", node54, node55, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link505 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_505", node55, node56, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link506 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_506", node56, node57, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);

		// Trasse 6 - Teltowkanal-Route
		Link link601 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_601", node61, node62, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link602 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_602", node62, node63, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link603 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_603", node63, node64, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link604 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_604", node64, node65, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link605 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_605", node65, node66, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link606 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_606", node66, node67, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link607 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_607", node67, node68, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link608 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_608", node68, node69, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);

		// Trasse 7 - Spandauer Damm-Freiheit
		Link link701 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_701", node71, node72, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link702 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_702", node72, node73, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link703 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_703", node73, node74, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link704 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_704", node74, node75, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link705 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_705", node75, node76, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);

		// Trasse 8 - Nonnendammallee-Falkenseer Chaussee
		Link link801 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_801", node81, node82, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link802 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_802", node82, node83, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link803 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_803", node83, node84, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link804 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_804", node84, node85, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link805 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_805", node85, node86, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link806 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_806", node86, node87, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link807 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_807", node87, node88, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);

		// Trasse 9 - Landsberger Allee-Marzahn
		Link link901 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_901", node91, node92, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link902 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_902", node92, node93, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link903 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_903", node93, node94, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link904 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_904", node94, node95, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);

		// Trasse 10 - Heiligensee
		Link link10001 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_10001", node1001, node1002, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link10002 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_10002", node1002, node1003, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link10003 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_10003", node1003, node1004, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link10004 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_10004", node1004, node221, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);

		// Links andere Richtung; Kennzeichnung mit _ hinter der ID

		// _Trasse 1 - Y-Trasse
		Link link101_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_101_", node112, node111, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link102_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_102_", node113, node112, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link103_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_103_", node114, node113, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link104_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_104_", node115, node114, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link105_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_105_", node116, node115, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link106_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_106_", node117, node116, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link107_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_107_", node118, node117, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link108_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_108_", node121, node114, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link109_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_109_", node122, node121, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link1010_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_1010_", node123, node122, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link1011_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_1011_", node124, node123, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link1012_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_1012_", node125, node124, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link1013_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_1013_", node126, node125, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);

		// _Trasse 2 - Tegel-Spandau
		Link link201_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_201_", node212, node211, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link202_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_202_", node213, node212, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link203_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_203_", node214, node213, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link204_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_204_", node215, node214, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link205_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_205_", node216, node215, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link206_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_206_", node221, node213, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);

		// _Trasse 3 - Königsweg
		Link link301_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_301_", node35, node31, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link302_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_302_", node36, node35, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link303_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_303_", node37, node36, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link304_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_304_", node35, node32, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link305_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_305_", node34, node33, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link306_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_306_", node35, node34, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);

		// _Trasse 4 - Panke-Trail
		Link link401_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_401_", node412, node411, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link402_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_402_", node413, node412, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link403_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_403_", node414, node413, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link404_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_404_", node415, node414, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link405_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_405_", node416, node415, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link406_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_406_", node422, node421, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link407_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_407_", node413, node422, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);

		// _Trasse 5 - West-Route
		Link link501_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_501_", node52, node51, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link502_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_502_", node53, node52, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link503_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_503_", node54, node53, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link504_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_504_", node55, node54, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link505_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_505_", node56, node55, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link506_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_506_", node57, node56, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);

		// _Trasse 6 - Teltowkanal-Route
		Link link601_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_601_", node62, node61, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link602_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_602_", node63, node62, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link603_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_603_", node64, node63, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link604_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_604_", node65, node64, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link605_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_605_", node66, node65, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link606_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_606_", node67, node66, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link607_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_607_", node68, node67, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link608_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_608_", node69, node68, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);

		// _Trasse 7 - Spandauer Damm-Freiheit
		Link link701_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_701_", node72, node71, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link702_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_702_", node73, node72, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link703_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_703_", node74, node73, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link704_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_704_", node75, node74, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link705_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_705_", node76, node75, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);

		// _Trasse 8 - Nonnendammallee-Falkenseer Chaussee
		Link link801_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_801_", node82, node81, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link802_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_802_", node83, node82, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link803_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_803_", node84, node83, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link804_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_804_", node85, node84, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link805_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_805_", node86, node85, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link806_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_806_", node87, node86, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link807_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_807_", node88, node87, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);

		// _Trasse 9 - Landsberger Allee-Marzahn
		Link link901_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_901_", node92, node91, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link902_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_902_", node93, node92, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link903_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_903_", node94, node93, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link904_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_904_", node95, node94, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);

		// _Trasse 10 - Heiligensee
		Link link10001_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_10001_", node1002, node1001, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link10002_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_10002_", node1003, node1002, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link10003_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_10003_", node1004, node1003, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
		Link link10004_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.FAST_CYCLE_TRACK + "_10004_", node221, node1004, CombineNetworks.FCT_BICYCLE_SPEED, CombineNetworks.FAST_CYCLE_TRACK);
	}
}