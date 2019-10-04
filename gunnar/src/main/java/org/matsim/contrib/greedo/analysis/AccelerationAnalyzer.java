/*
 * Copyright 2018 Gunnar Flötteröd
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
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.greedo.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AccelerationAnalyzer {

	static void save(final JFreeChart chart, final String fileName) {
		try {
			ChartUtilities.saveChartAsPNG(new File(fileName + ".png"), chart, 1000, 600);

			// SVGGraphics2D g2 = new SVGGraphics2D(500, 300);
			// Rectangle r = new Rectangle(0, 0, 500, 300);
			// chart.draw(g2, r);
			// SVGUtils.writeToSVG(new File(fileName + ".svg"), g2.getSVGElement());

			// Transcoder transcoder = new PDFTranscoder();
			// TranscoderInput transcoderInput = new TranscoderInput(new FileInputStream(new
			// File(fileName + ".svg")));
			// TranscoderOutput transcoderOutput = new TranscoderOutput(new
			// FileOutputStream(new File(fileName + ".pdf")));
			// transcoder.transcode(transcoderInput, transcoderOutput);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		System.out.println("STARTED...");

		// final AccelerationExperimentData acceptNegativeDisappointment = new
		// AccelerationExperimentData(
		// "/Users/GunnarF/NoBackup/data-workspace/searchacceleration/greedo_acceptNegativeDissapointment",
		// 0, 10,
		// 1000);

		final int scenarioCnt = 10;

		final boolean log = true;
		final boolean legend = false;

		final AccelerationExperimentData greedo_noAgeing_msa_enforceLambda = new AccelerationExperimentData(
				"./greedo_no-ageing_msa_enforce-lambda", 0, scenarioCnt, 1000);
		final AccelerationExperimentData greedo_noAgeing_msa_freeLambda = new AccelerationExperimentData(
				"./greedo_no-ageing_msa_free-lambda", 0, scenarioCnt, 1000);
		final AccelerationExperimentData greedo_noAgeing_sqrtMsa_enforceLambda = new AccelerationExperimentData(
				"./greedo_no-ageing_sqrt-msa_enforce-lambda", 0, scenarioCnt, 1000);
		final AccelerationExperimentData greedo_noAgeing_sqrtMsa_freeLambda = new AccelerationExperimentData(
				"./greedo_no-ageing_sqrt-msa_free-lambda", 0, scenarioCnt, 1000);
		final AccelerationExperimentData greedo_noAgeing_sqrtMsa_enforceLambda_congested = new AccelerationExperimentData(
				"./greedo_no-ageing_sqrt-msa_enforce-lambda_congested", 0, scenarioCnt, 1000);
		// final AccelerationExperimentData
		// greedo_noAgeing_sqrtMsa_enforceLambda_veryCongested = new
		// AccelerationExperimentData(
		// "./greedo_no-ageing_sqrt-msa_enforce-lambda_very-congested", 0, scenarioCnt,
		// 1000);
		final AccelerationExperimentData greedo_withAgeing_msa_enforceLambda = new AccelerationExperimentData(
				"./greedo_with-ageing_msa_enforce-lambda", 0, scenarioCnt, 1000);
		final AccelerationExperimentData greedo_withAgeing_msa_freeLambda = new AccelerationExperimentData(
				"./greedo_with-ageing_msa_free-lambda", 0, scenarioCnt, 1000);
		final AccelerationExperimentData greedo_withAgeing_sqrtMsa_enforceLambda = new AccelerationExperimentData(
				"./greedo_with-ageing_sqrt-msa_enforce-lambda", 0, scenarioCnt, 1000);
		final AccelerationExperimentData greedo_withAgeing_sqrtMsa_freeLambda = new AccelerationExperimentData(
				"./greedo_with-ageing_sqrt-msa_free-lambda", 0, scenarioCnt, 1000);
		final AccelerationExperimentData greedo_withAgeing_sqrtMsa_enforceLambda_congested = new AccelerationExperimentData(
				"./greedo_with-ageing_sqrt-msa_enforce-lambda_congested", 0, scenarioCnt, 1000);
		// final AccelerationExperimentData
		// greedo_withAgeing_sqrtMsa_enforceLambda_veryCongested = new
		// AccelerationExperimentData(
		// "./greedo_with-ageing_sqrt-msa_enforce-lambda_very-congested", 0,
		// scenarioCnt, 1000);

		final AccelerationExperimentData sbayti2007MSA = new AccelerationExperimentData("./Sbayti2007_MSA", 0,
				scenarioCnt, 1000);
		final AccelerationExperimentData sbayti2007SqrtMSA = new AccelerationExperimentData("./Sbayti2007_sqrt_MSA", 0,
				scenarioCnt, 1000);
		final AccelerationExperimentData sbayti2007SqrtMSACongested = new AccelerationExperimentData(
				"./Sbayti2007_sqrt_MSA_congested", 0, scenarioCnt, 1000);
		// final AccelerationExperimentData sbayti2007SqrtMSAVeryCongested = new
		// AccelerationExperimentData(
		// "./Sbayti2007_sqrt_MSA_very-congested", 0, scenarioCnt, 1000);

		final AccelerationExperimentData adaptiveMSA = new AccelerationExperimentData("./adaptive_MSA", 0, scenarioCnt,
				1000);
		final AccelerationExperimentData sqrtMSA = new AccelerationExperimentData("./sqrt_MSA", 0, scenarioCnt, 1000);
		final AccelerationExperimentData vanillaMSA = new AccelerationExperimentData("./vanilla_MSA", 0, scenarioCnt,
				1000);

		{
			final PairwiseTTest tTests = new PairwiseTTest();
			tTests.addData(greedo_noAgeing_sqrtMsa_enforceLambda.newExpectedUtilityChangesFinalPoints(),
					"proposed, no ageing, sqrtMSA");
			tTests.addData(greedo_withAgeing_sqrtMsa_enforceLambda.newExpectedUtilityChangesFinalPoints(),
					"proposed, with ageing, sqrtMSA");

			tTests.addData(sbayti2007MSA.newExpectedUtilityChangesFinalPoints(), "Sbayti (2007), MSA");
			tTests.addData(sbayti2007SqrtMSA.newExpectedUtilityChangesFinalPoints(), "Sbayti (2007), sqrt MSA");

			tTests.addData(adaptiveMSA.newExpectedUtilityChangesFinalPoints(), "adaptive MSA");
			tTests.addData(sqrtMSA.newExpectedUtilityChangesFinalPoints(), "sqrt MSA");
			tTests.addData(vanillaMSA.newExpectedUtilityChangesFinalPoints(), "vanilla MSA");

			try {
				PrintWriter writer = new PrintWriter("tTests.txt");
				writer.print(tTests.toString());
				writer.flush();
				writer.close();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}

		{
			final PairwiseTTest tTestsCongested = new PairwiseTTest();
			tTestsCongested.addData(
					greedo_noAgeing_sqrtMsa_enforceLambda_congested.newExpectedUtilityChangesFinalPoints(),
					"proposed, no ageing, sqrtMSA, congested");
			tTestsCongested.addData(
					greedo_withAgeing_sqrtMsa_enforceLambda_congested.newExpectedUtilityChangesFinalPoints(),
					"proposed, with ageing, sqrt, congested");
			tTestsCongested.addData(sbayti2007SqrtMSACongested.newExpectedUtilityChangesFinalPoints(),
					"Sbayti (2007), MSA, congested");

			try {
				PrintWriter writer = new PrintWriter("tTests_congested.txt");
				writer.print(tTestsCongested.toString());
				writer.flush();
				writer.close();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}

		// UTILITIES STARTING HERE ==========================================

		{ // REALIZED UTILITY, MSA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			plot.setLog(log);
			plot.addSeries(vanillaMSA.newRealizedUtilitiesSeries("vanilla MSA"));
			plot.addSeries(sqrtMSA.newRealizedUtilitiesSeries("sqrt MSA"));
			plot.addSeries(adaptiveMSA.newRealizedUtilitiesSeries("adaptive MSA"));
			if (legend) {
				plot.addLegend(100, -150, 100, 25);
			}
			if (log) {
			} else {
				plot.setRange(0, 1000, -250, 0);
			}
			plot.render("realized_utilities_msa.tex");
		}

		{ // REALIZED UTILITY, SBAYTI
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			plot.setLog(log);
			plot.addSeries(sbayti2007MSA.newRealizedUtilitiesSeries("with MSA"));
			plot.addSeries(sbayti2007SqrtMSA.newRealizedUtilitiesSeries("with sqrt MSA"));
			if (legend) {
				plot.addLegend(100, -150, 100, 25);
			}
			if (log) {
			} else {
				plot.setRange(0, 1000, -250, 0);
			}
			plot.render("realized_utilities_sbayti.tex");
		}

		{ // REALIZED UTILITY, PROPOSED, WITH AGEING, FREE LAMBDA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			plot.setLog(log);
			plot.addSeries(greedo_withAgeing_msa_freeLambda.newRealizedUtilitiesSeries("with MSA"));
			plot.addSeries(greedo_withAgeing_sqrtMsa_freeLambda.newRealizedUtilitiesSeries("with sqrt-MSA"));
			if (legend) {
				plot.addLegend(100, -150, 100, 25);
			}
			if (log) {
			} else {
				plot.setRange(0, 1000, -250, 0);
			}
			plot.render("realized_utilities_greedo_withAgeing_freeLambda.tex");
		}

		{ // REALIZED UTILITY, PROPOSED, WITH AGEING, ENFORCED LAMBDA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			plot.setLog(log);
			plot.addSeries(greedo_withAgeing_msa_enforceLambda.newRealizedUtilitiesSeries("with MSA"));
			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda.newRealizedUtilitiesSeries("with sqrt-MSA"));
			if (legend) {
				plot.addLegend(100, -150, 100, 25);
			}
			if (log) {
			} else {
				plot.setRange(0, 1000, -250, 0);
			}
			plot.render("realized_utilities_greedo_withAgeing_enforcedLambda.tex");
		}

		{ // REALIZED UTILITY, PROPOSED, NO AGEING, FREE LAMBDA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			plot.setLog(log);
			plot.addSeries(greedo_noAgeing_msa_freeLambda.newRealizedUtilitiesSeries("with MSA"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_freeLambda.newRealizedUtilitiesSeries("with sqrt-MSA"));
			if (legend) {
				plot.addLegend(100, -150, 100, 25);
			}
			if (log) {
			} else {
				plot.setRange(0, 1000, -250, 0);
			}
			plot.render("realized_utilities_greedo_noAgeing_freeLambda.tex");
		}

		{ // REALIZED UTILITY, PROPOSED, NO AGEING, ENFORCED LAMBDA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			plot.setLog(log);
			plot.addSeries(greedo_noAgeing_msa_enforceLambda.newRealizedUtilitiesSeries("with MSA"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda.newRealizedUtilitiesSeries("with sqrt-MSA"));
			if (legend) {
				plot.addLegend(100, -150, 100, 25);
			}
			if (log) {
			} else {
				plot.setRange(0, 1000, -250, 0);
			}
			plot.render("realized_utilities_greedo_noAgeing_enforcedLambda.tex");
		}

		// UTILITY GAPS STARTING HERE ==========================================

		{ // MSA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			plot.setLog(log);
			plot.addSeries(vanillaMSA.newExpectedUtilityChangesSeries("vanilla MSA"));
			plot.addSeries(sqrtMSA.newExpectedUtilityChangesSeries("sqrt MSA"));
			plot.addSeries(adaptiveMSA.newExpectedUtilityChangesSeries("adaptive MSA"));
			if (legend) {
				plot.addLegend(100, -150, 100, 25);
			}
			if (log) {
			} else {
				plot.setRange(0, 1000, 0, 100);
			}
			plot.render("utility_gaps_msa.tex");
		}

		{ // SBAYTI
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			plot.setLog(log);
			plot.addSeries(sbayti2007MSA.newExpectedUtilityChangesSeries("with MSA"));
			plot.addSeries(sbayti2007SqrtMSA.newExpectedUtilityChangesSeries("with sqrt MSA"));
			if (legend) {
				plot.addLegend(100, -150, 100, 25);
			}
			if (log) {
			} else {
				plot.setRange(0, 1000, 0, 100);
			}
			plot.render("utility_gaps_sbayti.tex");
		}

		{ // PROPOSED, WITH AGEING, FREE LAMBDA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			plot.setLog(log);
			plot.addSeries(greedo_withAgeing_msa_freeLambda.newExpectedUtilityChangesSeries("with MSA"));
			plot.addSeries(greedo_withAgeing_sqrtMsa_freeLambda.newExpectedUtilityChangesSeries("with sqrt-MSA"));
			if (legend) {
				plot.addLegend(100, -150, 100, 25);
			}
			if (log) {
			} else {
				plot.setRange(0, 1000, -250, 0);
			}
			plot.render("utility_gaps_greedo_withAgeing_freeLambda.tex");
		}

		{ // PROPOSED, WITH AGEING, ENFORCED LAMBDA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			plot.setLog(log);
			plot.addSeries(greedo_withAgeing_msa_enforceLambda.newExpectedUtilityChangesSeries("with MSA"));
			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda.newExpectedUtilityChangesSeries("with sqrt-MSA"));
			if (legend) {
				plot.addLegend(100, -150, 100, 25);
			}
			if (log) {
			} else {
				plot.setRange(0, 1000, 0, 100);
			}
			plot.render("utility_gaps_greedo_withAgeing_enforcedLambda.tex");
		}

		{ // PROPOSED, NO AGEING, FREE LAMBDA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			plot.setLog(log);
			plot.addSeries(greedo_noAgeing_msa_freeLambda.newExpectedUtilityChangesSeries("with MSA"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_freeLambda.newExpectedUtilityChangesSeries("with sqrt-MSA"));
			if (legend) {
				plot.addLegend(100, -150, 100, 25);
			}
			if (log) {
			} else {
				plot.setRange(0, 1000, 0, 100);
			}
			plot.render("utility_gaps_greedo_noAgeing_freeLambda.tex");
		}

		{ // PROPOSED, NO AGEING, ENFORCED LAMBDA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			plot.setLog(log);
			plot.addSeries(greedo_noAgeing_msa_enforceLambda.newExpectedUtilityChangesSeries("with MSA"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda.newExpectedUtilityChangesSeries("with sqrt-MSA"));
			if (legend) {
				plot.addLegend(100, -150, 100, 25);
			}
			if (log) {
			} else {
				plot.setRange(0, 1000, 0, 100);
			}
			plot.render("utility_gaps_greedo_noAgeing_enforcedLambda.tex");
		}

		// AGE PERCENTILES ==================================================

		{ // AGE PERCENTILES, SBAYTI, SQRTMSA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			plot.setLog(false);
			plot.addSeries(sbayti2007SqrtMSA.newAgePercentile90Series("90 percentile"));
			plot.addSeries(sbayti2007SqrtMSA.newAgePercentile60Series("60 percentile"));
			plot.addSeries(sbayti2007SqrtMSA.newAgePercentile30Series("30 percentile"));
			if (legend) {
				plot.addLegend(100, -150, 100, 25);
			}
			plot.setRange(0, 1000, 0, 1000);
			plot.render("agePercentiles_sbayti_sqrtMSA.tex");
		}

		{ // AGE PERCENTILES, GREEDO, WITH AGEING, sqrtMSA, enforcedLambda
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			plot.setLog(false);
			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda.newAgePercentile90Series("90 percentile"));
			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda.newAgePercentile60Series("60 percentile"));
			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda.newAgePercentile30Series("30 percentile"));
			if (legend) {
				plot.addLegend(100, -150, 100, 25);
			}
			plot.setRange(0, 1000, 0, 1000);
			plot.render("agePercentiles_greedo_withAgeing_sqrtMSA_enforcedLambda.tex");
		}

		{ // AGE PERCENTILES, GREEDO, NO AGEING, sqrtMSA, enforcedLambda
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			plot.setLog(false);
			plot.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda.newAgePercentile90Series("90 percentile"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda.newAgePercentile60Series("60 percentile"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda.newAgePercentile30Series("30 percentile"));
			if (legend) {
				plot.addLegend(100, -150, 100, 25);
			}
			plot.setRange(0, 1000, 0, 1000);
			plot.render("agePercentiles_greedo_noAgeing_sqrtMSA_enforcedLambda.tex");
		}

		System.out.println("...DONE");
	}

}
