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
public class AddNodesAndLinksToNetwork_Scenario2 {
	
	static void createNewBicycleNodesAndLinks(Network network) {

		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:4326", "EPSG:31464");
		
		// nn= new node, but not all are protected bike lanes, differenciation in links
		// Charlottenburg-Wilmersdorf

		Node node001 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_001"), transformation.transform(new Coord(13.325650, 52.538834)));
		Node node002 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_002"), transformation.transform(new Coord(13.297837, 52.534645)));
		Node node003 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_003"), transformation.transform(new Coord(13.249519, 52.523718)));
		Node node004 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_004"), transformation.transform(new Coord(13.262779, 52.521015)));
		Node node005 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_005"), transformation.transform(new Coord(13.320219, 52.478925)));
		Node node006 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_006"), transformation.transform(new Coord(13.313331, 52.480467)));
		Node node007 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_007"), transformation.transform(new Coord(13.329745, 52.505324)));
		Node node008 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_008"), transformation.transform(new Coord(13.331346, 52.505203)));

		//Friedrichshain - Kreuzberg
		Node node009 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_009"), transformation.transform(new Coord(13.383633, 52.493627)));
		Node node010 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_010"), transformation.transform(new Coord(13.405282, 52.489386)));
		Node node011 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_011"), transformation.transform(new Coord(13.376847, 52.491609)));
		Node node012 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_012"), transformation.transform(new Coord(13.383401, 52.493687)));
		Node node013 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_013"), transformation.transform(new Coord(13.417666, 52.499112)));
		Node node014 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_014"), transformation.transform(new Coord(13.391983, 52.497973)));
		Node node015 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_015"), transformation.transform(new Coord(13.453585, 52.515990)));
		Node node016 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_016"), transformation.transform(new Coord(13.429529, 52.518505)));
		Node node017 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_017"), transformation.transform(new Coord(13.454365, 52.515674)));
		Node node018 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_018"), transformation.transform(new Coord(13.476684, 52.513422)));
		Node node019 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_019"), transformation.transform(new Coord(13.446999, 52.502569)));
		Node node020 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_020"), transformation.transform(new Coord(13.464238, 52.497485)));
		Node node021 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_021"), transformation.transform(new Coord(13.410390, 52.503912)));
		Node node022 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_022"), transformation.transform(new Coord(13.398444, 52.507127)));
		Node node023 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_023"), transformation.transform(new Coord(13.410318, 52.503302)));
		Node node024 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_024"), transformation.transform(new Coord(13.405186, 52.496672)));
		Node node025 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_025"), transformation.transform(new Coord(13.469843, 52.498576)));
		Node node026 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_026"), transformation.transform(new Coord(13.470111, 52.500379)));
		Node node027 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_027"), transformation.transform(new Coord(13.474263, 52.500274)));
		Node node028 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_028"), transformation.transform(new Coord(13.380189, 52.498719)));
		Node node029 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_029"), transformation.transform(new Coord(13.379462, 52.492581)));
		Node node030 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_030"), transformation.transform(new Coord(13.386120, 52.485082)));
		Node node031 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_031"), transformation.transform(new Coord(13.386211, 52.487330)));
		Node node032 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_032"), transformation.transform(new Coord(13.389719, 52.495845)));

		// Lichtenberg

		Node node033 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_033"), transformation.transform(new Coord(13.500749, 52.510860)));
		Node node034 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_034"), transformation.transform(new Coord(13.505967, 52.512222)));
		Node node035 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_035"), transformation.transform(new Coord(13.497761, 52.586082)));
		Node node036 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_036"), transformation.transform(new Coord(13.499790, 52.586287)));

		//Marzahn-Hellersdorf

		Node node037 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_037"), transformation.transform(new Coord(13.554116, 52.563964)));
		Node node038 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_038"), transformation.transform(new Coord(13.574908, 52.560148)));
		Node node039 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_039"), transformation.transform(new Coord(13.579300, 52.525427)));
		Node node040 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_040"), transformation.transform(new Coord(13.574467, 52.513324)));
		Node node041 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_041"), transformation.transform(new Coord(13.519986, 52.525786)));
		Node node042 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_042"), transformation.transform(new Coord(13.529102, 52.525333)));
		Node node043 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_043"), transformation.transform(new Coord(13.596924, 52.507507)));
		Node node044 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_044"), transformation.transform(new Coord(13.592663, 52.508511)));
		Node node045 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_045"), transformation.transform(new Coord(13.530262, 52.556143)));
		Node node046 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_046"), transformation.transform(new Coord(13.528270, 52.556944)));

		//Mitte

		Node node047 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_047"), transformation.transform(new Coord(13.335147, 52.516955)));
		Node node048 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_048"), transformation.transform(new Coord(13.341731, 52.522406)));
		Node node049 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_049"), transformation.transform(new Coord(13.354569, 52.517328)));
		Node node050 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_050"), transformation.transform(new Coord(13.382212, 52.513937)));
		Node node051 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_051"), transformation.transform(new Coord(13.400756, 52.516161)));
		Node node052 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_052"), transformation.transform(new Coord(13.374344, 52.537267)));
		Node node053 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_053"), transformation.transform(new Coord(13.382092, 52.531288)));
		Node node054 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_054"), transformation.transform(new Coord(13.352424, 52.550406)));
		Node node055 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_055"), transformation.transform(new Coord(13.361351, 52.555056)));
		Node node056 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_056"), transformation.transform(new Coord(13.314493, 52.530336)));
		Node node057 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_057"), transformation.transform(new Coord(13.328251, 52.532825)));
		Node node058 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_058"), transformation.transform(new Coord(13.416554, 52.521825)));
		Node node059 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_059"), transformation.transform(new Coord(13.427306, 52.518555)));
		Node node060 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_060"), transformation.transform(new Coord(13.344054, 52.546008)));
		Node node061 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_061"), transformation.transform(new Coord(13.349180, 52.542245)));
		Node node062 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_062"), transformation.transform(new Coord(13.418203, 52.515364)));
		Node node063 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_063"), transformation.transform(new Coord(13.423919, 52.513442)));
		Node node064 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_064"), transformation.transform(new Coord(13.343040, 52.525345)));
		Node node065 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_065"), transformation.transform(new Coord(13.349493, 52.524491)));
		Node node066 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_066"), transformation.transform(new Coord(13.369706, 52.559615)));
		Node node067 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_067"), transformation.transform(new Coord(13.373171, 52.557025)));
		Node node068 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_068"), transformation.transform(new Coord(13.342194, 52.523117)));
		Node node069 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_069"), transformation.transform(new Coord(13.343030, 52.525347)));
		Node node070 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_070"), transformation.transform(new Coord(13.343107, 52.528446)));
		Node node071 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_071"), transformation.transform(new Coord(13.343003, 52.526398)));
		Node node072 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_072"), transformation.transform(new Coord(13.369078, 52.533810)));
		Node node073 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_073"), transformation.transform(new Coord(13.366133, 52.532880)));
		Node node074 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_074"), transformation.transform(new Coord(13.414119, 52.507876)));
		Node node075 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_075"), transformation.transform(new Coord(13.416317, 52.507012)));
		Node node076 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_076"), transformation.transform(new Coord(13.342307, 52.552424)));
		Node node077 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_077"), transformation.transform(new Coord(13.343740, 52.553343)));

		// Neukölln

		Node node078 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_078"), transformation.transform(new Coord(13.424014, 52.486361)));
		Node node079 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_079"), transformation.transform(new Coord(13.434217, 52.462926)));
		Node node080 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_080"), transformation.transform(new Coord(13.431380, 52.483065)));
		Node node081 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_081"), transformation.transform(new Coord(13.437822, 52.478497)));
		Node node082 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_082"), transformation.transform(new Coord(13.424440, 52.486196)));
		Node node083 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_083"), transformation.transform(new Coord(13.431255, 52.483123)));
		Node node084 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_084"), transformation.transform(new Coord(13.449541, 52.417767)));
		Node node085 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_085"), transformation.transform(new Coord(13.451937, 52.418560)));

		//Pankow

		Node node086 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_086"), transformation.transform(new Coord(13.420294, 52.566985)));
		Node node087 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_087"), transformation.transform(new Coord(13.424013, 52.552904)));
		Node node088 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_088"), transformation.transform(new Coord(13.364141, 52.581727)));
		Node node089 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_089"), transformation.transform(new Coord(13.376979, 52.574936)));
		Node node090 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_090"), transformation.transform(new Coord(13.433114, 52.535968)));
		Node node091 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_091"), transformation.transform(new Coord(13.441200, 52.532090)));
		Node node092 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_092"), transformation.transform(new Coord(13.447324, 52.526454)));
		Node node093 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_093"), transformation.transform(new Coord(13.375723, 52.597142)));
		Node node094 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_094"), transformation.transform(new Coord(13.384568, 52.591257)));
		Node node095 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_095"), transformation.transform(new Coord(13.402289, 52.602827)));
		Node node096 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_096"), transformation.transform(new Coord(13.414388, 52.604975)));
		Node node097 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_097"), transformation.transform(new Coord(13.385264, 52.591192)));
		Node node098 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_098"), transformation.transform(new Coord(13.402694, 52.593125)));
		Node node099 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_099"), transformation.transform(new Coord(13.369205, 52.597598)));
		Node node0100 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0100"), transformation.transform(new Coord(13.376294, 52.606544)));
		Node node0101 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0101"), transformation.transform(new Coord(13.491277, 52.637670)));
		Node node0102 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0102"), transformation.transform(new Coord(13.500189, 52.632729)));
		Node node0103 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0103"), transformation.transform(new Coord(13.376999, 52.577611)));
		Node node0104 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0104"), transformation.transform(new Coord(13.380116, 52.575025)));
		Node node0105 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0105"), transformation.transform(new Coord(13.424354, 52.538998)));
		Node node0106 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0106"), transformation.transform(new Coord(13.433002, 52.535989)));
		Node node0107 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0107"), transformation.transform(new Coord(13.428516, 52.582997)));
		Node node0108 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0108"), transformation.transform(new Coord(13.428687, 52.578088)));
		Node node0109 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0109"), transformation.transform(new Coord(13.479995, 52.553365)));
		Node node0110 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0110"), transformation.transform(new Coord(13.490074, 52.557785)));


		// Steglitz-Zehlendorf

		Node node0111 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0111"), transformation.transform(new Coord(13.270489, 52.450928)));
		Node node0112 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0112"), transformation.transform(new Coord(13.251114, 52.450145)));
		Node node0113 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0113"), transformation.transform(new Coord(13.232391, 52.437789)));
		Node node0114 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0114"), transformation.transform(new Coord(13.229131, 52.424843)));
		Node node0115 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0115"), transformation.transform(new Coord(13.352963, 52.433228)));
		Node node0116 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0116"), transformation.transform(new Coord(13.358843, 52.420046)));
		Node node0117 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0117"), transformation.transform(new Coord(13.313288, 52.430440)));
		Node node0118 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0118"), transformation.transform(new Coord(13.317322, 52.430721)));
		Node node0119 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0119"), transformation.transform(new Coord(13.325010, 52.428165)));
		Node node0120 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0120"), transformation.transform(new Coord(13.214843, 52.427320)));
		Node node0121 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0121"), transformation.transform(new Coord(13.223953, 52.429671)));
		Node node0122 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0122"), transformation.transform(new Coord(13.270531, 52.450835)));
		Node node0123 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0123"), transformation.transform(new Coord(13.267706, 52.446019)));
		Node node0124 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0124"), transformation.transform(new Coord(13.265372, 52.442257)));
		Node node0125 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0125"), transformation.transform(new Coord(13.346477, 52.430871)));
		Node node0126 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0126"), transformation.transform(new Coord(13.347546, 52.425705)));
		Node node0127 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0127"), transformation.transform(new Coord(13.282248, 52.440404)));
		Node node0128 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0128"), transformation.transform(new Coord(13.280730, 52.435614)));
		Node node0129 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0129"), transformation.transform(new Coord(13.344240, 52.421655)));
		Node node0130 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0130"), transformation.transform(new Coord(13.347614, 52.425636)));
		Node node0131 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0131"), transformation.transform(new Coord(13.351710, 52.440236)));
		Node node0132 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0132"), transformation.transform(new Coord(13.354113, 52.443290)));
		Node node0133 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0133"), transformation.transform(new Coord(13.347012, 52.450028)));
		Node node0134 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0134"), transformation.transform(new Coord(13.348609, 52.453438)));
		Node node0135 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0135"), transformation.transform(new Coord(13.265643, 52.442257)));
		Node node0136 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0136"), transformation.transform(new Coord(13.267744, 52.445477)));
		Node node0137 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0137"), transformation.transform(new Coord(13.327918, 52.428557)));
		Node node0138 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0138"), transformation.transform(new Coord(13.332551, 52.429761)));
		Node node0139 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0139"), transformation.transform(new Coord(13.279840, 52.432520)));
		Node node0140 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0140"), transformation.transform(new Coord(13.280876, 52.435578)));
		Node node0141 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0141"), transformation.transform(new Coord(13.175239, 52.420070)));
		Node node0142 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0142"), transformation.transform(new Coord(13.171163, 52.420911)));
		Node node0143 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0143"), transformation.transform(new Coord(13.346643, 52.430918)));
		Node node0144 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0144"), transformation.transform(new Coord(13.347711, 52.432086)));
		Node node0145 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0145"), transformation.transform(new Coord(13.347918, 52.432314)));
		Node node0146 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0146"), transformation.transform(new Coord(13.349032, 52.433317)));

		// Tempelhof-Schöneberg

		Node node0147 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0147"), transformation.transform(new Coord(13.381145, 52.465691)));
		Node node0148 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0148"), transformation.transform(new Coord(13.382244, 52.463175)));
		Node node0149 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0149"), transformation.transform(new Coord(13.380982, 52.462613)));
		Node node0150 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0150"), transformation.transform(new Coord(13.381237, 52.461425)));
		Node node0151 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0151"), transformation.transform(new Coord(13.369840, 52.470767)));
		Node node0152 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0152"), transformation.transform(new Coord(13.377943, 52.466384)));
		Node node0153 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0153"), transformation.transform(new Coord(13.385505, 52.465689)));
		Node node0154 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0154"), transformation.transform(new Coord(13.384387, 52.452768)));
		Node node0155 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0155"), transformation.transform(new Coord(13.365244, 52.446489)));
		Node node0156 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0156"), transformation.transform(new Coord(13.370389, 52.449172)));
		Node node0157 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0157"), transformation.transform(new Coord(13.372302, 52.444431)));
		Node node0158 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0158"), transformation.transform(new Coord(13.381240, 52.440871)));
		Node node0159 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0159"), transformation.transform(new Coord(13.382448, 52.445407)));
		Node node0160 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0160"), transformation.transform(new Coord(13.382615, 52.443839)));
		Node node0161 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0161"), transformation.transform(new Coord(13.381777, 52.439221)));
		Node node0162 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0162"), transformation.transform(new Coord(13.387152, 52.440126)));
		Node node0163 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0163"), transformation.transform(new Coord(13.403638, 52.406781)));
		Node node0164 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0164"), transformation.transform(new Coord(13.404252, 52.403983)));

		//Treptow-Köpenick

		Node node0165 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0165"), transformation.transform(new Coord(13.453987, 52.488986)));
		Node node0166 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0166"), transformation.transform(new Coord(13.457600, 52.491701)));
		Node node0167 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0167"), transformation.transform(new Coord(13.501485, 52.483076)));
		Node node0168 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0168"), transformation.transform(new Coord(13.509729, 52.472884)));
		Node node0169 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0169"), transformation.transform(new Coord(13.514213, 52.468875)));
		Node node0170 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0170"), transformation.transform(new Coord(13.457831, 52.491654)));
		Node node0171 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0171"), transformation.transform(new Coord(13.475222, 52.480067)));
		Node node0172 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0172"), transformation.transform(new Coord(13.473771, 52.474935)));
		Node node0173 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0173"), transformation.transform(new Coord(13.491224, 52.462626)));
		Node node0174 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0174"), transformation.transform(new Coord(13.514216, 52.468703)));
		Node node0175 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0175"), transformation.transform(new Coord(13.513913, 52.462565)));
		Node node0176 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0176"), transformation.transform(new Coord(13.513494, 52.464704)));
		Node node0177 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0177"), transformation.transform(new Coord(13.508653, 52.463817)));
		Node node0178 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0178"), transformation.transform(new Coord(13.526571, 52.425887)));
		Node node0179 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0179"), transformation.transform(new Coord(13.525904, 52.423225)));
		Node node0180 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0180"), transformation.transform(new Coord(13.562713, 52.437370)));
		Node node0181 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0181"), transformation.transform(new Coord(13.564674, 52.438522)));
		Node node0182 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0182"), transformation.transform(new Coord(13.592237, 52.454966)));
		Node node0183 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0183"), transformation.transform(new Coord(13.593153, 52.453121)));
		Node node0184 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0184"), transformation.transform(new Coord(13.582286, 52.442474)));
		Node node0185 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0185"), transformation.transform(new Coord(13.592310, 52.438582)));
		Node node0186 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0186"), transformation.transform(new Coord(13.625388, 52.457021)));
		Node node0187 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0187"), transformation.transform(new Coord(13.624165, 52.446951)));
		Node node0188 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0188"), transformation.transform(new Coord(13.616820, 52.446738)));
		Node node0189 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0189"), transformation.transform(new Coord(13.601147, 52.451593)));
		Node node0190 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0190"), transformation.transform(new Coord(13.624205, 52.446834)));
		Node node0191 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0191"), transformation.transform(new Coord(13.567164, 52.412578)));
		Node node0192 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0192"), transformation.transform(new Coord(13.570939, 52.412895)));
		Node node0193 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0193"), transformation.transform(new Coord(13.629525, 52.397106)));
		Node node0194 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0194"), transformation.transform(new Coord(13.638838, 52.393078)));
		Node node0195 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0195"), transformation.transform(new Coord(13.640819, 52.389982)));
		Node node0196 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0196"), transformation.transform(new Coord(13.648858, 52.375857)));
		Node node0197 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0197"), transformation.transform(new Coord(13.652929, 52.375041)));
		Node node0198 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0198"), transformation.transform(new Coord(13.656744, 52.374547)));
		Node node0199 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0199"), transformation.transform(new Coord(13.687365, 52.441526)));
		Node node0200 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0200"), transformation.transform(new Coord(13.691374, 52.444605)));
		Node node0201 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_201"), transformation.transform(new Coord(13.691273, 52.450982)));
		Node node0202 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0202"), transformation.transform(new Coord(13.699467, 52.435840)));
		Node node0203 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_203"), transformation.transform(new Coord(13.713622, 52.432069)));
		Node node0204 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0204"), transformation.transform(new Coord(13.730927, 52.430135)));
		Node node0205 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0205"), transformation.transform(new Coord(13.741611, 52.428102)));

		// Spandau

		Node node0206 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_206"), transformation.transform(new Coord(13.162409, 52.520153)));
		Node node0207 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0207"), transformation.transform(new Coord(13.119249, 52.529044)));
		Node node0208 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0208"), transformation.transform(new Coord(13.196943, 52.513129)));
		Node node0209 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0209"), transformation.transform(new Coord(13.216298, 52.526852)));
		Node node0210 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0210"), transformation.transform(new Coord(13.244918, 52.524905)));
		Node node0211 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0211"), transformation.transform(new Coord(13.265018, 52.536656)));
		Node node0212 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0212"), transformation.transform(new Coord(13.282319, 52.533756)));
		Node node0213 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0213"), transformation.transform(new Coord(13.176774, 52.476220)));
		Node node0214 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0214"), transformation.transform(new Coord(13.182725, 52.483262)));
		Node node0215 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0215"), transformation.transform(new Coord(13.153592, 52.553309)));
		Node node0216 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0216"), transformation.transform(new Coord(13.165231, 52.551077)));
		Node node0217 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0217"), transformation.transform(new Coord(13.184757, 52.547458)));
		Node node0218 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0218"), transformation.transform(new Coord(13.193961, 52.545641)));
		Node node0219 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0219"), transformation.transform(new Coord(13.179158, 52.516608)));
		Node node0220 = NetworkUtils.createAndAddNode(network, Id.createNodeId("nn_0220"), transformation.transform(new Coord(13.184416, 52.515540)));


		/* Add the links */
		
		// "_" following id indicates the counter direction in case of a bi-directional path

		// Charottenburg-Wilmersdorf

		Link link001 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_001", node001, node002, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link002 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_002", node003, node004, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link003 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_003", node005, node006, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link003_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_003_", node006, node005, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link004 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_004", node007, node008, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);

		// Friedrichshain-Kreuzberg
		Link link005 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_005", node009, node010, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link005_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_005_", node010, node009, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link006 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_006", node011, node012, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link006_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_006_", node012, node011, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link007 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_007", node013, node014, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link007_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_007_", node014, node013, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link008 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_008", node015, node016, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link008_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_008_", node016, node015, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link009 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_009", node017, node018, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link010 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_010", node019, node020, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link011 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_011", node021, node022, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link012 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_012", node023, node024, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link012_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_012_", node024, node023, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link013 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_013", node025, node026, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link013_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_013_", node026, node025, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link014 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_014", node026, node027, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link014_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_014_", node027, node026, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link015 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_015", node028, node029, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link015_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_015_", node029, node028, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link016 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_016", node030, node031, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link016_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_016_", node031, node030, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link017 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_017", node031, node032, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link017_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_017_", node032, node031, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);

		// Lichtenberg

		Link link018 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_018", node033, node034, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link018_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_018_", node034, node033, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link019 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_019", node035, node036, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link019_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_019_", node036, node035, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);

		//Marzahn-Hellersdorf

		Link link020 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_020", node037, node038, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link020_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_020_", node038, node037, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link021 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_021", node039, node040, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link021_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_021_", node040, node039, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link022 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_022", node041, node042, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link022_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_022_", node042, node041, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link023 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_023", node043, node044, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link023_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_023_", node044, node043, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link024 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_024", node045, node046, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link024_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_024_", node046, node045, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);

		//Mitte

		Link link025 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_025", node047, node048, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link025_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_025_", node048, node047, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link026 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_026", node048, node049, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link026_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_026_", node049, node048, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link027 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_027", node050, node051, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link027_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_027_", node051, node050, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link028 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_028", node052, node053, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link028_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_028_", node053, node052, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link029 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_029", node054, node055, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link029_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_029_", node055, node054, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link030 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_030", node056, node057, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link030_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_030_", node057, node056, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link031 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_031", node058, node059, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link031_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_031_", node059, node058, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link032 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_032", node060, node061, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link033 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_033", node062, node063, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link033_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_033_", node063, node062, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link034 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_034", node064, node065, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link034_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_034_", node065, node064, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link035 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_035", node066, node067, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link035_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_035_", node067, node066, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link036 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_036", node068, node069, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link037 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_037", node070, node071, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link038 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_038", node071, node070, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link039 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_039", node072, node073, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link039_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_039_", node073, node072, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link040 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_040", node074, node075, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link040_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_040_", node075, node074, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link041 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_041", node076, node077, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link041_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_041_", node077, node076, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);

		// Neukölln

		Link link042 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_042", node078, node079, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link042_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_042_", node079, node078, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link043 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_043", node080, node081, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link043_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_043_", node081, node080, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link044 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_044", node082, node083, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link044_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_044_", node083, node082, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link045 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_045", node084, node085, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link045_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_045_", node085, node084, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);

		// Pankow
		
		Link link046 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_046", node086, node087, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link046_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_046_", node087, node086, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link047 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_047", node088, node089, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link047_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_047_", node089, node088, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link048 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_048", node090, node091, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link048_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_048_", node091, node090, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link049 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_049", node091, node092, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link049_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_049_", node092, node091, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link050 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_050", node093, node094, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link050_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_050_", node094, node093, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link051 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_051", node095, node096, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link051_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_051_", node096, node095, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link052 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_052", node097, node098, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link052_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_052_", node098, node097, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link053 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_053", node099, node0100, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link053_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_053_", node0100, node099, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link054 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_054", node0101, node0102, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link054_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_054_", node0102, node0101, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link055 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_055", node0103, node0104, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link055_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_055_", node0104, node0103, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link056 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_056", node0105, node0106, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link056_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_056_", node0106, node0105, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link057 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_057", node0107, node0108, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link057_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_057_", node0108, node0107, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link058 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_058", node0109, node0110, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link058_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_058_", node0110, node0109, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);

		//Steglitz-Zahlendorf

		Link link059 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_059", node0111, node0112, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link059_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_059_", node0112, node0111, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link060 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_060", node0112, node0113, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link060_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_060_", node0113, node0112, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link061 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_061", node0113, node0114, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link062 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_062", node0115, node0116, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link063 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_063", node0117, node0118, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link063_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_063_", node0118, node0117, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link064 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_064", node0118, node0119, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link064_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_064_", node0119, node0118, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link065 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_065", node0120, node0121, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link066 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_066", node0122, node0123, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link067 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_067", node0123, node0124, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link068 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_068", node0125, node0126, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link068_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_068_", node0126, node0125, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link069 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_069", node0127, node0128, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link069_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.PROTECTED_BICYCLE_LANE + "_069_", node0128, node0127, CombineNetworks.PBL_BICYCLE_SPEED, CombineNetworks.PROTECTED_BICYCLE_LANE);
		Link link070 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_070", node0129, node0130, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link070_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_070_", node0130, node0129, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link071 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_071", node0131, node0132, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link071_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_071_", node0132, node0131, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link072 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_072", node0133, node0134, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link072_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_072_", node0134, node0133, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link073 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_073", node0135, node0136, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link073_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_073_", node0136, node0135, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link074 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_074", node0137, node0138, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link074_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_074_", node0138, node0137, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link075 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_075", node0139, node0140, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link076 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_076", node0141, node0142, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link076_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_076_", node0142, node0141, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link077 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_077", node0143, node0144, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link077_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_077_", node0144, node0143, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link078 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_078", node0145, node0146, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link078_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_078_", node0146, node0145, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);

		// Tempelhof-Schöneberg

		Link link079 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_079", node0147, node0148, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link079_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_079_", node0148, node0147, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link080 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_080", node0148, node0149, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link080_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_080_", node0149, node0148, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link081 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_081", node0149, node0150, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link081_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_081_", node0150, node0149, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link082 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_082", node0151, node0152, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link082_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_082_", node0152, node0151, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link083 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_083", node0153, node0154, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link083_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_083_", node0154, node0153, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link084 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_084", node0155, node0156, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link084_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_084_", node0156, node0155, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link085 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_085", node0157, node0158, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link085_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_085_", node0158, node0157, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link086 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_086", node0159, node0160, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link086_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_086_", node0160, node0159, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link087 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_087", node0161, node0162, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link087_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_087_", node0162, node0161, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link088 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_088", node0163, node0164, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link088_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_088_", node0164, node0163, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);

		//Treptow-Köpenick

		Link link089 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_089", node0165, node0166, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link090 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_090", node0167, node0168, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link090_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_090_", node0168, node0167, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link091 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_091", node0168, node0169, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link091_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_091_", node0169, node0168, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link092 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_092", node0170, node0171, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link093 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_093", node0172, node0173, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link093_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_093_", node0173, node0172, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link094 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_094", node0174, node0175, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link094_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_094_", node0175, node0174, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link095 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_095", node0176, node0177, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link095_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_095_", node0177, node0176, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link096 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_096", node0178, node0179, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link096_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_096_", node0179, node0178, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link097 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_097", node0180, node0181, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link098 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_098", node0182, node0183, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link098_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_098_", node0183, node0182, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link099 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_099", node0184, node0185, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0100 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0100", node0186, node0187, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0100_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0100_", node0187, node0186, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0101 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0101", node0188, node0189, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0101_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0101_", node0189, node0188, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0102 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0102", node0188, node0190, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0102_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0102_", node0190, node0188, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0103 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0103", node0191, node0192, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0103_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0103_", node0192, node0191, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0104 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0104", node0193, node0194, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0104_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0104_", node0194, node0193, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0105 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0105", node0194, node0195, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0105_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0105_", node0195, node0194, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0106 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0106", node0196, node0197, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0106_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0106_", node0197, node0196, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0107 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0107", node0197, node0198, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0107_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0107_", node0198, node0199, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0108 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0108", node0199, node0200, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0108_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0108_", node0200, node0199, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0109 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0109", node0200, node0201, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0109_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0109_", node0201, node0200, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0110 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0110", node0202, node0203, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0111 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0111", node0204, node0205, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);

		// Spandau

		Link link0112 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0112", node0206, node0207, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0112_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0112_", node0207, node0206, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0113 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0113", node0207, node0208, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0113_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0113_", node0208, node0207, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0114 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0114", node0209, node0210, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0114_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0114_", node0210, node0209, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0115 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0115", node0211, node0212, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0115_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0115_", node0212, node0211, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0116 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0116", node0213, node0214, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0116_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0116_", node0214, node0213, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0117 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0117", node0215, node0216, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0117_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0117_", node0216, node0215, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0118 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0118", node0217, node0218, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0118_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0118_", node0218, node0217, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0119 = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0119", node0219, node0220, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
		Link link0119_ = CombineNetworks.createAndAddBicycleLink(network, CombineNetworks.NORMAL_PATH + "_0119_", node0220, node0219, CombineNetworks.NP_BICYCLE_SPEED, CombineNetworks.NORMAL_PATH);
	}
}