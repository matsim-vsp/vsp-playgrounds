package playground.jmolloy.nodeModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

public abstract class AbstractNodeSimulation {
    protected final int timesteps;
    protected final String networkFile;
    protected Scenario scenario;
    protected Config config;
    protected String outputDirectory;
    private static AtomicInteger personCounter = new AtomicInteger(1);
    private TravelTimeCalculator travelTimeCalculator;
    private Map<Id<Link>, Integer> inflows = new HashMap<Id<Link>, Integer>();

    public AbstractNodeSimulation(String networkFile, String outputDirectory, int timesteps) {
        this.networkFile = networkFile;
        this.outputDirectory = outputDirectory;
        this.timesteps = timesteps;
        this.setup();
    }

    public void run() {
        Controler controler = new Controler(this.scenario);
        this.travelTimeCalculator = new TravelTimeCalculator(this.getNetwork(), 3600, 32400 + this.timesteps * 3600, new TravelTimeCalculatorConfigGroup());
        controler.getEvents().addHandler((EventHandler)this.travelTimeCalculator);
        controler.getConfig().controler().setOutputDirectory(this.outputDirectory);
        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        controler.run();
        this.printVolumes(this.scenario.getNetwork(), controler.getVolumes());
    }

    private void setup() {
        this.config = ConfigUtils.createConfig();
        this.config.controler().setLastIteration(0);
        this.config.qsim().setUseLanes(false);
        this.config.qsim().setStartTime(32400.0);
        this.config.qsim().setEndTime(54000.0);
        PlanCalcScoreConfigGroup.ActivityParams home = new PlanCalcScoreConfigGroup.ActivityParams("home");
        home.setTypicalDuration(57600.0);
        this.config.planCalcScore().addActivityParams(home);
        PlanCalcScoreConfigGroup.ActivityParams work = new PlanCalcScoreConfigGroup.ActivityParams("work");
        work.setTypicalDuration(28800.0);
        this.config.planCalcScore().addActivityParams(work);
        this.config.network().setInputFile(this.networkFile);
        this.scenario = ScenarioUtils.createScenario((Config)this.config);
        new MatsimNetworkReader(this.scenario.getNetwork()).readFile(this.config.network().getInputFile());
    }

    public void addFlows(Map<NetworkRoute, Integer> flows, boolean calculateRoutes) {
        flows.forEach((k, v) -> {
            this.addFlow(this.scenario.getPopulation(), k, v, this.timesteps, calculateRoutes);
        }
        );
    }

    private void addFlow(Population population, NetworkRoute route, int demand, int timesteps, boolean calculateRoute) {
        if (calculateRoute) {
            FreeSpeedTravelTime travelTime = new FreeSpeedTravelTime();
            OnlyTimeDependentTravelDisutility travelDisutility = new OnlyTimeDependentTravelDisutility((TravelTime)travelTime);
            Dijkstra leastCostPathCalculator = new Dijkstra(this.getNetwork(), (TravelDisutility)travelDisutility, (TravelTime)travelTime);
            Link inLink = (Link)this.getNetwork().getLinks().get(route.getStartLinkId());
            Link outLink = (Link)this.getNetwork().getLinks().get(route.getEndLinkId());
            LeastCostPathCalculator.Path links = leastCostPathCalculator.calcLeastCostPath(inLink.getToNode(), outLink.getFromNode(), 0.0, null, null);
            List link_ids = links.links.stream().map(Link::getId).collect(Collectors.toList());
            route.setLinkIds(route.getStartLinkId(), link_ids, route.getEndLinkId());
        }
        for (int i = 0; i < demand; ++i) {
            for (int t = 0; t < timesteps; ++t) {
                double gap = (double)i / (double)demand;
                this.createOnePerson(population, route, t, gap);
            }
            for (Id id : route.getLinkIds()) {
                this.inflows.putIfAbsent(id, 0);
                this.inflows.merge(id, 1, Math::addExact);
            }
        }
    }

    protected void createOnePerson(Population population, NetworkRoute r, int timestep, double gap) {
        String id = "p_" + personCounter.getAndIncrement() + "_" + timestep;
        Person person = population.getFactory().createPerson(Id.createPersonId((String)id));
        Plan plan = population.getFactory().createPlan();
        Activity home = population.getFactory().createActivityFromLinkId("home", r.getStartLinkId());
        home.setEndTime(((double)(timestep + 9) + gap) * 3600.0);
        plan.addActivity(home);
        Leg hinweg = population.getFactory().createLeg("car");
        if (r.getLinkIds().size() > 2) {
            hinweg.setRoute(r.clone());
        }
        plan.addLeg(hinweg);
        Activity work = population.getFactory().createActivityFromLinkId("work", r.getEndLinkId());
        work.setEndTime((double)((timestep + 17) * 60 * 60));
        plan.addActivity(work);
        person.addPlan(plan);
        population.addPerson(person);
    }

    protected void printVolumes(Network network, VolumesAnalyzer volumes) {
        System.out.println("  a     t:\t  q_a\t  s_a\tred_a\t  c_a\tred_a\ttt_q_a");
        for (Id linkid : volumes.getLinkIds()) {
            if (linkid.toString().startsWith("x")) continue;
            Link link = (Link)network.getLinks().get(Id.createLinkId(linkid));
            double[] link_volumes = volumes.getVolumesPerHourForLink(linkid);
            for (int i = 11; i < 12; ++i) {
                double tt_freeflow = link.getLength() / link.getFreespeed();
                double inflow = this.inflows.get(linkid);
                double capacity = link.getCapacity();
                double reduction_factor = link_volumes[i] / inflow;
                double cap_red_factor = link_volumes[i] / capacity;
                double tt_h = (this.travelTimeCalculator.getLinkTravelTime(link, (double)(i * 3600)) - tt_freeflow) / 3600.0;
                System.out.println(String.format("%3s (%2dh):\t%5.0f\t%5.0f\t%2.2f\t%5.0f\t%2.2f\t%3.2f", new Object[]{linkid, i, link_volumes[i], inflow, reduction_factor, capacity, cap_red_factor, tt_h}));
            }
            System.out.println();
        }
    }

    public Network getNetwork() {
        return this.scenario.getNetwork();
    }
}


 