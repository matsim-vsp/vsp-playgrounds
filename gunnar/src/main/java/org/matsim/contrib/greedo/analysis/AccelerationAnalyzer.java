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

		{ // REALIZED UTILITY, MSA
			final AccelerationAnalysisPlot plot = new AccelerationAnalysisPlot();
			plot.addSeries(vanillaMSA.newRealizedUtilitiesSeries("vanilla MSA"));
			plot.addSeries(sqrtMSA.newRealizedUtilitiesSeries("sqrt MSA"));
			plot.addSeries(adaptiveMSA.newRealizedUtilitiesSeries("adaptive MSA"));
			plot.addLegend(500, -150, 100, 25);
			plot.setRange(0, 1000, -250, 0);
			plot.render("realized_utilities_msa.tex");
		}

		{ // REALIZED UTILITY, PROPOSED
			final AccelerationAnalysisPlot plot = new AccelerationAnalysisPlot();
			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda
					.newRealizedUtilitiesSeries("proposed, with ageing, sqrtMSA"));
			plot.addSeries(
					greedo_noAgeing_sqrtMsa_enforceLambda.newRealizedUtilitiesSeries("proposed, no ageing, sqrt MSA"));
			plot.addLegend(500, -150, 100, 25);
			plot.setRange(0, 1000, -250, 0);
			plot.render("realized_utilities_proposed.tex");
		}

		{ // REALIZED UTILITY, SBAYTI
			final AccelerationAnalysisPlot plot = new AccelerationAnalysisPlot();
			plot.addSeries(sbayti2007MSA.newRealizedUtilitiesSeries("Sbayti (2007), MSA"));
			plot.addSeries(sbayti2007SqrtMSA.newRealizedUtilitiesSeries("Sbayti (2007), sqrt MSA"));
			plot.addLegend(500, -150, 100, 25);
			plot.setRange(0, 1000, -250, 0);
			plot.render("realized_utilities_sbayti.tex");
		}

//		{ // REALIZED UTILITY, CONGESTED
//
//			final AccelerationAnalysisPlot plot = new AccelerationAnalysisPlot();
//
//			plot.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda_congested
//					.newRealizedUtilitiesSeries("proposed, no ageing, sqrt MSA"));
//
//			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda_congested
//					.newRealizedUtilitiesSeries("proposed, with ageing, sqrtMSA"));
//			plot.addSeries(sbayti2007SqrtMSACongested.newRealizedUtilitiesSeries("Sbayti (2007), sqrt MSA"));
//
//			plot.render("realized_utilities_congested.tex");
//		}
//
//		{ // REALIZED UTILITY, VERY CONGESTED
//
//			final AccelerationAnalysisPlot plot = new AccelerationAnalysisPlot();
//
//			plot.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda_veryCongested
//					.newRealizedUtilitiesSeries("proposed, no ageing, sqrt MSA"));
//			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda_veryCongested
//					.newRealizedUtilitiesSeries("proposed, with ageing, sqrtMSA"));
//			plot.addSeries(sbayti2007SqrtMSAVeryCongested.newRealizedUtilitiesSeries("Sbayti (2007), sqrt MSA"));
//
//			plot.render("realized_utilities_very-congested.tex");
//		}

		{ // REALIZED UTILITY GAPS, MSA
			final AccelerationAnalysisPlot plot = new AccelerationAnalysisPlot();
			plot.addSeries(vanillaMSA.newExpectedUtilityChangesSeries("vanilla MSA"));
			plot.addSeries(sqrtMSA.newExpectedUtilityChangesSeries("sqrt MSA"));
			plot.addSeries(adaptiveMSA.newExpectedUtilityChangesSeries("adaptive MSA"));
			plot.render("utility_gaps_msa.tex");
		}

		{ // REALIZED UTILITY GAPS, PROPOSED
			final AccelerationAnalysisPlot plot = new AccelerationAnalysisPlot();
			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda
					.newExpectedUtilityChangesSeries("proposed, with ageing, sqrtMSA"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda
					.newExpectedUtilityChangesSeries("proposed, no ageing, sqrt MSA"));
			plot.render("utility_gaps_proposed.tex");
		}

		{ // REALIZED UTILITY GAPS, SBAYTI
			final AccelerationAnalysisPlot plot = new AccelerationAnalysisPlot();
			plot.addSeries(sbayti2007MSA.newExpectedUtilityChangesSeries("Sbayti (2007), MSA"));
			plot.addSeries(sbayti2007SqrtMSA.newExpectedUtilityChangesSeries("Sbayti (2007), sqrt MSA"));
			plot.render("utility_gaps_sbayti.tex");
		}

//		{ // REALIZED UTILITY GAPS, CONGESTED
//
//			final AccelerationAnalysisPlot plot = new AccelerationAnalysisPlot();
//
//			plot.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda_congested
//					.newExpectedUtilityChangesSeries("proposed, no ageing, sqrt MSA"));
//			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda_congested
//					.newExpectedUtilityChangesSeries("proposed, with ageing, sqrtMSA"));
//			plot.addSeries(sbayti2007SqrtMSACongested.newExpectedUtilityChangesSeries("Sbayti (2007), sqrt MSA"));
//
//			plot.render("utility_gaps_congested.tex");
//		}
//
//		{ // REALIZED UTILITY GAPS, VERY CONGESTED
//
//			final AccelerationAnalysisPlot plot = new AccelerationAnalysisPlot();
//
//			plot.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda_veryCongested
//					.newExpectedUtilityChangesSeries("proposed, no ageing, sqrt MSA"));
//			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda_veryCongested
//					.newExpectedUtilityChangesSeries("proposed, with ageing, sqrtMSA"));
//			plot.addSeries(sbayti2007SqrtMSAVeryCongested.newExpectedUtilityChangesSeries("Sbayti (2007), sqrt MSA"));
//
//			plot.render("utility_gaps_very-congested.tex");
//		}

//		{ // AGE CORRELATIONS
//
//			final AccelerationAnalysisPlot plot = new AccelerationAnalysisPlot();
//
//			plot.addSeries(
//					greedo_withAgeing_sqrtMsa_enforceLambda.newAgeCorrelationSeries("proposed, with ageing, sqrt MSA"));
//			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda_congested
//					.newAgeCorrelationSeries("proposed, with ageing, sqrt MSA, congested"));
//			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda_veryCongested
//					.newAgeCorrelationSeries("proposed, with ageing, sqrt MSA, very congested"));
//
//			plot.render("age-correlations.tex");
//		}

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
