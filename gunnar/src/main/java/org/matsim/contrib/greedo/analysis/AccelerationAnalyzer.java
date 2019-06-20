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
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AccelerationAnalyzer {

	static void save(final JFreeChart chart, final String fileName) {
		try {
			ChartUtilities.saveChartAsPNG(new File(fileName + ".png"), chart, 1000, 600);
			SVGGraphics2D g2 = new SVGGraphics2D(500, 300);
			Rectangle r = new Rectangle(0, 0, 500, 300);
			chart.draw(g2, r);
			SVGUtils.writeToSVG(new File(fileName + ".svg"), g2.getSVGElement());

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		System.out.println("STARTED...");

		final AccelerationExperimentData acceptNegativeDisappointment = new AccelerationExperimentData(
				"/Users/GunnarF/NoBackup/data-workspace/searchacceleration/greedo_acceptNegativeDissapointment", 0, 10,
				1000);

		{ // BETAS
			final YIntervalSeries betas = acceptNegativeDisappointment.newBetaSeries("unconstrained");

			final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
			dataset.addSeries(betas);

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

		{ // REALIZED LAMBDA
			final YIntervalSeries realizedLambdas = acceptNegativeDisappointment.newRealizedLambdaSeries("unconstrained");

			final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
			dataset.addSeries(realizedLambdas);

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

		{ // REALIZED UTILITY
			final YIntervalSeries realizedUtilities = acceptNegativeDisappointment.newRealizedUtilitiesSeries("unconstrained");

			final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
			dataset.addSeries(realizedUtilities);

			final JFreeChart chart = ChartFactory.createXYLineChart("realized utilities", // chart title
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

			save(chart, "realized-utilities");
		}

		{ // EXPECTED UTILITY CHANGES
			final YIntervalSeries expectedUtilityChanges = acceptNegativeDisappointment.newExpectedUtilityChangesSeries("unconstrained");

			final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
			dataset.addSeries(expectedUtilityChanges);

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

			save(chart, "expected-utility-changes");
		}

		{ // PERFORMANCE CORRELATIONS
			final YIntervalSeries performanceCorrelations = acceptNegativeDisappointment.newPerformanceCorrelationSeries("unconstrained");

			final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
			dataset.addSeries(performanceCorrelations);

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

			save(chart, "performance-correlations");
		}

		{ // PERFORMANCE CORRELATIONS
			final YIntervalSeries ageCorrelations = acceptNegativeDisappointment.newAgeCorrelationSeries("unconstrained");

			final YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
			dataset.addSeries(ageCorrelations);

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

		System.out.println("...DONE");
	}

}
