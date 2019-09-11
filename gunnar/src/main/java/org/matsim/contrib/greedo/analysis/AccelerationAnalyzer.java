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

		final AccelerationExperimentData greedo_noAgeing_sqrtMsa_enforceLambda = new AccelerationExperimentData(
				"./greedo_no-ageing_sqrt-msa_enforce-lambda", 0, scenarioCnt, 1000);
		final AccelerationExperimentData greedo_noAgeing_sqrtMsa_enforceLambda_congested = new AccelerationExperimentData(
				"./greedo_no-ageing_sqrt-msa_enforce-lambda_congested", 0, scenarioCnt, 1000);
		final AccelerationExperimentData greedo_noAgeing_sqrtMsa_enforceLambda_veryCongested = new AccelerationExperimentData(
				"./greedo_no-ageing_sqrt-msa_enforce-lambda_very-congested", 0, scenarioCnt, 1000);
		final AccelerationExperimentData greedo_withAgeing_sqrtMsa_enforceLambda = new AccelerationExperimentData(
				"./greedo_with-ageing_sqrt-msa_enforce-lambda", 0, scenarioCnt, 1000);
		final AccelerationExperimentData greedo_withAgeing_sqrtMsa_enforceLambda_congested = new AccelerationExperimentData(
				"./greedo_with-ageing_sqrt-msa_enforce-lambda_congested", 0, scenarioCnt, 1000);
		final AccelerationExperimentData greedo_withAgeing_sqrtMsa_enforceLambda_veryCongested = new AccelerationExperimentData(
				"./greedo_with-ageing_sqrt-msa_enforce-lambda_very-congested", 0, scenarioCnt, 1000);

		final AccelerationExperimentData sbayti2007MSA = new AccelerationExperimentData("./Sbayti2007_MSA", 0,
				scenarioCnt, 1000);
		final AccelerationExperimentData sbayti2007SqrtMSA = new AccelerationExperimentData("./Sbayti2007_sqrt_MSA", 0,
				scenarioCnt, 1000);
		final AccelerationExperimentData sbayti2007SqrtMSACongested = new AccelerationExperimentData(
				"./Sbayti2007_sqrt_MSA_congested", 0, scenarioCnt, 1000);
		final AccelerationExperimentData sbayti2007SqrtMSAVeryCongested = new AccelerationExperimentData(
				"./Sbayti2007_sqrt_MSA_very-congested", 0, scenarioCnt, 1000);

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

		{ // REALIZED UTILITY, MSA
			final AccelerationAnalysisLinePlot plot = new AccelerationAnalysisLinePlot();
			plot.addSeries(vanillaMSA.newRealizedUtilitiesSeries("vanilla MSA"));
			plot.addSeries(sqrtMSA.newRealizedUtilitiesSeries("sqrt MSA"));
			plot.addSeries(adaptiveMSA.newRealizedUtilitiesSeries("adaptive MSA"));
			plot.addLegend(500, -150, 100, 25);
			plot.setRange(0, 1000, -250, 0);
			plot.render("realized_utilities_msa.tex");
		}

		{ // REALIZED UTILITY, PROPOSED
			final AccelerationAnalysisLinePlot plot = new AccelerationAnalysisLinePlot();
			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda
					.newRealizedUtilitiesSeries("proposed, with ageing, sqrtMSA"));
			plot.addSeries(
					greedo_noAgeing_sqrtMsa_enforceLambda.newRealizedUtilitiesSeries("proposed, no ageing, sqrt MSA"));
			plot.addLegend(500, -150, 100, 25);
			plot.setRange(0, 1000, -250, 0);
			plot.render("realized_utilities_proposed.tex");
		}

		{ // REALIZED UTILITY, SBAYTI
			final AccelerationAnalysisLinePlot plot = new AccelerationAnalysisLinePlot();
			plot.addSeries(sbayti2007MSA.newRealizedUtilitiesSeries("Sbayti (2007), MSA"));
			plot.addSeries(sbayti2007SqrtMSA.newRealizedUtilitiesSeries("Sbayti (2007), sqrt MSA"));
			plot.addLegend(500, -150, 100, 25);
			plot.setRange(0, 1000, -250, 0);
			plot.render("realized_utilities_sbayti.tex");
		}

		{ // REALIZED UTILITY, PROPOSED, CONGESTED
			final AccelerationAnalysisLinePlot plot = new AccelerationAnalysisLinePlot();
			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda_congested
					.newRealizedUtilitiesSeries("proposed, with ageing, sqrtMSA, congested"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda_congested
					.newRealizedUtilitiesSeries("proposed, no ageing, sqrt MSA, congested"));
			plot.addLegend(500, -150, 100, 25);
			plot.setRange(0, 1000, -250, 0);
			plot.render("realized_utilities_proposed_congested.tex");
		}

		{ // REALIZED UTILITY, SBAYTI, CONGESTED
			final AccelerationAnalysisLinePlot plot = new AccelerationAnalysisLinePlot();
			plot.addSeries(sbayti2007SqrtMSACongested.newRealizedUtilitiesSeries("Sbayti (2007), MSA, congested"));
			plot.addSeries(sbayti2007SqrtMSACongested.newRealizedUtilitiesSeries("Sbayti (2007), sqrt MSA, congested"));
			plot.addLegend(500, -150, 100, 25);
			plot.setRange(0, 1000, -250, 0);
			plot.render("realized_utilities_sbayti_congested.tex");
		}

		{ // REALIZED UTILITY, PROPOSED, VERY CONGESTED
			final AccelerationAnalysisLinePlot plot = new AccelerationAnalysisLinePlot();
			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda_veryCongested
					.newRealizedUtilitiesSeries("proposed, with ageing, sqrtMSA, very congested"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda_veryCongested
					.newRealizedUtilitiesSeries("proposed, no ageing, sqrt MSA, very congested"));
			plot.addLegend(500, -150, 100, 25);
			plot.setRange(0, 1000, -250, 0);
			plot.render("realized_utilities_proposed_very-congested.tex");
		}

		{ // REALIZED UTILITY, SBAYTI, VERY CONGESTED
			final AccelerationAnalysisLinePlot plot = new AccelerationAnalysisLinePlot();
			plot.addSeries(
					sbayti2007SqrtMSAVeryCongested.newRealizedUtilitiesSeries("Sbayti (2007), MSA, very congested"));
			plot.addSeries(sbayti2007SqrtMSAVeryCongested
					.newRealizedUtilitiesSeries("Sbayti (2007), sqrt MSA, very congested"));
			plot.addLegend(500, -150, 100, 25);
			plot.setRange(0, 1000, -250, 0);
			plot.render("realized_utilities_sbayti_very-congested.tex");
		}

		{ // REALIZED UTILITY GAPS, MSA
			final AccelerationAnalysisLinePlot plot = new AccelerationAnalysisLinePlot();
			plot.addSeries(vanillaMSA.newExpectedUtilityChangesSeries("vanilla MSA"));
			plot.addSeries(sqrtMSA.newExpectedUtilityChangesSeries("sqrt MSA"));
			plot.addSeries(adaptiveMSA.newExpectedUtilityChangesSeries("adaptive MSA"));
			plot.addLegend(500, 150, 100, 25);
			plot.setRange(0, 1000, 0, 200);
			plot.render("utility_gaps_msa.tex");
		}

		{ // REALIZED UTILITY GAPS, PROPOSED
			final AccelerationAnalysisLinePlot plot = new AccelerationAnalysisLinePlot();
			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda
					.newExpectedUtilityChangesSeries("proposed, with ageing, sqrtMSA"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda
					.newExpectedUtilityChangesSeries("proposed, no ageing, sqrt MSA"));
			plot.addLegend(500, 150, 100, 25);
			plot.setRange(0, 1000, 0, 200);
			plot.render("utility_gaps_proposed.tex");
		}

		{ // REALIZED UTILITY GAPS, SBAYTI
			final AccelerationAnalysisLinePlot plot = new AccelerationAnalysisLinePlot();
			plot.addSeries(sbayti2007MSA.newExpectedUtilityChangesSeries("Sbayti (2007), MSA"));
			plot.addSeries(sbayti2007SqrtMSA.newExpectedUtilityChangesSeries("Sbayti (2007), sqrt MSA"));
			plot.addLegend(500, 150, 100, 25);
			plot.setRange(0, 1000, 0, 200);
			plot.render("utility_gaps_sbayti.tex");
		}

		{ // REALIZED UTILITY GAPS, PROPOSED, CONGESTED
			final AccelerationAnalysisLinePlot plot = new AccelerationAnalysisLinePlot();
			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda_congested
					.newExpectedUtilityChangesSeries("proposed, with ageing, sqrtMSA, congested"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda_congested
					.newExpectedUtilityChangesSeries("proposed, no ageing, sqrt MSA, congested"));
			plot.addLegend(500, 150, 100, 25);
			plot.setRange(0, 1000, 0, 200);
			plot.render("utility_gaps_proposed_congested.tex");
		}

		{ // REALIZED UTILITY GAPS, SBAYTI, CONGESTED
			final AccelerationAnalysisLinePlot plot = new AccelerationAnalysisLinePlot();
			plot.addSeries(sbayti2007SqrtMSACongested.newExpectedUtilityChangesSeries("Sbayti (2007), MSA, congested"));
			plot.addSeries(
					sbayti2007SqrtMSACongested.newExpectedUtilityChangesSeries("Sbayti (2007), sqrt MSA, congested"));
			plot.addLegend(500, 150, 100, 25);
			plot.setRange(0, 1000, 0, 200);
			plot.render("utility_gaps_sbayti_congested.tex");
		}

		{ // REALIZED UTILITY GAPS, PROPOSED, VERY CONGESTED
			final AccelerationAnalysisLinePlot plot = new AccelerationAnalysisLinePlot();
			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda_veryCongested
					.newExpectedUtilityChangesSeries("proposed, with ageing, sqrtMSA, very congested"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda_veryCongested
					.newExpectedUtilityChangesSeries("proposed, no ageing, sqrt MSA, very congested"));
			plot.addLegend(500, 150, 100, 25);
			plot.setRange(0, 1000, 0, 200);
			plot.render("utility_gaps_proposed_very-congested.tex");
		}

		{ // REALIZED UTILITY GAPS, SBAYTI, VERY CONGESTED
			final AccelerationAnalysisLinePlot plot = new AccelerationAnalysisLinePlot();
			plot.addSeries(sbayti2007SqrtMSAVeryCongested
					.newExpectedUtilityChangesSeries("Sbayti (2007), MSA, very congested"));
			plot.addSeries(sbayti2007SqrtMSAVeryCongested
					.newExpectedUtilityChangesSeries("Sbayti (2007), sqrt MSA, very congested"));
			plot.addLegend(500, 150, 100, 25);
			plot.setRange(0, 1000, 0, 200);
			plot.render("utility_gaps_sbayti_very-congested.tex");
		}

		/*
		 * { // AGE PERCENTILES final YIntervalSeriesCollection dataset = new
		 * YIntervalSeriesCollection();
		 * 
		 * final YIntervalSeries agePercentile10AcceptNegativeDisappointment =
		 * acceptNegativeDisappointment .newAgePercentile10Series("10% unconstrained");
		 * final YIntervalSeries agePercentile50AcceptNegativeDisappointment =
		 * acceptNegativeDisappointment .newAgePercentile50Series("50% unconstrained");
		 * final YIntervalSeries agePercentile90AcceptNegativeDisappointment =
		 * acceptNegativeDisappointment .newAgePercentile90Series("90% unconstrained");
		 * 
		 * dataset.addSeries(agePercentile10AcceptNegativeDisappointment);
		 * dataset.addSeries(agePercentile50AcceptNegativeDisappointment);
		 * dataset.addSeries(agePercentile90AcceptNegativeDisappointment);
		 * 
		 * final YIntervalSeries agePercentile10RejectNegativeDisappointment =
		 * rejectNegativeDisappointment .newAgePercentile10Series("10% constrained");
		 * final YIntervalSeries agePercentile50RejectNegativeDisappointment =
		 * rejectNegativeDisappointment .newAgePercentile50Series("50% constrained");
		 * final YIntervalSeries agePercentile90RejectNegativeDisappointment =
		 * rejectNegativeDisappointment .newAgePercentile90Series("90% constrained");
		 * 
		 * dataset.addSeries(agePercentile10RejectNegativeDisappointment);
		 * dataset.addSeries(agePercentile50RejectNegativeDisappointment);
		 * dataset.addSeries(agePercentile90RejectNegativeDisappointment);
		 * 
		 * final JFreeChart chart = ChartFactory.createXYLineChart("age percentiles", //
		 * chart title "iteration", // x axis label "percentile", // y axis label
		 * dataset, // data PlotOrientation.VERTICAL, true, // include legend false, //
		 * tooltips false // urls ); chart.setBackgroundPaint(null);
		 * 
		 * final XYPlot plot = (XYPlot) chart.getPlot(); plot.setBackgroundPaint(null);
		 * 
		 * final DeviationRenderer renderer = new DeviationRenderer(true, false);
		 * renderer.setSeriesStroke(0, new BasicStroke(3.0f, BasicStroke.CAP_ROUND,
		 * BasicStroke.JOIN_ROUND)); renderer.setSeriesStroke(1, new BasicStroke(3.0f,
		 * BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		 * renderer.setSeriesFillPaint(0, new Color(255, 200, 200));
		 * renderer.setSeriesFillPaint(1, new Color(200, 200, 255));
		 * plot.setRenderer(renderer);
		 * 
		 * // final NumberAxis yAxis = (NumberAxis) plot.getRangeAxis(); //
		 * yAxis.setAutoRangeIncludesZero(false); //
		 * yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		 * 
		 * save(chart, "age percentiles"); }
		 */

		/*
		 * { // UTILITY GAP COMPARISON final YIntervalSeries
		 * expectedUtilityChangesAcceptNegativeDisappointment =
		 * acceptNegativeDisappointment .newExpectedUtilityChangesSeries("proposed");
		 * final YIntervalSeries
		 * expectedUtilityChangesAcceptNegativeDisappointmentCongested =
		 * acceptNegativeDisappointmentCongested
		 * .newExpectedUtilityChangesSeries("proposed, congested"); // final
		 * YIntervalSeries expectedUtilityChangesRejectNegativeDisappointment = //
		 * rejectNegativeDisappointment //
		 * .newExpectedUtilityChangesSeries("constrained"); final YIntervalSeries
		 * expectedUtilityChangesAdaptiveMSA = adaptiveMSA
		 * .newExpectedUtilityChangesSeries("adaptive MSA"); final YIntervalSeries
		 * expectedUtilityChangesSqrtMSA =
		 * sqrtMSA.newExpectedUtilityChangesSeries("sqrt MSA"); final YIntervalSeries
		 * expectedUtilityChangesVanillaMSA = vanillaMSA
		 * .newExpectedUtilityChangesSeries("vanilla MSA"); final YIntervalSeries
		 * expectedUtilityChangesSbayti2007MSA = sbayti2007MSA
		 * .newExpectedUtilityChangesSeries("Sbayti (2007), MSA"); final YIntervalSeries
		 * expectedUtilityChangesSbayti2007SqrtMSA = sbayti2007SqrtMSA
		 * .newExpectedUtilityChangesSeries("Sbayti (2007), sqrt MSA"); final
		 * YIntervalSeries expectedUtilityChangesSbayti2007SqrtMSACongested =
		 * sbayti2007SqrtMSACongested
		 * .newExpectedUtilityChangesSeries("Sbayti (2007), sqrt MSA, congested");
		 * 
		 * final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
		 * dataset.addSeries(expectedUtilityChangesAcceptNegativeDisappointment);
		 * dataset.addSeries(expectedUtilityChangesAcceptNegativeDisappointmentCongested
		 * ); dataset.addSeries(expectedUtilityChangesAdaptiveMSA);
		 * dataset.addSeries(expectedUtilityChangesSqrtMSA);
		 * dataset.addSeries(expectedUtilityChangesVanillaMSA);
		 * dataset.addSeries(expectedUtilityChangesSbayti2007MSA);
		 * dataset.addSeries(expectedUtilityChangesSbayti2007SqrtMSA);
		 * dataset.addSeries(expectedUtilityChangesSbayti2007SqrtMSACongested);
		 * 
		 * final JFreeChart chart =
		 * ChartFactory.createXYLineChart("expected utility changes", // chart title
		 * "iteration", // x axis label "utility change", // y axis label dataset, //
		 * data PlotOrientation.VERTICAL, true, // include legend false, // tooltips
		 * false // urls ); chart.setBackgroundPaint(null);
		 * 
		 * final XYPlot plot = (XYPlot) chart.getPlot(); plot.setBackgroundPaint(null);
		 * 
		 * final DeviationRenderer renderer = new DeviationRenderer(true, false);
		 * renderer.setSeriesStroke(0, new BasicStroke(3.0f, BasicStroke.CAP_ROUND,
		 * BasicStroke.JOIN_ROUND)); renderer.setSeriesStroke(1, new BasicStroke(3.0f,
		 * BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		 * renderer.setSeriesFillPaint(0, new Color(255, 200, 200));
		 * renderer.setSeriesFillPaint(1, new Color(200, 200, 255));
		 * plot.setRenderer(renderer);
		 * 
		 * // final NumberAxis yAxis = (NumberAxis) plot.getRangeAxis(); //
		 * yAxis.setAutoRangeIncludesZero(false); //
		 * yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		 * 
		 * save(chart, "utility gap comparison"); }
		 */

		/*
		 * { // REALIZED LAMBDA COMPARISON final YIntervalSeries realizedLambdasProposed
		 * = acceptNegativeDisappointment .newRealizedLambdaSeries("proposed"); final
		 * YIntervalSeries realizedLambdasAdaptiveMSA =
		 * adaptiveMSA.newRealizedLambdaSeries("adaptive MSA"); final YIntervalSeries
		 * realizedLambdasSqrtMSA = sqrtMSA.newRealizedLambdaSeries("sqrt MSA"); final
		 * YIntervalSeries realizedLambdasVanillaMSA =
		 * vanillaMSA.newRealizedLambdaSeries("vanilla MSA"); final YIntervalSeries
		 * realizedLambdasSbayti2007MSA = sbayti2007MSA
		 * .newRealizedLambdaSeries("Sbayti (2007), MSA"); final YIntervalSeries
		 * realizedLambdasSbayti2007SqrtMSA = sbayti2007SqrtMSA
		 * .newRealizedLambdaSeries("Sbayti (2007), sqrt MSA");
		 * 
		 * final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
		 * dataset.addSeries(realizedLambdasProposed);
		 * dataset.addSeries(realizedLambdasAdaptiveMSA);
		 * dataset.addSeries(realizedLambdasSqrtMSA);
		 * dataset.addSeries(realizedLambdasVanillaMSA);
		 * dataset.addSeries(realizedLambdasSbayti2007MSA);
		 * dataset.addSeries(realizedLambdasSbayti2007SqrtMSA);
		 * 
		 * final JFreeChart chart =
		 * ChartFactory.createXYLineChart("realized lambdas comparison", // chart title
		 * "iteration", // x axis label "lambda value", // y axis label dataset, // data
		 * PlotOrientation.VERTICAL, true, // include legend false, // tooltips false //
		 * urls ); chart.setBackgroundPaint(null);
		 * 
		 * final XYPlot plot = (XYPlot) chart.getPlot(); plot.setBackgroundPaint(null);
		 * 
		 * final DeviationRenderer renderer = new DeviationRenderer(true, false);
		 * renderer.setSeriesStroke(0, new BasicStroke(3.0f, BasicStroke.CAP_ROUND,
		 * BasicStroke.JOIN_ROUND)); renderer.setSeriesStroke(1, new BasicStroke(3.0f,
		 * BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		 * renderer.setSeriesFillPaint(0, new Color(255, 200, 200));
		 * renderer.setSeriesFillPaint(1, new Color(200, 200, 255));
		 * plot.setRenderer(renderer);
		 * 
		 * // final NumberAxis yAxis = (NumberAxis) plot.getRangeAxis(); //
		 * yAxis.setAutoRangeIncludesZero(false); //
		 * yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		 * 
		 * save(chart, "realized lambdas comparison"); }
		 */

		System.out.println("...DONE");
	}

}
