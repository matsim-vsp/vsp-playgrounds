/*
 * Copyright 2018 Mohammad Saleem
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: salee@kth.se
 *
 */ 
package saleem.p0.stockholm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;

import saleem.p0.policy.GenericP0ControlHandler;

/**
 * To set up P0 for Stockholm
 * 
 * @author Mohammad Saleem
 */
public class StockholmP0ControlListener implements StartupListener, IterationStartsListener,IterationEndsListener {
	public Network network;
	List<GenericP0ControlHandler> handlers = new ArrayList<GenericP0ControlHandler>();
	 Map<String, List<Link>> incominglinks;
	 Map<String, List<Link>> outgoinglinks;
	Scenario scenario;
	
	
	public StockholmP0ControlListener(Scenario scenario, Network network, Map<String, List<Link>> incominglinks, Map<String, List<Link>> outgoinglinks){
		this.network = network;
		this.scenario=scenario;
		this.incominglinks=incominglinks;
		this.outgoinglinks=outgoinglinks;
		
	}
	/**
	 * Notifies all observers of the Controler that a iteration is setup
	 *
	 * @param event
	 */
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		List<NetworkChangeEvent> allchangeevents = new ArrayList<NetworkChangeEvent>() ;
		Iterator<GenericP0ControlHandler> hiter = handlers.iterator();
		while(hiter.hasNext()){
			GenericP0ControlHandler handler = hiter.next();
			handler.setIteration(event.getIteration());//To run without P0 call with 0, and comment out initialisation of events in initialise function
			allchangeevents.addAll(handler.getChangeEvents());
			handler.initialise();
		}
		final List<NetworkChangeEvent> events = allchangeevents;
	    NetworkUtils.setNetworkChangeEvents(network,events);
	    allchangeevents.removeAll(allchangeevents);
	}
	/**
	 * Notifies all observers of the Controler that a iteration is finished
	 * @param event
	 */
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		Iterator<GenericP0ControlHandler> hiter = handlers.iterator();
		while(hiter.hasNext()){
			GenericP0ControlHandler handler = hiter.next();
			handler.populatelastCapacities();//To keep track of capacities on the last day
			handler.printDelayStats();
		}
	}
	/**
	 * Notifies all observers that the controler is initialized and they should do the same
	 *
	 * @param event
	 */
	@Override
	public void notifyStartup(StartupEvent event) {
		Iterator<String> pretimednodes = this.incominglinks.keySet().iterator();
		while(pretimednodes.hasNext()){
			String timednode = pretimednodes.next();
			List<Link> inlinks = incominglinks.get(timednode);
			List<Link> outlinks = outgoinglinks.get(timednode);
			GenericP0ControlHandler handler = new GenericP0ControlHandler(scenario, inlinks, outlinks,network);
			event.getServices().getEvents().addHandler(handler);
			handlers.add(handler);
		}
		System.out.println("Startup Complete");
	}
}

