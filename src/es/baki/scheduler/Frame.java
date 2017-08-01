package es.baki.scheduler;

import java.util.concurrent.ThreadLocalRandom;

import javax.swing.JOptionPane;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Frame extends Application {

	private Scene intro;
	private Stage stage;
	private ComboBox<String> algoCombo, rrSortCombo;
	private VBox algoOptsL, algoOptsR;

	private GridPane currProcess;
	private GridPane queuedProcesses;
	private GridPane futureProcesses;
	private GridPane doneProcesses;

	private CPU cpu;

	private Spinner<Integer> stepSpinner;
	private Button stepButton;

	// add a process fields
	private TextField pidField, btField, prioField, arrivalField;
	private Button addProcess, addRandProcess;

	private CheckBox preemptBox, variableBox;
	private Spinner<Integer> quantumSpinner;
	private GridPane grid;

	private Node ganttLegend;
	private HBox statusBar;

	public static void main(String... strings) {
		launch(strings);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		stage = primaryStage;
		cpu = new CPU();
		algoOptsL = new VBox();
		algoOptsR = new VBox();
		primaryStage.setTitle("Scheduler");

		primaryStage.setScene(makeScene());
		primaryStage.show();

		cpu.updateViz(currProcess, queuedProcesses, futureProcesses, doneProcesses);
		stage.sizeToScene();
	}

	public Scene makeScene() {
		grid = new GridPane();
		grid.setVgap(4);
		grid.setHgap(10);
		// grid.setPrefWidth(200);
		grid.setPadding(new Insets(5, 5, 5, 5));

		// Radial Label
		Text label = new Text("Algorithm:");
		grid.add(label, 0, 0);

		// Algo Selection
		preemptBox = new CheckBox("Preempt");
		preemptBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				cpu.setPreemptive(newValue.booleanValue());
			}
		});
		variableBox = new CheckBox("Variable");
		variableBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				cpu.setVariable(newValue.booleanValue());
				updateStatus();
			}
		});
		quantumSpinner = new Spinner<Integer>();
		SpinnerValueFactory<Integer> valFac = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE);
		quantumSpinner.setValueFactory(valFac);
		quantumSpinner.setEditable(false);

		rrSortCombo = new ComboBox<>();
		rrSortCombo.getItems().addAll("FCFS", "SJF", "Priority");
		rrSortCombo.setValue("FCFS");
		rrSortCombo.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				cpu.setRRSort(rrSortCombo.getValue());
			}
		});

		algoCombo = new ComboBox<>();
		algoCombo.getItems().addAll("FCFS", "SJF", "Priority", "Round Robin");
		algoCombo.setValue("FCFS");
		algoOptsL.setAlignment(Pos.CENTER_LEFT);
		algoOptsL.getChildren().add(preemptBox);

		algoCombo.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				while (algoOptsR.getChildren().size() > 0)
					algoOptsR.getChildren().remove(0);
				while (algoOptsL.getChildren().size() > 0)
					algoOptsL.getChildren().remove(0);
				algoOptsL.getChildren().add(preemptBox);
				if (algoCombo.getValue().equals("FCFS")) {
					cpu.setAlgo(cpu.new FCFS());
				} else if (algoCombo.getValue().equals("SJF")) {
					cpu.setAlgo(cpu.new SJF());
				} else if (algoCombo.getValue().equals("SRT")) {
					cpu.setAlgo(cpu.new SRT());
				} else if (algoCombo.getValue().equals("Priority")) {
					cpu.setAlgo(cpu.new Priority());
				} else if (algoCombo.getValue().equals("Round Robin")) {
					algoOptsL.getChildren().remove(0);
					algoOptsL.getChildren().addAll(variableBox, new Text("Sort by:"), rrSortCombo);
					algoOptsR.getChildren().add(new Text("Quantum:"));
					Button qApply = new Button("Apply");
					qApply.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent event) {
							cpu.setQuantum(quantumSpinner.getValue().intValue());
							updateStatus();
						}
					});
					algoOptsR.getChildren().addAll(quantumSpinner, qApply);
					cpu.setAlgo(cpu.new RoundRobin());
				} else {
					System.err.println("This shouldnt happen!");
				}
				cpu.updateViz(currProcess, queuedProcesses, futureProcesses, doneProcesses);
				updateStatus();
				stage.sizeToScene();
			}
		});
		grid.add(algoCombo, 0, 1);
		
		Text algoOptsTitle = new Text("Options:");
		grid.add(algoOptsTitle, 1, 0);
		GridPane.setHalignment(algoOptsL, HPos.LEFT);
		GridPane.setValignment(algoOptsL, VPos.CENTER);

		grid.add(algoOptsL, 1, 1);
		grid.add(algoOptsR, 2, 1);
		GridPane.setFillWidth(algoCombo, true);

		// grid.add(new Separator(), 0, 3, 3, 1);
		grid.add(new Text("Add a process:"), 0, 3, 3, 1);
		
		pidField = new TextField();
		pidField.setPromptText("PID");
		pidField.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (!newValue.matches("\\d*")) {
					pidField.setText(newValue.replaceAll("[^-\\d]", ""));
				}
			}
		});
		pidField.setTooltip(new Tooltip("PID"));
		pidField.setPrefWidth(100);

		btField = new TextField();
		btField.setPromptText("Burst Time");
		btField.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (!newValue.matches("\\d*")) {
					btField.setText(newValue.replaceAll("[^\\d]", ""));
				}
			}
		});
		btField.setTooltip(new Tooltip("Burst Time"));
		btField.setPrefWidth(100);

		arrivalField = new TextField();
		arrivalField.setPromptText("Arrival Time");
		arrivalField.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (!newValue.matches("\\d*")) {
					arrivalField.setText(newValue.replaceAll("[^-\\d]", ""));
				}
			}
		});
		arrivalField.setTooltip(new Tooltip("Arrival Time"));
		arrivalField.setPrefWidth(100);

		prioField = new TextField();
		prioField.setPromptText("Priority");
		prioField.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (!newValue.matches("\\d*")) {
					prioField.setText(newValue.replaceAll("[^-\\d]", ""));
				}
			}
		});
		prioField.setTooltip(new Tooltip("Priority"));
		prioField.setPrefWidth(100);

		grid.addRow(4, new Text("PID"), new Text("Burst Time"), new Text("Arrival Time"), new Text("Priority"));
		grid.addRow(5, pidField, btField, arrivalField, prioField);

		addProcess = new Button("Add Process");
		GridPane.setHalignment(addProcess, HPos.LEFT);
		GridPane.setValignment(addProcess, VPos.TOP);
		addProcess.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if (cpu.newProcess(pidField.getText(), btField.getText(), prioField.getText(),
						arrivalField.getText())) {
					cpu.updateViz(currProcess, queuedProcesses, futureProcesses, doneProcesses);
					stage.sizeToScene();
				} else {
					System.err.println("Could not create process");
				}
			}
		});
		addRandProcess = new Button("Add Random");
		addRandProcess.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				String s = JOptionPane.showInputDialog(
						"This will generate random processes with the arrival time the arrival time box.\n A random number generated from 1 to the value in the box for burst time and priority.\n The process will be assigned the next available PID.\n How many processes?");
				int x = 0, pid = 0;
				try { 
					x = Integer.parseInt(s);
				} catch(NumberFormatException e ) { 
					System.err.println("NaN - Dialog");
					return;
				}
				if (Integer.parseInt(btField.getText()) <= 0) {
					System.err.println("Trying to loop with bad burst time");
					JOptionPane.showMessageDialog(null, "Invalid Burst Time", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}

				for (;x > 0;)  {
					if (cpu.realNewProcess(true, pid + "",
							Integer.parseInt(btField.getText()) == 1 ? 1 + ""
									: ThreadLocalRandom.current().nextInt(1, Integer.parseInt(btField.getText())) + "",
							Integer.parseInt(prioField.getText()) == 0 ? "0" : ThreadLocalRandom.current().nextInt(0, Integer.parseInt(prioField.getText())) + "",
							"" + Integer.parseInt(arrivalField.getText())))
						x--;
					pid++;
				}
				cpu.updateViz(currProcess, queuedProcesses, futureProcesses, doneProcesses);
				stage.sizeToScene();
			}
		});
		addRandProcess.setTooltip(
				new Tooltip("Adds a process with 3 random values that are at most what is in the labels above"));

		grid.add(addProcess, 0, 6);
		GridPane.setHalignment(addRandProcess, HPos.RIGHT);
		GridPane.setValignment(addRandProcess, VPos.TOP);
		grid.add(addRandProcess, 3, 6);

		currProcess = new GridPane();
		queuedProcesses = new GridPane();
		futureProcesses = new GridPane();
		doneProcesses = new GridPane();

		grid.add(new Separator(Orientation.VERTICAL), 4, 0, 1, 7); //
		currProcess.maxWidth(Double.POSITIVE_INFINITY);
		GridPane processGrid = new GridPane();
		processGrid.add(
				new Text(
						"Current Process:                                                                                                    "),
				0, 0);
		processGrid.add(currProcess, 0, 1);
		processGrid.add(new Text("\nQueued Processes: "), 0, 2);
		processGrid.add(queuedProcesses, 0, 3);
		processGrid.add(new Text("\nFuture Processes: "), 0, 4);
		processGrid.add(futureProcesses, 0, 5);
		processGrid.add(new Text("\nFinished Processes:"), 0, 6);
		processGrid.add(doneProcesses, 0, 7);

		GridPane.setHgrow(processGrid, Priority.ALWAYS);
		GridPane.setFillWidth(processGrid, true);

		grid.add(processGrid, 5, 0, 1, 7);

		cpu.updateViz(currProcess, queuedProcesses, futureProcesses, doneProcesses);

		grid.add(new Separator(), 0, 7, 6, 1); // BOTTOM SEP
		statusBar = new HBox();
		statusBar.setAlignment(Pos.CENTER_LEFT);
		grid.add(statusBar, 0, 10, 4, 1);

		updateChart();

		intro = new Scene(grid);
		stepButton = new Button("Step");
		stepButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				int ticks = stepSpinner.getValue().intValue();
				for (int x = 0; x < ticks; x++) {
					cpu.tick();
					cpu.updateViz(currProcess, queuedProcesses, futureProcesses, doneProcesses);
					updateStatus();
					updateChart();
					stage.sizeToScene();
					stage.setWidth(stage.getWidth() + 1);
				}
				// }
			}
		});
		stepSpinner = new Spinner<Integer>();
		stepSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE));
		stepSpinner.setEditable(false);

		HBox stepper = new HBox();
		stepper.setAlignment(Pos.CENTER_RIGHT);
		grid.add(stepper, 5, 10, 2, 1);
		stepper.getChildren().addAll(new Text("Step Size: "), stepSpinner, stepButton);

		HBox.setHgrow(stepSpinner, Priority.ALWAYS);
		updateStatus();
		grid.add(GanttChart.getChart().getFXChart(), 0, 8, 6, 1);
		return intro;
	}

	private void updateStatus() {
		while (statusBar.getChildren().size() > 0)
			statusBar.getChildren().remove(0);

		statusBar.getChildren().addAll(new Text("Current time: " + cpu.getCurrTime()),
				new Separator(Orientation.VERTICAL), new Text("Quantum: " + cpu.getQuantum()),
				new Separator(Orientation.VERTICAL), new Text("Variable Quantum " + cpu.getVariableQuantum()));

	}

	public void updateChart() {
		// grid.getChildren().remove(ganttChart);
		grid.getChildren().remove(ganttLegend);
		GanttChart.getChart().getFXChart();
		ganttLegend = GanttChart.getChart().getFXLegend();
		grid.add(ganttLegend, 0, 9, 6, 1);
	}
}
