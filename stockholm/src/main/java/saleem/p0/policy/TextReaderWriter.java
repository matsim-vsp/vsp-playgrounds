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
package saleem.p0.policy;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
/**
 * A class to read from and write to text files
 * 
 * @author Mohammad Saleem
 */
public class TextReaderWriter {
	public void writeToTextFile(ArrayList<Double> values, String path){
		Iterator<Double> iter = values.iterator();
		try { 
			File file=new File(path);
			String text="";
			 while(iter.hasNext()){
		        	double d = iter.next();
		        	text = text + d + " ";
		        }
		    FileOutputStream fileOutputStream=new FileOutputStream(file);
		    fileOutputStream.write(text.getBytes());
		    fileOutputStream.close();
	       
	    } catch(Exception ex) {
	        //catch logic here
	    }
	}
	public ArrayList<Double> readFromTextFile(String path){
		ArrayList<Double> values = new ArrayList<>();
		Scanner scan;
	    File file = new File(path);
	    try {
	        scan = new Scanner(file);

	        while(scan.hasNext())
	        {
	        	values.add(Double.parseDouble(scan.next()));
	        }
	        scan.close();

	    } catch(Exception ex) {
	        //catch logic here
	    }
		return values;
	}
}
