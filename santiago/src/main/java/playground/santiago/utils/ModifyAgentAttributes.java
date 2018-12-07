package playground.santiago.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;




public class ModifyAgentAttributes {
	
	private final static Logger log = Logger.getLogger(ModifyAgentAttributes.class);

//	
//	final static String expandedPlansFolder = svnWorkingDir + "plans/2_10pct/";
//	final static String expandedPlansFile = expandedPlansFolder + "randomized_expanded_plans.xml.gz";
//
//	final static String sampledPlansFolder = svnWorkingDir + "plans/3_1pct/";
//	final static String sampledPlansFile = sampledPlansFolder + "randomized_sampled_plans.xml.gz";
//	
//
//	final static String expandedAgentAttributes = expandedPlansFolder + "expandedAgentAttributes.xml";
//	final static String sampledAgentAttributes = sampledPlansFolder + "sampledAgentAttributes.xml";
//	final static String agentsWithCar  = svnWorkingDir + "plans/1_initial/workDaysOnly/agentsWithCar.txt";
	
	private String svnWorkingDir;
	private String inPlans;
	private String agentsWithCar;
	
	public ModifyAgentAttributes(String svnWorkingDir){
		this.svnWorkingDir=svnWorkingDir;
		
		this.inPlans = this.svnWorkingDir + "inputForMATSim/plans/expanded/randomized_expanded_plans.xml.gz";		
		this.agentsWithCar = this.svnWorkingDir + "inputForMATSim/plans/1_initial/workDaysOnly/agentsWithCar.txt";
		
	}
	
	private void createDir(File file){
		log.info("Directory " + file + " created: "+ file.mkdirs());	
	}	

	private void writeAttributes(Population population, LinkedList<String> carUsers){
			
		LinkedList<Person> persons = new LinkedList<>(population.getPersons().values());		
		LinkedList<String> clonedIds = new LinkedList<>(); //example: 10508202_1
		LinkedList<String> originalIds = new LinkedList<>(); //example: 10508202
		
		for (Person p : persons) {
			clonedIds.add(p.getId().toString());			
			String [] keyId = p.getId().toString().split("_");
			originalIds.add( keyId[0] );
			
		}
		
		Collections.sort(originalIds);
		Collections.sort(clonedIds);
		
		
		
		//not necessary
		Collections.sort(carUsers);
		
		ObjectAttributes oa = new ObjectAttributes();
		
		for(String id : carUsers){
			
				int start = originalIds.indexOf(id);
				int end = originalIds.lastIndexOf(id);
	
				if (start!=-1){
					for (int i = start; i<=end; i++){					
					oa.putAttribute(clonedIds.get(i), "carUsers", "carAvail");					
					}
				}

			}
		
		String agentAttributesDir = this.svnWorkingDir + "inputForMATSim/";
		File agentAttributesDirFile = new File(agentAttributesDir);
		if(!agentAttributesDirFile.exists()) createDir(agentAttributesDirFile);		
		String agentAttributes = "expandedAgentAttributes.xml";
		
		new ObjectAttributesXmlWriter(oa).writeFile(agentAttributesDir + agentAttributes);
		
		
	}
		
	public void run(){
		LinkedList <String> carUsers = new LinkedList<>(); 

		try {			
			BufferedReader br = IOUtils.getBufferedReader(agentsWithCar);
			String line = br.readLine();
			while ((line = br.readLine()) != null) {				
				carUsers.add(line);	//example: 10508202			
			}						
			br.close();

		} catch (IOException e) {
			log.error(new Exception(e));

		}

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(inPlans);
		Population population = scenario.getPopulation();
		writeAttributes(population, carUsers);

	}	
		
	}

