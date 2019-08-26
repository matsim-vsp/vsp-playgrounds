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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;

import umontreal.ssj.charts.XYLineChart;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AccelerationAnalysisPlot {

	private Double legendLeftX = null;
	private Double legendTopY = null;
	private Double legendLineLength = null;
	private Double legendRowDistance = null;

	public void addLegend(final double legendLeftX, final double legendTopY, final double legendLineLength,
			final double legendRowDistance) {
		this.legendLeftX = legendLeftX;
		this.legendTopY = legendTopY;
		this.legendLineLength = legendLineLength;
		this.legendRowDistance = legendRowDistance;
	}

	private double[] range = null;

	public void setRange(final double xmin, final double xmax, final double ymin, final double ymax) {
		this.range = new double[] { xmin, xmax, ymin, ymax };
	}

	// private final List<Color> colors = Arrays.asList(Color.RED, Color.BLUE,
	// Color.GREEN, Color.YELLOW, Color.MAGENTA,
	// Color.CYAN, Color.CYAN, Color.CYAN, Color.CYAN, Color.CYAN, Color.CYAN,
	// Color.CYAN);

	private final List<String> dashPatterns = Arrays.asList("solid", "dashed", "dotted");
	// "densely dotted", "loosely dotted",
	// "densely dashed", "loosely dashed", "loosely dashed", "loosely dashed",
	// "loosely dashed",
	// "loosely dashed", "loosely dashed", "loosely dashed");

	private final List<YIntervalSeries> allSeries = new ArrayList<>();

	public AccelerationAnalysisPlot() {
	}

	public void addSeries(final YIntervalSeries series) {
		if (this.allSeries.size() == 3) {
			System.err.println("Cannot plot more than 3 series in one figure.");
		} else {
			this.allSeries.add(series);
		}
	}

	public void render(String fileName) {

		final XYSeriesCollection data = new XYSeriesCollection();
		for (int seriesIndex = 0; seriesIndex < this.allSeries.size(); seriesIndex++) {
			YIntervalSeries intervalSeries = this.allSeries.get(seriesIndex);
			final XYSeries lowerLine = new XYSeries("lower" + seriesIndex);
			// final XYSeries meanLine = new XYSeries("mean" + seriesIndex);
			final XYSeries upperLine = new XYSeries("upper" + seriesIndex);
			for (int itemIndex = 0; itemIndex < intervalSeries.getItemCount(); itemIndex++) {
				final double yLow = intervalSeries.getYLowValue(itemIndex);
				final double y = intervalSeries.getYValue(itemIndex);
				final double yHigh = intervalSeries.getYHighValue(itemIndex);
				if (!Double.isNaN(yLow) && !Double.isNaN(y) && !Double.isNaN(yHigh)) {
					lowerLine.add(intervalSeries.getX(itemIndex).doubleValue(), yLow);
					// meanLine.add(intervalSeries.getX(itemIndex).doubleValue(), y);
					upperLine.add(intervalSeries.getX(itemIndex).doubleValue(), yHigh);
				}
			}
			data.addSeries(lowerLine);
			// data.addSeries(meanLine);
			data.addSeries(upperLine);

		}

		XYLineChart chart = new XYLineChart(null, "X", "Y", data);

		for (int seriesIndex = 0; seriesIndex < this.allSeries.size(); seriesIndex++) {

			// TODO [CONTINUE HERE] TURN OFF LABELS IN INTERVAL PLOTS
			
			chart.getSeriesCollection().setColor(seriesIndex * 2 + 0, Color.BLACK);
			chart.getSeriesCollection().setColor(seriesIndex * 2 + 1, Color.BLACK);

			chart.getSeriesCollection().setDashPattern(seriesIndex * 2 + 0, this.dashPatterns.get(seriesIndex));
			chart.getSeriesCollection().setDashPattern(seriesIndex * 2 + 1, this.dashPatterns.get(seriesIndex));

			// chart.getSeriesCollection().setName(seriesIndex * 2 + 0,
			// this.allSeries.get(seriesIndex).getKey().toString() + " lower");
			// chart.getSeriesCollection().setName(seriesIndex * 2 + 1,
			// this.allSeries.get(seriesIndex).getKey().toString() + " upper");

		}

		if (this.legendLeftX != null) {
			for (int seriesIndex = 0; seriesIndex < this.allSeries.size(); seriesIndex++) {
				final int lineIndex = chart.add(
						new double[] { this.legendLeftX, this.legendLeftX + this.legendLineLength },
						new double[] { this.legendTopY - seriesIndex * this.legendRowDistance,
								this.legendTopY - seriesIndex * this.legendRowDistance });
				chart.getSeriesCollection().setColor(lineIndex, Color.BLACK);
				chart.getSeriesCollection().setDashPattern(lineIndex, this.dashPatterns.get(seriesIndex));
				chart.getSeriesCollection().setName(lineIndex, this.allSeries.get(seriesIndex).getKey().toString());
			}
		}

		if (this.range != null) {
			chart.setManualRange(this.range);
		} else {
			chart.setAutoRange00(true, true);

		}
		chart.setLatexDocFlag(false);
		chart.toLatexFile(fileName, 6, 4);
		// chart.setLatexDocFlag(true);
		// System.out.println(chart.toLatex(12, 8));
	}

	public static void main(String[] args) {
		System.out.println("STARTED");

		AccelerationAnalysisPlot aap = new AccelerationAnalysisPlot();

		{
			YIntervalSeries series1 = new YIntervalSeries("series1");
			series1.add(0, 1, 1, 1);
			series1.add(1, 2, 1.5, 2.5);
			series1.add(2, 3, 2, 4);
			aap.addSeries(series1);
		}

		{
			YIntervalSeries series2 = new YIntervalSeries("series2");
			series2.add(0, -1, -1, -1);
			series2.add(1, -2, -2.5, -1.5);
			series2.add(2, -3, -4, -2);
			aap.addSeries(series2);
		}

		{
			YIntervalSeries series3 = new YIntervalSeries("series3");
			series3.add(0, 0, 0, 0);
			series3.add(1, 0, -0.5, 0.5);
			series3.add(2, 0, -1, 1);
			aap.addSeries(series3);
		}

		{
			YIntervalSeries series4 = new YIntervalSeries("series4");
			series4.add(0, -3, -3, -3);
			series4.add(1, 0, -0.5, 0.5);
			series4.add(2, 3, 2, 4);
			aap.addSeries(series4);
		}

		aap.render("NormalChart.tex");

		System.out.println("DONE");
	}

}
