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

		final AccelerationExperimentData acceptNegativeDisappointment = new AccelerationExperimentData(
				"./greedo_acceptNegativeDisappointment", 0, 10, 1000);
		final AccelerationExperimentData rejectNegativeDisappointment = new AccelerationExperimentData(
				"./greedo_rejectNegativeDisappointment", 0, 10, 1000);
		final AccelerationExperimentData adaptiveMSA = new AccelerationExperimentData("./adaptive_MSA", 0, 10, 1000);
		final AccelerationExperimentData sqrtMSA = new AccelerationExperimentData("./sqrt_MSA", 0, 10, 1000);
		final AccelerationExperimentData vanillaMSA = new AccelerationExperimentData("./vanilla_MSA", 0, 10, 1000);

		{ // BETAS
			final YIntervalSeries betasAcceptNegativeDissappointment = acceptNegativeDisappointment
					.newBetaSeries("unconstrained");
			final YIntervalSeries betasRejectNegativeDissappointment = rejectNegativeDisappointment
					.newBetaSeries("constrained");

			final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
			dataset.addSeries(betasAcceptNegativeDissappointment);
			dataset.addSeries(betasRejectNegativeDissappointment);

			final JFreeChart chart = ChartFactory.createXYLineChart("beta", // chart title
					"iteration", // x axis label
					"beta value", // y axis label
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

			save(chart, "betas");
		}

		{ // REALIZED LAMBDA (COMPARISON)
			final YIntervalSeries realizedLambdasAcceptNegativeDisappointment = acceptNegativeDisappointment
					.newRealizedLambdaSeries("unconstrained");
			final YIntervalSeries realizedLambdasRejectNegativeDisappointment = rejectNegativeDisappointment
					.newRealizedLambdaSeries("constrained");

			final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
			dataset.addSeries(realizedLambdasAcceptNegativeDisappointment);
			dataset.addSeries(realizedLambdasRejectNegativeDisappointment);

			final JFreeChart chart = ChartFactory.createXYLineChart("realized lambda", // chart title
					"iteration", // x axis label
					"lambda value", // y axis label
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

			save(chart, "realized-lambdas");
		}

		// { // REALIZED UTILITY
		// final YIntervalSeries realizedUtilities = acceptNegativeDisappointment
		// .newRealizedUtilitiesSeries("unconstrained");
		//
		// final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
		// dataset.addSeries(realizedUtilities);
		//
		// final JFreeChart chart = ChartFactory.createXYLineChart("realized utilities",
		// // chart title
		// "iteration", // x axis label
		// "utility value", // y axis label
		// dataset, // data
		// PlotOrientation.VERTICAL, true, // include legend
		// false, // tooltips
		// false // urls
		// );
		// chart.setBackgroundPaint(null);
		//
		// final XYPlot plot = (XYPlot) chart.getPlot();
		// plot.setBackgroundPaint(null);
		//
		// final DeviationRenderer renderer = new DeviationRenderer(true, false);
		// renderer.setSeriesStroke(0, new BasicStroke(3.0f, BasicStroke.CAP_ROUND,
		// BasicStroke.JOIN_ROUND));
		// renderer.setSeriesStroke(1, new BasicStroke(3.0f, BasicStroke.CAP_ROUND,
		// BasicStroke.JOIN_ROUND));
		// renderer.setSeriesFillPaint(0, new Color(255, 200, 200));
		// renderer.setSeriesFillPaint(1, new Color(200, 200, 255));
		// plot.setRenderer(renderer);
		//
		// // final NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
		// // yAxis.setAutoRangeIncludesZero(false);
		// // yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		//
		// save(chart, "realized-utilities");
		// }

		{ // UTILITY GAP
			final YIntervalSeries expectedUtilityChangesAcceptNegativeDisappointment = acceptNegativeDisappointment
					.newExpectedUtilityChangesSeries("unconstrained");
			final YIntervalSeries expectedUtilityChangesRejectNegativeDisappointment = rejectNegativeDisappointment
					.newExpectedUtilityChangesSeries("constrained");

			final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
			dataset.addSeries(expectedUtilityChangesAcceptNegativeDisappointment);
			dataset.addSeries(expectedUtilityChangesRejectNegativeDisappointment);

			final JFreeChart chart = ChartFactory.createXYLineChart("expected utility changes", // chart title
					"iteration", // x axis label
					"utility change", // y axis label
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

			save(chart, "utility gap");
		}

		{ // PERFORMANCE CORRELATIONS
			final YIntervalSeries performanceCorrelationsAcceptNegativeDisappointment = acceptNegativeDisappointment
					.newPerformanceCorrelationSeries("unconstrained");
			final YIntervalSeries performanceCorrelationsRejectNegativeDisappointment = rejectNegativeDisappointment
					.newPerformanceCorrelationSeries("constrained");

			final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
			dataset.addSeries(performanceCorrelationsAcceptNegativeDisappointment);
			dataset.addSeries(performanceCorrelationsRejectNegativeDisappointment);

			final JFreeChart chart = ChartFactory.createXYLineChart("performance correlations", // chart title
					"iteration", // x axis label
					"correlation", // y axis label
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

			save(chart, "performance correlations");
		}

		{ // AGE CORRELATIONS
			final YIntervalSeries ageCorrelationsAcceptNegativeDisappointment = acceptNegativeDisappointment
					.newAgeCorrelationSeries("unconstrained");
			final YIntervalSeries ageCorrelationsRejectNegativeDisappointment = rejectNegativeDisappointment
					.newAgeCorrelationSeries("constrained");

			final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
			dataset.addSeries(ageCorrelationsAcceptNegativeDisappointment);
			dataset.addSeries(ageCorrelationsRejectNegativeDisappointment);

			final JFreeChart chart = ChartFactory.createXYLineChart("age correlations", // chart title
					"iteration", // x axis label
					"correlation", // y axis label
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

			save(chart, "age-correlations");
		}

		{ // AGE PERCENTILES
			final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();

			final YIntervalSeries agePercentile10AcceptNegativeDisappointment = acceptNegativeDisappointment
					.newAgePercentile10Series("10% unconstrained");
			final YIntervalSeries agePercentile50AcceptNegativeDisappointment = acceptNegativeDisappointment
					.newAgePercentile50Series("50% unconstrained");
			final YIntervalSeries agePercentile90AcceptNegativeDisappointment = acceptNegativeDisappointment
					.newAgePercentile90Series("90% unconstrained");

			dataset.addSeries(agePercentile10AcceptNegativeDisappointment);
			dataset.addSeries(agePercentile50AcceptNegativeDisappointment);
			dataset.addSeries(agePercentile90AcceptNegativeDisappointment);

			final YIntervalSeries agePercentile10RejectNegativeDisappointment = rejectNegativeDisappointment
					.newAgePercentile10Series("10% constrained");
			final YIntervalSeries agePercentile50RejectNegativeDisappointment = rejectNegativeDisappointment
					.newAgePercentile50Series("50% constrained");
			final YIntervalSeries agePercentile90RejectNegativeDisappointment = rejectNegativeDisappointment
					.newAgePercentile90Series("90% constrained");

			dataset.addSeries(agePercentile10RejectNegativeDisappointment);
			dataset.addSeries(agePercentile50RejectNegativeDisappointment);
			dataset.addSeries(agePercentile90RejectNegativeDisappointment);

			final JFreeChart chart = ChartFactory.createXYLineChart("age percentiles", // chart title
					"iteration", // x axis label
					"percentile", // y axis label
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

			save(chart, "age percentiles");
		}

		{ // UTILITY GAP COMPARISON
			final YIntervalSeries expectedUtilityChangesAcceptNegativeDisappointment = acceptNegativeDisappointment
					.newExpectedUtilityChangesSeries("proposed");
			// final YIntervalSeries expectedUtilityChangesRejectNegativeDisappointment =
			// rejectNegativeDisappointment
			// .newExpectedUtilityChangesSeries("constrained");
			final YIntervalSeries expectedUtilityChangesAdaptiveMSA = adaptiveMSA
					.newExpectedUtilityChangesSeries("adaptive MSA");
			final YIntervalSeries expectedUtilityChangesSqrtMSA = sqrtMSA
					.newExpectedUtilityChangesSeries("sqrt MSA");			
			final YIntervalSeries expectedUtilityChangesVanillaMSA = vanillaMSA
					.newExpectedUtilityChangesSeries("vanilla MSA");			

			final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
			dataset.addSeries(expectedUtilityChangesAcceptNegativeDisappointment);
			dataset.addSeries(expectedUtilityChangesAdaptiveMSA);
			dataset.addSeries(expectedUtilityChangesSqrtMSA);
			dataset.addSeries(expectedUtilityChangesVanillaMSA);

			final JFreeChart chart = ChartFactory.createXYLineChart("expected utility changes", // chart title
					"iteration", // x axis label
					"utility change", // y axis label
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

			save(chart, "utility gap comparison");
		}

		{ // REALIZED LAMBDA COMPARISON
			final YIntervalSeries realizedLambdasProposed = acceptNegativeDisappointment
					.newRealizedLambdaSeries("proposed");
			final YIntervalSeries realizedLambdasAdaptiveMSA = adaptiveMSA.newRealizedLambdaSeries("adaptive MSA");
			final YIntervalSeries realizedLambdasSqrtMSA = sqrtMSA.newRealizedLambdaSeries("sqrt MSA");
			final YIntervalSeries realizedLambdasVanillaMSA = vanillaMSA.newRealizedLambdaSeries("vanilla MSA");

			final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
			dataset.addSeries(realizedLambdasProposed);
			dataset.addSeries(realizedLambdasAdaptiveMSA);
			dataset.addSeries(realizedLambdasSqrtMSA);
			dataset.addSeries(realizedLambdasVanillaMSA);

			final JFreeChart chart = ChartFactory.createXYLineChart("realized lambdas comparison", // chart title
					"iteration", // x axis label
					"lambda value", // y axis label
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

			save(chart, "realized lambdas comparison");
		}

		System.out.println("...DONE");
	}

}
