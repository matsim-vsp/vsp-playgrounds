package playground.santiago.analysis;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.misc.Time;

/**
 * Useful to write the departure times and distances by trip made by the "pt" mode. Not designed to handle activities without end_times. See the following:
 * 
 * transit_walk - transit_walk --> walk &
 * transit_walk - pt - transit_walk --> pt &
 * transit_walk - pt - transit_walk - pt - transit_walk --> pt
 *
 * Designed to be used together with SantiagoModeTripTravelDistanceHandler.
 */
public class SantiagoPTDistanceFromPlans {

	private Network network;
	private Population population;
	public SortedMap<String,Map<Id<Person>,List<String>>> Pt2PersonId2TravelDistances = new TreeMap<>();

	//	public static void main (String[]args){
	//		String netFile = "../../../runs-svn/santiago/baseCase1pct/outputOfStep1/output_network.xml.gz";
	//		String plansFile = "../../../runs-svn/santiago/baseCase1pct/outputOfStep1/ITERS/it.0/0.plans.xml.gz";
	//		SantiagoPTDistanceFromPlans spd = new SantiagoPTDistanceFromPlans(netFile,plansFile);
	//		spd.Run();
	//	}


	public SantiagoPTDistanceFromPlans(/*String netFile,*/ Population population){

		this.population = population;

		//		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario.getNetwork());
		//		netReader.readFile(netFile);
		//		this.network=scenario.getNetwork();						

	}

	private void getData(){

		for (Person p: population.getPersons().values()) {

			Id<Person> personId = p.getId();

			String lastAct="";
			String currentAct="";

			double lastDepartureTime=0;

			double distanceTravelled = 0;			
			String departureTimeTransitTrip = "";


			List<String> modes = new ArrayList<>();

			for(PlanElement pe : p.getSelectedPlan().getPlanElements()) {

				if (pe instanceof Activity){
					currentAct=((Activity) pe).getType();
					double currentDepartureTime = ((Activity) pe).getEndTime();					

					/*start of the trip -> departureTime should be stored -> same as lastAct endTime*/
					if (!lastAct.equals("pt interaction")&&currentAct.equals("pt interaction")){

						if(lastDepartureTime==Time.UNDEFINED_TIME){
							departureTimeTransitTrip="noInfo";
						} else {

							departureTimeTransitTrip=String.valueOf(lastDepartureTime);
							//System.out.println(lastDepartureTime);
						}
					}

					/*Special case: regularAct - regularAct by a PT leg*/
					else if (!lastAct.equals("pt interaction")&&!currentAct.equals("pt interaction")){
						
						if(modes.contains(TransportMode.pt)||modes.contains(TransportMode.transit_walk)){
							if(lastDepartureTime==Time.UNDEFINED_TIME){
								departureTimeTransitTrip="noInfo";
							} else {
								
								departureTimeTransitTrip=String.valueOf(lastDepartureTime);

							}
							String actualMode="";

							if (modes.contains(TransportMode.pt)) {

								actualMode="pt";

							} else {

								actualMode="walk";
							}												


							if (Pt2PersonId2TravelDistances.containsKey(actualMode)){

								Map<Id<Person>,List<String>> personId2Departure2Distances = Pt2PersonId2TravelDistances.get(actualMode);

								if(personId2Departure2Distances.containsKey(personId)){

									List<String> departure2Distances = personId2Departure2Distances.get(personId);


									departure2Distances.add(departureTimeTransitTrip+"-"+String.valueOf(distanceTravelled));
									distanceTravelled=0;

								} else {				

									List<String> departure2Distances = new ArrayList<>();
									departure2Distances.add(departureTimeTransitTrip+"-"+String.valueOf(distanceTravelled));
									personId2Departure2Distances.put(personId, departure2Distances);
									distanceTravelled=0;
								}



							} else {

								Map<Id<Person>,List<String>> personId2Departure2Distances = new TreeMap<>();
								List<String> departure2Distances = new ArrayList<>();
								departure2Distances.add(departureTimeTransitTrip+"-"+String.valueOf(distanceTravelled));
								personId2Departure2Distances.put(personId, departure2Distances);
								Pt2PersonId2TravelDistances.put(actualMode, personId2Departure2Distances);
								distanceTravelled=0;
							}			

							modes.clear();
							

						}



						/*end of the trip -> personId and distanceTravelled should be stored together with departureTime. Also, refresh distanceTravelled to 0*/
					} else if(lastAct.equals("pt interaction")&&!currentAct.equals("pt interaction")){
						//Getting the mode and end the process.
						//modes should be refreshed to an empty list after executing the code inside this if.

						String actualMode="";

						if (modes.contains(TransportMode.pt)) {

							actualMode="pt";

						} else {

							actualMode="walk";
						}												


						if (Pt2PersonId2TravelDistances.containsKey(actualMode)){

							Map<Id<Person>,List<String>> personId2Departure2Distances = Pt2PersonId2TravelDistances.get(actualMode);

							if(personId2Departure2Distances.containsKey(personId)){

								List<String> departure2Distances = personId2Departure2Distances.get(personId);


								departure2Distances.add(departureTimeTransitTrip+"-"+String.valueOf(distanceTravelled));
								distanceTravelled=0;

							} else {				

								List<String> departure2Distances = new ArrayList<>();
								departure2Distances.add(departureTimeTransitTrip+"-"+String.valueOf(distanceTravelled));
								personId2Departure2Distances.put(personId, departure2Distances);
								distanceTravelled=0;
							}



						} else {

							Map<Id<Person>,List<String>> personId2Departure2Distances = new TreeMap<>();
							List<String> departure2Distances = new ArrayList<>();
							departure2Distances.add(departureTimeTransitTrip+"-"+String.valueOf(distanceTravelled));
							personId2Departure2Distances.put(personId, departure2Distances);
							Pt2PersonId2TravelDistances.put(actualMode, personId2Departure2Distances);
							distanceTravelled=0;
						}			

						modes.clear();

					}


					lastAct=currentAct;
					lastDepartureTime=currentDepartureTime;

				} else if (pe instanceof Leg) {					
					if (((Leg) pe).getMode().equals(TransportMode.pt)||((Leg) pe).getMode().equals(TransportMode.transit_walk)){
						modes.add(((Leg) pe).getMode());
						double currentDistance = ((Leg) pe).getRoute().getDistance();
						distanceTravelled+=currentDistance;

					}

				}

			}

		}


	}


	public SortedMap<String,Map<Id<Person>,List<String>>> getPt2PersonId2TravelDistances(){
		getData();
		return this.Pt2PersonId2TravelDistances;
	}

	//	private void Run() {
	//		getData();
	//		SortedMap<String,Map<Id<Person>,List<String>>> test = getPt2PersonId2TravelDistances();
	//		
	//		for(String mode : test.keySet()){
	//			for(Id<Person> person: test.get(mode).keySet()){
	//				for (String depDistances : test.get(mode).get(person)){
	//					System.out.println(mode + " " + person.toString() + " " + depDistances);
	//				}
	//			}
	//			
	//		}
	//		
	//	}
}
