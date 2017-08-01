package es.baki.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JOptionPane;

import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;

public class CPU {

	private ArrayList<Process> queued, future, done;
	private Process currProcess;
	private int currTime = 0;
	private int rrTicker = 0;

	// Options
	private int quantum = 1;
	private boolean varQuantum = false, preemptive = false;
	private String rrSort = "FCFS";


	public void setVariableQuantum(boolean varQ) {
		this.varQuantum = varQ;
	}
	public int getQuantum() {
		return this.quantum;
	}

	public boolean getPreemptive() {
		return this.preemptive;
	}

	public void setPreemptive(boolean b) {
		this.preemptive = b;
	}

	private Algo algo;

	public CPU() {
		queued = new ArrayList<>();
		done = new ArrayList<>();
		future = new ArrayList<>();
		this.algo = new FCFS();
	}

	public void setAlgo(Algo algo) {
		if (currProcess != null && currProcess.getRemainingTime() >= 0)
			queued.add(currProcess);
		currProcess = null;
		this.algo = algo;
		algo.init();
	}

	public void tick() {
		if (currProcess != null && currProcess.getRemainingTime() <= 0) {
			done.add(currProcess);
			currProcess = null;
		}
		for (int x = 0; x < future.size(); x++) {
			if (future.get(x).arrivalTime <= currTime) {
				queued.add(future.get(x));
			}
			future.removeAll(queued);
		}
		algo.tick();
		if (currProcess != null) {
			GanttChart.getChart().setTime(currProcess.PID, currTime);
		}
		currTime++;
		for (Process p : queued)
			p.incWait();
	}

	abstract class Algo {
		public Algo() {
			updateQueue();
		}
		abstract void tick();
		abstract void updateQueue();

		abstract void init();
	}

	public class FCFS extends Algo {
		@Override
		void tick() {
			if (currProcess == null) {
				if (queued.size() > 0) {
					currProcess = queued.get(0);
					queued.remove(0);
				} else {
					return;
				}
			}
			currProcess.decBurst(currTime);
		}
		@Override
		void updateQueue() {
			if (queued.size() > 1) {
				Collections.sort(queued, new Comparator<Process>() {
					@Override
					public int compare(Process o1, Process o2) {
						if (o1.arrivalTime == o2.arrivalTime)
							return 0;
						return o1.arrivalTime > o2.arrivalTime ? 1 : -1;
					}
				});
			}
		}

		@Override
		void init() {
		};
	}

	public class SJF extends FCFS {
		@Override
		void updateQueue() {
			if (queued.size() > 1)
				Collections.sort(queued, new Comparator<Process>() {
					@Override
					public int compare(Process o1, Process o2) {
						if (o1.burstTime == o2.burstTime)
							return 0;
						return o1.burstTime > o2.burstTime ? 1 : -1;
					}
				});
		}
	}

	public class SRT extends FCFS {
		@Override
		void updateQueue() {
			if (queued.size() > 1)
				Collections.sort(queued, new Comparator<Process>() {
					@Override
					public int compare(Process o1, Process o2) {
						if (o1.getRemainingTime() == o2.getRemainingTime())
							return 0;
						return o1.getRemainingTime() > o2.getRemainingTime() ? 1 : -1;
					}
				});
		}
	}

	public class Priority extends FCFS {
		@Override
		void tick() {
			if (currProcess != null && preemptive) {
				int highDex = -1;
				for (int x = 0; x < queued.size(); x++) {
					if (queued.get(x).prio < currProcess.prio
							&& (highDex == -1 || queued.get(highDex).prio > queued.get(x).prio))
						highDex = x;
				}
				if (highDex >= 0) {
					Process t = currProcess;
					currProcess = queued.get(highDex);
					queued.add(t);
					updateQueue();
				}
			}
			super.tick();
		}
		@Override
		void updateQueue() {
			if (queued.size() > 1)
				Collections.sort(queued, new Comparator<Process>() {
					@Override
					public int compare(Process o1, Process o2) {
						if (o1 == null || o2 == null) {
							return 0;
					}
						if (o1.prio == o2.prio)
							return 0;
						return o1.prio > o2.prio ? 1 : -1;
					}
				});

		}
	}

	public class RoundRobin extends Algo {
		public RoundRobin() {
			super();
			rrTicker = 0;
		}

		@Override
		void init() {
			if (queued.size() > 0) {
				currProcess = queued.get(0);
				queued.remove(0);
			}
		}
		@Override
		void tick() {
			if (currProcess == null && rrTicker == 0) {
				currProcess = queued.get(0);
				queued.remove(0);
			}
			if (currProcess != null && currProcess.getRemainingTime() <= 0) {
				done.add(currProcess);
				currProcess = null;
				if (varQuantum) {
					rrTicker = quantum + 1; // Force the next quantum
				}
			}
			if (queued.size() > 0 && ((varQuantum && currProcess == null) || rrTicker >= quantum)) {
				if (currProcess != null)
					queued.add(currProcess);
				currProcess = queued.get(0);
				for (int x = 0; x < queued.size() - 1; x++) { // SLOW!
					queued.set(x, queued.get(x + 1));
				}
				queued.remove(queued.size() - 1);
				rrTicker = 0;
			}
			if (currProcess != null)
				currProcess.decBurst(currTime);
			rrTicker++;
		}
		@Override
		void updateQueue() {
			if (rrSort.equals("FCFS"))
				new FCFS().updateQueue();
			if (rrSort.equals("SJF"))
				new SJF().updateQueue();
			if (rrSort.equals("Priority"))
				new Priority().updateQueue();
		}
	}

	public void setVariable(boolean variableQuantum) {
		this.varQuantum = variableQuantum;
	}

	public void setQuantum(int quantum) {
		this.quantum = quantum;
	}

	public void setRRSort(String value) {
		this.rrSort = value;
		algo.updateQueue();
	}

	public boolean realNewProcess(boolean quiet, String pid, String burstTime, String priority, String arrivalTime) {
		int pidInt, btInt, prioInt, arrivalInt;
		try {
			pidInt = Integer.parseInt(pid);
			btInt = Integer.parseInt(burstTime);
			prioInt = Integer.parseInt(priority);
			arrivalInt = Integer.parseInt(arrivalTime);
		} catch (NumberFormatException e) {
			System.err.println("NaN");
			return false;
		}

		if (btInt <= 0) {
			if (!quiet)
			JOptionPane.showMessageDialog(null, "Invalid Burst Time", "Error", JOptionPane.ERROR_MESSAGE);
			System.err.println("Bad Burst Time");
			return false;
		}

		if (currProcess != null && currProcess.PID == pidInt) {
			if (!quiet)
			JOptionPane.showMessageDialog(null, "PID Exists", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		for (Process p : queued)
			if (pidInt == p.PID) {
				System.err.println("PID Exists");
				if (!quiet)
				JOptionPane.showMessageDialog(null, "PID Exists", "Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		for (Process p : future)
			if (pidInt == p.PID) {
				System.err.println("PID Exists");
				if (!quiet)
				JOptionPane.showMessageDialog(null, "PID Exists", "Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		for (Process p : done) {
			if (pidInt == p.PID) {
				System.err.println("PID Exists");
				if (!quiet)
				JOptionPane.showMessageDialog(null, "PID Exists", "Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}

		if (arrivalInt <= currTime) {
			queued.add(new Process(pidInt, btInt, prioInt, arrivalInt));
			if (preemptive && currProcess != null) {
				queued.add(currProcess);
				currProcess = null;
			}
			algo.updateQueue();
		} else
			future.add(new Process(pidInt, btInt, prioInt, arrivalInt));
		return true;

	}

	public boolean newProcess(String pid, String burstTime, String priority, String arrivalTime) {
		return realNewProcess(false, pid, burstTime, priority, arrivalTime);
	}

	public void updateViz(GridPane currProcess, GridPane queuedProcesses, GridPane futureProcesses,
			GridPane doneProcesses) {
		currProcess.getChildren().clear();
		queuedProcesses.getChildren().clear();
		futureProcesses.getChildren().clear();
		doneProcesses.getChildren().clear();

		String[] titles = { " PID", " Remaining", " Burst", " Priority", " Arrival" };
		int currCol = 0;
		for (String s : titles) {
			Text titleCurr = new Text(s);
			Text titleQ1 = new Text(s);
			// Text titleQ2 = new Text(s);
			Text titleF1 = new Text(s);
			// Text titleF2 = new Text(s);

			GridPane.setFillWidth(titleCurr, true);
			GridPane.setFillWidth(titleQ1, true);
			// GridPane.setFillWidth(titleQ2, true);
			GridPane.setFillWidth(titleF1, true);
			// GridPane.setFillWidth(titleF2, true);

			GridPane.setHgrow(titleCurr, javafx.scene.layout.Priority.ALWAYS);
			GridPane.setHgrow(titleQ1, javafx.scene.layout.Priority.ALWAYS);
			// GridPane.setHgrow(titleQ2, javafx.scene.layout.Priority.ALWAYS);
			GridPane.setHgrow(titleF1, javafx.scene.layout.Priority.ALWAYS);
			// GridPane.setHgrow(titleF2, javafx.scene.layout.Priority.ALWAYS);

			currProcess.add(titleCurr, currCol, 0);
			queuedProcesses.add(titleQ1, currCol, 0);
			// queuedProcesses.add(titleQ2, currCol + 5, 0);
			futureProcesses.add(titleF1, currCol, 0);
			// futureProcesses.add(titleF2, currCol + 5, 0);

			currCol++;
		}
		// Separator qSep = new Separator(Orientation.VERTICAL);
		// queuedProcesses.add(qSep, 4, 0, 1, 100);
		// Separator fSep = new Separator(Orientation.VERTICAL);
		// futureProcesses.add(fSep, 4, 0, 1, 100);

		currProcess.setGridLinesVisible(true);
		futureProcesses.setGridLinesVisible(true);
		queuedProcesses.setGridLinesVisible(true);

		if (this.currProcess != null)
			currProcess.addRow(1, new Text(this.currProcess.PID + ""),
					new Text(this.currProcess.getRemainingTime() + ""), new Text(this.currProcess.prio + ""),
					new Text(this.currProcess.arrivalTime + ""));
		int row = 1;
		for (Process p : queued) {
			queuedProcesses.addRow(row, new Text(p.PID + ""), new Text(p.getRemainingTime() + ""),
					new Text(p.burstTime + ""),
					new Text(p.prio + ""),
					new Text(p.arrivalTime + ""));
			row++;
		}
		row = 1;
		for (Process p : future) {
			futureProcesses.addRow(row, new Text(p.PID + ""), new Text(p.getRemainingTime() + ""),
					new Text(p.burstTime + ""),
					new Text(p.prio + ""),
					new Text(p.arrivalTime + ""));
			row++;
		}
		row = 1;
		String[] doneTitles = { "PID", "Burst Time", "Turn Around Time", "Wait Time", "Arrival", "Finish" };
		currCol = 0;
		for (String s : doneTitles) {
			Text title = new Text(s);
			GridPane.setFillWidth(title, true);
			GridPane.setHgrow(title, javafx.scene.layout.Priority.ALWAYS);
			doneProcesses.add(title, currCol, 0);

			currCol++;
		}
		int btTot = 0, tatTot = 0, wtTot = 0;
		for (Process p : done) { 
			doneProcesses.addRow(row, new Text(p.PID + ""), new Text(p.burstTime + ""), new Text("" + p.getAliveTime()),
					new Text(p.getWaitTime() + ""), new Text(p.arrivalTime + ""), new Text(p.getFinishTime() + ""));
			btTot += p.burstTime;
			tatTot += p.getAliveTime();
			wtTot += p.getWaitTime();
			row++;
		}
		doneProcesses.add(new Separator(), 0, row, 6, 1);
		row++;
		if (done.size() > 0)
			doneProcesses.addRow(row, new Text("Averages: "), new Text(btTot / done.size() + ""),
					new Text(tatTot / done.size() + ""), new Text(wtTot / done.size() + ""));
		futureProcesses.setGridLinesVisible(true);
		queuedProcesses.setGridLinesVisible(true);
	}

	public int getCurrTime() {
		return currTime;
	}

	public boolean getVariableQuantum() {
		return this.varQuantum;
	}
}
