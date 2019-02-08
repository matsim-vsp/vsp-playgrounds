package playground.vsp.cadyts.marginals;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import playground.vsp.cadyts.marginals.prep.DistanceBin;
import playground.vsp.cadyts.marginals.prep.DistanceDistribution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EventsToBeelineDistanceRangeTest {

    private static final Id<Person> DEFAULT_PERSON_ID = Id.create(123, Person.class);
    private static final Id<Link> DEFAULT_LINK_ID = Id.createLinkId(1);

    @Test
    public void simpleTrip() {

        MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

        Network network = NetworkUtils.createNetwork();
        Node from = network.getFactory().createNode(Id.createNodeId("from-node"), new Coord(1000, 0));
        Node to = network.getFactory().createNode(Id.createNodeId("to-node"), new Coord(2000, 0));
        Link link = network.getFactory().createLink(DEFAULT_LINK_ID, from, to);
        link.setLength(1000);
        link.setFreespeed(12.333);
        link.setCapacity(2000);
        link.setNumberOfLanes(1);
        network.addNode(from);
        network.addNode(to);
        network.addLink(link);
        scenario.setNetwork(network);

        ActivityFacility home = scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create("home", ActivityFacility.class), new Coord(0, 0));
        ActivityFacility work = scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create("work", ActivityFacility.class), new Coord(3000, 0));

        scenario.getActivityFacilities().addActivityFacility(home);
        scenario.getActivityFacilities().addActivityFacility(work);

        Person person = PopulationUtils.getFactory().createPerson(DEFAULT_PERSON_ID);
        scenario.getPopulation().addPerson(person);
        Plan plan = PopulationUtils.createPlan(person);
        Activity homeActivity = scenario.getPopulation().getFactory().createActivityFromCoord("home", new Coord(0, 0));
        homeActivity.setFacilityId(home.getId());
        plan.addActivity(homeActivity);
        Activity workActivity = scenario.getPopulation().getFactory().createActivityFromCoord("work", new Coord(3000, 0));
        workActivity.setFacilityId(work.getId());
        plan.addActivity(workActivity);
        plan.setScore(12.0);

        EventsToBeelinDistanceRange collector = new EventsToBeelinDistanceRange(scenario, createDistanceDistribution());

        Leg leg = PopulationUtils.createLeg(TransportMode.car);
        leg.setDepartureTime(Time.parseTime("07:10:00"));
        leg.setTravelTime(Time.parseTime("07:30:00") - leg.getDepartureTime());
        collector.handleEvent(new ActivityEndEvent(leg.getDepartureTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, home.getId(), "home"));
        collector.handleEvent(new PersonDepartureEvent(leg.getDepartureTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));
        collector.handleEvent(new ActivityStartEvent(leg.getDepartureTime() + leg.getTravelTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, work.getId(), "work"));

        assertNotNull(collector.getPersonToMode().get(DEFAULT_PERSON_ID));
        assertEquals(leg.getMode(), collector.getPersonToMode().get(DEFAULT_PERSON_ID));
    }

    private DistanceDistribution createDistanceDistribution() {

        DistanceDistribution inputDistanceDistribution = new DistanceDistribution();
        inputDistanceDistribution.setBeelineDistanceFactorForNetworkModes(TransportMode.car, 1.0);
        inputDistanceDistribution.addToDistribution(TransportMode.car, new DistanceBin.DistanceRange(0, 10000), 1234);
        return inputDistanceDistribution;
    }
}
