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

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

/**
 * A class to write CSV files.
 * 
 */
public final class CSVFileWriter {
	private static final Logger log = Logger.getLogger(CSVFileWriter.class);

	private String separator;
	private BufferedWriter writer ;

	
	/**
	 * writes the header
	 */
	public CSVFileWriter(String path, String separator){
		log.info("Initializing the writer.");
		
		this.separator = separator;
		
		try {
		writer = IOUtils.getBufferedWriter(path);
		} catch (Exception ee) {
			ee.printStackTrace();
			throw new RuntimeException("writer could not be instantiated");
		}

		if (writer==null) {
			throw new RuntimeException("writer is null");
		}

		log.info("... done!");
	}

	
	public final void writeField(double value) {
		try {
			writer.write(value + this.separator);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}

	
	public final void writeField(int value) {
		try {
			writer.write(value + this.separator);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}

	
	public final void writeField(Id<?> value) {
		try {
			writer.write(value + this.separator);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}
	
	
	public final void writeField(String value) {
		try {
			writer.write(value + this.separator);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}

	
	public final void writeNewLine() {
		try {
			writer.newLine() ;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("i/o failure") ;
		}
	}

	
	/**
	 * finalize and close csv file
	 */
	public final void close(){
		try {
			log.info("Closing ...");
			assert(writer != null);
			writer.flush();
			writer.close();
			log.info("... done!");
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
