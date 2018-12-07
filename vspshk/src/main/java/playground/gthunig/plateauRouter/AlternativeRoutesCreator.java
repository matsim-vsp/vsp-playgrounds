package playground.gthunig.plateauRouter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioUtils;
import playground.vsp.openberlinscenario.planmodification.PlanFileModifier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

public class AlternativeRoutesCreator {
    private final static Logger LOG = Logger.getLogger(AlternativeRoutesCreator.class);

    public static void main(String[] args) {

        String networkFile = "C:\\Users\\gthunig\\VSP\\matsim\\matsim\\matsim-berlin\\scenarios\\berlin-v5.2-10pct\\input\\berlin-v5.0.network.xml.gz";
        String plansFile = "C:\\Users\\gthunig\\VSP\\matsim\\matsim\\matsim-berlin\\scenarios\\berlin-v5.2-1pct\\input\\berlin-v5.2-1pct.plans.xml.gz";
        String outputPlansFile = "C:\\Users\\gthunig\\VSP\\matsim\\matsim\\matsim-berlin\\scenarios\\berlin-v5.2-1pct\\input\\berlin-v5.2-1pct.plans_withAlternativeCarRoutes.xml.gz";
        Config config = ConfigUtils.createConfig();
        config.global().setCoordinateSystem("GK4");
        config.network().setInputFile(networkFile);
        config.plans().setInputFile(plansFile);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        Population population = scenario.getPopulation();

        AlternativeRoutesCreator alternativeRoutesCreator = new AlternativeRoutesCreator(network, config);
        alternativeRoutesCreator.createAlternativeRoutes(population);

        PopulationUtils.writePopulation(population, outputPlansFile);

        System.out.println(network.getLinks().size());
    }

    private Network network;
    private PlateauRouter plateauRouter;

    private int resultSetSize = 3;
    private int maxChoiceSetSize = 25;

    public AlternativeRoutesCreator(Network network, Config config) {

        FreespeedTravelTimeAndDisutility travelTimeAndDisutility = new FreespeedTravelTimeAndDisutility(config.planCalcScore());
        this.network = network;
        plateauRouter = new PlateauRouter(network, travelTimeAndDisutility, travelTimeAndDisutility);
        LOG.info("Initialized");
    }

    /**
     * alternative routes for selected plan and car routes
     * @param population
     */
    public void createAlternativeRoutes(Population population) {
        LOG.info("Start creating and adding alternative Routes");
        LOG.info("# of Persons: " + population.getPersons().size());

        double maxBeelineDistance = 0;
        int counter = 0;
        int nextMsg = 1;
        for (Person person : population.getPersons().values()) {
            // show counter
            counter++;
            if (counter % nextMsg == 0) {
                nextMsg *= 4;
                printPlansCount(counter);
            }
            List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
            Plan[] plans = new Plan[resultSetSize];
            for (int i = 0; i < resultSetSize; i++) {
                plans[i] = person.getSelectedPlan();
            }
            for (int i = 0; i < planElements.size(); i++) {
                PlanElement currentPlanElement = planElements.get(i);
                if (currentPlanElement instanceof Leg
                        && ((Leg) currentPlanElement).getMode().equals(TransportMode.car)) {

                    Activity previousActivity = (Activity) planElements.get(i-1);
                    Activity followingActivity = (Activity) planElements.get(i+1);
                    Node fromNode = NetworkUtils.getNearestNode(network, previousActivity.getCoord());
                    Node toNode = NetworkUtils.getNearestNode(network, followingActivity.getCoord());
                    double currentBeelineDistance = NetworkUtils.getEuclideanDistance(fromNode.getCoord(), toNode.getCoord());
                    int currentChoiceSetSize = maxChoiceSetSize / 2;
                    if (currentBeelineDistance != 0) {
                        if (maxBeelineDistance < currentBeelineDistance) {
                            maxBeelineDistance = currentBeelineDistance;
                        }
                        currentChoiceSetSize = (int) Math.round(maxChoiceSetSize * (maxBeelineDistance / currentBeelineDistance));
                    }
                    if (currentChoiceSetSize < resultSetSize) {
                        currentChoiceSetSize = resultSetSize;
                    }
                    TreeSet<Path> alternativePaths = plateauRouter.calculateBestPlateauPaths(fromNode, toNode, previousActivity.getEndTime(), currentChoiceSetSize, resultSetSize);

                    Iterator<Path> pathsIterator = alternativePaths.iterator();
                    int e = 0;
                    while(pathsIterator.hasNext()) {

                        Path currentPath = pathsIterator.next();
                        NetworkRoute route = RouteUtils.createNetworkRoute(extractLinkIds(currentPath.links), network);
                        Leg currentPlanLeg = (Leg) plans[e].getPlanElements().get(i);
                        currentPlanLeg.setRoute(route);
                        currentPlanLeg.setTravelTime(currentPath.travelTime);

                        e++;
                    }

                }
            }
            //remove every old plan
            for (Plan plan : person.getPlans()) {
                person.removePlan(plan);
            }
            //add the newly calculated plans to the person
            for (Plan plan : plans) {
                person.addPlan(plan);
            }
            person.setSelectedPlan(plans[0]);
        }
        LOG.info("Finished creating and adding alternative Routes");
    }

    private void printPlansCount(int counter) {
        LOG.info(" person # " + counter);
    }

    private List<Id<Link>> extractLinkIds(List<Link> links) {
        List<Id<Link>> linkIds = new ArrayList<>();
        for (Link link : links) {
            linkIds.add(link.getId());
        }
        return linkIds;
    }

    public void setResultSetSize(int resultSetSize) {
        this.resultSetSize = resultSetSize;
    }

    public void setMaxChoiceSetSize(int maxChoiceSetSize) {
        this.maxChoiceSetSize = maxChoiceSetSize;
    }
}
