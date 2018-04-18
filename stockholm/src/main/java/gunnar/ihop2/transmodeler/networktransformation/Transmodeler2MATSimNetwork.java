/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */ 
package gunnar.ihop2.transmodeler.networktransformation;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.lanes.data.*;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import saleem.stockholmmodel.utils.StockholmTransformationFactory;
import floetteroed.utilities.Units;

/**
 * Turns a mesoscopic Transmodeler network (defined through a set of files in
 * csv format) into a MATSim network (in xml format). Also creates MATSim lane
 * information that reflects the allowed turning moves in Transmodeler, and a
 * roadpricing input file.
 *
 * TODO: Detailed lane information in MATSim is missing (everything is still
 * single-lane); the only information taken over from Transmodeler are the
 * turning moves. Makes probably a difference if MATSim also does the network
 * loading.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class Transmodeler2MATSimNetwork {

	public static final String TMPATHID_ATTR = "TMPathID";

	public static final String TMFROMNODEID_ATTR = "TMFromNodeID";

	public static final String TMLINKDIRPREFIX_ATTR = "TMLinkDirPrefix";

	// -------------------- STATIC PACKAGE HELPERS --------------------

	static String unquote(final String original) {
		String result = original;
		if ((result.length() > 0) && "\"".equals(result.substring(0, 1))) {
			result = result.substring(1, result.length());
		}
		if ((result.length() > 0)
				&& "\"".equals(result.substring(result.length() - 1,
						result.length()))) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

	static enum DIR {
		AB, BA
	};

	static String newUnidirectionalId(final String bidirectionalId,
			final DIR dir) {
		return (bidirectionalId + "_" + dir);
	}

	static String newUnidirectionalLinkId(final String bidirectionalId,
			final DIR dir, final String abDir, final String baDir) {
		if (DIR.AB.equals(dir)) {
			return (bidirectionalId + "_" + abDir);
		} else if (DIR.BA.equals(dir)) {
			return (bidirectionalId + "_" + baDir);
		} else {
			throw new RuntimeException("unknown direction " + dir
					+ " for link " + bidirectionalId);
		}
	}

	// -------------------- MEMBERS --------------------

	private final String tmNodesFileName;

	private final String tmLinksFileName;

	private final String tmSegmentsFileName;

	private final String tmLanesFileName;

	private final String tmLaneConnectorsFileName;

	private final String matsimPlainNetworkFileName;

	private final String matsimFullFileName;

	private final String linkAttributesFileName;

	private final String matsimLanesFile;

	private final String matsimRoadPricingFileName;

	// -------------------- CONSTRUCTION --------------------

	private double totalNetworkArea(final Network network) {
		double result = 0;
		for (Link link : network.getLinks().values()) {
			result += link.getLength() * link.getNumberOfLanes();
		}
		return result;
	}

	public Transmodeler2MATSimNetwork(final String tmNodesFileName,
									  final String tmLinksFileName, final String tmSegmentsFileName,
									  final String tmLanesFileName,
									  final String tmLaneConnectorsFileName,
									  final String matsimPlainNetworkFileName,
									  final String matsimFullFileName,
									  final String linkAttributesFileName,
									  final String matsimLanesFile,
									  final String matsimRoadPricingFileName) {
		this.tmNodesFileName = tmNodesFileName;
		this.tmLinksFileName = tmLinksFileName;
		this.tmSegmentsFileName = tmSegmentsFileName;
		this.tmLanesFileName = tmLanesFileName;
		this.tmLaneConnectorsFileName = tmLaneConnectorsFileName;
		this.matsimPlainNetworkFileName = matsimPlainNetworkFileName;
		this.matsimFullFileName = matsimFullFileName;
		this.linkAttributesFileName = linkAttributesFileName;
		this.matsimLanesFile = matsimLanesFile;
		this.matsimRoadPricingFileName = matsimRoadPricingFileName;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void run() throws IOException {

		/*
		 * (1) Read all Transmodeler data.
		 */

		final TransmodelerNodesReader nodesReader = new TransmodelerNodesReader(
				this.tmNodesFileName);
		final TransmodelerLinksReader linksReader = new TransmodelerLinksReader(
				this.tmLinksFileName, nodesReader.id2node);
		final TransmodelerSegmentsReader segmentsReader = new TransmodelerSegmentsReader(
				this.tmSegmentsFileName, linksReader.id2link);
		final TransmodelerLaneReader lanesReader = new TransmodelerLaneReader(
				this.tmLanesFileName, segmentsReader.unidirSegmentId2link);
		final TransmodelerLaneConnectorReader connectorsReader = new TransmodelerLaneConnectorReader(
				this.tmLaneConnectorsFileName, lanesReader.upstrLaneId2link,
				lanesReader.downstrLaneId2link);

		System.out.println();
		System.out
				.println("------------------------------------------------------------");
		System.out.println("TRANSMODELER FILES SUMMARY");
		System.out.println("Loaded " + nodesReader.id2node.size() + " nodes.");
		System.out.println("Loaded " + linksReader.id2link.size()
				+ " links; the number of "
				+ (linksReader.ignoreCircularLinks ? "ignored" : "included")
				+ " circular links is " + linksReader.getCircularLinksCnt()
				+ ".");
		System.out.println("Loaded "
				+ segmentsReader.unidirSegmentId2link.size()
				+ " segments; ignored " + segmentsReader.getIgnoredSegmentCnt()
				+ " segments.");
		System.out
				.println("Loaded "
						+ (lanesReader.upstrLaneId2link.size() + lanesReader.downstrLaneId2link
								.size()) + " lanes; ignored "
						+ lanesReader.getIgnoredLaneCnt() + " lanes.");
		System.out.println("Loaded "
				+ (connectorsReader.getLoadedConnectionCnt())
				+ " lane connections; ignored "
				+ connectorsReader.getIgnoredConnectionCnt() + " connections.");
		System.out
				.println("------------------------------------------------------------");
		System.out.println();

		/*
		 * (2a) Create a MATSim network and additional object attributes.
		 */

		final Network matsimNetwork = NetworkUtils.createNetwork();
		final NetworkFactory matsimNetworkFactory = matsimNetwork.getFactory();
		final ObjectAttributes linkAttributes = new ObjectAttributes();

		/*
		 * (2b) Create and add all MATSim nodes.
		 */

		final CoordinateTransformation coordinateTransform = StockholmTransformationFactory
				.getCoordinateTransformation(
						StockholmTransformationFactory.WGS84,
						StockholmTransformationFactory.WGS84_SWEREF99);

		for (TransmodelerNode transmodelerNode : nodesReader.id2node.values()) {

			final Coord coord = coordinateTransform.transform(new Coord(
					1e-6 * transmodelerNode.getLongitude(),
					1e-6 * transmodelerNode.getLatitude()));

			final Node matsimNode = matsimNetworkFactory.createNode(
					Id.create(transmodelerNode.getId(), Node.class), coord);
			matsimNetwork.addNode(matsimNode);
		}

		/*
		 * (2c) Create and add all MATSim links.
		 */

		// final Set<String> unknownLinkTypes = new LinkedHashSet<String>();
		// final Set<String> linksWithUnknownTypes = new
		// LinkedHashSet<String>();

		for (TransmodelerLink transmodelerLink : linksReader.id2link.values()) {

			final Node matsimFromNode = matsimNetwork.getNodes().get(
					Id.create(transmodelerLink.getFromNode().getId(),
							Node.class));
			final Node matsimToNode = matsimNetwork.getNodes()
					.get(Id.create(transmodelerLink.getToNode().getId(),
							Node.class));

			final Link matsimLink = matsimNetworkFactory.createLink(
					Id.create(transmodelerLink.getId(), Link.class),
					matsimFromNode, matsimToNode);

			LinkTypeParameters parameters = LinkTypeParameters.TYPE2PARAMS
					.get(transmodelerLink.getType());
			if (parameters == null) {
				parameters = LinkTypeParameters.TYPE2PARAMS
						.get(LinkTypeParameters.UNDEFINED);
			}

			// if (parameters != null) {
			final SortedSet<TransmodelerSegment> segments = transmodelerLink.segments;
			double lanes = 0.0;
			double length = 0.0;
			for (TransmodelerSegment segment : segments) {
				lanes += segment.getLanes() * segment.getLength();
				length += segment.getLength();
			}
			lanes /= length;
			matsimLink.setNumberOfLanes(lanes);
			matsimLink.setLength(length * Units.M_PER_KM);
			matsimLink.setCapacity(parameters.flowCapacity_veh_hLane * lanes);
			matsimLink.setFreespeed(parameters.maxSpeed_km_h
					* Units.M_S_PER_KM_H);
			matsimNetwork.addLink(matsimLink);
			// } else {
			// unknownLinkTypes.add(transmodelerLink.getType());
			// linksWithUnknownTypes.add(transmodelerLink.getId());
			// }

			linkAttributes.putAttribute(matsimLink.getId().toString(),
					TMPATHID_ATTR, transmodelerLink.getBidirectionalId());
			linkAttributes.putAttribute(matsimLink.getId().toString(),
					TMFROMNODEID_ATTR, transmodelerLink.getFromNode().getId());
			linkAttributes.putAttribute(matsimLink.getId().toString(),
					TMLINKDIRPREFIX_ATTR,
					DIR.AB.equals(transmodelerLink.getDirection()) ? "" : "-");
		}

		NetworkWriter networkWriter = new NetworkWriter(matsimNetwork);
		networkWriter.write(this.matsimFullFileName);

		System.out.println();
		System.out
				.println("------------------------------------------------------------");
		System.out.println("RAW MATSIM NETWORK STATISTICS");
		System.out.println("(This network is saved as "
				+ this.matsimFullFileName + ".)");
		System.out.println("Number of nodes: "
				+ matsimNetwork.getNodes().size());
		System.out.println("Number of links: "
				+ matsimNetwork.getLinks().size());
		// System.out.println("Unknown (and ignored) link types: "
		// + unknownLinkTypes);
		// System.out.println("Ignored links with unknown types: "
		// + linksWithUnknownTypes);
		System.out.println("Total network area (link lengths times lanes): "
				+ totalNetworkArea(matsimNetwork));
		System.out
				.println("------------------------------------------------------------");
		System.out.println();

		/*
		 * (2d) Clean up the network and save it to file.
		 */

		NetworkCleaner cleaner = new NetworkCleaner();
		cleaner.run(matsimNetwork);

		System.out.println();
		System.out
				.println("------------------------------------------------------------");
		System.out.println("MATSIM NETWORK STATISTICS AFTER NETWORK CLEANING");
		System.out.println("(This network is not saved to file.)");
		System.out.println("Number of nodes: "
				+ matsimNetwork.getNodes().size());
		System.out.println("Number of links: "
				+ matsimNetwork.getLinks().size());
		System.out.println("Total network area (link lengths times lanes): "
				+ totalNetworkArea(matsimNetwork));
		System.out
				.println("------------------------------------------------------------");
		System.out.println();

		/*
		 * (2e) Identify the largest connected component given the turning
		 * moves.
		 */

		final Set<Link> removedLinks = new LinkedHashSet<Link>(matsimNetwork
				.getLinks().values());
		removedLinks.removeAll(ConnectedLinks.connectedLinks(matsimNetwork,
				linksReader.id2link));
		for (Link removedLink : removedLinks) {
			matsimNetwork.removeLink(removedLink.getId());
		}

		/*
		 * (2f) Clean the network once again and save it to file.
		 */

		cleaner = new NetworkCleaner();
		cleaner.run(matsimNetwork);

		networkWriter = new NetworkWriter(matsimNetwork);
		networkWriter.write(this.matsimPlainNetworkFileName);

		final ObjectAttributesXmlWriter linkAttributesWriter = new ObjectAttributesXmlWriter(
				linkAttributes);
		linkAttributesWriter.writeFile(this.linkAttributesFileName);

		System.out.println();
		System.out
				.println("------------------------------------------------------------");
		System.out
				.println("MATSIM NETWORK STATISTICS AFTER DEADEND REMOVAL AND REPEATED CLEANING");
		System.out.println("(This network is saved as "
				+ this.matsimPlainNetworkFileName + ".)");
		System.out.println("Number of nodes: "
				+ matsimNetwork.getNodes().size());
		System.out.println("Number of links: "
				+ matsimNetwork.getLinks().size());
		System.out.println("Total network area (link lengths times lanes): "
				+ totalNetworkArea(matsimNetwork));
		System.out
				.println("------------------------------------------------------------");
		System.out.println();

		/*
		 * (2d) Write out lanes.
		 */
		final Lanes lanedefs = LanesUtils.createLanesContainer();
		LanesFactory lanesFactory = lanedefs.getFactory();

		for (Node node : matsimNetwork.getNodes().values()) {

			for (Link matsimInLink : node.getInLinks().values()) {
				final TransmodelerLink tmInLink = linksReader.id2link
						.get(matsimInLink.getId().toString());

				final Lane lane = lanesFactory.createLane(Id.create(
						matsimInLink.getId().toString() + "-singleLane",
						Lane.class));
				lane.setNumberOfRepresentedLanes(matsimInLink
						.getNumberOfLanes());
				lane.setStartsAtMeterFromLinkEnd(matsimInLink.getLength() / 2.0);
				for (TransmodelerLink tmOutLink : tmInLink.downstreamLink2turnLength
						.keySet()) {
					final Id<Link> outLinkId = Id.create(tmOutLink.getId(),
							Link.class);
					if (node.getOutLinks().containsKey(outLinkId)) {
						lane.addToLinkId(outLinkId);
					}
				}

				if ((lane.getToLinkIds() != null)
						&& (!lane.getToLinkIds().isEmpty())) {
					final LanesToLinkAssignment lanesToLink = lanesFactory.createLanesToLinkAssignment(matsimInLink.getId());
					lanesToLink.addLane(lane);
					lanedefs.addLanesToLinkAssignment(lanesToLink);
				} else {
					throw new RuntimeException(
							"impossible state after preprocessing ...");
				}
			}
		}
		LanesUtils.createOriginalLanesAndSetLaneCapacities(matsimNetwork, lanedefs);

		final LanesWriter laneWriter = new LanesWriter(lanedefs);
		laneWriter.write(this.matsimLanesFile);

		/*
		 * Write out road pricing file.
		 * 
		 * TODO There exists a writer for this.
		 */
		final PrintWriter tollWriter = new PrintWriter(
				this.matsimRoadPricingFileName);

		tollWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		tollWriter.println("<!DOCTYPE roadpricing SYSTEM "
				+ "\"http://www.matsim.org/files/dtd/roadpricing_v1.dtd\">");

		tollWriter.println("<roadpricing type=\"link\" name=\"abc\">");

		tollWriter.println("\t<links>");
		for (Link link : matsimNetwork.getLinks().values()) {
			if (linksReader.id2tollLink.containsKey(link.getId().toString())) {
				tollWriter.println("\t\t<link id=\"" + link.getId() + "\">");
				tollWriter.println("\t\t\t<cost start_time=\"06:30\" "
						+ "end_time=\"07:00\" " + "amount=\"10.00\"/>");
				tollWriter.println("\t\t\t<cost start_time=\"07:00\" "
						+ "end_time=\"07:30\" " + "amount=\"15.00\"/>");
				tollWriter.println("\t\t\t<cost start_time=\"07:30\" "
						+ "end_time=\"08:30\" " + "amount=\"20.00\"/>");
				tollWriter.println("\t\t\t<cost start_time=\"08:30\" "
						+ "end_time=\"09:00\" " + "amount=\"15.00\"/>");
				tollWriter.println("\t\t\t<cost start_time=\"09:00\" "
						+ "end_time=\"15:30\" " + "amount=\"10.00\"/>");
				tollWriter.println("\t\t\t<cost start_time=\"15:30\" "
						+ "end_time=\"16:00\" " + "amount=\"15.00\"/>");
				tollWriter.println("\t\t\t<cost start_time=\"16:00\" "
						+ "end_time=\"17:30\" " + "amount=\"20.00\"/>");
				tollWriter.println("\t\t\t<cost start_time=\"17:30\" "
						+ "end_time=\"18:00\" " + "amount=\"15.00\"/>");
				tollWriter.println("\t\t\t<cost start_time=\"18:00\" "
						+ "end_time=\"18:30\" " + "amount=\"10.00\"/>");
				tollWriter.println("\t\t</link>");
			}
		}
		tollWriter.println("\t</links>");

		tollWriter.println("</roadpricing>");

		tollWriter.flush();
		tollWriter.close();
	}

	// -------------------- MAIN-FUNCTION --------------------

	public static void main(String[] args) throws IOException {

		final String inputPath = "./ihop2/network-input/";
		final String nodesFile = inputPath + "Nodes.csv";
		final String segmentsFile = inputPath + "Segments.csv";
		final String lanesFile = inputPath + "Lanes.csv";
		final String laneConnectorsFile = inputPath + "Lane Connectors.csv";
		final String linksFile = inputPath + "Links.csv";

		final String outputPath = "./ihop2/network-output/";
		final String matsimPlainFile = outputPath + "network.xml";
		final String matsimFullFile = outputPath + "network-raw.xml";
		final String linkAttributesFile = outputPath + "link-attributes.xml";
		final String matsimLanesFile = outputPath + "lanes.xml";
		final String matsimRoadPricingFile = outputPath + "toll.xml";

		System.out.println("STARTED ...");

		final Transmodeler2MATSimNetwork tm2MATSim = new Transmodeler2MATSimNetwork(
				nodesFile, linksFile, segmentsFile, lanesFile,
				laneConnectorsFile, matsimPlainFile, matsimFullFile,
				linkAttributesFile, matsimLanesFile,
				matsimRoadPricingFile);
		tm2MATSim.run();

		System.out.println("... DONE");
	}
}
