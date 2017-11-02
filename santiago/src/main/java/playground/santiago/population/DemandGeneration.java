package playground.santiago.population;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;


public class DemandGeneration {
	
	
	private final static Logger log = Logger.getLogger(DemandGeneration.class);
	
	private String runsWorkingDir;	
	private String svnWorkingDir;
	
	private String inConfig;
	private String inPlans;
	private String personas;
	
	private double percentage;
	
	private Population originalPopulation;
	private Map<String,Double> idsFactorsSantiago;
	private Map<String,Double> idsFactorsMatsim;
	private int totalPopulation;
	private double proportionalFactor;
	
	public DemandGeneration(String runsWorkingDir, String svnWorkingDir, double percentage){
		
		this.runsWorkingDir=runsWorkingDir;
		this.svnWorkingDir=svnWorkingDir;
		this.percentage=percentage;
		
		this.inConfig = this.svnWorkingDir + "inputForMATSim/config_final.xml";
		this.inPlans = this.svnWorkingDir + "inputForMATSim/plans/1_initial/workDaysOnly/plans_final.xml.gz";
		this.personas = this.svnWorkingDir + "inputFromElsewhere/exportedFilesFromDatabase/Normal/Persona.csv";
						
	}
	
	private void createDir(File file){
		log.info("Directory " + file + " created: "+ file.mkdirs());	
	}

	private void getIdsAndFactorsSantiago(){

		this.idsFactorsSantiago = new TreeMap<String,Double>();

			try {
					
				BufferedReader bufferedReader = IOUtils.getBufferedReader(personas);				
				String currentLine = bufferedReader.readLine();				
					while ((currentLine = bufferedReader.readLine()) != null) {
						String[] entries = currentLine.split(",");
						idsFactorsSantiago.put(entries[1], Double.parseDouble(entries[33]));
							
					}

				bufferedReader.close();
					
				} catch (IOException e) {
					
					log.error(new Exception(e));
				
				}


	}
	
	private void getTotalPopulationSantiago(){

		double population=0;

		for (Map.Entry<String, Double> entry : idsFactorsSantiago.entrySet()){
			population += entry.getValue();
		}

		this.totalPopulation = (int)Math.round(population);
		System.out.println("The total number of persons in Santiago is " + totalPopulation + ". Obs: this number differs from the total population stored in SantiagoScenarioConstants.java by 35 persons... ");

	}
	
	private void getIdsAndFactorsMatsimPop(){
	
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());		
		PopulationReader pr = new PopulationReader(scenario);
		pr.readFile(inPlans);
		this.originalPopulation = scenario.getPopulation();
		
		List<Person> persons = new ArrayList<>(originalPopulation.getPersons().values());		

		List<String> IdsMatsim = new ArrayList<>();
		

		for (Person p : persons){			
			IdsMatsim.add(p.getId().toString());	
		}
		
		this.idsFactorsMatsim = new TreeMap <String,Double>();
		
		for(String Ids : IdsMatsim ) {
			idsFactorsMatsim.put(Ids, idsFactorsSantiago.get(Ids));		
		}



	
	}

	private void getProportionalFactor(){
		

		double sumFactors = 0;
		
		for (Map.Entry<String,Double> entry : idsFactorsMatsim.entrySet()){		
			sumFactors += entry.getValue();
		}

		this.proportionalFactor = (percentage*totalPopulation)/sumFactors;

		}

	private void clonePersons(){
		

		List<Person> persons = new ArrayList<>(originalPopulation.getPersons().values());
	
		for (Person p : persons) {
			String keyId = p.getId().toString();
			int clonateFactor = (int)Math.round(proportionalFactor*idsFactorsMatsim.get(keyId));
			
			for(int cf = 1; cf < clonateFactor ; cf++) {
				Id<Person> pOutId = Id.createPersonId( p.getId().toString().concat("_").concat(String.valueOf(cf)) );
				Person pOut = originalPopulation.getFactory().createPerson( pOutId  );
				originalPopulation.addPerson(pOut);
				
				for (Plan plan : p.getPlans()){
					Plan planOut = originalPopulation.getFactory().createPlan();
					List<PlanElement> pes = plan.getPlanElements();
					for ( PlanElement pe : pes){
						if(pe instanceof Leg) {
							Leg leg = (Leg) pe;
							Leg legOut = originalPopulation.getFactory().createLeg(leg.getMode());
							planOut.addLeg(legOut);
						} else { 
							Activity actIn = (Activity)pe;
							Activity actOut = originalPopulation.getFactory().createActivityFromCoord(actIn.getType(), actIn.getCoord());
							planOut.addActivity(actOut);
							actOut.setEndTime(actIn.getEndTime());
							actOut.setStartTime(actIn.getStartTime());
					}
				}
					pOut.addPlan(planOut);
			}
		}
		}
		

		String outPlansDir = this.svnWorkingDir + "inputForMATSim/plans/expanded/";

		File outPlansDirFile = new File(outPlansDir);
		if(!outPlansDirFile.exists()) createDir(outPlansDirFile);
		
		String outPlans = "expanded_plans_0.xml.gz";		
		new PopulationWriter(originalPopulation).write(outPlansDir + outPlans);
		log.info("expanded_plans_0 has the entire population WITHOUT "
				+ "randomized activity end times and WITHOUT the classification of the activities");

		
	}
		
	private void writeNewConfigFile (){
		
		Config oldConfig = ConfigUtils.loadConfig(inConfig);
		
		QSimConfigGroup qsim = oldConfig.qsim();
		qsim.setFlowCapFactor(percentage);
		double storageCapFactor = Math.ceil(((percentage / (Math.pow(percentage, 0.25))))*100)/100;
		qsim.setStorageCapFactor(storageCapFactor);
	
		PlansConfigGroup plans = oldConfig.plans();
		plans.setInputFile(runsWorkingDir + "expanded_plans_0.xml.gz");
		plans.setInputPersonAttributeFile(runsWorkingDir + "expandedAgentAttributes.xml");

		CountsConfigGroup counts = oldConfig.counts();
		counts.setCountsScaleFactor(Math.pow(percentage,-1));
		
		String outConfigDir = this.svnWorkingDir + "inputForMATSim/";
		String outConfig = "expanded_config_0.xml";

		new ConfigWriter(oldConfig).write(outConfigDir + outConfig);
		
	}
				
	public void run (){

		getIdsAndFactorsSantiago();
		getTotalPopulationSantiago();
		getIdsAndFactorsMatsimPop();
		getProportionalFactor();
		clonePersons();
		writeNewConfigFile();
		
	}
	

}
