package playground.dziemke.moblaw;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * @author dziemke, cliesenjohann
 */
public class AddNodesAndLinksToNetwork_V2 {

	public static void main(String[] args) {
		String inputNetworkFileName = "../../runs-svn/open_berlin_scenario/v5.5-bicycle/bc-22/output/berlin-v5.5-1pct-22.output_network.xml.gz";
		String outputNetworkFileName = "../../runs-svn/open_berlin_scenario/v5.5-bicycle/network_bicycle_1.xml.gz";
		double bicycleSpeed = 25.0 / 3.6;

		// bicycleSpeed = fast cycle track speed; im zweiten Szenario gibt es pblSpeed (protected bike lane speed) ud npSpeed (normal path speed) 

		// string path type in Methode createBicycleLink unten?? 

		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:4326", "EPSG:31464");

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(inputNetworkFileName);

		Network network = scenario.getNetwork();

		// ---

		// Trasse 1 - Y-Trasse

		Node node111 = NetworkUtils.createNode(Id.createNodeId("fct_111"), transformation.transform(new Coord(13.540334, 52.435332)));
		network.addNode(node111);
		Node node112 = NetworkUtils.createNode(Id.createNodeId("fct_112"), transformation.transform(new Coord(13.526474, 52.426728)));
		network.addNode(node112);
		Node node113 = NetworkUtils.createNode(Id.createNodeId("fct_113"), transformation.transform(new Coord(13.511306, 52.427824)));
		network.addNode(node113);
		Node node114 = NetworkUtils.createNode(Id.createNodeId("fct_114"), transformation.transform(new Coord(13.457823, 52.458283)));
		network.addNode(node114);
		Node node115 = NetworkUtils.createNode(Id.createNodeId("fct_115"), transformation.transform(new Coord(13.460933, 52.468574)));
		network.addNode(node115);
		Node node116 = NetworkUtils.createNode(Id.createNodeId("fct_116"), transformation.transform(new Coord(13.460254, 52.484609)));
		network.addNode(node116);
		Node node117 = NetworkUtils.createNode(Id.createNodeId("fct_117"), transformation.transform(new Coord(13.443956, 52.494342)));
		network.addNode(node117);
		Node node118 = NetworkUtils.createNode(Id.createNodeId("fct_118"), transformation.transform(new Coord(13.426799, 52.499172)));
		network.addNode(node118);
		Node node121 = NetworkUtils.createNode(Id.createNodeId("fct_121"), transformation.transform(new Coord(13.441711, 52.458539)));
		network.addNode(node121);
		Node node122 = NetworkUtils.createNode(Id.createNodeId("fct_122"), transformation.transform(new Coord(13.435594, 52.469479)));
		network.addNode(node122);
		Node node123 = NetworkUtils.createNode(Id.createNodeId("fct_123"), transformation.transform(new Coord(13.420939, 52.467537)));
		network.addNode(node123);
		Node node124 = NetworkUtils.createNode(Id.createNodeId("fct_124"), transformation.transform(new Coord(13.417849, 52.480464)));
		network.addNode(node124);
		Node node125 = NetworkUtils.createNode(Id.createNodeId("fct_125"), transformation.transform(new Coord(13.406133, 52.482738)));
		network.addNode(node125);
		Node node126 = NetworkUtils.createNode(Id.createNodeId("fct_126"), transformation.transform(new Coord(13.407806, 52.488853)));
		network.addNode(node126);
		

		// Trasse 2 - Tegel-Spandau 
		Node node211 = NetworkUtils.createNode(Id.createNodeId("fct_211"), transformation.transform(new Coord(13.208553, 52.561455)));
		network.addNode(node211);
		Node node212 = NetworkUtils.createNode(Id.createNodeId("fct_212"), transformation.transform(new Coord(13.243014, 52.556564)));
		network.addNode(node212);
		Node node213 = NetworkUtils.createNode(Id.createNodeId("fct_213"), transformation.transform(new Coord(13.317921, 52.548542)));
		network.addNode(node213);
		Node node214 = NetworkUtils.createNode(Id.createNodeId("fct_214"), transformation.transform(new Coord(13.332571, 52.541571)));
		network.addNode(node214);
		Node node215 = NetworkUtils.createNode(Id.createNodeId("fct_215"), transformation.transform(new Coord(13.366000, 52.538190)));
		network.addNode(node215);
		Node node216 = NetworkUtils.createNode(Id.createNodeId("fct_216"), transformation.transform(new Coord(13.374095, 52.528755)));
		network.addNode(node216);
		Node node221 = NetworkUtils.createNode(Id.createNodeId("fct_221"), transformation.transform(new Coord(13.323781, 52.560932)));
		network.addNode(node221);

		// Trasse 3 Königsweg
		Node node31 = NetworkUtils.createNode(Id.createNodeId("fct_31"), transformation.transform(new Coord(13.091243, 52.413506)));
		network.addNode(node31);
		Node node32 = NetworkUtils.createNode(Id.createNodeId("fct_32"), transformation.transform(new Coord(13.135272, 52.396828)));
		network.addNode(node32);
		Node node33 = NetworkUtils.createNode(Id.createNodeId("fct_33"), transformation.transform(new Coord(13.130722, 52.391486)));
		network.addNode(node33);
		Node node34 = NetworkUtils.createNode(Id.createNodeId("fct_34"), transformation.transform(new Coord(13.196755, 52.415625)));
		network.addNode(node34);
		Node node35 = NetworkUtils.createNode(Id.createNodeId("fct_35"), transformation.transform(new Coord(13.179144, 52.421956)));
		network.addNode(node35);
		Node node36 = NetworkUtils.createNode(Id.createNodeId("fct_36"), transformation.transform(new Coord(13.189282, 52.433801)));
		network.addNode(node36);
		Node node37 = NetworkUtils.createNode(Id.createNodeId("fct_37"), transformation.transform(new Coord(13.270527, 52.498079)));
		network.addNode(node37);

		// Trasse 4 - Panke-Trail
		Node node411 = NetworkUtils.createNode(Id.createNodeId("fct_411"), transformation.transform(new Coord(13.386752, 52.531538 )));
		network.addNode(node411);
		Node node412 = NetworkUtils.createNode(Id.createNodeId("fct_412"), transformation.transform(new Coord(13.379252, 52.544204)));
		network.addNode(node412);
		Node node413 = NetworkUtils.createNode(Id.createNodeId("fct_413"), transformation.transform(new Coord(13.417620, 52.570229)));
		network.addNode(node413);
		Node node414 = NetworkUtils.createNode(Id.createNodeId("fct_414"), transformation.transform(new Coord(13.442371, 52.590746)));
		network.addNode(node414);
		Node node415 = NetworkUtils.createNode(Id.createNodeId("fct_415"), transformation.transform(new Coord(13.451556, 52.609713)));
		network.addNode(node415);
		Node node416 = NetworkUtils.createNode(Id.createNodeId("fct_416"), transformation.transform(new Coord(13.468089, 52.614573)));
		network.addNode(node416);
		Node node421 = NetworkUtils.createNode(Id.createNodeId("fct_421"), transformation.transform(new Coord(13.410311, 52.528444)));
		network.addNode(node421);
		Node node422 = NetworkUtils.createNode(Id.createNodeId("fct_422"), transformation.transform(new Coord(13.430438, 52.550693)));
		network.addNode(node422);

		//Trasse 5 - West-Route
		Node node51 = NetworkUtils.createNode(Id.createNodeId("fct_51"), transformation.transform(new Coord(13.119318, 52.528783)));
		network.addNode(node51);
		Node node52 = NetworkUtils.createNode(Id.createNodeId("fct_52"), transformation.transform(new Coord(13.178154, 52.516628)));
		network.addNode(node52);
		Node node53 = NetworkUtils.createNode(Id.createNodeId("fct_53"), transformation.transform(new Coord(13.21523, 52.509104)));
		network.addNode(node53);
		Node node54 = NetworkUtils.createNode(Id.createNodeId("fct_54"), transformation.transform(new Coord(13.228536, 52.506561)));
		network.addNode(node54);
		Node node55 = NetworkUtils.createNode(Id.createNodeId("fct_55"), transformation.transform(new Coord(13.259416, 52.508465)));
		network.addNode(node55);
		Node node56 = NetworkUtils.createNode(Id.createNodeId("fct_56"), transformation.transform(new Coord(13.30482, 52.511338)));
		network.addNode(node56);
		Node node57 = NetworkUtils.createNode(Id.createNodeId("fct_57"), transformation.transform(new Coord(13.337822, 52.513823)));
		network.addNode(node57);

		//Trasse 6 - Teltowkanal-Route
		Node node61 = NetworkUtils.createNode(Id.createNodeId("fct_61"), transformation.transform(new Coord(13.250979, 52.406020)));
		network.addNode(node61);
		Node node62 = NetworkUtils.createNode(Id.createNodeId("fct_62"), transformation.transform(new Coord(13.268574, 52.404710)));
		network.addNode(node62);
		Node node63 = NetworkUtils.createNode(Id.createNodeId("fct_63"), transformation.transform(new Coord(13.310245, 52.430623)));
		network.addNode(node63);
		Node node64 = NetworkUtils.createNode(Id.createNodeId("fct_64"), transformation.transform(new Coord(13.332821, 52.445797)));
		network.addNode(node64);
		Node node65 = NetworkUtils.createNode(Id.createNodeId("fct_65"), transformation.transform(new Coord(13.340202, 52.445666)));
		network.addNode(node65);
		Node node66 = NetworkUtils.createNode(Id.createNodeId("fct_66"), transformation.transform(new Coord(13.354879, 52.459031)));
		network.addNode(node66);
		Node node67 = NetworkUtils.createNode(Id.createNodeId("fct_67"), transformation.transform(new Coord(13.370071, 52.485173)));
		network.addNode(node67);
		Node node68 = NetworkUtils.createNode(Id.createNodeId("fct_68"), transformation.transform(new Coord(13.373333, 52.503097)));
		network.addNode(node68);
		Node node69 = NetworkUtils.createNode(Id.createNodeId("fct_69"), transformation.transform(new Coord(13.375265, 52.509210)));
		network.addNode(node69);

		// Trasse 7 - Spandauer Damm - Freiheit
		Node node71 = NetworkUtils.createNode(Id.createNodeId("fct_71"), transformation.transform(new Coord(13.199121, 52.535049)));
		network.addNode(node71);
		Node node72 = NetworkUtils.createNode(Id.createNodeId("fct_72"), transformation.transform(new Coord(13.204196, 52.536168)));
		network.addNode(node72);
		Node node73 = NetworkUtils.createNode(Id.createNodeId("fct_73"), transformation.transform(new Coord(13.210268, 52.532504)));
		network.addNode(node73);
		Node node74 = NetworkUtils.createNode(Id.createNodeId("fct_74"), transformation.transform(new Coord(13.246702, 52.527653)));
		network.addNode(node74);
		Node node75 = NetworkUtils.createNode(Id.createNodeId("fct_75"), transformation.transform(new Coord(13.247562, 52.524665)));
		network.addNode(node75);
		Node node76 = NetworkUtils.createNode(Id.createNodeId("fct_76"), transformation.transform(new Coord(13.284425, 52.519064)));
		network.addNode(node76);

		// Trasse 8 - Nonnendammallee - Falkenseer Chaussee
		Node node81 = NetworkUtils.createNode(Id.createNodeId("fct_81"), transformation.transform(new Coord(13.148180, 52.554237)));
		network.addNode(node81);
		Node node82 = NetworkUtils.createNode(Id.createNodeId("fct_82"), transformation.transform(new Coord(13.194871, 52.545678)));
		network.addNode(node82);
		Node node83 = NetworkUtils.createNode(Id.createNodeId("fct_83"), transformation.transform(new Coord(13.213325, 52.538291)));
		network.addNode(node83);
		Node node84 = NetworkUtils.createNode(Id.createNodeId("fct_84"), transformation.transform(new Coord(13.233894, 52.538838)));
		network.addNode(node84);
		Node node85 = NetworkUtils.createNode(Id.createNodeId("fct_85"), transformation.transform(new Coord(13.264246, 52.537053)));
		network.addNode(node85);
		Node node86 = NetworkUtils.createNode(Id.createNodeId("fct_86"), transformation.transform(new Coord(13.283642, 52.534297)));
		network.addNode(node86);
		Node node87 = NetworkUtils.createNode(Id.createNodeId("fct_87"), transformation.transform(new Coord(13.299605, 52.533249)));
		network.addNode(node87);
		Node node88 = NetworkUtils.createNode(Id.createNodeId("fct_88"), transformation.transform(new Coord(13.300990, 52.531030)));
		network.addNode(node88);

		// Trasse 9 Landsberger Allee - Marzahn
		Node node91 = NetworkUtils.createNode(Id.createNodeId("fct_91"), transformation.transform(new Coord(13.585235, 52.549332)));
		network.addNode(node91);
		Node node92 = NetworkUtils.createNode(Id.createNodeId("fct_92"), transformation.transform(new Coord(13.533895, 52.540273)));
		network.addNode(node92);
		Node node93 = NetworkUtils.createNode(Id.createNodeId("fct_93"), transformation.transform(new Coord(13.458395, 52.529840)));
		network.addNode(node93);
		Node node94 = NetworkUtils.createNode(Id.createNodeId("fct_94"), transformation.transform(new Coord(13.418211, 52.522535)));
		network.addNode(node94);
		Node node95 = NetworkUtils.createNode(Id.createNodeId("fct_95"), transformation.transform(new Coord(13.336485, 52.513829)));
		network.addNode(node95);

		// Trasse 10 Heiligensee
		Node node1001 = NetworkUtils.createNode(Id.createNodeId("fct_1001"), transformation.transform(new Coord(13.225422, 52.627326)));
		network.addNode(node1001);
		Node node1002 = NetworkUtils.createNode(Id.createNodeId("fct_1002"), transformation.transform(new Coord(13.253250, 52.607936)));
		network.addNode(node1002);
		Node node1003 = NetworkUtils.createNode(Id.createNodeId("fct_1003"), transformation.transform(new Coord(13.281659, 52.598631)));
		network.addNode(node1003);
		Node node1004 = NetworkUtils.createNode(Id.createNodeId("fct_1004"), transformation.transform(new Coord(13.292734, 52.581554)));
		network.addNode(node1004);



		// ... more nodes accorinding to list...



		// Trasse 1 - Y-Trasse
		Link link101 = createBicycleLink(network,"fct_101", node111, node112, bicycleSpeed, "fct");
		Link link102 = createBicycleLink(network,"fct_102", node112, node113, bicycleSpeed, "fct");
		Link link103 = createBicycleLink(network,"fct_103", node113, node114, bicycleSpeed, "fct");
		Link link104 = createBicycleLink(network,"fct_104", node114, node115, bicycleSpeed, "fct");
		Link link105 = createBicycleLink(network,"fct_105", node115, node116, bicycleSpeed, "fct");
		Link link106 = createBicycleLink(network,"fct_106", node116, node117, bicycleSpeed, "fct");
		Link link107 = createBicycleLink(network,"fct_107", node117, node118, bicycleSpeed, "fct");
		Link link108 = createBicycleLink(network,"fct_108", node114, node121, bicycleSpeed, "fct");
		Link link109 = createBicycleLink(network,"fct_109", node121, node122, bicycleSpeed, "fct");
		Link link1010 = createBicycleLink(network,"fct_1010", node122, node123, bicycleSpeed, "fct");
		Link link1011 = createBicycleLink(network,"fct_1011", node123, node124, bicycleSpeed, "fct");
		Link link1012 = createBicycleLink(network,"fct_1012", node124, node125, bicycleSpeed, "fct");
		Link link1013 = createBicycleLink(network,"fct_1013", node125, node126, bicycleSpeed, "fct");

		// Trasse 2 - Tegel-Spandau
		Link link201 = createBicycleLink(network,"fct_201", node211, node212, bicycleSpeed, "fct");
		Link link202 = createBicycleLink(network,"fct_202", node212, node213, bicycleSpeed, "fct");
		Link link203 = createBicycleLink(network,"fct_203", node213, node214, bicycleSpeed, "fct");
		Link link204 = createBicycleLink(network,"fct_204", node214, node215, bicycleSpeed, "fct");
		Link link205 = createBicycleLink(network,"fct_205", node215, node216, bicycleSpeed, "fct");
		Link link206 = createBicycleLink(network,"fct_206", node213, node221, bicycleSpeed, "fct");

		// Trasse 3 - Königsweg
		Link link301 = createBicycleLink(network,"fct_301", node31, node35, bicycleSpeed, "fct");
		Link link302 = createBicycleLink(network,"fct_302", node35, node36, bicycleSpeed, "fct");
		Link link303 = createBicycleLink(network,"fct_303", node36, node37, bicycleSpeed, "fct");
		Link link304 = createBicycleLink(network,"fct_304", node32, node35, bicycleSpeed, "fct");
		Link link305 = createBicycleLink(network,"fct_305", node33, node34, bicycleSpeed, "fct");
		Link link306 = createBicycleLink(network,"fct_306", node34, node35, bicycleSpeed, "fct");

		// Trasse 4 - Panke-Trail
		Link link401 = createBicycleLink(network,"fct_401", node411, node412, bicycleSpeed, "fct");
		Link link402 = createBicycleLink(network,"fct_402", node412, node413, bicycleSpeed, "fct");
		Link link403 = createBicycleLink(network,"fct_403", node413, node414, bicycleSpeed, "fct");
		Link link404 = createBicycleLink(network,"fct_404", node414, node415, bicycleSpeed, "fct");
		Link link405 = createBicycleLink(network,"fct_405", node415, node416, bicycleSpeed, "fct");
		Link link406 = createBicycleLink(network,"fct_406", node421, node422, bicycleSpeed, "fct");
		Link link407 = createBicycleLink(network,"fct_407", node422, node413, bicycleSpeed, "fct");

		// Trasse 5 - West-Route
		Link link501 = createBicycleLink(network,"fct_501", node51, node52, bicycleSpeed, "fct");
		Link link502 = createBicycleLink(network,"fct_502", node52, node53, bicycleSpeed, "fct");
		Link link503 = createBicycleLink(network,"fct_503", node53, node54, bicycleSpeed, "fct");
		Link link504 = createBicycleLink(network,"fct_504", node54, node55, bicycleSpeed, "fct");
		Link link505 = createBicycleLink(network,"fct_505", node55, node56, bicycleSpeed, "fct");
		Link link506 = createBicycleLink(network,"fct_506", node56, node57, bicycleSpeed, "fct");

		// Trasse 6 - Teltowkanal-Route
		Link link601 = createBicycleLink(network,"fct_601", node61, node62, bicycleSpeed, "fct");
		Link link602 = createBicycleLink(network,"fct_602", node62, node63, bicycleSpeed, "fct");
		Link link603 = createBicycleLink(network,"fct_603", node63, node64, bicycleSpeed, "fct");
		Link link604 = createBicycleLink(network,"fct_604", node64, node65, bicycleSpeed, "fct");
		Link link605 = createBicycleLink(network,"fct_605", node65, node66, bicycleSpeed, "fct");
		Link link606 = createBicycleLink(network,"fct_606", node66, node67, bicycleSpeed, "fct");
		Link link607 = createBicycleLink(network,"fct_607", node67, node68, bicycleSpeed, "fct");
		Link link608 = createBicycleLink(network,"fct_608", node68, node69, bicycleSpeed, "fct");

		// Trasse 7 - Spandauer Damm-Freiheit
		Link link701 = createBicycleLink(network,"fct_701", node71, node72, bicycleSpeed, "fct");
		Link link702 = createBicycleLink(network,"fct_702", node72, node73, bicycleSpeed, "fct");
		Link link703 = createBicycleLink(network,"fct_703", node73, node74, bicycleSpeed, "fct");
		Link link704 = createBicycleLink(network,"fct_704", node74, node75, bicycleSpeed, "fct");
		Link link705 = createBicycleLink(network,"fct_705", node75, node76, bicycleSpeed, "fct");

		// Trasse 8 - Nonnendammallee-Falkenseer Chaussee
		Link link801 = createBicycleLink(network,"fct_801", node81, node82, bicycleSpeed, "fct");
		Link link802 = createBicycleLink(network,"fct_802", node82, node83, bicycleSpeed, "fct");
		Link link803 = createBicycleLink(network,"fct_803", node83, node84, bicycleSpeed, "fct");
		Link link804 = createBicycleLink(network,"fct_804", node84, node85, bicycleSpeed, "fct");
		Link link805 = createBicycleLink(network,"fct_805", node85, node86, bicycleSpeed, "fct");
		Link link806 = createBicycleLink(network,"fct_806", node86, node87, bicycleSpeed, "fct");
		Link link807 = createBicycleLink(network,"fct_807", node87, node88, bicycleSpeed, "fct");

		// Trasse 9 - Landsberger Allee-Marzahn
		Link link901 = createBicycleLink(network,"fct_901", node91, node92, bicycleSpeed, "fct");
		Link link902 = createBicycleLink(network,"fct_902", node92, node93, bicycleSpeed, "fct");
		Link link903 = createBicycleLink(network,"fct_903", node93, node94, bicycleSpeed, "fct");
		Link link904 = createBicycleLink(network,"fct_904", node94, node95, bicycleSpeed, "fct");

		// Trasse 10 - Heiligensee
		Link link10001 = createBicycleLink(network,"fct_10001", node1001, node1002, bicycleSpeed, "fct");
		Link link10002 = createBicycleLink(network,"fct_10002", node1002, node1003, bicycleSpeed, "fct");
		Link link10003 = createBicycleLink(network,"fct_10003", node1003, node1004, bicycleSpeed, "fct");
		Link link10004 = createBicycleLink(network,"fct_10004", node1004, node221, bicycleSpeed, "fct");

		// Links andere Richtung; Kennzeichnung mit _ hinter der ID

		// _Trasse 1 - Y-Trasse
		Link link101_ = createBicycleLink(network,"fct_101_", node112, node111, bicycleSpeed, "fct");
		Link link102_ = createBicycleLink(network,"fct_102_", node113, node112, bicycleSpeed, "fct");
		Link link103_ = createBicycleLink(network,"fct_103_", node114, node113, bicycleSpeed, "fct");
		Link link104_ = createBicycleLink(network,"fct_104_", node115, node114, bicycleSpeed, "fct");
		Link link105_ = createBicycleLink(network,"fct_105_", node116, node115, bicycleSpeed, "fct");
		Link link106_ = createBicycleLink(network,"fct_106_", node117, node116, bicycleSpeed, "fct");
		Link link107_ = createBicycleLink(network,"fct_107_", node118, node117, bicycleSpeed, "fct");
		Link link108_ = createBicycleLink(network,"fct_108_", node121, node114, bicycleSpeed, "fct");
		Link link109_ = createBicycleLink(network,"fct_109_", node122, node121, bicycleSpeed, "fct");
		Link link1010_ = createBicycleLink(network,"fct_1010_", node123, node122, bicycleSpeed, "fct");
		Link link1011_ = createBicycleLink(network,"fct_1011_", node124, node123, bicycleSpeed, "fct");
		Link link1012_ = createBicycleLink(network,"fct_1012_", node125, node124, bicycleSpeed, "fct");
		Link link1013_ = createBicycleLink(network,"fct_1013_", node126, node125, bicycleSpeed, "fct");

		// _Trasse 2 - Tegel-Spandau
		Link link201_ = createBicycleLink(network,"fct_201_", node212, node211, bicycleSpeed, "fct");
		Link link202_ = createBicycleLink(network,"fct_202_", node213, node212, bicycleSpeed, "fct");
		Link link203_ = createBicycleLink(network,"fct_203_", node214, node213, bicycleSpeed, "fct");
		Link link204_ = createBicycleLink(network,"fct_204_", node215, node214, bicycleSpeed, "fct");
		Link link205_ = createBicycleLink(network,"fct_205_", node216, node215, bicycleSpeed, "fct");
		Link link206_ = createBicycleLink(network,"fct_206_", node221, node213, bicycleSpeed, "fct");

		// _Trasse 3 - Königsweg
		Link link301_ = createBicycleLink(network,"fct_301_", node35, node31, bicycleSpeed, "fct");
		Link link302_ = createBicycleLink(network,"fct_302_", node36, node35, bicycleSpeed, "fct");
		Link link303_ = createBicycleLink(network,"fct_303_", node37, node36, bicycleSpeed, "fct");
		Link link304_ = createBicycleLink(network,"fct_304_", node35, node32, bicycleSpeed, "fct");
		Link link305_ = createBicycleLink(network,"fct_305_", node34, node33, bicycleSpeed, "fct");
		Link link306_ = createBicycleLink(network,"fct_306_", node35, node34, bicycleSpeed, "fct");

		// _Trasse 4 - Panke-Trail
		Link link401_ = createBicycleLink(network,"fct_401_", node412, node411, bicycleSpeed, "fct");
		Link link402_ = createBicycleLink(network,"fct_402_", node413, node412, bicycleSpeed, "fct");
		Link link403_ = createBicycleLink(network,"fct_403_", node414, node413, bicycleSpeed, "fct");
		Link link404_ = createBicycleLink(network,"fct_404_", node415, node414, bicycleSpeed, "fct");
		Link link405_ = createBicycleLink(network,"fct_405_", node416, node415, bicycleSpeed, "fct");
		Link link406_ = createBicycleLink(network,"fct_406_", node422, node421, bicycleSpeed, "fct");
		Link link407_ = createBicycleLink(network,"fct_407_", node413, node422, bicycleSpeed, "fct");

		// _Trasse 5 - West-Route
		Link link501_ = createBicycleLink(network,"fct_501_", node52, node51, bicycleSpeed, "fct");
		Link link502_ = createBicycleLink(network,"fct_502_", node53, node52, bicycleSpeed, "fct");
		Link link503_ = createBicycleLink(network,"fct_503_", node54, node53, bicycleSpeed, "fct");
		Link link504_ = createBicycleLink(network,"fct_504_", node55, node54, bicycleSpeed, "fct");
		Link link505_ = createBicycleLink(network,"fct_505_", node56, node55, bicycleSpeed, "fct");
		Link link506_ = createBicycleLink(network,"fct_506_", node57, node56, bicycleSpeed, "fct");

		// _Trasse 6 - Teltowkanal-Route
		Link link601_ = createBicycleLink(network,"fct_601_", node62, node61, bicycleSpeed, "fct");
		Link link602_ = createBicycleLink(network,"fct_602_", node63, node62, bicycleSpeed, "fct");
		Link link603_ = createBicycleLink(network,"fct_603_", node64, node63, bicycleSpeed, "fct");
		Link link604_ = createBicycleLink(network,"fct_604_", node65, node64, bicycleSpeed, "fct");
		Link link605_ = createBicycleLink(network,"fct_605_", node66, node65, bicycleSpeed, "fct");
		Link link606_ = createBicycleLink(network,"fct_606_", node67, node66, bicycleSpeed, "fct");
		Link link607_ = createBicycleLink(network,"fct_607_", node68, node67, bicycleSpeed, "fct");
		Link link608_ = createBicycleLink(network,"fct_608_", node69, node68, bicycleSpeed, "fct");

		// _Trasse 7 - Spandauer Damm-Freiheit
		Link link701_ = createBicycleLink(network,"fct_701_", node72, node71, bicycleSpeed, "fct");
		Link link702_ = createBicycleLink(network,"fct_702_", node73, node72, bicycleSpeed, "fct");
		Link link703_ = createBicycleLink(network,"fct_703_", node74, node73, bicycleSpeed, "fct");
		Link link704_ = createBicycleLink(network,"fct_704_", node75, node74, bicycleSpeed, "fct");
		Link link705_ = createBicycleLink(network,"fct_705_", node76, node75, bicycleSpeed, "fct");

		// _Trasse 8 - Nonnendammallee-Falkenseer Chaussee
		Link link801_ = createBicycleLink(network,"fct_801_", node82, node81, bicycleSpeed, "fct");
		Link link802_ = createBicycleLink(network,"fct_802_", node83, node82, bicycleSpeed, "fct");
		Link link803_ = createBicycleLink(network,"fct_803_", node84, node83, bicycleSpeed, "fct");
		Link link804_ = createBicycleLink(network,"fct_804_", node85, node84, bicycleSpeed, "fct");
		Link link805_ = createBicycleLink(network,"fct_805_", node86, node85, bicycleSpeed, "fct");
		Link link806_ = createBicycleLink(network,"fct_806_", node87, node86, bicycleSpeed, "fct");
		Link link807_ = createBicycleLink(network,"fct_807_", node88, node87, bicycleSpeed, "fct");

		// _Trasse 9 - Landsberger Allee-Marzahn
		Link link901_ = createBicycleLink(network,"fct_901_", node92, node91, bicycleSpeed, "fct");
		Link link902_ = createBicycleLink(network,"fct_902_", node93, node92, bicycleSpeed, "fct");
		Link link903_ = createBicycleLink(network,"fct_903_", node94, node93, bicycleSpeed, "fct");
		Link link904_ = createBicycleLink(network,"fct_904_", node95, node94, bicycleSpeed, "fct");

		// _Trasse 10 - Heiligensee
		Link link10001_ = createBicycleLink(network,"fct_10001_", node1002, node1001, bicycleSpeed, "fct");
		Link link10002_ = createBicycleLink(network,"fct_10002_", node1003, node1002, bicycleSpeed, "fct");
		Link link10003_ = createBicycleLink(network,"fct_10003_", node1004, node1003, bicycleSpeed, "fct");
		Link link10004_ = createBicycleLink(network,"fct_10004_", node221, node1004, bicycleSpeed, "fct");




		// Link link = createBicycleLink(network,"fct_301", node31, node32, bicylceSpeed);
		// ... more links accorinding to list...

		// ---

		// To create PBL link "parallel" to existing link
		Link pblLink = createPBLLink(network, "8770", bicycleSpeed);

		// ---

		NetworkWriter writer = new NetworkWriter(scenario.getNetwork());
		writer.writeV2(outputNetworkFileName);


	}

	// string pathType hinzufügen??

	private static Link createBicycleLink(Network network, String linkName, Node fromNode, Node toNode, double bicycleSpeed, String type) {
			return NetworkUtils.createAndAddLink(
					network, Id.createLinkId(linkName), fromNode, toNode,
					CoordUtils.calcEuclideanDistance(fromNode.getCoord(), toNode.getCoord()),
					bicycleSpeed, 1800., 1, null, "bicycle");
	}

	private static Link createPBLLink(Network network, String linkName, double bicycleSpeed) {
		Link extistinglink = network.getLinks().get(Id.createLinkId(linkName));
		return NetworkUtils.createAndAddLink(network, extistinglink.getId(), extistinglink.getFromNode(), extistinglink.getToNode(),
				extistinglink.getLength(), bicycleSpeed, 1800., 1, null, "bicycle");
	}
}