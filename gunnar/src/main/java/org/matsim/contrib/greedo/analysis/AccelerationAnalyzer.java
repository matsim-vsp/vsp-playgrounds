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

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;

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
		final AccelerationExperimentData greedo_withAgeing_sqrtMsa_enforceLambda = new AccelerationExperimentData(
				"./greedo_with-ageing_sqrt-msa_enforce-lambda", 0, scenarioCnt, 1000);
		final AccelerationExperimentData greedo_withAgeing_sqrtMsa_enforceLambda_congested = new AccelerationExperimentData(
				"./greedo_with-ageing_sqrt-msa_enforce-lambda_congested", 0, scenarioCnt, 1000);

		final AccelerationExperimentData sbayti2007MSA = new AccelerationExperimentData("./Sbayti2007_MSA", 0,
				scenarioCnt, 1000);
		final AccelerationExperimentData sbayti2007SqrtMSA = new AccelerationExperimentData("./Sbayti2007_sqrt_MSA", 0,
				scenarioCnt, 1000);
		final AccelerationExperimentData sbayti2007SqrtMSACongested = new AccelerationExperimentData(
				"./Sbayti2007_sqrt_MSA_congested", 0, scenarioCnt, 1000);

		final AccelerationExperimentData adaptiveMSA = new AccelerationExperimentData("./adaptive_MSA", 0, scenarioCnt,
				1000);
		final AccelerationExperimentData sqrtMSA = new AccelerationExperimentData("./sqrt_MSA", 0, scenarioCnt, 1000);
		final AccelerationExperimentData vanillaMSA = new AccelerationExperimentData("./vanilla_MSA", 0, scenarioCnt,
				1000);

		/*
		 * { // BETAS final YIntervalSeries betasAcceptNegativeDissappointment =
		 * acceptNegativeDisappointment .newBetaSeries("unconstrained"); final
		 * YIntervalSeries betasRejectNegativeDissappointment =
		 * rejectNegativeDisappointment .newBetaSeries("constrained");
		 * 
		 * final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
		 * dataset.addSeries(betasAcceptNegativeDissappointment);
		 * dataset.addSeries(betasRejectNegativeDissappointment);
		 * 
		 * final JFreeChart chart = ChartFactory.createXYLineChart("beta", // chart
		 * title "iteration", // x axis label "beta value", // y axis label dataset, //
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
		 * save(chart, "betas"); }
		 */

		/*
		 * { // REALIZED LAMBDA (COMPARISON) final YIntervalSeries
		 * realizedLambdasAcceptNegativeDisappointment = acceptNegativeDisappointment
		 * .newRealizedLambdaSeries("unconstrained"); final YIntervalSeries
		 * realizedLambdasRejectNegativeDisappointment = rejectNegativeDisappointment
		 * .newRealizedLambdaSeries("constrained");
		 * 
		 * final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
		 * dataset.addSeries(realizedLambdasAcceptNegativeDisappointment);
		 * dataset.addSeries(realizedLambdasRejectNegativeDisappointment);
		 * 
		 * final JFreeChart chart = ChartFactory.createXYLineChart("realized lambda", //
		 * chart title "iteration", // x axis label "lambda value", // y axis label
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
		 * save(chart, "realized-lambdas"); }
		 */

		{ // REALIZED UTILITY

			final YIntervalSeries greedo_noAgeing_sqrtMsa_enforceLambda_data = greedo_noAgeing_sqrtMsa_enforceLambda
					.newRealizedUtilitiesSeries("proposed, no ageing, sqrt MSA");
			final YIntervalSeries greedo_withAgeing_sqrtMsa_enforceLambda_data = greedo_withAgeing_sqrtMsa_enforceLambda
					.newRealizedUtilitiesSeries("proposed, with ageing, sqrtMSA");

			final YIntervalSeries sbayti2007MSA_data = sbayti2007MSA.newRealizedUtilitiesSeries("Sbayti (2007), MSA");
			final YIntervalSeries sbayti2007SqrtMSA_data = sbayti2007SqrtMSA
					.newRealizedUtilitiesSeries("Sbayti (2007), sqrt MSA");

			final YIntervalSeries adaptiveMSA_data = adaptiveMSA.newRealizedUtilitiesSeries("adaptive MSA");
			final YIntervalSeries sqrtMSA_data = sqrtMSA.newRealizedUtilitiesSeries("sqrt MSA");
			final YIntervalSeries vanillaMSA_data = vanillaMSA.newRealizedUtilitiesSeries("vanilla MSA");

			final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
			dataset.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda_data);
			dataset.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda_data);
			dataset.addSeries(sbayti2007MSA_data);
			dataset.addSeries(sbayti2007SqrtMSA_data);
			dataset.addSeries(adaptiveMSA_data);
			dataset.addSeries(sqrtMSA_data);
			dataset.addSeries(vanillaMSA_data);

			final JFreeChart chart = ChartFactory.createXYLineChart("realized utilities",
					// chart title
					"iteration", // x axis label
					"utility value", // y axis label
					dataset, // data
					PlotOrientation.VERTICAL, true, // include legend
					false, // tooltips
					false // urls
			);
			chart.setBackgroundPaint(null);

			final XYPlot plot = (XYPlot) chart.getPlot();
			plot.setBackgroundPaint(null);

			final DeviationRenderer renderer = new DeviationRenderer(true, false);
			renderer.setSeriesStroke(0, new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			renderer.setSeriesStroke(1, new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			renderer.setSeriesFillPaint(0, new Color(255, 200, 200));
			renderer.setSeriesFillPaint(1, new Color(200, 200, 255));
			plot.setRenderer(renderer);

			// final NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
			// yAxis.setAutoRangeIncludesZero(false);
			// yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

			save(chart, "realized utilities");
		}

		{ // REALIZED UTILITY, CONGESTED

			final YIntervalSeries greedo_noAgeing_sqrtMsa_enforceLambda_congested_data = greedo_noAgeing_sqrtMsa_enforceLambda_congested
					.newRealizedUtilitiesSeries("proposed, no ageing, sqrt MSA");
			final YIntervalSeries greedo_withAgeing_sqrtMsa_enforceLambda_congested_data = greedo_withAgeing_sqrtMsa_enforceLambda_congested
					.newRealizedUtilitiesSeries("proposed, with ageing, sqrtMSA");
			final YIntervalSeries sbayti2007SqrtMSACongested_data = sbayti2007SqrtMSACongested
					.newRealizedUtilitiesSeries("Sbayti (2007), sqrt MSA");

			final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
			dataset.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda_congested_data);
			dataset.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda_congested_data);
			dataset.addSeries(sbayti2007SqrtMSACongested_data);

			final JFreeChart chart = ChartFactory.createXYLineChart("realized utilities",
					// chart title
					"iteration", // x axis label
					"utility value", // y axis label
					dataset, // data
					PlotOrientation.VERTICAL, true, // include legend
					false, // tooltips
					false // urls
			);
			chart.setBackgroundPaint(null);

			final XYPlot plot = (XYPlot) chart.getPlot();
			plot.setBackgroundPaint(null);

			final DeviationRenderer renderer = new DeviationRenderer(true, false);
			renderer.setSeriesStroke(0, new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			renderer.setSeriesStroke(1, new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			renderer.setSeriesFillPaint(0, new Color(255, 200, 200));
			renderer.setSeriesFillPaint(1, new Color(200, 200, 255));
			plot.setRenderer(renderer);

			// final NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
			// yAxis.setAutoRangeIncludesZero(false);
			// yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

			save(chart, "realized utilities congested");
		}

		{ // REALIZED UTILITY GAPS

			final YIntervalSeries greedo_noAgeing_sqrtMsa_enforceLambda_data = greedo_noAgeing_sqrtMsa_enforceLambda
					.newExpectedUtilityChangesSeries("proposed, no ageing, sqrt MSA");
			final YIntervalSeries greedo_withAgeing_sqrtMsa_enforceLambda_data = greedo_withAgeing_sqrtMsa_enforceLambda
					.newExpectedUtilityChangesSeries("proposed, with ageing, sqrtMSA");

			final YIntervalSeries sbayti2007MSA_data = sbayti2007MSA.newExpectedUtilityChangesSeries("Sbayti (2007), MSA");
			final YIntervalSeries sbayti2007SqrtMSA_data = sbayti2007SqrtMSA
					.newExpectedUtilityChangesSeries("Sbayti (2007), sqrt MSA");

			final YIntervalSeries adaptiveMSA_data = adaptiveMSA.newExpectedUtilityChangesSeries("adaptive MSA");
			final YIntervalSeries sqrtMSA_data = sqrtMSA.newExpectedUtilityChangesSeries("sqrt MSA");
			final YIntervalSeries vanillaMSA_data = vanillaMSA.newExpectedUtilityChangesSeries("vanilla MSA");

			final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
			dataset.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda_data);
			dataset.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda_data);
			dataset.addSeries(sbayti2007MSA_data);
			dataset.addSeries(sbayti2007SqrtMSA_data);
			dataset.addSeries(adaptiveMSA_data);
			dataset.addSeries(sqrtMSA_data);
			dataset.addSeries(vanillaMSA_data);

			final JFreeChart chart = ChartFactory.createXYLineChart("utility gaps",
					// chart title
					"iteration", // x axis label
					"utility value", // y axis label
					dataset, // data
					PlotOrientation.VERTICAL, true, // include legend
					false, // tooltips
					false // urls
			);
			chart.setBackgroundPaint(null);

			final XYPlot plot = (XYPlot) chart.getPlot();
			plot.setBackgroundPaint(null);

			final DeviationRenderer renderer = new DeviationRenderer(true, false);
			renderer.setSeriesStroke(0, new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			renderer.setSeriesStroke(1, new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			renderer.setSeriesFillPaint(0, new Color(255, 200, 200));
			renderer.setSeriesFillPaint(1, new Color(200, 200, 255));
			plot.setRenderer(renderer);

			// final NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
			// yAxis.setAutoRangeIncludesZero(false);
			// yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

			save(chart, "utility gaps");
		}

		{ // REALIZED UTILITY GAPS, CONGESTED

			final YIntervalSeries greedo_noAgeing_sqrtMsa_enforceLambda_congested_data = greedo_noAgeing_sqrtMsa_enforceLambda_congested
					.newExpectedUtilityChangesSeries("proposed, no ageing, sqrt MSA");
			final YIntervalSeries greedo_withAgeing_sqrtMsa_enforceLambda_congested_data = greedo_withAgeing_sqrtMsa_enforceLambda_congested
					.newExpectedUtilityChangesSeries("proposed, with ageing, sqrtMSA");
			final YIntervalSeries sbayti2007SqrtMSACongested_data = sbayti2007SqrtMSACongested
					.newExpectedUtilityChangesSeries("Sbayti (2007), sqrt MSA");

			final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
			dataset.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda_congested_data);
			dataset.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda_congested_data);
			dataset.addSeries(sbayti2007SqrtMSACongested_data);

			final JFreeChart chart = ChartFactory.createXYLineChart("utility gaps congested",
					// chart title
					"iteration", // x axis label
					"utility value", // y axis label
					dataset, // data
					PlotOrientation.VERTICAL, true, // include legend
					false, // tooltips
					false // urls
			);
			chart.setBackgroundPaint(null);

			final XYPlot plot = (XYPlot) chart.getPlot();
			plot.setBackgroundPaint(null);

			final DeviationRenderer renderer = new DeviationRenderer(true, false);
			renderer.setSeriesStroke(0, new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			renderer.setSeriesStroke(1, new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			renderer.setSeriesFillPaint(0, new Color(255, 200, 200));
			renderer.setSeriesFillPaint(1, new Color(200, 200, 255));
			plot.setRenderer(renderer);

			// final NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
			// yAxis.setAutoRangeIncludesZero(false);
			// yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

			save(chart, "utility gaps congested");
		}

		/*
		 * { // UTILITY GAP final YIntervalSeries
		 * expectedUtilityChangesAcceptNegativeDisappointment =
		 * acceptNegativeDisappointment
		 * .newExpectedUtilityChangesSeries("unconstrained"); final YIntervalSeries
		 * expectedUtilityChangesRejectNegativeDisappointment =
		 * rejectNegativeDisappointment .newExpectedUtilityChangesSeries("constrained");
		 * 
		 * final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
		 * dataset.addSeries(expectedUtilityChangesAcceptNegativeDisappointment);
		 * dataset.addSeries(expectedUtilityChangesRejectNegativeDisappointment);
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
		 * save(chart, "utility gap"); }
		 */

		/*
		 * { // PERFORMANCE CORRELATIONS final YIntervalSeries
		 * performanceCorrelationsAcceptNegativeDisappointment =
		 * acceptNegativeDisappointment
		 * .newPerformanceCorrelationSeries("unconstrained"); final YIntervalSeries
		 * performanceCorrelationsRejectNegativeDisappointment =
		 * rejectNegativeDisappointment .newPerformanceCorrelationSeries("constrained");
		 * 
		 * final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
		 * dataset.addSeries(performanceCorrelationsAcceptNegativeDisappointment);
		 * dataset.addSeries(performanceCorrelationsRejectNegativeDisappointment);
		 * 
		 * final JFreeChart chart =
		 * ChartFactory.createXYLineChart("performance correlations", // chart title
		 * "iteration", // x axis label "correlation", // y axis label dataset, // data
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
		 * save(chart, "performance correlations"); }
		 */

		/*
		 * { // AGE CORRELATIONS final YIntervalSeries
		 * ageCorrelationsAcceptNegativeDisappointment = acceptNegativeDisappointment
		 * .newAgeCorrelationSeries("unconstrained"); final YIntervalSeries
		 * ageCorrelationsRejectNegativeDisappointment = rejectNegativeDisappointment
		 * .newAgeCorrelationSeries("constrained");
		 * 
		 * final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
		 * dataset.addSeries(ageCorrelationsAcceptNegativeDisappointment);
		 * dataset.addSeries(ageCorrelationsRejectNegativeDisappointment);
		 * 
		 * final JFreeChart chart = ChartFactory.createXYLineChart("age correlations",
		 * // chart title "iteration", // x axis label "correlation", // y axis label
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
		 * save(chart, "age-correlations"); }
		 */

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
