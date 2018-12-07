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
 * Class specifying a vehicle instance within transit schedule
 * 
 * 
 * @author Mohammad Saleem
 */
public class Vehicle {
	private String id;
	private String type;
	public void setID(String id ){
		this.id=id;
	}
	public String getID(){
		return this.id;
	}
	public void setType(String type ){
		this.type=type;
	}
	public String getType(){
		return this.type;
	}
}
