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
public class AddNodesAndLinksToNetwork_Scenario2 {

	public static void main(String[] args) {
		String inputNetworkFileName = "path-to-input-network-file/network.xml.gz";
		String outputNetworkFileName = "path-to-output-network-file/network-with-fct.xml.gz";
		double bicycleSpeed = 25.0 / 3.6;

		// double np_bicycleSpeed = 20.0 / 3.6;
		// double pbl_bicycleSpeed = 15.0 / 3.6;

		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:4326", "EPSG:31464");

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(inputNetworkFileName);

		Network network = scenario.getNetwork();

		// ---

		
		// nn= new node, but not all are protected bike lanes, differenciation in links
		// Charlottenburg-Wilmersdorf

		Node node001 = NetworkUtils.createNode(Id.createNodeId("nn_001"), transformation.transform(new Coord(13.325650, 52.538834)));
		network.addNode(node001);
		Node node002 = NetworkUtils.createNode(Id.createNodeId("nn_002"), transformation.transform(new Coord(13.297837, 52.534645)));
		network.addNode(node002);
		Node node003 = NetworkUtils.createNode(Id.createNodeId("nn_003"), transformation.transform(new Coord(13.249519, 52.523718)));
		network.addNode(node003);
		Node node004 = NetworkUtils.createNode(Id.createNodeId("nn_004"), transformation.transform(new Coord(13.262779, 52.521015)));
		network.addNode(node004);
		Node node005 = NetworkUtils.createNode(Id.createNodeId("nn_005"), transformation.transform(new Coord(13.320219, 52.478925)));
		network.addNode(node005);
		Node node006 = NetworkUtils.createNode(Id.createNodeId("nn_006"), transformation.transform(new Coord(13.313331, 52.480467)));
		network.addNode(node006);
		Node node007 = NetworkUtils.createNode(Id.createNodeId("nn_007"), transformation.transform(new Coord(13.329745, 52.505324)));
		network.addNode(node007);
		Node node008 = NetworkUtils.createNode(Id.createNodeId("nn_008"), transformation.transform(new Coord(13.331346, 52.505203)));
		network.addNode(node008);

		//Friedrichshain - Kreuzberg
		Node node009 = NetworkUtils.createNode(Id.createNodeId("nn_009"), transformation.transform(new Coord(13.383633, 52.493627)));
		network.addNode(node009);
		Node node010 = NetworkUtils.createNode(Id.createNodeId("nn_010"), transformation.transform(new Coord(13.405282, 52.489386)));
		network.addNode(node010);
		Node node011 = NetworkUtils.createNode(Id.createNodeId("nn_011"), transformation.transform(new Coord(13.376847, 52.491609)));
		network.addNode(node011);
		Node node012 = NetworkUtils.createNode(Id.createNodeId("nn_012"), transformation.transform(new Coord(13.383401, 52.493687)));
		network.addNode(node012);
		Node node013 = NetworkUtils.createNode(Id.createNodeId("nn_013"), transformation.transform(new Coord(13.417666, 52.499112)));
		network.addNode(node013);
		Node node014 = NetworkUtils.createNode(Id.createNodeId("nn_014"), transformation.transform(new Coord(13.391983, 52.497973)));
		network.addNode(node014);
		Node node015 = NetworkUtils.createNode(Id.createNodeId("nn_015"), transformation.transform(new Coord(13.453585, 52.515990)));
		network.addNode(node015);
		Node node016 = NetworkUtils.createNode(Id.createNodeId("nn_016"), transformation.transform(new Coord(13.429529, 52.518505)));
		network.addNode(node016);
		Node node017 = NetworkUtils.createNode(Id.createNodeId("nn_017"), transformation.transform(new Coord(13.454365, 52.515674)));
		network.addNode(node017);
		Node node018 = NetworkUtils.createNode(Id.createNodeId("nn_018"), transformation.transform(new Coord(13.476684, 52.513422)));
		network.addNode(node018);
		Node node019 = NetworkUtils.createNode(Id.createNodeId("nn_019"), transformation.transform(new Coord(13.446999, 52.502569)));
		network.addNode(node019);
		Node node020 = NetworkUtils.createNode(Id.createNodeId("nn_020"), transformation.transform(new Coord(13.464238, 52.497485)));
		network.addNode(node020);
		Node node021 = NetworkUtils.createNode(Id.createNodeId("nn_021"), transformation.transform(new Coord(13.410390, 52.503912)));
		network.addNode(node021);
		Node node022 = NetworkUtils.createNode(Id.createNodeId("nn_022"), transformation.transform(new Coord(13.398444, 52.507127)));
		network.addNode(node022);
		Node node023 = NetworkUtils.createNode(Id.createNodeId("nn_023"), transformation.transform(new Coord(13.410318, 52.503302)));
		network.addNode(node023);
		Node node024 = NetworkUtils.createNode(Id.createNodeId("nn_024"), transformation.transform(new Coord(13.405186, 52.496672)));
		network.addNode(node024);
		Node node025 = NetworkUtils.createNode(Id.createNodeId("nn_025"), transformation.transform(new Coord(13.469843, 52.498576)));
		network.addNode(node025);
		Node node026 = NetworkUtils.createNode(Id.createNodeId("nn_026"), transformation.transform(new Coord(13.470111, 52.500379)));
		network.addNode(node026);
		Node node027 = NetworkUtils.createNode(Id.createNodeId("nn_027"), transformation.transform(new Coord(13.474263, 52.500274)));
		network.addNode(node027);
		Node node028 = NetworkUtils.createNode(Id.createNodeId("nn_028"), transformation.transform(new Coord(13.380189, 52.498719)));
		network.addNode(node028);
		Node node029 = NetworkUtils.createNode(Id.createNodeId("nn_029"), transformation.transform(new Coord(13.379462, 52.492581)));
		network.addNode(node029);
		Node node030 = NetworkUtils.createNode(Id.createNodeId("nn_030"), transformation.transform(new Coord(13.386120, 52.485082)));
		network.addNode(node030);
		Node node031 = NetworkUtils.createNode(Id.createNodeId("nn_031"), transformation.transform(new Coord(13.386211, 52.487330)));
		network.addNode(node031);
		Node node032 = NetworkUtils.createNode(Id.createNodeId("nn_032"), transformation.transform(new Coord(13.389719, 52.495845)));
		network.addNode(node032);

		// Lichtenberg

		Node node033 = NetworkUtils.createNode(Id.createNodeId("nn_033"), transformation.transform(new Coord(13.500749, 52.510860)));
		network.addNode(node033);
		Node node034 = NetworkUtils.createNode(Id.createNodeId("nn_034"), transformation.transform(new Coord(13.505967, 52.512222)));
		network.addNode(node034);
		Node node035 = NetworkUtils.createNode(Id.createNodeId("nn_035"), transformation.transform(new Coord(13.497761, 52.586082)));
		network.addNode(node035);
		Node node036 = NetworkUtils.createNode(Id.createNodeId("nn_036"), transformation.transform(new Coord(13.499790, 52.586287)));
		network.addNode(node036);

		//Marzahn-Hellersdorf

		Node node037 = NetworkUtils.createNode(Id.createNodeId("nn_037"), transformation.transform(new Coord(13.554116, 52.563964)));
		network.addNode(node037);
		Node node038 = NetworkUtils.createNode(Id.createNodeId("nn_038"), transformation.transform(new Coord(13.574908, 52.560148)));
		network.addNode(node038);
		Node node039 = NetworkUtils.createNode(Id.createNodeId("nn_039"), transformation.transform(new Coord(13.579300, 52.525427)));
		network.addNode(node039);
		Node node040 = NetworkUtils.createNode(Id.createNodeId("nn_040"), transformation.transform(new Coord(13.574467, 52.513324)));
		network.addNode(node040);
		Node node041 = NetworkUtils.createNode(Id.createNodeId("nn_041"), transformation.transform(new Coord(13.519986, 52.525786)));
		network.addNode(node041);
		Node node042 = NetworkUtils.createNode(Id.createNodeId("nn_042"), transformation.transform(new Coord(13.529102, 52.525333)));
		network.addNode(node042);
		Node node043 = NetworkUtils.createNode(Id.createNodeId("nn_043"), transformation.transform(new Coord(13.596924, 52.507507)));
		network.addNode(node043);
		Node node044 = NetworkUtils.createNode(Id.createNodeId("nn_044"), transformation.transform(new Coord(13.592663, 52.508511)));
		network.addNode(node044);
		Node node045 = NetworkUtils.createNode(Id.createNodeId("nn_045"), transformation.transform(new Coord(13.530262, 52.556143)));
		network.addNode(node045);
		Node node046 = NetworkUtils.createNode(Id.createNodeId("nn_046"), transformation.transform(new Coord(13.528270, 52.556944)));
		network.addNode(node046);

		//Mitte

		Node node047 = NetworkUtils.createNode(Id.createNodeId("nn_047"), transformation.transform(new Coord(13.335147, 52.516955)));
		network.addNode(node047);
		Node node048 = NetworkUtils.createNode(Id.createNodeId("nn_048"), transformation.transform(new Coord(13.341731, 52.522406)));
		network.addNode(node048);
		Node node049 = NetworkUtils.createNode(Id.createNodeId("nn_049"), transformation.transform(new Coord(13.354569, 52.517328)));
		network.addNode(node049);
		Node node050 = NetworkUtils.createNode(Id.createNodeId("nn_050"), transformation.transform(new Coord(13.382212, 52.513937)));
		network.addNode(node050);
		Node node051 = NetworkUtils.createNode(Id.createNodeId("nn_051"), transformation.transform(new Coord(13.400756, 52.516161)));
		network.addNode(node051);
		Node node052 = NetworkUtils.createNode(Id.createNodeId("nn_052"), transformation.transform(new Coord(13.374344, 52.537267)));
		network.addNode(node052);
		Node node053 = NetworkUtils.createNode(Id.createNodeId("nn_053"), transformation.transform(new Coord(13.382092, 52.531288)));
		network.addNode(node053);
		Node node054 = NetworkUtils.createNode(Id.createNodeId("nn_054"), transformation.transform(new Coord(13.352424, 52.550406)));
		network.addNode(node054);
		Node node055 = NetworkUtils.createNode(Id.createNodeId("nn_055"), transformation.transform(new Coord(13.361351, 52.555056)));
		network.addNode(node055);
		Node node056 = NetworkUtils.createNode(Id.createNodeId("nn_056"), transformation.transform(new Coord(13.314493, 52.530336)));
		network.addNode(node056);
		Node node057 = NetworkUtils.createNode(Id.createNodeId("nn_057"), transformation.transform(new Coord(13.328251, 52.532825)));
		network.addNode(node057);
		Node node058 = NetworkUtils.createNode(Id.createNodeId("nn_058"), transformation.transform(new Coord(13.416554, 52.521825)));
		network.addNode(node058);
		Node node059 = NetworkUtils.createNode(Id.createNodeId("nn_059"), transformation.transform(new Coord(13.427306, 52.518555)));
		network.addNode(node059);
		Node node060 = NetworkUtils.createNode(Id.createNodeId("nn_060"), transformation.transform(new Coord(13.344054, 52.546008)));
		network.addNode(node060);
		Node node061 = NetworkUtils.createNode(Id.createNodeId("nn_061"), transformation.transform(new Coord(13.349180, 52.542245)));
		network.addNode(node061);
		Node node062 = NetworkUtils.createNode(Id.createNodeId("nn_062"), transformation.transform(new Coord(13.418203, 52.515364)));
		network.addNode(node062);
		Node node063 = NetworkUtils.createNode(Id.createNodeId("nn_063"), transformation.transform(new Coord(13.423919, 52.513442)));
		network.addNode(node063);
		Node node064 = NetworkUtils.createNode(Id.createNodeId("nn_064"), transformation.transform(new Coord(13.343040, 52.525345)));
		network.addNode(node064);
		Node node065 = NetworkUtils.createNode(Id.createNodeId("nn_065"), transformation.transform(new Coord(13.349493, 52.524491)));
		network.addNode(node065);
		Node node066 = NetworkUtils.createNode(Id.createNodeId("nn_066"), transformation.transform(new Coord(13.369706, 52.559615)));
		network.addNode(node066);
		Node node067 = NetworkUtils.createNode(Id.createNodeId("nn_067"), transformation.transform(new Coord(13.373171, 52.557025)));
		network.addNode(node067);
		Node node068 = NetworkUtils.createNode(Id.createNodeId("nn_068"), transformation.transform(new Coord(13.342194, 52.523117)));
		network.addNode(node068);
		Node node069 = NetworkUtils.createNode(Id.createNodeId("nn_069"), transformation.transform(new Coord(13.343030, 52.525347)));
		network.addNode(node069);
		Node node070 = NetworkUtils.createNode(Id.createNodeId("nn_070"), transformation.transform(new Coord(13.343107, 52.528446)));
		network.addNode(node070);
		Node node071 = NetworkUtils.createNode(Id.createNodeId("nn_071"), transformation.transform(new Coord(13.343003, 52.526398)));
		network.addNode(node071);
		Node node072 = NetworkUtils.createNode(Id.createNodeId("nn_072"), transformation.transform(new Coord(13.369078, 52.533810)));
		network.addNode(node072);
		Node node073 = NetworkUtils.createNode(Id.createNodeId("nn_073"), transformation.transform(new Coord(13.366133, 52.532880)));
		network.addNode(node073);
		Node node074 = NetworkUtils.createNode(Id.createNodeId("nn_074"), transformation.transform(new Coord(13.414119, 52.507876)));
		network.addNode(node074);
		Node node075 = NetworkUtils.createNode(Id.createNodeId("nn_075"), transformation.transform(new Coord(13.416317, 52.507012)));
		network.addNode(node075);
		Node node076 = NetworkUtils.createNode(Id.createNodeId("nn_076"), transformation.transform(new Coord(13.342307, 52.552424)));
		network.addNode(node076);
		Node node077 = NetworkUtils.createNode(Id.createNodeId("nn_077"), transformation.transform(new Coord(13.343740, 52.553343)));
		network.addNode(node077);

		// Neukölln

		Node node078 = NetworkUtils.createNode(Id.createNodeId("nn_078"), transformation.transform(new Coord(13.424014, 52.486361)));
		network.addNode(node078);
		Node node079 = NetworkUtils.createNode(Id.createNodeId("nn_079"), transformation.transform(new Coord(13.434217, 52.462926)));
		network.addNode(node079);
		Node node080 = NetworkUtils.createNode(Id.createNodeId("nn_080"), transformation.transform(new Coord(13.431380, 52.483065)));
		network.addNode(node080);
		Node node081 = NetworkUtils.createNode(Id.createNodeId("nn_081"), transformation.transform(new Coord(13.437822, 52.478497)));
		network.addNode(node081);
		Node node082 = NetworkUtils.createNode(Id.createNodeId("nn_082"), transformation.transform(new Coord(13.424440, 52.486196)));
		network.addNode(node082);
		Node node083 = NetworkUtils.createNode(Id.createNodeId("nn_083"), transformation.transform(new Coord(13.431255, 52.483123)));
		network.addNode(node083);
		Node node084 = NetworkUtils.createNode(Id.createNodeId("nn_084"), transformation.transform(new Coord(13.449541, 52.417767)));
		network.addNode(node084);
		Node node085 = NetworkUtils.createNode(Id.createNodeId("nn_085"), transformation.transform(new Coord(13.451937, 52.418560)));
		network.addNode(node085);

		//Pankow

		Node node086 = NetworkUtils.createNode(Id.createNodeId("nn_086"), transformation.transform(new Coord(13.420294, 52.566985)));
		network.addNode(node086);
		Node node087 = NetworkUtils.createNode(Id.createNodeId("nn_087"), transformation.transform(new Coord(13.424013, 52.552904)));
		network.addNode(node087);
		Node node088 = NetworkUtils.createNode(Id.createNodeId("nn_088"), transformation.transform(new Coord(13.364141, 52.581727)));
		network.addNode(node088);
		Node node089 = NetworkUtils.createNode(Id.createNodeId("nn_089"), transformation.transform(new Coord(13.376979, 52.574936)));
		network.addNode(node089);
		Node node090 = NetworkUtils.createNode(Id.createNodeId("nn_090"), transformation.transform(new Coord(13.433114, 52.535968)));
		network.addNode(node090);
		Node node091 = NetworkUtils.createNode(Id.createNodeId("nn_091"), transformation.transform(new Coord(13.441200, 52.532090)));
		network.addNode(node091);
		Node node092 = NetworkUtils.createNode(Id.createNodeId("nn_092"), transformation.transform(new Coord(13.447324, 52.526454)));
		network.addNode(node092);
		Node node093 = NetworkUtils.createNode(Id.createNodeId("nn_093"), transformation.transform(new Coord(13.375723, 52.597142)));
		network.addNode(node093);
		Node node094 = NetworkUtils.createNode(Id.createNodeId("nn_094"), transformation.transform(new Coord(13.384568, 52.591257)));
		network.addNode(node094);
		Node node095 = NetworkUtils.createNode(Id.createNodeId("nn_095"), transformation.transform(new Coord(13.402289, 52.602827)));
		network.addNode(node095);
		Node node096 = NetworkUtils.createNode(Id.createNodeId("nn_096"), transformation.transform(new Coord(13.414388, 52.604975)));
		network.addNode(node096);
		Node node097 = NetworkUtils.createNode(Id.createNodeId("nn_097"), transformation.transform(new Coord(13.385264, 52.591192)));
		network.addNode(node097);
		Node node098 = NetworkUtils.createNode(Id.createNodeId("nn_098"), transformation.transform(new Coord(13.402694, 52.593125)));
		network.addNode(node098);
		Node node099 = NetworkUtils.createNode(Id.createNodeId("nn_099"), transformation.transform(new Coord(13.369205, 52.597598)));
		network.addNode(node099);
		Node node0100 = NetworkUtils.createNode(Id.createNodeId("nn_0100"), transformation.transform(new Coord(13.376294, 52.606544)));
		network.addNode(node0100);
		Node node0101 = NetworkUtils.createNode(Id.createNodeId("nn_0101"), transformation.transform(new Coord(13.491277, 52.637670)));
		network.addNode(node0101);
		Node node0102 = NetworkUtils.createNode(Id.createNodeId("nn_0102"), transformation.transform(new Coord(13.500189, 52.632729)));
		network.addNode(node0102);
		Node node0103 = NetworkUtils.createNode(Id.createNodeId("nn_0103"), transformation.transform(new Coord(13.376999, 52.577611)));
		network.addNode(node0103);
		Node node0104 = NetworkUtils.createNode(Id.createNodeId("nn_0104"), transformation.transform(new Coord(13.380116, 52.575025)));
		network.addNode(node0104);
		Node node0105 = NetworkUtils.createNode(Id.createNodeId("nn_0105"), transformation.transform(new Coord(13.424354, 52.538998)));
		network.addNode(node0105);
		Node node0106 = NetworkUtils.createNode(Id.createNodeId("nn_0106"), transformation.transform(new Coord(13.433002, 52.535989)));
		network.addNode(node0106);
		Node node0107 = NetworkUtils.createNode(Id.createNodeId("nn_0107"), transformation.transform(new Coord(13.428516, 52.582997)));
		network.addNode(node0107);
		Node node0108 = NetworkUtils.createNode(Id.createNodeId("nn_0108"), transformation.transform(new Coord(13.428687, 52.578088)));
		network.addNode(node0108);
		Node node0109 = NetworkUtils.createNode(Id.createNodeId("nn_0109"), transformation.transform(new Coord(13.479995, 52.553365)));
		network.addNode(node0109);
		Node node0110 = NetworkUtils.createNode(Id.createNodeId("nn_0110"), transformation.transform(new Coord(13.490074, 52.557785)));
		network.addNode(node0110);


		// Steglitz-Zehlendorf

		Node node0111 = NetworkUtils.createNode(Id.createNodeId("nn_0111"), transformation.transform(new Coord(13.270489, 52.450928)));
		network.addNode(node0111);
		Node node0112 = NetworkUtils.createNode(Id.createNodeId("nn_0112"), transformation.transform(new Coord(13.251114, 52.450145)));
		network.addNode(node0112);
		Node node0113 = NetworkUtils.createNode(Id.createNodeId("nn_0113"), transformation.transform(new Coord(13.232391, 52.437789)));
		network.addNode(node0113);
		Node node0114 = NetworkUtils.createNode(Id.createNodeId("nn_0114"), transformation.transform(new Coord(13.229131, 52.424843)));
		network.addNode(node0114);
		Node node0115 = NetworkUtils.createNode(Id.createNodeId("nn_0115"), transformation.transform(new Coord(13.352963, 52.433228)));
		network.addNode(node0115);
		Node node0116 = NetworkUtils.createNode(Id.createNodeId("nn_0116"), transformation.transform(new Coord(13.358843, 52.420046)));
		network.addNode(node0116);
		Node node0117 = NetworkUtils.createNode(Id.createNodeId("nn_0117"), transformation.transform(new Coord(13.313288, 52.430440)));
		network.addNode(node0117);
		Node node0118 = NetworkUtils.createNode(Id.createNodeId("nn_0118"), transformation.transform(new Coord(13.317322, 52.430721)));
		network.addNode(node0118);
		Node node0119 = NetworkUtils.createNode(Id.createNodeId("nn_0119"), transformation.transform(new Coord(13.325010, 52.428165)));
		network.addNode(node0119);
		Node node0120 = NetworkUtils.createNode(Id.createNodeId("nn_0120"), transformation.transform(new Coord(13.214843, 52.427320)));
		network.addNode(node0120);
		Node node0121 = NetworkUtils.createNode(Id.createNodeId("nn_0121"), transformation.transform(new Coord(13.223953, 52.429671)));
		network.addNode(node0121);
		Node node0122 = NetworkUtils.createNode(Id.createNodeId("nn_0122"), transformation.transform(new Coord(13.270531, 52.450835)));
		network.addNode(node0122);
		Node node0123 = NetworkUtils.createNode(Id.createNodeId("nn_0123"), transformation.transform(new Coord(13.267706, 52.446019)));
		network.addNode(node0123);
		Node node0124 = NetworkUtils.createNode(Id.createNodeId("nn_0124"), transformation.transform(new Coord(13.265372, 52.442257)));
		network.addNode(node0124);
		Node node0125 = NetworkUtils.createNode(Id.createNodeId("nn_0125"), transformation.transform(new Coord(13.346477, 52.430871)));
		network.addNode(node0125);
		Node node0126 = NetworkUtils.createNode(Id.createNodeId("nn_0126"), transformation.transform(new Coord(13.347546, 52.425705)));
		network.addNode(node0126);
		Node node0127 = NetworkUtils.createNode(Id.createNodeId("nn_0127"), transformation.transform(new Coord(13.282248, 52.440404)));
		network.addNode(node0127);
		Node node0128 = NetworkUtils.createNode(Id.createNodeId("nn_0128"), transformation.transform(new Coord(13.280730, 52.435614)));
		network.addNode(node0128);
		Node node0129 = NetworkUtils.createNode(Id.createNodeId("nn_0129"), transformation.transform(new Coord(13.344240, 52.421655)));
		network.addNode(node0129);
		Node node0130 = NetworkUtils.createNode(Id.createNodeId("nn_0130"), transformation.transform(new Coord(13.347614, 52.425636)));
		network.addNode(node0130);
		Node node0131 = NetworkUtils.createNode(Id.createNodeId("nn_0131"), transformation.transform(new Coord(13.351710, 52.440236)));
		network.addNode(node0131);
		Node node0132 = NetworkUtils.createNode(Id.createNodeId("nn_0132"), transformation.transform(new Coord(13.354113, 52.443290)));
		network.addNode(node0132);
		Node node0133 = NetworkUtils.createNode(Id.createNodeId("nn_0133"), transformation.transform(new Coord(13.347012, 52.450028)));
		network.addNode(node0133);
		Node node0134 = NetworkUtils.createNode(Id.createNodeId("nn_0134"), transformation.transform(new Coord(13.348609, 52.453438)));
		network.addNode(node0134);
		Node node0135 = NetworkUtils.createNode(Id.createNodeId("nn_0135"), transformation.transform(new Coord(13.265643, 52.442257)));
		network.addNode(node0135);
		Node node0136 = NetworkUtils.createNode(Id.createNodeId("nn_0136"), transformation.transform(new Coord(13.267744, 52.445477)));
		network.addNode(node0136);
		Node node0137 = NetworkUtils.createNode(Id.createNodeId("nn_0137"), transformation.transform(new Coord(13.327918, 52.428557)));
		network.addNode(node0137);
		Node node0138 = NetworkUtils.createNode(Id.createNodeId("nn_0138"), transformation.transform(new Coord(13.332551, 52.429761)));
		network.addNode(node0138);
		Node node0139 = NetworkUtils.createNode(Id.createNodeId("nn_0139"), transformation.transform(new Coord(13.279840, 52.432520)));
		network.addNode(node0139);
		Node node0140 = NetworkUtils.createNode(Id.createNodeId("nn_0140"), transformation.transform(new Coord(13.280876, 52.435578)));
		network.addNode(node0140);
		Node node0141 = NetworkUtils.createNode(Id.createNodeId("nn_0141"), transformation.transform(new Coord(13.175239, 52.420070)));
		network.addNode(node0141);
		Node node0142 = NetworkUtils.createNode(Id.createNodeId("nn_0142"), transformation.transform(new Coord(13.171163, 52.420911)));
		network.addNode(node0142);
		Node node0143 = NetworkUtils.createNode(Id.createNodeId("nn_0143"), transformation.transform(new Coord(13.346643, 52.430918)));
		network.addNode(node0143);
		Node node0144 = NetworkUtils.createNode(Id.createNodeId("nn_0144"), transformation.transform(new Coord(13.347711, 52.432086)));
		network.addNode(node0144);
		Node node0145 = NetworkUtils.createNode(Id.createNodeId("nn_0145"), transformation.transform(new Coord(13.347918, 52.432314)));
		network.addNode(node0145);
		Node node0146 = NetworkUtils.createNode(Id.createNodeId("nn_0146"), transformation.transform(new Coord(13.349032, 52.433317)));
		network.addNode(node0146);

		// Tempelhof-Schöneberg

		Node node0147 = NetworkUtils.createNode(Id.createNodeId("nn_0147"), transformation.transform(new Coord(13.381145, 52.465691)));
		network.addNode(node0147);
		Node node0148 = NetworkUtils.createNode(Id.createNodeId("nn_0148"), transformation.transform(new Coord(13.382244, 52.463175)));
		network.addNode(node0148);
		Node node0149 = NetworkUtils.createNode(Id.createNodeId("nn_0149"), transformation.transform(new Coord(13.380982, 52.462613)));
		network.addNode(node0149);
		Node node0150 = NetworkUtils.createNode(Id.createNodeId("nn_0150"), transformation.transform(new Coord(13.381237, 52.461425)));
		network.addNode(node0150);
		Node node0151 = NetworkUtils.createNode(Id.createNodeId("nn_0151"), transformation.transform(new Coord(13.369840, 52.470767)));
		network.addNode(node0151);
		Node node0152 = NetworkUtils.createNode(Id.createNodeId("nn_0152"), transformation.transform(new Coord(13.377943, 52.466384)));
		network.addNode(node0152);
		Node node0153 = NetworkUtils.createNode(Id.createNodeId("nn_0153"), transformation.transform(new Coord(13.385505, 52.465689)));
		network.addNode(node0153);
		Node node0154 = NetworkUtils.createNode(Id.createNodeId("nn_0154"), transformation.transform(new Coord(13.384387, 52.452768)));
		network.addNode(node0154);
		Node node0155 = NetworkUtils.createNode(Id.createNodeId("nn_0155"), transformation.transform(new Coord(13.365244, 52.446489)));
		network.addNode(node0155);
		Node node0156 = NetworkUtils.createNode(Id.createNodeId("nn_0156"), transformation.transform(new Coord(13.370389, 52.449172)));
		network.addNode(node0156);
		Node node0157 = NetworkUtils.createNode(Id.createNodeId("nn_0157"), transformation.transform(new Coord(13.372302, 52.444431)));
		network.addNode(node0157);
		Node node0158 = NetworkUtils.createNode(Id.createNodeId("nn_0158"), transformation.transform(new Coord(13.381240, 52.440871)));
		network.addNode(node0158);
		Node node0159 = NetworkUtils.createNode(Id.createNodeId("nn_0159"), transformation.transform(new Coord(13.382448, 52.445407)));
		network.addNode(node0159);
		Node node0160 = NetworkUtils.createNode(Id.createNodeId("nn_0160"), transformation.transform(new Coord(13.382615, 52.443839)));
		network.addNode(node0160);
		Node node0161 = NetworkUtils.createNode(Id.createNodeId("nn_0161"), transformation.transform(new Coord(13.381777, 52.439221)));
		network.addNode(node0161);
		Node node0162 = NetworkUtils.createNode(Id.createNodeId("nn_0162"), transformation.transform(new Coord(13.387152, 52.440126)));
		network.addNode(node0162);
		Node node0163 = NetworkUtils.createNode(Id.createNodeId("nn_0163"), transformation.transform(new Coord(13.403638, 52.406781)));
		network.addNode(node0163);
		Node node0164 = NetworkUtils.createNode(Id.createNodeId("nn_0164"), transformation.transform(new Coord(13.404252, 52.403983)));
		network.addNode(node0164);

		//Treptow-Köpenick

		Node node0165 = NetworkUtils.createNode(Id.createNodeId("nn_0165"), transformation.transform(new Coord(13.453987, 52.488986)));
		network.addNode(node0165);
		Node node0166 = NetworkUtils.createNode(Id.createNodeId("nn_0166"), transformation.transform(new Coord(13.457600, 52.491701)));
		network.addNode(node0166);
		Node node0167 = NetworkUtils.createNode(Id.createNodeId("nn_0167"), transformation.transform(new Coord(13.501485, 52.483076)));
		network.addNode(node0167);
		Node node0168 = NetworkUtils.createNode(Id.createNodeId("nn_0168"), transformation.transform(new Coord(13.509729, 52.472884)));
		network.addNode(node0168);
		Node node0169 = NetworkUtils.createNode(Id.createNodeId("nn_0169"), transformation.transform(new Coord(13.514213, 52.468875)));
		network.addNode(node0169);
		Node node0170 = NetworkUtils.createNode(Id.createNodeId("nn_0170"), transformation.transform(new Coord(13.457831, 52.491654)));
		network.addNode(node0170);
		Node node0171 = NetworkUtils.createNode(Id.createNodeId("nn_0171"), transformation.transform(new Coord(13.475222, 52.480067)));
		network.addNode(node0171);
		Node node0172 = NetworkUtils.createNode(Id.createNodeId("nn_0172"), transformation.transform(new Coord(13.473771, 52.474935)));
		network.addNode(node0172);
		Node node0173 = NetworkUtils.createNode(Id.createNodeId("nn_0173"), transformation.transform(new Coord(13.491224, 52.462626)));
		network.addNode(node0173);
		Node node0174 = NetworkUtils.createNode(Id.createNodeId("nn_0174"), transformation.transform(new Coord(13.514216, 52.468703)));
		network.addNode(node0174);
		Node node0175 = NetworkUtils.createNode(Id.createNodeId("nn_0175"), transformation.transform(new Coord(13.513913, 52.462565)));
		network.addNode(node0175);
		Node node0176 = NetworkUtils.createNode(Id.createNodeId("nn_0176"), transformation.transform(new Coord(13.513494, 52.464704)));
		network.addNode(node0176);
		Node node0177 = NetworkUtils.createNode(Id.createNodeId("nn_0177"), transformation.transform(new Coord(13.508653, 52.463817)));
		network.addNode(node0177);
		Node node0178 = NetworkUtils.createNode(Id.createNodeId("nn_0178"), transformation.transform(new Coord(13.526571, 52.425887)));
		network.addNode(node0178);
		Node node0179 = NetworkUtils.createNode(Id.createNodeId("nn_0179"), transformation.transform(new Coord(13.525904, 52.423225)));
		network.addNode(node0179);
		Node node0180 = NetworkUtils.createNode(Id.createNodeId("nn_0180"), transformation.transform(new Coord(13.562713, 52.437370)));
		network.addNode(node0180);
		Node node0181 = NetworkUtils.createNode(Id.createNodeId("nn_0181"), transformation.transform(new Coord(13.564674, 52.438522)));
		network.addNode(node0181);
		Node node0182 = NetworkUtils.createNode(Id.createNodeId("nn_0182"), transformation.transform(new Coord(13.592237, 52.454966)));
		network.addNode(node0182);
		Node node0183 = NetworkUtils.createNode(Id.createNodeId("nn_0183"), transformation.transform(new Coord(13.593153, 52.453121)));
		network.addNode(node0183);
		Node node0184 = NetworkUtils.createNode(Id.createNodeId("nn_0184"), transformation.transform(new Coord(13.582286, 52.442474)));
		network.addNode(node0184);
		Node node0185 = NetworkUtils.createNode(Id.createNodeId("nn_0185"), transformation.transform(new Coord(13.592310, 52.438582)));
		network.addNode(node0185);
		Node node0186 = NetworkUtils.createNode(Id.createNodeId("nn_0186"), transformation.transform(new Coord(13.625388, 52.457021)));
		network.addNode(node0186);
		Node node0187 = NetworkUtils.createNode(Id.createNodeId("nn_0187"), transformation.transform(new Coord(13.624165, 52.446951)));
		network.addNode(node0187);
		Node node0188 = NetworkUtils.createNode(Id.createNodeId("nn_0188"), transformation.transform(new Coord(13.616820, 52.446738)));
		network.addNode(node0188);
		Node node0189 = NetworkUtils.createNode(Id.createNodeId("nn_0189"), transformation.transform(new Coord(13.601147, 52.451593)));
		network.addNode(node0189);
		Node node0190 = NetworkUtils.createNode(Id.createNodeId("nn_0190"), transformation.transform(new Coord(13.624205, 52.446834)));
		network.addNode(node0190);
		Node node0191 = NetworkUtils.createNode(Id.createNodeId("nn_0191"), transformation.transform(new Coord(13.567164, 52.412578)));
		network.addNode(node0191);
		Node node0192 = NetworkUtils.createNode(Id.createNodeId("nn_0192"), transformation.transform(new Coord(13.570939, 52.412895)));
		network.addNode(node0192);
		Node node0193 = NetworkUtils.createNode(Id.createNodeId("nn_0193"), transformation.transform(new Coord(13.629525, 52.397106)));
		network.addNode(node0193);
		Node node0194 = NetworkUtils.createNode(Id.createNodeId("nn_0194"), transformation.transform(new Coord(13.638838, 52.393078)));
		network.addNode(node0194);
		Node node0195 = NetworkUtils.createNode(Id.createNodeId("nn_0195"), transformation.transform(new Coord(13.640819, 52.389982)));
		network.addNode(node0195);
		Node node0196 = NetworkUtils.createNode(Id.createNodeId("nn_0196"), transformation.transform(new Coord(13.648858, 52.375857)));
		network.addNode(node0196);
		Node node0197 = NetworkUtils.createNode(Id.createNodeId("nn_0197"), transformation.transform(new Coord(13.652929, 52.375041)));
		network.addNode(node0197);
		Node node0198 = NetworkUtils.createNode(Id.createNodeId("nn_0198"), transformation.transform(new Coord(13.656744, 52.374547)));
		network.addNode(node0198);
		Node node0199 = NetworkUtils.createNode(Id.createNodeId("nn_0199"), transformation.transform(new Coord(13.687365, 52.441526)));
		network.addNode(node0199);
		Node node0200 = NetworkUtils.createNode(Id.createNodeId("nn_0200"), transformation.transform(new Coord(13.691374, 52.444605)));
		network.addNode(node0200);
		Node node0201 = NetworkUtils.createNode(Id.createNodeId("nn_201"), transformation.transform(new Coord(13.691273, 52.450982)));
		network.addNode(node0201);
		Node node0202 = NetworkUtils.createNode(Id.createNodeId("nn_0202"), transformation.transform(new Coord(13.699467, 52.435840)));
		network.addNode(node0202);
		Node node0203 = NetworkUtils.createNode(Id.createNodeId("nn_203"), transformation.transform(new Coord(13.713622, 52.432069)));
		network.addNode(node0203);
		Node node0204 = NetworkUtils.createNode(Id.createNodeId("nn_0204"), transformation.transform(new Coord(13.730927, 52.430135)));
		network.addNode(node0204);
		Node node0205 = NetworkUtils.createNode(Id.createNodeId("nn_0205"), transformation.transform(new Coord(13.741611, 52.428102)));
		network.addNode(node0205);

		// Spandau

		Node node0206 = NetworkUtils.createNode(Id.createNodeId("nn_206"), transformation.transform(new Coord(13.162409, 52.520153)));
		network.addNode(node0206);
		Node node0207 = NetworkUtils.createNode(Id.createNodeId("nn_0207"), transformation.transform(new Coord(13.119249, 52.529044)));
		network.addNode(node0207);
		Node node0208 = NetworkUtils.createNode(Id.createNodeId("nn_0208"), transformation.transform(new Coord(13.196943, 52.513129)));
		network.addNode(node0208);
		Node node0209 = NetworkUtils.createNode(Id.createNodeId("nn_0209"), transformation.transform(new Coord(13.216298, 52.526852)));
		network.addNode(node0209);
		Node node0210 = NetworkUtils.createNode(Id.createNodeId("nn_0210"), transformation.transform(new Coord(13.244918, 52.524905)));
		network.addNode(node0210);
		Node node0211 = NetworkUtils.createNode(Id.createNodeId("nn_0211"), transformation.transform(new Coord(13.265018, 52.536656)));
		network.addNode(node0211);
		Node node0212 = NetworkUtils.createNode(Id.createNodeId("nn_0212"), transformation.transform(new Coord(13.282319, 52.533756)));
		network.addNode(node0212);
		Node node0213 = NetworkUtils.createNode(Id.createNodeId("nn_0213"), transformation.transform(new Coord(13.176774, 52.476220)));
		network.addNode(node0213);
		Node node0214 = NetworkUtils.createNode(Id.createNodeId("nn_0214"), transformation.transform(new Coord(13.182725, 52.483262)));
		network.addNode(node0214);
		Node node0215 = NetworkUtils.createNode(Id.createNodeId("nn_0215"), transformation.transform(new Coord(13.153592, 52.553309)));
		network.addNode(node0215);
		Node node0216 = NetworkUtils.createNode(Id.createNodeId("nn_0216"), transformation.transform(new Coord(13.165231, 52.551077)));
		network.addNode(node0216);
		Node node0217 = NetworkUtils.createNode(Id.createNodeId("nn_0217"), transformation.transform(new Coord(13.184757, 52.547458)));
		network.addNode(node0217);
		Node node0218 = NetworkUtils.createNode(Id.createNodeId("nn_0218"), transformation.transform(new Coord(13.193961, 52.545641)));
		network.addNode(node0218);
		Node node0219 = NetworkUtils.createNode(Id.createNodeId("nn_0219"), transformation.transform(new Coord(13.179158, 52.516608)));
		network.addNode(node0219);
		Node node0220 = NetworkUtils.createNode(Id.createNodeId("nn_0220"), transformation.transform(new Coord(13.184416, 52.515540)));
		network.addNode(node0220);


		// ... more nodes accorinding to list...

		double np_bicycleSpeed = 0.;
		double pbl_bicycleSpeed = 0.;

		// np= normal path
		// np_bicycleSpeed < pbl_bicycle speed
		// "np" added as type
		// "_" following id indicates the counter direction in case of a bi-directional path

		// Charottenburg-Wilmersdorf

		Link link001 = createBicycleLink(network,"np_001", node001, node002, np_bicycleSpeed, "np");
		network.addLink(link001);
		Link link002 = createBicycleLink(network,"np_002", node003, node004, np_bicycleSpeed, "np");
		network.addLink(link002);
		Link link003 = createBicycleLink(network,"np_003", node005, node006, np_bicycleSpeed, "np");
		network.addLink(link003);
		Link link003_ = createBicycleLink(network,"np_003_", node006, node005, np_bicycleSpeed, "np");
		network.addLink(link003_);
		Link link004 = createBicycleLink(network,"np_004", node007, node008, np_bicycleSpeed, "np");
		network.addLink(link004);

		// Friedrichshain-Kreuzberg
		Link link005 = createBicycleLink(network,"np_005", node009, node010, pbl_bicycleSpeed, "pbl");
		network.addLink(link005);
		Link link005_ = createBicycleLink(network,"np_005_", node010, node009, pbl_bicycleSpeed, "pbl");
		network.addLink(link005_);
		Link link006 = createBicycleLink(network,"np_006", node011, node012, pbl_bicycleSpeed, "pbl");
		network.addLink(link006);
		Link link006_ = createBicycleLink(network,"np_006_", node012, node011, pbl_bicycleSpeed, "pbl");
		network.addLink(link006_);
		Link link007 = createBicycleLink(network,"np_007", node013, node014, np_bicycleSpeed, "np");
		network.addLink(link007); 
		Link link007_ = createBicycleLink(network,"np_007_", node014, node013, np_bicycleSpeed, "np");
		network.addLink(link007_); 
		Link link008 = createBicycleLink(network,"np_008", node015, node016, np_bicycleSpeed, "np");
		network.addLink(link008);
		Link link008_ = createBicycleLink(network,"np_008_", node016, node015, np_bicycleSpeed, "np");
		network.addLink(link008_); 
		Link link009 = createBicycleLink(network,"np_009", node017, node018, pbl_bicycleSpeed, "pbl");
		network.addLink(link009);
		Link link010 = createBicycleLink(network,"np_010", node019, node020, np_bicycleSpeed, "np");
		network.addLink(link010);
		Link link011 = createBicycleLink(network,"np_011", node021, node022, np_bicycleSpeed, "np");
		network.addLink(link011);
		Link link012 = createBicycleLink(network,"np_012", node023, node024, np_bicycleSpeed, "np");
		network.addLink(link012); 
		Link link012_ = createBicycleLink(network,"np_012_", node024, node023, np_bicycleSpeed, "np");
		network.addLink(link012_);
		Link link013 = createBicycleLink(network,"np_013", node025, node026, np_bicycleSpeed, "np");
		network.addLink(link012); 
		Link link013_ = createBicycleLink(network,"np_013_", node026, node025, np_bicycleSpeed, "np");
		network.addLink(link013_); 
		Link link014 = createBicycleLink(network,"np_014", node026, node027, np_bicycleSpeed, "np");
		network.addLink(link014);
		Link link014_ = createBicycleLink(network,"np_014_", node027, node026, np_bicycleSpeed, "np");
		network.addLink(link014_);
		Link link015 = createBicycleLink(network,"np_015", node028, node029, pbl_bicycleSpeed, "pbl");
		network.addLink(link015); 
		Link link015_ = createBicycleLink(network,"np_015_", node029, node028, np_bicycleSpeed, "np");
		network.addLink(link015_);
		Link link016 = createBicycleLink(network,"np_016", node030, node031, pbl_bicycleSpeed, "pbl");
		network.addLink(link016);
		Link link016_ = createBicycleLink(network,"np_016_", node031, node030, pbl_bicycleSpeed, "pbl");
		network.addLink(link016_);
		Link link017 = createBicycleLink(network,"np_017", node031, node032, np_bicycleSpeed, "np");
		network.addLink(link017);
		Link link017_ = createBicycleLink(network,"np_017_", node032, node031, np_bicycleSpeed, "np");
		network.addLink(link017_);

		// Lichtenberg

		Link link018 = createBicycleLink(network,"np_018", node033, node034, np_bicycleSpeed, "np");
		network.addLink(link018);
		Link link018_ = createBicycleLink(network,"np_018_", node034, node033, np_bicycleSpeed, "np");
		network.addLink(link018_); 
		Link link019 = createBicycleLink(network,"np_019", node035, node036, np_bicycleSpeed, "np");
		network.addLink(link019);
		Link link019_ = createBicycleLink(network,"np_019_", node036, node035, np_bicycleSpeed, "np");
		network.addLink(link019_);

		//Marzahn-Hellersdorf

		Link link020 = createBicycleLink(network,"np_020", node037, node038, np_bicycleSpeed, "np");
		network.addLink(link020);
		Link link020_ = createBicycleLink(network,"np_020_", node038, node037, np_bicycleSpeed, "np");
		network.addLink(link020_);
		Link link021 = createBicycleLink(network,"np_021", node039, node040, np_bicycleSpeed, "np");
		network.addLink(link021); 
		Link link021_ = createBicycleLink(network,"np_021_", node040, node039, np_bicycleSpeed, "np");
		network.addLink(link021_);
		Link link022 = createBicycleLink(network,"np_022", node041, node042, np_bicycleSpeed, "np");
		network.addLink(link022);
		Link link022_ = createBicycleLink(network,"np_022_", node042, node041, np_bicycleSpeed, "np");
		network.addLink(link022_);
		Link link023 = createBicycleLink(network,"np_023", node043, node044, np_bicycleSpeed, "np");
		network.addLink(link023);
		Link link023_ = createBicycleLink(network,"np_023_", node044, node043, np_bicycleSpeed, "np");
		network.addLink(link023_);
		Link link024 = createBicycleLink(network,"np_024", node045, node046, np_bicycleSpeed, "np");
		network.addLink(link024);
		Link link024_ = createBicycleLink(network,"np_024_", node046, node045, np_bicycleSpeed, "np");
		network.addLink(link024_);

		//Mitte

		Link link025 = createBicycleLink(network,"np_025", node047, node048, np_bicycleSpeed, "np");
		network.addLink(link025);
		Link link025_ = createBicycleLink(network,"np_025_", node048, node047, np_bicycleSpeed, "np");
		network.addLink(link025_);
		Link link026 = createBicycleLink(network,"np_026", node048, node049, np_bicycleSpeed, "np");
		network.addLink(link026);
		Link link026_ = createBicycleLink(network,"np_026_", node049, node048, np_bicycleSpeed, "np");
		network.addLink(link026_); 
		Link link027 = createBicycleLink(network,"np_027", node050, node051, np_bicycleSpeed, "np");
		network.addLink(link027);
		Link link027_ = createBicycleLink(network,"np_027_", node051, node050, np_bicycleSpeed, "np");
		network.addLink(link027_);
		Link link028 = createBicycleLink(network,"np_028", node052, node053, np_bicycleSpeed, "np");
		network.addLink(link028);
		Link link028_ = createBicycleLink(network,"np_028_", node053, node052, np_bicycleSpeed, "np");
		network.addLink(link028_); 
		Link link029 = createBicycleLink(network,"np_029", node054, node055, np_bicycleSpeed, "np");
		network.addLink(link029);
		Link link029_ = createBicycleLink(network,"np_029_", node055, node054, np_bicycleSpeed, "np");
		network.addLink(link029_);
		Link link030 = createBicycleLink(network,"np_030", node056, node057, np_bicycleSpeed, "np");
		network.addLink(link030); 
		Link link030_ = createBicycleLink(network,"np_030_", node057, node056, np_bicycleSpeed, "np");
		network.addLink(link030_);
		Link link031 = createBicycleLink(network,"np_031", node058, node059, pbl_bicycleSpeed, "pbl");
		network.addLink(link031);
		Link link031_ = createBicycleLink(network,"np_031_", node059, node058, pbl_bicycleSpeed, "pbl");
		network.addLink(link031_);
		Link link032 = createBicycleLink(network,"np_032", node060, node061, pbl_bicycleSpeed, "pbl");
		network.addLink(link032);
		Link link033 = createBicycleLink(network,"np_033", node062, node063, pbl_bicycleSpeed, "pbl");
		network.addLink(link033);
		Link link033_ = createBicycleLink(network,"np_033_", node063, node062, pbl_bicycleSpeed, "pbl");
		network.addLink(link033_);
		Link link034 = createBicycleLink(network,"np_034", node064, node065, np_bicycleSpeed, "np");
		network.addLink(link034); 
		Link link034_ = createBicycleLink(network,"np_034_", node065, node064, np_bicycleSpeed, "np");
		network.addLink(link034_);
		Link link035 = createBicycleLink(network,"np_035", node066, node067, np_bicycleSpeed, "np");
		network.addLink(link035); 
		Link link035_ = createBicycleLink(network,"np_035_", node067, node066, np_bicycleSpeed, "np");
		network.addLink(link035_);
		Link link036 = createBicycleLink(network,"np_036", node068, node069, pbl_bicycleSpeed, "pbl");
		network.addLink(link036);
		Link link037 = createBicycleLink(network,"np_037", node070, node071, np_bicycleSpeed, "np");
		network.addLink(link037);
		Link link038 = createBicycleLink(network,"np_038", node071, node070, pbl_bicycleSpeed, "pbl");
		network.addLink(link038);
		Link link039 = createBicycleLink(network,"np_039", node072, node073, np_bicycleSpeed, "np");
		network.addLink(link039);
		Link link039_ = createBicycleLink(network,"np_039_", node073, node072, np_bicycleSpeed, "np");
		network.addLink(link039_);
		Link link040 = createBicycleLink(network,"np_040", node074, node075, np_bicycleSpeed, "np");
		network.addLink(link040); 
		Link link040_ = createBicycleLink(network,"np_040_", node075, node074, np_bicycleSpeed, "np");
		network.addLink(link040_); 
		Link link041 = createBicycleLink(network,"np_041", node076, node077, np_bicycleSpeed, "np");
		network.addLink(link041); 
		Link link041_ = createBicycleLink(network,"np_041_", node077, node076, np_bicycleSpeed, "np");
		network.addLink(link041_);

		// Neukölln

		Link link042 = createBicycleLink(network,"np_042", node078, node079, pbl_bicycleSpeed, "pbl");
		network.addLink(link042);
		Link link042_ = createBicycleLink(network,"np_042_", node079, node078, pbl_bicycleSpeed, "pbl");
		network.addLink(link042_);
		Link link043 = createBicycleLink(network,"np_043", node080, node081, np_bicycleSpeed, "np");
		network.addLink(link043); 
		Link link043_ = createBicycleLink(network,"np_043_", node081, node080, np_bicycleSpeed, "np");
		network.addLink(link043_);
		Link link044 = createBicycleLink(network,"np_044", node082, node083, pbl_bicycleSpeed, "pbl");
		network.addLink(link044);
		Link link044_ = createBicycleLink(network,"np_044_", node083, node082, np_bicycleSpeed, "np");
		network.addLink(link044_);
		Link link045 = createBicycleLink(network,"np_045", node084, node085, np_bicycleSpeed, "np");
		network.addLink(link045); 
		Link link045_ = createBicycleLink(network,"np_045_", node085, node084, np_bicycleSpeed, "np");
		network.addLink(link045_);



		// Pankow
		
		Link link046 = createBicycleLink(network,"np_046", node086, node087, np_bicycleSpeed, "np");
		network.addLink(link046);
		Link link046_ = createBicycleLink(network,"np_046_", node087, node086, np_bicycleSpeed, "np");
		network.addLink(link046_);
		Link link047 = createBicycleLink(network,"np_047", node088, node089, pbl_bicycleSpeed, "pbl");
		network.addLink(link047);
		Link link047_ = createBicycleLink(network,"np_047_", node089, node088, pbl_bicycleSpeed, "pbl");
		network.addLink(link047_);
		Link link048 = createBicycleLink(network,"np_048", node090, node091, np_bicycleSpeed, "np");
		network.addLink(link046);
		Link link048_ = createBicycleLink(network,"np_048_", node091, node090, np_bicycleSpeed, "np");
		network.addLink(link046_); 
		Link link049 = createBicycleLink(network,"np_049", node091, node092, np_bicycleSpeed, "np");
		network.addLink(link049);
		Link link049_ = createBicycleLink(network,"np_049_", node092, node091, np_bicycleSpeed, "np");
		network.addLink(link049_); 
		Link link050 = createBicycleLink(network,"np_050", node093, node094, np_bicycleSpeed, "np");
		network.addLink(link050); 
		Link link050_ = createBicycleLink(network,"np_050_", node094, node093, np_bicycleSpeed, "np");
		network.addLink(link050_); 
		Link link051 = createBicycleLink(network,"np_051", node095, node096, np_bicycleSpeed, "np");
		network.addLink(link051); 
		Link link051_ = createBicycleLink(network,"np_051_", node096, node095, np_bicycleSpeed, "np");
		network.addLink(link051_); 
		Link link052 = createBicycleLink(network,"np_052", node097, node098, np_bicycleSpeed, "np");
		network.addLink(link052);
		Link link052_ = createBicycleLink(network,"np_052_", node098, node097, np_bicycleSpeed, "np");
		network.addLink(link052_);
		Link link053 = createBicycleLink(network,"np_053", node099, node0100, pbl_bicycleSpeed, "pbl");
		network.addLink(link053);
		Link link053_ = createBicycleLink(network,"np_053_", node0100, node099, pbl_bicycleSpeed, "pbl");
		network.addLink(link053_);
		Link link054 = createBicycleLink(network,"np_054", node0101, node0102, np_bicycleSpeed, "np");
		network.addLink(link054); 
		Link link054_ = createBicycleLink(network,"np_054_", node0102, node0101, np_bicycleSpeed, "np");
		network.addLink(link054_); 
		Link link055 = createBicycleLink(network,"np_055", node0103, node0104, np_bicycleSpeed, "np");
		network.addLink(link055); 
		Link link055_ = createBicycleLink(network,"np_055_", node0104, node0103, np_bicycleSpeed, "np");
		network.addLink(link055_); 
		Link link056 = createBicycleLink(network,"np_056", node0105, node0106, np_bicycleSpeed, "np");
		network.addLink(link056); 
		Link link056_ = createBicycleLink(network,"np_056_", node0106, node0105, np_bicycleSpeed, "np");
		network.addLink(link056_); 
		Link link057 = createBicycleLink(network,"np_057", node0107, node0108, np_bicycleSpeed, "np");
		network.addLink(link057); 
		Link link057_ = createBicycleLink(network,"np_057_", node0108, node0107, np_bicycleSpeed, "np");
		network.addLink(link057_);
		Link link058 = createBicycleLink(network,"np_058", node0109, node0110, np_bicycleSpeed, "np");
		network.addLink(link058);
		Link link058_ = createBicycleLink(network,"np_058_", node0110, node0109, np_bicycleSpeed, "np");
		network.addLink(link058_);

		//Steglitz-Zahlendorf

		Link link059 = createBicycleLink(network,"np_059", node0111, node0112, np_bicycleSpeed, "np");
		network.addLink(link059);
		Link link059_ = createBicycleLink(network,"np_059_", node0112, node0111, np_bicycleSpeed, "np");
		network.addLink(link059_);
		Link link060 = createBicycleLink(network,"np_060", node0112, node0113, np_bicycleSpeed, "np");
		network.addLink(link060);
		Link link060_ = createBicycleLink(network,"np_060_", node0113, node0112, np_bicycleSpeed, "np");
		network.addLink(link060_);
		Link link061 = createBicycleLink(network,"np_061", node0113, node0114, np_bicycleSpeed, "np");
		network.addLink(link061);
		Link link062 = createBicycleLink(network,"np_062", node0115, node0116, pbl_bicycleSpeed, "pbl");
		network.addLink(link062);
		Link link063 = createBicycleLink(network,"np_063", node0117, node0118, np_bicycleSpeed, "np");
		network.addLink(link063); 
		Link link063_ = createBicycleLink(network,"np_063_", node0118, node0117, np_bicycleSpeed, "np");
		network.addLink(link063_);
		Link link064 = createBicycleLink(network,"np_064", node0118, node0119, np_bicycleSpeed, "np");
		network.addLink(link064);
		Link link064_ = createBicycleLink(network,"np_064_", node0119, node0118, np_bicycleSpeed, "np");
		network.addLink(link064_); 
		Link link065 = createBicycleLink(network,"np_065", node0120, node0121, np_bicycleSpeed, "np");
		network.addLink(link065);
		Link link066 = createBicycleLink(network,"np_066", node0122, node0123, np_bicycleSpeed, "np");
		network.addLink(link066);
		Link link067 = createBicycleLink(network,"np_067", node0123, node0124, np_bicycleSpeed, "np");
		network.addLink(link067);
		Link link068 = createBicycleLink(network,"np_068", node0125, node0126, np_bicycleSpeed, "np");
		network.addLink(link068);
		Link link068_ = createBicycleLink(network,"np_068_", node0126, node0125, np_bicycleSpeed, "np");
		network.addLink(link068_);
		Link link069 = createBicycleLink(network,"np_069", node0127, node0128, pbl_bicycleSpeed, "pbl");
		network.addLink(link069);
		Link link069_ = createBicycleLink(network,"np_069_", node0128, node0127, pbl_bicycleSpeed, "pbl");
		network.addLink(link069_);
		Link link070 = createBicycleLink(network,"np_070", node0129, node0130, np_bicycleSpeed, "np");
		network.addLink(link070); 
		Link link070_ = createBicycleLink(network,"np_070_", node0130, node0129, np_bicycleSpeed, "np");
		network.addLink(link070_); 
		Link link071 = createBicycleLink(network,"np_071", node0131, node0132, np_bicycleSpeed, "np");
		network.addLink(link071); 
		Link link071_ = createBicycleLink(network,"np_071_", node0132, node0131, np_bicycleSpeed, "np");
		network.addLink(link071_); 
		Link link072 = createBicycleLink(network,"np_072", node0133, node0134, np_bicycleSpeed, "np");
		network.addLink(link072); 
		Link link072_ = createBicycleLink(network,"np_072_", node0134, node0133, np_bicycleSpeed, "np");
		network.addLink(link072_); 
		Link link073 = createBicycleLink(network,"np_073", node0135, node0136, np_bicycleSpeed, "np");
		network.addLink(link073); 
		Link link073_ = createBicycleLink(network,"np_073_", node0136, node0135, np_bicycleSpeed, "np");
		network.addLink(link073_); 
		Link link074 = createBicycleLink(network,"np_074", node0137, node0138, np_bicycleSpeed, "np");
		network.addLink(link074); 
		Link link074_ = createBicycleLink(network,"np_074_", node0138, node0137, np_bicycleSpeed, "np");
		network.addLink(link074_); 
		Link link075 = createBicycleLink(network,"np_075", node0139, node0140, np_bicycleSpeed, "np");
		network.addLink(link075); 
		Link link076 = createBicycleLink(network,"np_076", node0141, node0142, np_bicycleSpeed, "np");
		network.addLink(link076); 
		Link link076_ = createBicycleLink(network,"np_076_", node0142, node0141, np_bicycleSpeed, "np");
		network.addLink(link076_);
		Link link077 = createBicycleLink(network,"np_077", node0143, node0144, np_bicycleSpeed, "np");
		network.addLink(link077);
		Link link077_ = createBicycleLink(network,"np_077_", node0144, node0143, np_bicycleSpeed, "np");
		network.addLink(link077_); 
		Link link078 = createBicycleLink(network,"np_078", node0145, node0146, np_bicycleSpeed, "np");
		network.addLink(link078); 
		Link link078_ = createBicycleLink(network,"np_078_", node0146, node0145, np_bicycleSpeed, "np");
		network.addLink(link078_);

		// Tempelhof-Schöneberg

		Link link079 = createBicycleLink(network,"np_079", node0147, node0148, np_bicycleSpeed, "np");
		network.addLink(link079);
		Link link079_ = createBicycleLink(network,"np_079_", node0148, node0147, np_bicycleSpeed, "np");
		network.addLink(link079_);
		Link link080 = createBicycleLink(network,"np_080", node0148, node0149, np_bicycleSpeed, "np");
		network.addLink(link080); 
		Link link080_ = createBicycleLink(network,"np_080_", node0149, node0148, np_bicycleSpeed, "np");
		network.addLink(link080_);
		Link link081 = createBicycleLink(network,"np_081", node0149, node0150, np_bicycleSpeed, "np");
		network.addLink(link081);
		Link link081_ = createBicycleLink(network,"np_081_", node0150, node0149, np_bicycleSpeed, "np");
		network.addLink(link081_); 
		Link link082 = createBicycleLink(network,"np_082", node0151, node0152, np_bicycleSpeed, "np");
		network.addLink(link082);
		Link link082_ = createBicycleLink(network,"np_082_", node0152, node0151, np_bicycleSpeed, "np");
		network.addLink(link082_); 
		Link link083 = createBicycleLink(network,"np_083", node0153, node0154, np_bicycleSpeed, "np");
		network.addLink(link083);
		Link link083_ = createBicycleLink(network,"np_083_", node0154, node0153, np_bicycleSpeed, "np");
		network.addLink(link083_);
		Link link084 = createBicycleLink(network,"np_084", node0155, node0156, np_bicycleSpeed, "np");
		network.addLink(link084); 
		Link link084_ = createBicycleLink(network,"np_084_", node0156, node0155, np_bicycleSpeed, "np");
		network.addLink(link084_);
		Link link085 = createBicycleLink(network,"np_085", node0157, node0158, np_bicycleSpeed, "np");
		network.addLink(link085);
		Link link085_ = createBicycleLink(network,"np_085_", node0158, node0157, np_bicycleSpeed, "np");
		network.addLink(link085_);
		Link link086 = createBicycleLink(network,"np_086", node0159, node0160, np_bicycleSpeed, "np");
		network.addLink(link086);
		Link link086_ = createBicycleLink(network,"np_086_", node0160, node0159, np_bicycleSpeed, "np");
		network.addLink(link086_); 
		Link link087 = createBicycleLink(network,"np_087", node0161, node0162, np_bicycleSpeed, "np");
		network.addLink(link087);
		Link link087_ = createBicycleLink(network,"np_087_", node0162, node0161, np_bicycleSpeed, "np");
		network.addLink(link087_);
		Link link088 = createBicycleLink(network,"np_088", node0163, node0164, np_bicycleSpeed, "np");
		network.addLink(link088); 
		Link link088_ = createBicycleLink(network,"np_088_", node0164, node0163, np_bicycleSpeed, "np");
		network.addLink(link088_); 

		//Treptow-Köpenick

		Link link089 = createBicycleLink(network,"np_089", node0165, node0166, np_bicycleSpeed, "np");
		network.addLink(link089);
		Link link090 = createBicycleLink(network,"np_090", node0167, node0168, np_bicycleSpeed, "np");
		network.addLink(link090); 
		Link link090_ = createBicycleLink(network,"np_090_", node0168, node0167, np_bicycleSpeed, "np");
		network.addLink(link090_); 
		Link link091 = createBicycleLink(network,"np_091", node0168, node0169, np_bicycleSpeed, "np");
		network.addLink(link091); 
		Link link091_ = createBicycleLink(network,"np_091_", node0169, node0168, np_bicycleSpeed, "np");
		network.addLink(link091_); 
		Link link092 = createBicycleLink(network,"np_092", node0170, node0171, np_bicycleSpeed, "np");
		network.addLink(link092); 
		Link link093 = createBicycleLink(network,"np_093", node0172, node0173, np_bicycleSpeed, "np");
		network.addLink(link093); 
		Link link093_ = createBicycleLink(network,"np_093_", node0173, node0172, np_bicycleSpeed, "np");
		network.addLink(link093_); 
		Link link094 = createBicycleLink(network,"np_094", node0174, node0175, np_bicycleSpeed, "np");
		network.addLink(link094); 
		Link link094_ = createBicycleLink(network,"np_094_", node0175, node0174, np_bicycleSpeed, "np");
		network.addLink(link094_); 
		Link link095 = createBicycleLink(network,"np_095", node0176, node0177, np_bicycleSpeed, "np");
		network.addLink(link095); 
		Link link095_ = createBicycleLink(network,"np_095_", node0177, node0176, np_bicycleSpeed, "np");
		network.addLink(link095_); 
		Link link096 = createBicycleLink(network,"np_096", node0178, node0179, np_bicycleSpeed, "np");
		network.addLink(link096); 
		Link link096_ = createBicycleLink(network,"np_096_", node0179, node0178, np_bicycleSpeed, "np");
		network.addLink(link096_); 
		Link link097 = createBicycleLink(network,"np_097", node0180, node0181, np_bicycleSpeed, "np");
		network.addLink(link097); 
		Link link098 = createBicycleLink(network,"np_098", node0182, node0183, np_bicycleSpeed, "np");
		network.addLink(link098); 
		Link link098_ = createBicycleLink(network,"np_098_", node0183, node0182, np_bicycleSpeed, "np");
		network.addLink(link098_); 
		Link link099 = createBicycleLink(network,"np_099", node0184, node0185, np_bicycleSpeed, "np");
		network.addLink(link099); 
		Link link0100 = createBicycleLink(network,"np_0100", node0186, node0187, np_bicycleSpeed, "np");
		network.addLink(link0100); 
		Link link0100_ = createBicycleLink(network,"np_0100_", node0187, node0186, np_bicycleSpeed, "np");
		network.addLink(link0100_); 
		Link link0101 = createBicycleLink(network,"np_0101", node0188, node0189, np_bicycleSpeed, "np");
		network.addLink(link0101); 
		Link link0101_ = createBicycleLink(network,"np_0101_", node0189, node0188, np_bicycleSpeed, "np");
		network.addLink(link0101_); 
		Link link0102 = createBicycleLink(network,"np_0102", node0188, node0190, np_bicycleSpeed, "np");
		network.addLink(link0102); 
		Link link0102_ = createBicycleLink(network,"np_0102_", node0190, node0188, np_bicycleSpeed, "np");
		network.addLink(link0102_); 
		Link link0103 = createBicycleLink(network,"np_0103", node0191, node0192, np_bicycleSpeed, "np");
		network.addLink(link0103); 
		Link link0103_ = createBicycleLink(network,"np_0103_", node0192, node0191, np_bicycleSpeed, "np");
		network.addLink(link0103_); 
		Link link0104 = createBicycleLink(network,"np_0104", node0193, node0194, np_bicycleSpeed, "np");
		network.addLink(link0104); 
		Link link0104_ = createBicycleLink(network,"np_0104_", node0194, node0193, np_bicycleSpeed, "np");
		network.addLink(link0104_); 
		Link link0105 = createBicycleLink(network,"np_0105", node0194, node0195, np_bicycleSpeed, "np");
		network.addLink(link0105); 
		Link link0105_ = createBicycleLink(network,"np_0105_", node0195, node0194, np_bicycleSpeed, "np");
		network.addLink(link0105_); 
		Link link0106 = createBicycleLink(network,"np_0106", node0196, node0197, np_bicycleSpeed, "np");
		network.addLink(link0106); 
		Link link0106_ = createBicycleLink(network,"np_0106_", node0197, node0196, np_bicycleSpeed, "np");
		network.addLink(link0106_); 
		Link link0107 = createBicycleLink(network,"np_0107", node0197, node0198, np_bicycleSpeed, "np");
		network.addLink(link0107); 
		Link link0107_ = createBicycleLink(network,"np_0107_", node0198, node0199, np_bicycleSpeed, "np");
		network.addLink(link0107_); 
		Link link0108 = createBicycleLink(network,"np_0108", node0199, node0200, np_bicycleSpeed, "np");
		network.addLink(link0108); 
		Link link0108_ = createBicycleLink(network,"np_0108_", node0200, node0199, np_bicycleSpeed, "np");
		network.addLink(link0108_); 
		Link link0109 = createBicycleLink(network,"np_0109", node0200, node0201, np_bicycleSpeed, "np");
		network.addLink(link0109); 
		Link link0109_ = createBicycleLink(network,"np_0109_", node0201, node0200, np_bicycleSpeed, "np");
		network.addLink(link0109_); 
		Link link0110 = createBicycleLink(network,"np_0110", node0202, node0203, np_bicycleSpeed, "np");
		network.addLink(link0110); 
		Link link0111 = createBicycleLink(network,"np_0111", node0204, node0205, np_bicycleSpeed, "np");
		network.addLink(link0111); 

		// Spandau

		Link link0112 = createBicycleLink(network,"np_0112", node0206, node0207, np_bicycleSpeed, "np");
		network.addLink(link0112); 
		Link link0112_ = createBicycleLink(network,"np_0112_", node0207, node0206, np_bicycleSpeed, "np");
		network.addLink(link0112_); 
		Link link0113 = createBicycleLink(network,"np_0113", node0207, node0208, np_bicycleSpeed, "np");
		network.addLink(link0113); 
		Link link0113_ = createBicycleLink(network,"np_0113_", node0208, node0207, np_bicycleSpeed, "np");
		network.addLink(link0113_);
		Link link0114 = createBicycleLink(network,"np_0114", node0209, node0210, np_bicycleSpeed, "np");
		network.addLink(link0114); 
		Link link0114_ = createBicycleLink(network,"np_0114_", node0210, node0209, np_bicycleSpeed, "np");
		network.addLink(link0114_); 
		Link link0115 = createBicycleLink(network,"np_0115", node0211, node0212, np_bicycleSpeed, "np");
		network.addLink(link0115); 
		Link link0115_ = createBicycleLink(network,"np_0115_", node0212, node0211, np_bicycleSpeed, "np");
		network.addLink(link0115_);
		Link link0116 = createBicycleLink(network,"np_0116", node0213, node0214, np_bicycleSpeed, "np");
		network.addLink(link0116); 
		Link link0116_ = createBicycleLink(network,"np_0116_", node0214, node0213, np_bicycleSpeed, "np");
		network.addLink(link0116_); 
		Link link0117 = createBicycleLink(network,"np_0117", node0215, node0216, np_bicycleSpeed, "np");
		network.addLink(link0117); 
		Link link0117_ = createBicycleLink(network,"np_0117_", node0216, node0215, np_bicycleSpeed, "np");
		network.addLink(link0117_);
		Link link0118 = createBicycleLink(network,"np_0118", node0217, node0218, np_bicycleSpeed, "np");
		network.addLink(link0118); 
		Link link0118_ = createBicycleLink(network,"np_0118_", node0218, node0217, np_bicycleSpeed, "np");
		network.addLink(link0118_);
		Link link0119 = createBicycleLink(network,"np_0119", node0219, node0220, np_bicycleSpeed, "np");
		network.addLink(link0119); 
		Link link0119_ = createBicycleLink(network,"np_0119_", node0220, node0219, np_bicycleSpeed, "np");
		network.addLink(link0119_); 


	}

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