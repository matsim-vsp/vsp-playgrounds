/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.analysis.pngSequence2Video;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.jcodec.api.awt.SequenceEncoder;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

/**
* @author ikaddoura
*/

public class MATSimVideoUtils {
	private static final Logger log = Logger.getLogger(MATSimVideoUtils.class);

	public static void createLegHistogramVideo(String runDirectory, String runId, String outputDirectory) throws IOException {
		createVideo(runDirectory, runId, outputDirectory, 1, "legHistogram_all");
	}
	
	public static void createLegHistogramVideo(String runDirectory) throws IOException {
		createVideo(runDirectory, null, runDirectory, 1, "legHistogram_all");
	}

	public static void createVideo(String runDirectory, String runId, String outputDirectory, int interval, String pngFileName) throws IOException {
		createVideoX(runDirectory, runId, outputDirectory, interval, pngFileName);
	}
	
	public static void createVideo(String runDirectory, int interval, String pngFileName) throws IOException {
		createVideoX(runDirectory, null, runDirectory, interval, pngFileName);
	}
	
	private static void createVideoX(String runDirectory, String runId, String outputDirectory, int interval, String pngFileName) throws IOException {

		log.info("Generating a video using a png sequence... (file name: " + pngFileName + ", iteration interval: " + interval + ")");
		
		if (!runDirectory.endsWith("/")) {
			runDirectory = runDirectory + "/";
		}
		
		if (!outputDirectory.endsWith("/")) {
			outputDirectory = outputDirectory + "/";
		}
		
		String configFile;
		String outputDirectoryWithRunId;
		if (runId != null) {
			configFile = runDirectory + runId + ".output_config.xml";
			outputDirectoryWithRunId = outputDirectory + runId + ".";
		} else {
			configFile = runDirectory + "output_config.xml";
			outputDirectoryWithRunId = outputDirectory;
		}
		
		String outputFile = outputDirectoryWithRunId + pngFileName + ".mp4";
		SequenceEncoder enc = new SequenceEncoder(new File(outputFile));
			
		Config config;
		
		if (new File(configFile).exists()) {
			config = ConfigUtils.loadConfig(configFile);		
		} else if (new File(runDirectory + "output_config.xml").exists()) {
			config = ConfigUtils.loadConfig(runDirectory + "output_config.xml");			
		} else {
			throw new RuntimeException("No (output) config file: " + configFile + ". Aborting...");
		}

		int counter = 0;
		for (int i = config.controler().getFirstIteration(); i<= config.controler().getLastIteration(); i++) {
			
			if (counter % interval == 0) {
				
				if (counter % 10 == 0) log.info("Creating frame for iteration " + counter);
				
				String pngFile = null;
				BufferedImage image = null;
				
				try {
					if (runId == null) {
						pngFile = runDirectory + "ITERS/it." + i + "/" + i + "." + pngFileName + ".png";
					} else {
						pngFile = runDirectory + "ITERS/it." + i + "/" + runId + "." + i + "." + pngFileName + ".png";
					}
					image = ImageIO.read(new File(pngFile));

				} catch (IOException e) {

					try {
						pngFile = runDirectory + "ITERS/it." + i + "/" + i + "." + pngFileName + ".png";
						image = ImageIO.read(new File(pngFile));

					} catch (IOException e2){
						log.warn("Couldn't find png for iteration " + i + "." );
					}
				}
								
				if (image != null) {
					enc.encodeImage(image);
				} else {
//					log.warn("Skipping image...");
				}
			}
			counter++;
		}
		
		enc.finish();
		
		log.info("Generating a video using a png sequence... Done. Video written to " + outputFile);
	}

}

