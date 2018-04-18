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
package saleem.stockholmmodel.transitdataconversion;

/**
 * Execution class to convert Excel reports of transit data into MatSim forms. 
 * VehicleTypes.xls, Stoppstallen.xls and all transit schedule excel reports are required.
 *
 * @author Mohammad Saleem
 */
public class ExceltoMatSimDataConversion {
	public static void main(String[] args){
		//It may take a few minutes, so please be patient...
		/*
		 * Add the following at the very start of transit schedule if there are SAX parsing errors while running the simulation.
		 
		  <?xml version="1.0" encoding="UTF-8"?>
		  <!DOCTYPE transitSchedule SYSTEM "http://www.matsim.org/files/dtd/transitSchedule_v1.dtd">
 
		 */
		ExcelReportsReader ex = new ExcelReportsReader();
		TransitSchedule transit = new TransitSchedule();
		ex.readExcelReportStopStallen(transit, "H:\\Matsim\\Reports\\Stoppstallen.xls" );
		ex.readExcelReports(transit, "H:\\Matsim\\Reports");//Folder containing all transit schedule excel reports from SL.
		ex.readVehicleTypes(transit, "H:\\Matsim\\Reports\\VehicleTypes.xls");
		XMLWriter xmlwriter = new XMLWriter();
		xmlwriter.createTransitSchedule(transit, "./ihop2/TransitSchedule.xml");
		xmlwriter.createVehiclesXML(transit, "./ihop2/vehicles.xml");
	}
}
