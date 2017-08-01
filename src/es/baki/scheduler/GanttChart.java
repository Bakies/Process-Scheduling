package es.baki.scheduler;

import java.util.ArrayList;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class GanttChart {
	private static GanttChart chart;

	private ArrayList<String> timeChart = new ArrayList<>();
	private ArrayList<Integer> pids = new ArrayList<>();
	private int x = 0;
	private static String[] colors = { "BLUE", "BLUEVIOLET", "BROWN", "CADETBLUE", "CHARTREUSE", "CHOCOLATE", "CORAL",
			"CORNFLOWERBLUE", "CORNSILK", "CRIMSON", "CYAN", "DARKBLUE", "DARKCYAN", "DARKGOLDENROD", "DARKGREEN",
			"DARKKHAKI", "DARKMAGENTA", "DARKOLIVEGREEN", "DARKORANGE", "DARKORCHID", "DARKRED", "DARKSALMON",
			"DARKSEAGREEN", "DARKSLATEBLUE", "DARKTURQUOISE", "DARKVIOLET", "DEEPPINK", "DEEPSKYBLUE", "DODGERBLUE",
			"FIREBRICK", "FORESTGREEN", "FUCHSIA", "GAINSBORO", "GOLD", "GOLDENROD", "GREEN", "GREENYELLOW", "HOTPINK",
			"INDIANRED", "INDIGO", "KHAKI", "LAVENDER", "LAWNGREEN", "LEMONCHIFFON", "LIGHTBLUE", "LIGHTCORAL",
			"LIGHTGREEN", "LIGHTSALMON", "LIGHTSEAGREEN", "LIGHTSKYBLUE", "LIGHTYELLOW", "LIME", "LIMEGREEN", "MAGENTA",
			"MAROON", "MEDIUMAQUAMARINE", "MEDIUMBLUE", "MEDIUMORCHID", "MEDIUMPURPLE", "MEDIUMSEAGREEN",
			"MEDIUMSLATEBLUE", "MEDIUMSPRINGGREEN", "MEDIUMTURQUOISE", "MEDIUMVIOLETRED", "MIDNIGHTBLUE", "MISTYROSE",
			"MOCCASIN", "NAVY", "OLIVE", "OLIVEDRAB", "ORANGE", "ORANGERED", "ORCHID", "PALEGOLDENROD", "PALEGREEN",
			"PALETURQUOISE", "PALEVIOLETRED", "PAPAYAWHIP", "PEACHPUFF", "PERU", "PINK", "PLUM", "POWDERBLUE", "PURPLE",
			"RED", "ROSYBROWN", "ROYALBLUE", "SADDLEBROWN", "SALMON", "SANDYBROWN", "SEAGREEN", "SIENNA", "SILVER",
			"SKYBLUE", "SLATEBLUE", "SPRINGGREEN", "STEELBLUE", "TAN", "TEAL", "THISTLE", "TOMATO", "TURQUOISE",
			"VIOLET", "WHEAT", "YELLOW", "YELLOWGREEN" };
	StackedBarChart<Number, String> barChart;
	CategoryAxis yAxis = new CategoryAxis();
	NumberAxis xAxis = new NumberAxis();
	boolean timeSet = false;
	int curr = 0;
	private GanttChart() {
		barChart = new StackedBarChart<Number, String>(xAxis, yAxis);
		xAxis.setLabel("Time");
		xAxis.setUpperBound(2.0);
	}

	public void setTime(int pid, int time) {
		timeSet = true;
		while (time > timeChart.size())
			timeChart.add(null);
		if (!pids.contains(pid))
			pids.add(pid);
		timeChart.add(time, pid + "");
	}

	public static GanttChart getChart() {
		if (chart == null)
			chart = new GanttChart();
		return chart;
	}

	public static void resetChart() {
		chart = new GanttChart();
	}

	public StackedBarChart<Number, String> getFXChart() {
		for (x = curr; (timeSet) && x < timeChart.size(); x++) {
			XYChart.Series<Number, String> dataSeries = new XYChart.Series<Number, String>();
			dataSeries.setName("");
			Data<Number, String> data = new XYChart.Data<Number, String>(1, "");
			dataSeries.getData().add(data);
			data.nodeProperty().addListener(new ChangeListener<Node>() {
				@Override
				public void changed(ObservableValue<? extends Node> observable, Node oldValue, Node newValue) {
						newValue.setStyle(String.format("-fx-bar-fill: %s;",
							timeChart.get(x) == null ? "black"
									: colors[Math.abs(Integer.parseInt(timeChart.get(x))) % colors.length]));
				}
			});
			barChart.getData().add(dataSeries);
		}
		curr = x;
		barChart.setLegendVisible(false);
		// barChart.setCategoryGap(10);
		barChart.setPrefHeight(200);
		barChart.setTitle("Gantt Chart");
		barChart.setAnimated(true);
		return barChart;
	}
	public GridPane getFXLegend() {
		GridPane grid = new GridPane();
		Label color, text;

		int y = 0, x = 0;
		for (; x < pids.size(); x++) {
			if (x % 7 == 0)
				y++;
			color = new Label("   ");
			text = new Label(" PID: " + pids.get(x));
			text.setPadding(new Insets(3, 3, 3, 3));
			color.setStyle(String.format(
					"-fx-background-color: %s;",
					colors[Math.abs(pids.get(x)) % colors.length]));
			GridPane.setFillWidth(text, true);
			GridPane.setHgrow(text, Priority.ALWAYS);

			grid.add(color, (x * 2) % 14, y);
			grid.add(text, (x * 2) % 14 + 1, y);
		}
		if (x % 7 == 0)
			y++;
		color = new Label("   ");
		color.setStyle("-fx-background-color: black;");

		text = new Label(" IDLE");
		text.setPadding(new Insets(3, 3, 3, 3));
		GridPane.setFillWidth(text, true);
		GridPane.setHgrow(text, Priority.ALWAYS);

		grid.add(color, (x * 2) % 14, y);
		grid.add(text, (x * 2) % 14 + 1, y);



		return grid;
	}
}
