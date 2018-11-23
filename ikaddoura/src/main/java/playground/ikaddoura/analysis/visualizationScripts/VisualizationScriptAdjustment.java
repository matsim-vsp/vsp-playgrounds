/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.ikaddoura.analysis.visualizationScripts;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.ikaddoura.analysis.IKAnalysisRun;

/**
* @author ikaddoura
*/

public class VisualizationScriptAdjustment {
	private static final Logger log = Logger.getLogger(IKAnalysisRun.class);

	private final String runIdMarker = "policyCaseRunId";
	private final String rundIdToCompareWithMarker = "baseCaseRunId";
	private final String scaleFactorMarker = "_MATSimScenarioScaleFactor_";
	private final String crsMarker = "crs-specification";
	
	private String runIdToCompareWith = rundIdToCompareWithMarker;
	private String scalingFactor = scaleFactorMarker;
	private String runId = runIdMarker;
	private String scenarioCRS = crsMarker;
	
	private final String visScriptTemplateInputFile;
	private final String visScriptOutputFile;

	public VisualizationScriptAdjustment(String visScriptTemplateInputFile, String visScriptOutputFile) {
		this.visScriptTemplateInputFile = visScriptTemplateInputFile;
		this.visScriptOutputFile = visScriptOutputFile;
	}

	public void setRunId(String runId) {
		this.runId = runId;
	}

	public void setRunIdToCompareWith(String runIdToCompareWith) {
		this.runIdToCompareWith = runIdToCompareWith;
	}

	public void setScalingFactor(String scalingFactor) {
		this.scalingFactor = scalingFactor;
	}

	public void setCRS(String scenarioCRS) {
		this.scenarioCRS = scenarioCRS;
	}

	public void write() {
		
		File srcFile = new File(visScriptTemplateInputFile);			
		File destDir = new File(visScriptOutputFile);
		try {
			FileUtils.copyFile(srcFile, destDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Path path = Paths.get(visScriptOutputFile);
		Charset charset = StandardCharsets.UTF_8;

		String content = null;
		try {
			content = new String(Files.readAllBytes(path), charset);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		// template adjustments
		if (this.runIdToCompareWith != "" && this.runIdToCompareWith != null) {
			content = content.replaceAll(rundIdToCompareWithMarker, this.runIdToCompareWith);
		} else {
			content = content.replaceAll(rundIdToCompareWithMarker + ".", "");
		}
		if (this.runId != "" && this.runId != null) {
			content = content.replaceAll(runIdMarker, this.runId);
		} else {
			content = content.replaceAll(runIdMarker + ".", "");
		}
		content = content.replaceAll(scaleFactorMarker, this.scalingFactor);
		
		if (this.scenarioCRS != null) {
			if (this.scenarioCRS.equals(TransformationFactory.DHDN_GK4)) {
				content = content.replaceAll(crsMarker,
						  "  <proj4>+proj=tmerc +lat_0=0 +lon_0=12 +k=1 +x_0=4500000 +y_0=0 +ellps=bessel +towgs84=598.1,73.7,418.2,0.202,0.045,-2.455,6.7 +units=m +no_defs</proj4>" + 
						  "  <srsid>2648</srsid>" + 
						  "  <srid>31468</srid>" + 
						  "  <authid>EPSG:31468</authid>" + 
						  "  <description>DHDN / Gauss-Kruger zone 4</description>" + 
						  "  <projectionacronym>tmerc</projectionacronym>" + 
						  "  <ellipsoidacronym>bessel</ellipsoidacronym>" + 
						  "  <geographicflag>false</geographicflag>");
				
			} else if (this.scenarioCRS.equals(TransformationFactory.DHDN_SoldnerBerlin)) {
				content = content.replaceAll(crsMarker,
					      "	 <proj4>+proj=cass +lat_0=52.41864827777778 +lon_0=13.62720366666667 +x_0=40000 +y_0=10000 +ellps=bessel +towgs84=598.1,73.7,418.2,0.202,0.045,-2.455,6.7 +units=m +no_defs</proj4>" + 
					      "	 <srsid>1031</srsid>" + 
					      "	 <srid>3068</srid>" + 
					      "	 <authid>EPSG:3068</authid>" + 
					      "	 <description>DHDN / Soldner Berlin</description>" + 
					      "  <projectionacronym>cass</projectionacronym>" + 
					      "	 <ellipsoidacronym>bessel</ellipsoidacronym>" + 
					      "	 <geographicflag>false</geographicflag>");
					
			} else {
				log.warn("The crs needs to be set manually via QGIS.");
			}
		} else {
			log.warn("No scenario crs provided. The crs needs to be set manually via QGIS.");
		}
		
		try {
			Files.write(path, content.getBytes(charset));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

