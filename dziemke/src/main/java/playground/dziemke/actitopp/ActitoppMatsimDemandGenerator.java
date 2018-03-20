package playground.dziemke.actitopp;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import edu.kit.ifv.mobitopp.actitopp.ActitoppPerson;
import edu.kit.ifv.mobitopp.actitopp.HActivity;
import edu.kit.ifv.mobitopp.actitopp.HWeekPattern;
import edu.kit.ifv.mobitopp.actitopp.InvalidPatternException;
import edu.kit.ifv.mobitopp.actitopp.ModelFileBase;
import edu.kit.ifv.mobitopp.actitopp.RNGHelper;
import playground.vsp.demandde.cemdap.input.DemandGeneratorCensus;

/**
 * @author dziemke
 */
public class ActitoppMatsimDemandGenerator {
	private static final Logger LOG = Logger.getLogger(ActitoppMatsimDemandGenerator.class);
	
	private static ModelFileBase fileBase = new ModelFileBase();
	private static RNGHelper randomgenerator = new RNGHelper(1234);
	
//	public static void main(String[] args) {
//		// Input and output files		
//		String commuterFileOutgoing1 = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/pendlerstatistik_2009/Berlin_2009/B2009Ga.txt";
//		String commuterFileOutgoing2 = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/pendlerstatistik_2009/Brandenburg_2009/Teil1BR2009Ga.txt";
//		String commuterFileOutgoing3 = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/pendlerstatistik_2009/Brandenburg_2009/Teil2BR2009Ga.txt";
//		String commuterFileOutgoing4 = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/pendlerstatistik_2009/Brandenburg_2009/Teil3BR2009Ga.txt";
//		String[] commuterFilesOutgoing = {commuterFileOutgoing1, commuterFileOutgoing2, commuterFileOutgoing3, commuterFileOutgoing4};
//		String censusFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/zensus_2011/bevoelkerung/csv_Bevoelkerung/Zensus11_Datensatz_Bevoelkerung_BE_BB.csv";
//		
//		String outputBase = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_x/population_2/";
//		
//		// Parameters
//		int numberOfPlansPerPerson = 1; // temporarily, to speed it up
//		List<String> idsOfFederalStatesIncluded = Arrays.asList("12");
//		// Default ratios are used for cases where information is missing, which is the case for smaller municipalities.
//		double defaultAdultsToEmployeesRatio = 1.23;  // Calibrated based on sum value from Zensus 2011.
//		double defaultCensusEmployeesToCommutersRatio = 2.5;  // This is an assumption, oriented on observed values, deliberately chosen slightly too high.
//		// Choosing this too high effects that too many commuter relations are created, which is uncritical as relative shares will still be correct.
//		// Choosing this too low effects that employed people (according to the census) are left without workplace. Minimize this number!
//
//		DemandGeneratorCensus demandGeneratorCensus = new DemandGeneratorCensus(commuterFilesOutgoing, censusFile, outputBase, numberOfPlansPerPerson,
//				idsOfFederalStatesIncluded, defaultAdultsToEmployeesRatio, defaultCensusEmployeesToCommutersRatio);
//		
//		demandGeneratorCensus.setWriteMatsimPlanFiles(true); // default is false
//		demandGeneratorCensus.setWriteCemdapInputFiles(false); // default is true
//		
//		demandGeneratorCensus.setShapeFileForSpatialRefinement("../../shared-svn/studies/countries/de/open_berlin_scenario/input/shapefiles/2013/Bezirksregion_EPSG_25833.shp");
//		demandGeneratorCensus.setIdsOfMunicipalityForSpatialRefinement(Arrays.asList("11000000")); // "Amtliche Gemeindeschlüssel (AGS)" of Berlin is "11000000"
//		demandGeneratorCensus.setFeatureKeyInShapeFileForRefinement("SCHLUESSEL"); //e.g., PLZ/LOR
//
//		// municipality id (AGS)
//		String municipalityKeyInShapeFile = "NR";//not available in refinement shape for Berlin --> setting to null. Amit Nov'17
//		demandGeneratorCensus.setMunicipalityFeatureKeyInShapeFile(null);
//
//		demandGeneratorCensus.generateDemand();
//		
//		List<Population> populations = demandGeneratorCensus.getAllPopulations();
//		
//		for (Population population : populations) {
//			runActitopp(population);
//			writeMatsimPlansFile(population, outputBase + "/plan_with_activities.xml.gz"); // TODO currently only works with one population file
//		}
//	}
	
	public static void main(String[] args) {
		// Input and output files
		String outputBase = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_x/population_2/";
		String plansFile = outputBase + "plans_10000.xml.gz";
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader reader = new PopulationReader(scenario);
		reader.readFile(plansFile);
		
		runActitopp(scenario.getPopulation());
		writeMatsimPlansFile(scenario.getPopulation(), outputBase + "plan_10000_w_act.xml.gz"); // TODO currently only works with one population file
	}

	
	private static void runActitopp(Population population) {
		for (Person matsimPerson : population.getPersons().values()) {
			ActitoppPerson actitoppPerson = createActitoppPerson(matsimPerson);
			HWeekPattern weekPattern = createActitoppWeekPattern(actitoppPerson);
			
			PopulationFactory populationFactory = population.getFactory();
			matsimPerson.addPlan(createMatsimPlan(weekPattern, populationFactory));
		}
	}


	private static Plan createMatsimPlan(HWeekPattern weekPattern, PopulationFactory populationFactory) {
		Plan matsimPlan = populationFactory.createPlan();
		
		List<HActivity> activityList = weekPattern.getAllActivities();
		for (HActivity actitoppActivity : activityList) {
			if (actitoppActivity.getIndexDay() == 0) { // Only use activities of first day; until 1,440min
				
				actitoppActivity.getType(); // Letter-based type
				String matsimActivityType = transformActType(actitoppActivity.getType());
				Coord dummyCoord = CoordUtils.createCoord(0, 0); // TODO choose location
			
				Activity matsimActivity = populationFactory.createActivityFromCoord(matsimActivityType,
						dummyCoord);
				matsimPlan.addActivity(matsimActivity);
				
				int activityEndTime = actitoppActivity.getEndTime();
				if (activityEndTime <= 24 * 60) { // i.e. midnight in minutes
					matsimActivity.setEndTime(activityEndTime * 60); // times in ActiTopp in min, in MATSim in s
					
					Leg matsimLeg = populationFactory.createLeg(TransportMode.car); // TODO
					matsimPlan.addLeg(matsimLeg);
				}
			}
		}
		return matsimPlan;
	}


	private static HWeekPattern createActitoppWeekPattern(ActitoppPerson actitoppPerson) {
		boolean scheduleOK = false;
		while (!scheduleOK)	{
			try	 {
				// create weekly activity plan
				actitoppPerson.generateSchedule(fileBase, randomgenerator);
					
				scheduleOK = true;                
			} catch (InvalidPatternException e) {
				System.err.println(e.getReason());
				System.err.println("person involved: " + actitoppPerson.getPersIndex());
			}
		}
//		actitoppPerson.getWeekPattern().printAllActivitiesList();
		
		return actitoppPerson.getWeekPattern();
	}


	private static ActitoppPerson createActitoppPerson(Person matsimPerson) {
		// Attributes contained in the population are "age", "employed", "gender", "hasLicense", "householdId", "locationOfSchool",
		// "locationOfWork", "parent", and "student"
		
		// CEMDAPPersonAttributes: householdId, age, employed, gender, hasLicense, locationOfSchool, locationOfWork, parent, student
		int personIndex = Integer.parseInt(matsimPerson.getId().toString());
		int childrenFrom0To10 = getChildrenFrom0To10(); // TODO
		int childrenUnder18 = getChildrenUnder18(); // TODO
		int age = Integer.parseInt(matsimPerson.getAttributes().getAttribute("age").toString());
		int employment = getEmploymentClass(
				Boolean.parseBoolean(matsimPerson.getAttributes().getAttribute("employed").toString()),
				Boolean.parseBoolean(matsimPerson.getAttributes().getAttribute("employed").toString()));
		int gender = getGenderClass(Integer.parseInt(matsimPerson.getAttributes().getAttribute("gender").toString()));
		int areaType = getAreaType(); // TODO
		int numberOfCarsInHousehold = getNumberOfCarsInHousehold(); // TODO
		double commutingDistanceToWork = getDistanceEstimate(
				Integer.parseInt(matsimPerson.getAttributes().getAttribute("householdId").toString()),
				Integer.parseInt(matsimPerson.getAttributes().getAttribute("locationOfWork").toString()));
		double commutingDistanceToEducation = getDistanceEstimate(
				Integer.parseInt(matsimPerson.getAttributes().getAttribute("householdId").toString()),
				Integer.parseInt(matsimPerson.getAttributes().getAttribute("locationOfSchool").toString()));
		
		ActitoppPerson actitoppPerson = new ActitoppPerson(personIndex, childrenFrom0To10, childrenUnder18, age, employment, gender, areaType,
				numberOfCarsInHousehold, commutingDistanceToWork, commutingDistanceToEducation);
		return actitoppPerson;
	}
	
	
	// Information from "https://github.com/mobitopp/actitopp"
	// 1 = full-time occupied; 2 = half-time occupied; 3 = not occupied; 4 = student (school or university); 5 = worker in vocational program; 7 = retired person / pensioner
	private static int getEmploymentClass(boolean employment, boolean student) {
		int employmentClass = -1;
		if (employment) {
			employmentClass = 1; // TODO distinguish between full- and half-time occupation
		} else {
			employmentClass = 3;
		}
		if (student) {
			employmentClass = 4;
		}
		// TODO "worker in vocational program" and "retired person / pensioner" is not yet considered
		return employmentClass;
	}
	
	
	// Information from "https://github.com/mobitopp/actitopp"
	// 1 = male; 2 = female
	private static int getGenderClass(Integer cemdapGenderIdentifier) {
		// CEMDAP: 0 = Male, 1 = Female
		// ActiTopp: 1 = Male, 2 = Female
		int genderClass = -1;
		if (cemdapGenderIdentifier == 0) {
			genderClass = 1;
		} else if (cemdapGenderIdentifier == 1) {
			genderClass = 2;
		} else {
			throw new IllegalArgumentException("Gender (in CEMDAP classification, which is used here) can't have a value other than 0 or 1.");
		}
		return genderClass;
	}

	private static int getNumberOfCarsInHousehold() {
		return 0;
	}

	
	// Information from "https://github.com/mobitopp/actitopp"
	// 1 = rural; 2 = provincial; 3 = cityoutskirt; 4 = metropolitan; 5 = conurbation
	private static int getAreaType() {
		// TODO Right now everybody is "metropolitan"
		return 4;
	}

	
	private static int getChildrenUnder18() {
		// TODO Right now nobody has a child
		return 0;
	}

	
	private static int getChildrenFrom0To10() {
		// TODO Right now nobody has a child
		return 0;
	}
	
	// Information from "https://github.com/mobitopp/actitopp"
	// Commuting distance to work in kilometers (0 if non existing) or
	// commuting distance to school/university in kilometers (0 if non existing)
	private static double getDistanceEstimate(int householdId, int destinationZoneId) {
		// TODO Right now everybody makes trips of 5 kilometers
		return 5.;
	}
	
	
	// Information from "https://github.com/mobitopp/actitopp/blob/master/src/main/java/edu/kit/ifv/mobitopp/actitopp/Configuration.java"
	private static String transformActType(char activityTypeLetter) {
		if (activityTypeLetter == 'H') {
			return "home";
		} else if (activityTypeLetter == 'W') {
			return "work";
		} else if (activityTypeLetter == 'E') {
			return "eudation";
		} else if (activityTypeLetter == 'L') {
			return "leisure";
		} else if (activityTypeLetter == 'S') {
			return "shopping";
		} else if (activityTypeLetter == 'T') {
			return "other";
		} else {
			LOG.error(new IllegalArgumentException("Activity type " + activityTypeLetter + " not allowed."));
			return null;
		}
	}
	
	
	private static void writeMatsimPlansFile(Population population, String fileName) {
	    MatsimWriter popWriter = new PopulationWriter(population);
	    popWriter.write(fileName);
	}
}