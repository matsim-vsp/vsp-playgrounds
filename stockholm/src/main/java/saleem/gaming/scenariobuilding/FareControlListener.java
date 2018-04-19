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
package saleem.gaming.scenariobuilding;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
/**
 * A control listener class aimed at enforcing PT fare. Notifies the fare hanlder class.
 * 
 * @author Mohammad Saleem
 */
public class FareControlListener implements StartupListener{

	@Override
	public void notifyStartup(StartupEvent event) {
		EventsManager eventmanager = event.getServices().getEvents();
		FareControlHandler farehandler = new FareControlHandler(eventmanager);
		event.getServices().getEvents().addHandler(farehandler);
	}

}
