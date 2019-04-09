package playground.santiago.landuse;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
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
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.ActivityFacility;
import org.opengis.feature.simple.SimpleFeature;

import com.google.common.collect.Multimap;


public class RandomizeCoordinates {
	
	private final static Logger log = Logger.getLogger(RandomizeCoordinates.class);

	private String runsWorkingDir;
	private String svnWorkingDir;
	private String landUseDir;
	
	private String inConfig;
	private String inPlans;
	
	private Collection<SimpleFeature> features;
	
	public RandomizeCoordinates(String runsWorkingDir,String svnWorkingDir, String landUseDir){
		
		this.runsWorkingDir = runsWorkingDir;
		this.svnWorkingDir = svnWorkingDir;
		this.landUseDir = landUseDir;
		
		this.inConfig = this.svnWorkingDir + "inputForMATSim/expanded_config_1.xml";
		this.inPlans = this.svnWorkingDir + "inputForMATSim/plans/expanded/expanded_plans_1.xml.gz";
		
		
		ShapeFileReader reader = new ShapeFileReader();		
		this.features = reader.readFileAndInitialize(this.landUseDir + "3_ShapeZonasEOD/zonificacion_eod2012.shp");
		
	}
	
	private void createDir(File file){
		log.info("Directory " + file + " created: "+ file.mkdirs());	
	}
	
	public void run(){
		
		Config config = ConfigUtils.loadConfig(this.inConfig);
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());		
		PopulationReader pr = new PopulationReader(scenario);		
		pr.readFile(this.inPlans);		
		Population expandedPlans = scenario.getPopulation();		
		Map <Id,Integer> AgentCondition = getAgentCondition(expandedPlans);
		Population newPlans = createNewPlans(AgentCondition, expandedPlans, features);
		writeNewPopulation (newPlans);
		writeNewConfig (config);

		
	}

	private long getEODZone (Coord coord){
		
		Point point = MGC.xy2Point(coord.getX(), coord.getY());		
		Map<Long,Geometry>geometriesById = new HashMap<>();
		
		for (SimpleFeature feature : features) {
			
			geometriesById.put((Long) feature.getAttribute("ID"),(Geometry) feature.getDefaultGeometry());
			
		}
		
		long zone=1;
		for (long id : geometriesById.keySet()){
			if(geometriesById.get(id).contains(point)){
				
				zone = id ;
				break;

			}
		}
		
		return zone;
		
	}	
		
	private Coord selectRandomFacility (Multimap <Long,ActivityFacility> activityByTAZ, Long TAZId){
 
	Collection<ActivityFacility> setToSearch = activityByTAZ.get(TAZId);

	int size = setToSearch.size();
	
	Coord coord=new Coord(0,0);

	if (size!=0){
		
		int facilityNumber = new Random().nextInt(size);		
		int facilityIndex = 0;
		
		for(ActivityFacility facility : setToSearch){	

			if (facilityIndex == facilityNumber){
				coord = facility.getCoord();
				break;
			}
				
			++facilityIndex;	


		}

	}	
	return coord;

 }

	private Map <Id,Integer> getAgentCondition(Population originalPlans){
		
		Map <Id,Integer> agentCondition = new HashMap<Id,Integer>();
		
		for (Person p : originalPlans.getPersons().values()) {
			
			String tempIds = p.getId().toString();
			
			int indicator = 0;
			
			if(tempIds.contains("_")){
				indicator = 1;				
				
			}			
			
			agentCondition.put(p.getId(), indicator);
			
		}	

		
		return agentCondition;

		}
		
	private Population createNewPlans(Map<Id, Integer> agentCondition, Population originalPlans, Collection<SimpleFeature> features){
		
		
		FacilitiesByZone fbzHome = new FacilitiesByZone(features, landUseDir);
		Multimap <Long,ActivityFacility> homeByTAZ = fbzHome.build("home");		
		
		FacilitiesByZone fbzWork = new FacilitiesByZone(features, landUseDir);
		Multimap <Long,ActivityFacility> workByTAZ = fbzWork.build("work");
		
		FacilitiesByZone fbzBusiest = new FacilitiesByZone(features, landUseDir);
		Multimap <Long,ActivityFacility> busiestByTAZ = fbzBusiest.build("busiest");
		
		FacilitiesByZone fbzEducation = new FacilitiesByZone(features, landUseDir);
		Multimap <Long,ActivityFacility> educationByTAZ = fbzEducation.build("education");
		
		FacilitiesByZone fbzHealth = new FacilitiesByZone(features, landUseDir);
		Multimap <Long,ActivityFacility> healthByTAZ = fbzHealth.build("health");
		
		FacilitiesByZone fbzVisit = new FacilitiesByZone(features, landUseDir);
		Multimap <Long,ActivityFacility> visitByTAZ = fbzVisit.build("visit");
		
		FacilitiesByZone fbzShop = new FacilitiesByZone(features, landUseDir);
		Multimap <Long,ActivityFacility> shopByTAZ = fbzShop.build("shopping");
		
		FacilitiesByZone fbzLeisure = new FacilitiesByZone(features, landUseDir);
		Multimap <Long,ActivityFacility> leisureByTAZ = fbzLeisure.build("leisure");
		
		FacilitiesByZone fbzOther = new FacilitiesByZone(features, landUseDir);
		Multimap <Long,ActivityFacility> otherByTAZ = fbzOther.build("other");
	
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		Population newPlans = scenario.getPopulation();
		Coord zeroCoord = new Coord (0,0);
		

		
		
		List<Coord> homeCoords = new ArrayList<>();
		List<Coord> workCoords = new ArrayList<>();
		List<Coord> busCoords = new ArrayList<>();		
		List<Coord> educCoords = new ArrayList<>();
		List<Coord> healthCoords = new ArrayList<>();	
		List<Coord> visitCoords = new ArrayList<>();
		List<Coord> shopCoords = new ArrayList<>();
		List<Coord> leisCoords = new ArrayList<>();
		List<Coord> otherCoords = new ArrayList<>();
		
		
		for (Person p : originalPlans.getPersons().values()){

			Id<Person> pInId = Id.createPersonId( p.getId() );
			Person pIn = newPlans.getFactory().createPerson( pInId  );
			newPlans.addPerson( pIn );
			

			if(agentCondition.get(p.getId()).equals(0)){
			
				
				homeCoords = new ArrayList<>();
				workCoords = new ArrayList<>();
				busCoords = new ArrayList<>();		
				educCoords = new ArrayList<>();
				healthCoords = new ArrayList<>();	
				visitCoords = new ArrayList<>();
				shopCoords = new ArrayList<>();
				leisCoords = new ArrayList<>();
				otherCoords = new ArrayList<>();
				
			
				for (Plan plan : p.getPlans()){
					
					Plan planIn = newPlans.getFactory().createPlan();
					List<PlanElement> pes = plan.getPlanElements();
					
					for ( PlanElement pe : pes){
					
					if(pe instanceof Leg) {
					
						Leg leg = (Leg) pe;
						Leg legIn = newPlans.getFactory().createLeg(leg.getMode());
						planIn.addLeg(legIn);
						
					} else {
						
						Activity actIn = (Activity)pe;
						String actType = actIn.getType();
						

						switch(actType.substring(0,2)){

						case("ho"):
						homeCoords.add(actIn.getCoord());
						break;

						case("wo"):
						workCoords.add(actIn.getCoord());
						break;

						case("bu"):
						busCoords.add(actIn.getCoord());
						break;
						
						case("ed"):
						educCoords.add(actIn.getCoord());							
						break;
						
						case("he"):
						healthCoords.add(actIn.getCoord());
						break;
						
						case("vi"):
						visitCoords.add(actIn.getCoord());
						break;					
	
						case("sh"):
						shopCoords.add(actIn.getCoord());
						break;
						
						case("le"):
						leisCoords.add(actIn.getCoord());						
						
						case("ot"):
						otherCoords.add(actIn.getCoord());				

						}
						
						
						
	

					Activity actOut = newPlans.getFactory().createActivityFromCoord(actType, actIn.getCoord());
						planIn.addActivity(actOut);				
						actOut.setEndTime(actIn.getEndTime());
						actOut.setStartTime(actIn.getStartTime());

					}



					}
					
					pIn.addPlan(planIn);
				}
			
			
		
		} else if (agentCondition.get(p.getId()).equals(1)){
			
		
			
			ArrayList <Coord> homeShots = new ArrayList <> ();
			ArrayList <Coord> workShots = new ArrayList <> ();
			ArrayList <Coord> busShots = new ArrayList <> ();
			ArrayList <Coord> educShots = new ArrayList <> ();
			ArrayList <Coord> healthShots = new ArrayList <> ();
			ArrayList <Coord> visitShots = new ArrayList <> ();
			ArrayList <Coord> shopShots = new ArrayList <> ();
			ArrayList <Coord> leisShots = new ArrayList <> ();
			ArrayList <Coord> otherShots = new ArrayList <> ();
			
			
			
			
			
			int homeAppearences = 0;
			int workAppearences = 0;
			int busAppearences = 0;
			int educAppearences = 0;
			int healthAppearences = 0;
			int visitAppearences = 0;
			int shopAppearences = 0;
			int leisAppearences = 0;
			int otherAppearences = 0;
			
			
			
				for (Plan plan : p.getPlans()){
				
					Plan planIn = newPlans.getFactory().createPlan();
					List<PlanElement> pes = plan.getPlanElements();
					
					
					for ( PlanElement pe : pes){

					if(pe instanceof Leg) {
					
						Leg leg = (Leg) pe;
						Leg legIn = newPlans.getFactory().createLeg(leg.getMode());
						planIn.addLeg(legIn);
						
					} else {
											
						Activity actTemp = (Activity) pe; 
						String tempType = actTemp.getType();						
						String IDAct = tempType.substring(0,2);
						Coord tempCoord = actTemp.getCoord();			
						long TAZId = getEODZone(tempCoord);
						Coord newCoord = new Coord (0,0);						
						
						int repeated = 0;
						
						switch (IDAct){
						
						
						case ("ho"): 						
						++homeAppearences;
						//IF FIRST APPEARENCE, SHOOT
						if (homeAppearences == 1){
							newCoord = selectRandomFacility(homeByTAZ, TAZId);
							homeShots.add(newCoord);

						//IF NOT...							
						} else {

							for (int homeCoordsIndex = 0; homeCoordsIndex<homeAppearences-1;++homeCoordsIndex){
								//IF I'M ABLE TO FIND A REPEATED LOCATION
								if(homeCoords.get(homeAppearences-1).equals(homeCoords.get(homeCoordsIndex))){

									newCoord = homeShots.get(homeCoordsIndex);
									homeShots.add(newCoord);
									repeated=1;
									break;

								}
								

								
							}
							//IF I'M NOT ABLE TO FIND A REPEATED LOCATION, THEN SHOOT
							if(repeated==0){
								newCoord = selectRandomFacility(homeByTAZ, TAZId);
								homeShots.add(newCoord);								
								
							}
							
							
						}							
						
							break;


						case ("wo"):							
						++workAppearences;
						//IF FIRST APPEARENCE, SHOOT
						if (workAppearences == 1){
							newCoord = selectRandomFacility(workByTAZ, TAZId);
							workShots.add(newCoord);

						//IF NOT...							
						} else {

							for (int workCoordsIndex = 0; workCoordsIndex<workAppearences-1;++workCoordsIndex){
								//IF I'M ABLE TO FIND A REPEATED LOCATION
								if(workCoords.get(workAppearences-1).equals(workCoords.get(workCoordsIndex))){
									newCoord = workShots.get(workCoordsIndex);
									workShots.add(newCoord);
									repeated=1;
									break;

								}
								

								
							}
							//IF I'M NOT ABLE TO FIND A REPEATED LOCATION, THEN SHOOT
							if(repeated==0){
								newCoord = selectRandomFacility(workByTAZ, TAZId);
								workShots.add(newCoord);								
								
							}
							
							
						}	
							
							break;
							
						case ("bu"):						
							
						++busAppearences;
						//IF FIRST APPEARENCE, SHOOT
						if (busAppearences == 1){
							newCoord = selectRandomFacility(busiestByTAZ, TAZId);
							busShots.add(newCoord);

						//IF NOT...							
						} else {

							for (int busCoordsIndex = 0; busCoordsIndex<busAppearences-1;++busCoordsIndex){
								//IF I'M ABLE TO FIND A REPEATED LOCATION
								if(busCoords.get(busAppearences-1).equals(busCoords.get(busCoordsIndex))){

									newCoord = busShots.get(busCoordsIndex);
									busShots.add(newCoord);
									repeated=1;
									break;

								}
								

								
							}
							//IF I'M NOT ABLE TO FIND A REPEATED LOCATION, THEN SHOOT
							if(repeated==0){
								newCoord = selectRandomFacility(busiestByTAZ, TAZId);
								busShots.add(newCoord);								
								
							}
							
							
						}
							
											
							break;

							
						case("ed"):
							
						++educAppearences;
						//IF FIRST APPEARENCE, SHOOT
						if (educAppearences == 1){
							newCoord = selectRandomFacility(educationByTAZ, TAZId);
							educShots.add(newCoord);

						//IF NOT...							
						} else {

							for (int educCoordsIndex = 0; educCoordsIndex<educAppearences-1;++educCoordsIndex){
								//IF I'M ABLE TO FIND A REPEATED LOCATION
								if(educCoords.get(educAppearences-1).equals(educCoords.get(educCoordsIndex))){
									newCoord = educShots.get(educCoordsIndex);
									educShots.add(newCoord);
									repeated=1;
									break;

								}
								

								
							}
							//IF I'M NOT ABLE TO FIND A REPEATED LOCATION, THEN SHOOT
							if(repeated==0){
								newCoord = selectRandomFacility(educationByTAZ, TAZId);
								educShots.add(newCoord);				
								
							}
							
							
						}							
								
							break;
							
						case("he"):
							
							++healthAppearences;
							//IF FIRST APPEARENCE, SHOOT
							if (healthAppearences == 1){
								newCoord = selectRandomFacility(healthByTAZ, TAZId);
								healthShots.add(newCoord);

							//IF NOT...							
							} else {

								for (int healthCoordsIndex = 0; healthCoordsIndex<healthAppearences-1;++healthCoordsIndex){
									//IF I'M ABLE TO FIND A REPEATED LOCATION
									if(healthCoords.get(healthAppearences-1).equals(healthCoords.get(healthCoordsIndex))){
										newCoord = healthShots.get(healthCoordsIndex);
										healthShots.add(newCoord);
										repeated=1;
										break;

									}
									

									
								}
								//IF I'M NOT ABLE TO FIND A REPEATED LOCATION, THEN SHOOT
								if(repeated==0){
									newCoord = selectRandomFacility(healthByTAZ, TAZId);
									healthShots.add(newCoord);				
									
								}
								
								
							}	
							
					
							break;
							
						case("vi"):
							
							++visitAppearences;
							//IF FIRST APPEARENCE, SHOOT
							if (visitAppearences == 1){
								newCoord = selectRandomFacility(visitByTAZ, TAZId);
								visitShots.add(newCoord);

							//IF NOT...							
							} else {

								for (int visitCoordsIndex = 0; visitCoordsIndex<visitAppearences-1;++visitCoordsIndex){
									//IF I'M ABLE TO FIND A REPEATED LOCATION
									if(visitCoords.get(visitAppearences-1).equals(visitCoords.get(visitCoordsIndex))){
										newCoord = visitShots.get(visitCoordsIndex);
										visitShots.add(newCoord);
										repeated=1;
										break;

									}
									

									
								}
								//IF I'M NOT ABLE TO FIND A REPEATED LOCATION, THEN SHOOT
								if(repeated==0){
									newCoord = selectRandomFacility(visitByTAZ, TAZId);
									visitShots.add(newCoord);									
								}								
								
							}	
							
					
							break;
							
						case("sh"):
							
							++shopAppearences;
							//IF FIRST APPEARENCE, SHOOT
							if (shopAppearences == 1){
								newCoord = selectRandomFacility(shopByTAZ, TAZId);
								shopShots.add(newCoord);

							//IF NOT...							
							} else {

								for (int shopCoordsIndex = 0; shopCoordsIndex<shopAppearences-1;++shopCoordsIndex){
									//IF I'M ABLE TO FIND A REPEATED LOCATION
									if(shopCoords.get(shopAppearences-1).equals(shopCoords.get(shopCoordsIndex))){
										newCoord = shopShots.get(shopCoordsIndex);
										shopShots.add(newCoord);
										repeated=1;
										break;

									}
									

									
								}
								//IF I'M NOT ABLE TO FIND A REPEATED LOCATION, THEN SHOOT
								if(repeated==0){
									newCoord = selectRandomFacility(shopByTAZ, TAZId);
									shopShots.add(newCoord);									
								}								
								
							}	
							
					
							break;
							
						case("le"):	
							
						++leisAppearences;
						//IF FIRST APPEARENCE, SHOOT
						if (leisAppearences == 1){
							newCoord = selectRandomFacility(leisureByTAZ, TAZId);
							leisShots.add(newCoord);

						//IF NOT...							
						} else {

							for (int leisCoordsIndex = 0; leisCoordsIndex<leisAppearences-1;++leisCoordsIndex){
								//IF I'M ABLE TO FIND A REPEATED LOCATION
								if(leisCoords.get(leisAppearences-1).equals(leisCoords.get(leisCoordsIndex))){
									newCoord = leisShots.get(leisCoordsIndex);
									leisShots.add(newCoord);
									repeated=1;
									break;

								}
								

								
							}
							//IF I'M NOT ABLE TO FIND A REPEATED LOCATION, THEN SHOOT
							if(repeated==0){
								newCoord = selectRandomFacility(leisureByTAZ, TAZId);
								leisShots.add(newCoord);									
							}								
							
						}	
											
							break;
							
						case("ot"):
							
							
							++otherAppearences;
							//IF FIRST APPEARENCE, SHOOT
							if (otherAppearences == 1){
								newCoord = selectRandomFacility(otherByTAZ, TAZId);
								otherShots.add(newCoord);

							//IF NOT...							
							} else {

								for (int otherCoordsIndex = 0; otherCoordsIndex<otherAppearences-1;++otherCoordsIndex){
									//IF I'M ABLE TO FIND A REPEATED LOCATION
									if(otherCoords.get(otherAppearences-1).equals(otherCoords.get(otherCoordsIndex))){
										newCoord = otherShots.get(otherCoordsIndex);
										otherShots.add(newCoord);
										repeated=1;
										break;

									}
									

									
								}
								//IF I'M NOT ABLE TO FIND A REPEATED LOCATION, THEN SHOOT
								if(repeated==0){
									newCoord = selectRandomFacility(otherByTAZ, TAZId);
									otherShots.add(newCoord);									
								}								
								
							}	
				
							break;
						}
						
						
						
						
						
						
						
						
						//TODO
						
						if (!newCoord.equals(zeroCoord)){
						
							Activity actIn = newPlans.getFactory().createActivityFromCoord(tempType, newCoord);
							planIn.addActivity(actIn);
							actIn.setEndTime(actTemp.getEndTime());
							actIn.setStartTime(actTemp.getStartTime());
							
						} else {
							
							Activity actIn = newPlans.getFactory().createActivityFromCoord(tempType, tempCoord);
							planIn.addActivity(actIn);
							actIn.setEndTime(actTemp.getEndTime());
							actIn.setStartTime(actTemp.getStartTime());
							
						}
						
	
					}
					
					
					}
					pIn.addPlan(planIn);
					
					
				}


			}
		}

	
		return newPlans;
		
	}
	
	private void writeNewPopulation (Population population){
		
		String outPlansDir = this.svnWorkingDir + "inputForMATSim/plans/expanded/";
		File outPlansDirFile = new File(outPlansDir);
		if(!outPlansDirFile.exists()) createDir(outPlansDirFile);
		
		String outPlans = "randomized_expanded_plans.xml.gz";	
		
		PopulationWriter pw = new PopulationWriter(population);
		pw.write(outPlansDir + outPlans);
		
	}
		
	private void writeNewConfig (Config config){
		
		String outConfigDir = this.svnWorkingDir + "inputForMATSim/";
		String outConfig = "randomized_expanded_config.xml";
		
		PlansConfigGroup plans = config.plans();
		plans.setInputFile(runsWorkingDir + "randomized_expanded_plans.xml.gz");
		new ConfigWriter(config).write(outConfigDir + outConfig);
		
	}
	
}
