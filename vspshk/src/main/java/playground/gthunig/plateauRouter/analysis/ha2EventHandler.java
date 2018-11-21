package playground.gthunig.plateauRouter.analysis;

import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.util.*;

public class ha2EventHandler implements PersonArrivalEventHandler, PersonDepartureEventHandler,
										LinkEnterEventHandler {

	private Map<Id<Person>,Double> travelTimes = new HashMap<>();
	private Map<Id<Person>,Double> travelBeginMap = new HashMap<>();
	private Map<Id<Link>,String> routeSpecificLinks = new HashMap<>();
	private Map<Integer,Map<Id<Link>,Integer>> routeUsers = new HashMap<>();
	private int iteration;
	private List<Integer> allIterations;
	private Map<Integer, Double> averageTTperIter = new HashMap<>();
	private Map<Integer, Double> totalTTperIter = new HashMap<>();
	
	public ha2EventHandler(Map<Id<Link>,String> routeSpecificLinks){
		this.routeSpecificLinks = routeSpecificLinks;
		this.iteration = 0;
		this.allIterations = new ArrayList<>();
		reset(0);
	}
	
	public void setRouteSpecificLinks(Map<Id<Link>,String> list){
		this.routeSpecificLinks = list;
	}
	
	@Override
	public void reset(int iteration) {
		this.travelTimes.clear();
		this.travelBeginMap.clear();
		this.iteration = iteration;
		this.routeUsers.put(iteration, new HashMap<>());
		this.allIterations.add(iteration);
	}
	
// merkt sich die Abfahrtszeit eines Agenten
	@Override
	public void handleEvent(PersonDepartureEvent event) {
			this.travelBeginMap.put(event.getPersonId(), event.getTime());
	}

// berechnet aus Ankunfts- und Abfahrtszeit die TravelTime eines Agenten und speichert sie in travelTimes
	@Override
	public void handleEvent(PersonArrivalEvent event) {
			if(this.travelTimes.containsKey(event.getPersonId())){	
				Log.error("+++++++++  EIN ANGENT HAT 2 TRIPS  ++++++++++");
//				double travelTime = event.getTime() - this.travelBeginMap.get(event.getPersonId()) + travelTimes.get(event.getPersonId());
//				this.travelTimes.put(event.getPersonId(), travelTime);
			}
			else{
			double travelTime = event.getTime() - this.travelBeginMap.get(event.getPersonId());
			this.travelTimes.put(event.getPersonId(), travelTime);
			}
		
	} 
	
	
	public double getTravelTimeofPerson(Id<Person> id){
		if(!this.travelTimes.containsKey(id)) throw new IllegalArgumentException();
		return this.travelTimes.get(id);
	}
	
	public Map<Id<Person>, Double> getTravelTimes(){
		return this.travelTimes;
	}
	
	public Map<Id<Person>, Double> getTravelBegins() {
		return this.travelBeginMap;
	}
	
	public double getTotalTravelTime(){
		double totalTT = 0.0;
		for(Id<Person> id : this.travelTimes.keySet()){
			totalTT += this.travelTimes.get(id);
		}
		return totalTT;
	}

	public double getAverageTravelTime(){
		int size = 0;
		double totalTT = 0.0;
		for(Id<Person> id : this.travelTimes.keySet()){
			totalTT += this.travelTimes.get(id);
			size ++;
		}
		return totalTT / ((double) size);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(this.routeSpecificLinks.keySet().contains(event.getLinkId())){
			Map<Id<Link>,Integer> map = this.routeUsers.get(this.iteration);
			if(map.containsKey(event.getLinkId())){
				int zz = map.get(event.getLinkId());
				zz++;
				map.put(event.getLinkId(),zz);
			}
			else{
				map.put(event.getLinkId(), 1);
			}
		}
	}

	public void notifyIterationEnds() {
		rememberTotalTT();
		rememberAverageTT();
	}

	private void rememberAverageTT() {
		this.averageTTperIter.put(this.iteration, getAverageTravelTime());
	}

	private void rememberTotalTT() {
		this.totalTTperIter.put(this.iteration, getTotalTravelTime());		
	}
	
	public void print(){
		double averageAverage = 0.0;
		double averageTotal = 0.0;
		Collections.sort(this.allIterations);
		int count = 0;
		for(int i : this.allIterations){
			System.out.println("\n ITERATION " + i);
			System.out.println("Routenaufteilung:\t");
			Map<Id<Link>,Integer> map = this.routeUsers.get(i);
			for(Id<Link> id: map.keySet()){
				System.out.println("Route " + this.routeSpecificLinks.get(id) + ":\t" + map.get(id));
			}
			double average = this.averageTTperIter.get(i);
			double total = this.totalTTperIter.get(i);
			System.out.println("\n mittlere Reisezeit:\t" + average) ;
			System.out.println("Gesamtreisezeit:\t" + total);
			averageAverage += average;
			averageTotal += total;
			count ++;
		}
		System.out.println("\n mittlere mittlere Reisezeit der 50 Iterationen:\t" + averageAverage/count);
		System.out.println("\n mittlere Gesamtreisezeit der 50 Iterationen:\t" + averageTotal/count);
	}
	
}


