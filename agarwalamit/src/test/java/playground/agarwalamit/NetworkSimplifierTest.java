/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.agarwalamit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * Created by amit on 31.10.17.
 */

@RunWith(Parameterized.class)
public class NetworkSimplifierTest {

    public NetworkSimplifierTest(boolean letTestFail){
        this.letTestFail = letTestFail;
    }

    @Parameterized.Parameters
    public static Collection<Object> runCases() {
        return Arrays.asList(false, true);
    }

    private final boolean letTestFail ;

    @Test
    public void testSimplifying(){
        Network network = buildNetwork();

        assertEquals("Wrong number of links", 10, network.getLinks().size());

        NetworkSimplifier networkSimplifier = new NetworkSimplifier();
        networkSimplifier.setMergeLinkStats(false);
        networkSimplifier.run(network);

        network.getLinks().values().stream().forEach(l ->System.out.println(l.toString()));

        if (letTestFail) {
            assertEquals("Wrong number of links", 5, network.getLinks().size());
            assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("AB-BC-CD-DE-EF")));

            assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("CB")));
            assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("BA")));

            assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("FE-ED")));

            assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("AB-BC-CD-DC"))); // this is undesired link.
        } else {
            assertEquals("Wrong number of links", 4, network.getLinks().size());
            assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("AB-BC-CD-DE-EF")));

            assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("CB")));
            assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("BA")));

            assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("FE-ED-DC")));
        }
    }

    /**
     * Builds a test network like the following diagram.
     *
     * A<===>B<===>C<===>D<===>E<===>F
     *
     * Each link has one lane, 10 m, A to F is one direction and F to A is other.
     * The capacity of each link is same except CB.
     *
     * @return
     */
    private Network buildNetwork(){
        Network network = NetworkUtils.createNetwork();

        Node a,f,c,b,e,d;

        if (letTestFail) {
            a = NetworkUtils.createAndAddNode(network, Id.createNodeId("A"), CoordUtils.createCoord(0.0,  0.0));
            f = NetworkUtils.createAndAddNode(network, Id.createNodeId("F"), CoordUtils.createCoord(50.0,  0.0));
            c = NetworkUtils.createAndAddNode(network, Id.createNodeId("C"), CoordUtils.createCoord(20.0,  0.0));
            b = NetworkUtils.createAndAddNode(network, Id.createNodeId("B"), CoordUtils.createCoord(10.0,  0.0));
            e = NetworkUtils.createAndAddNode(network, Id.createNodeId("E"), CoordUtils.createCoord(40.0,  0.0));
            d = NetworkUtils.createAndAddNode(network, Id.createNodeId("D"), CoordUtils.createCoord(30.0,  0.0));
        } else {
            a = NetworkUtils.createAndAddNode(network, Id.createNodeId("A"), CoordUtils.createCoord(0.0,  0.0));
            b = NetworkUtils.createAndAddNode(network, Id.createNodeId("B"), CoordUtils.createCoord(10.0,  0.0));
            c = NetworkUtils.createAndAddNode(network, Id.createNodeId("C"), CoordUtils.createCoord(20.0,  0.0));
            d = NetworkUtils.createAndAddNode(network, Id.createNodeId("D"), CoordUtils.createCoord(30.0,  0.0));
            e = NetworkUtils.createAndAddNode(network, Id.createNodeId("E"), CoordUtils.createCoord(40.0,  0.0));
            f = NetworkUtils.createAndAddNode(network, Id.createNodeId("F"), CoordUtils.createCoord(50.0,  0.0));
        }

        NetworkUtils.createAndAddLink(network, Id.createLinkId("AB"), a, b, 10.0, 60.0/3.6, 1000.0, 1);
        NetworkUtils.createAndAddLink(network, Id.createLinkId("BC"), b, c, 10.0, 60.0/3.6, 1000.0, 1);
        NetworkUtils.createAndAddLink(network, Id.createLinkId("CD"), c, d, 10.0, 60.0/3.6, 1000.0, 1);
        NetworkUtils.createAndAddLink(network, Id.createLinkId("DE"), d, e, 10.0, 60.0/3.6, 1000.0, 1);
        NetworkUtils.createAndAddLink(network, Id.createLinkId("EF"), e, f, 10.0, 60.0/3.6, 1000.0, 1);

        NetworkUtils.createAndAddLink(network, Id.createLinkId("BA"), b, a, 10.0, 60.0/3.6, 1000.0, 1);
        NetworkUtils.createAndAddLink(network, Id.createLinkId("CB"), c, b, 10.0, 60.0/3.6, 2000.0, 1);
        NetworkUtils.createAndAddLink(network, Id.createLinkId("DC"), d, c, 10.0, 60.0/3.6, 1000.0, 1);
        NetworkUtils.createAndAddLink(network, Id.createLinkId("ED"), e, d, 10.0, 60.0/3.6, 1000.0, 1);
        NetworkUtils.createAndAddLink(network, Id.createLinkId("FE"), f, e, 10.0, 60.0/3.6, 1000.0, 1);

        return network;
    }

}
