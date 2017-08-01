package es.baki.scheduler;

public class Process {
	public final int burstTime, prio, PID, arrivalTime;
	private int waitTime, finishTime, aliveTime;
	private int remainingTime;

	public static int sort = 0;

	public Process(int PID, int burstTime, int prio, int arrivalTime) {
		this.PID = PID;
		this.arrivalTime = arrivalTime;
		this.remainingTime = this.burstTime = burstTime;
		this.prio = prio;
	}

	public void incWait() {
		waitTime++;
		aliveTime++;
	}

	public int getRemainingTime() {
		return remainingTime;
	}

	public void decBurst(int currTime) {
		remainingTime--;
		finishTime = currTime;
		aliveTime++;
	}

	public int getWaitTime() {
		return waitTime;
	}

	public int getFinishTime() {
		return finishTime;
	}

	public int getAliveTime() {
		return aliveTime;
	}
}
