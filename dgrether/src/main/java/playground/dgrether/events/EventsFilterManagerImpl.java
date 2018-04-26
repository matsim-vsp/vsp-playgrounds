/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.dgrether.events;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;

import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import playground.dgrether.events.filters.EventFilter;



/**
 * @author dgrether
 *
 */
public class EventsFilterManagerImpl implements EventsFilterManager {
	
	private final EventsManager delegate = EventsUtils.createEventsManager() ;
	private List<EventFilter> filters = new ArrayList<EventFilter>();
	
	
	@Override
	public void addFilter(EventFilter filter) {
		EventsFilterManagerImpl.this.filters.add(filter);
	}
	
	/**
	 * Delegates to List.remove() and returns the appropriate value
	 * @param filter
	 * @return the value of List.remove() see interface
	 */
	@Override
	public boolean removeFilter(EventFilter filter) {
		return EventsFilterManagerImpl.this.filters.remove(filter);
	}
	
	public void addHandler(final EventHandler handler) {
		delegate.addHandler(handler);
	}
	
	public void removeHandler(final EventHandler handler) {
		delegate.removeHandler(handler);
	}
	
	public void resetHandlers(final int iteration) {
		delegate.resetHandlers(iteration);
	}
	
	public void initProcessing() {
		delegate.initProcessing();
	}
	
	public void afterSimStep(double time) {
		delegate.afterSimStep(time);
	}
	
	public void finishProcessing() {
		delegate.finishProcessing();
	}
	
	/**
	 * If all filters set in this class are returning true on the
	 * event given as parameter the Events.processEvent() method is called.
	 * Otherwise nothing is done at all.
	 */
	@Override
	public void processEvent(final Event event) {
		boolean doProcess = true;
		for (EventFilter f : EventsFilterManagerImpl.this.filters) {
			if (!f.doProcessEvent(event)) {
				doProcess = false;
				break;
			}
		}
		if (doProcess) {
			delegate.processEvent(event);
		}
	}
}
