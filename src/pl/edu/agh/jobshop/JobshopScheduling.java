package pl.edu.agh.jobshop;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jacop.constraints.Diff2;
import org.jacop.constraints.Linear;
import org.jacop.constraints.Max;
import org.jacop.core.IntVar;
import org.jacop.core.Store;
import org.jacop.search.DepthFirstSearch;
import org.jacop.search.IndomainMin;
import org.jacop.search.Search;
import org.jacop.search.SelectChoicePoint;
import org.jacop.search.SimpleSelect;
import org.jacop.search.SmallestDomain;

public class JobshopScheduling {

	private static int BIG_NUMBER = 1000;
	private static final int MACHINES_NUMBER = 8;
	private static final int DELTA = 15;
	private static int it = 0;
	private static int lastJobId;
	private static List<Machine> machines;
	private static List<Job> jobs;
	private static Store store;
	private static List<Job> prevJobs;
	private static ArrayList<IntVar> vars = new ArrayList<IntVar>();

	public static void main(String[] args) {
		long T1, T2, T;
		T1 = System.currentTimeMillis();

		lastJobId = 0;
		store = new Store();

		machines = new ArrayList<Machine>();

		for (int i = 0; i < MACHINES_NUMBER; i++) {
			machines.add(new Machine(i));
		}
		jobs = new ArrayList<JobshopScheduling.Job>();

		jobshopScheduling();

		T2 = System.currentTimeMillis();
		T = T2 - T1;
		System.out.println("\n\t*** Execution time = " + T + " ms");
	}

	private static void jobshopScheduling() {
		Random rand = new Random();
		int numOfJobs = rand.nextInt(MACHINES_NUMBER) + 1;
		for (int id = lastJobId; id < numOfJobs + lastJobId; id++) {
			jobs.add(new Job(id));
			for (int i = 0; i < MACHINES_NUMBER; i++) {
				Machine m = machines.get(i);
				int duration = 2;
				jobs.get(id - lastJobId).addTask(m, duration, i);
			}
			jobs.get(id - lastJobId).setConstraints();
		}
		lastJobId += numOfJobs;

		if (prevJobs != null && !prevJobs.isEmpty()) {
			jobs.addAll(prevJobs);
		}

		for (int i = 0; i < machines.size(); i++) {
			machines.get(i).setConstraints();
		}

		IntVar[] v = vars.toArray(new IntVar[0]);

		IntVar[] lastTimes = new IntVar[jobs.size()];
		for (int i = 0; i < jobs.size(); i++) {
			lastTimes[i] = jobs.get(i).getLastEndTime();
		}

		IntVar lastJob = new IntVar(store, "lastJob", 0, 100);

		store.impose(new Max(lastTimes, lastJob));

		Search<IntVar> search = new DepthFirstSearch<IntVar>();

		SelectChoicePoint<IntVar> select = new SimpleSelect<IntVar>(v,
				new SmallestDomain<IntVar>(), new IndomainMin<IntVar>());

		boolean result = search.labeling(store, select, lastJob);

		if (result) {
			new Graph(machines, DELTA);
			System.out.println("Solution:");
			for (int i = 0; i < machines.size(); i++) {
				machines.get(i).printTasks(DELTA);
			}

			clearJobs();

			System.out.printf("\n\nLast job ends at %d\n", lastJob.value());
		} else
			System.out.println("No solution");

	}

	private static void clearJobs() {
		store = new Store();

		machines = new ArrayList<Machine>();

		for (int i = 0; i < MACHINES_NUMBER; i++) {
			machines.add(new Machine(i));
		}

		prevJobs = new ArrayList<Job>();
		for (Job job : jobs) {
			Job tmpJob = new Job(job.id - 1);
			for (Task task : job.tasks) {
				if (task.end.value() > DELTA) {
					tmpJob.addTask(machines.get(task.m.n), task.duration,
							task.n);
				}
			}
			if (!tmpJob.tasks.isEmpty()) {
				tmpJob.setConstraints();
				prevJobs.add(tmpJob);
			}
		}
		jobs = new ArrayList<JobshopScheduling.Job>();

		it++;
		if (it < 5) {
			jobshopScheduling();
		}
	}

	static class Machine {
		int n;
		ArrayList<Task> tasks = new ArrayList<Task>();
		Rectangles rect = new Rectangles();

		public Machine(int n) {
			this.n = n;
		}

		public void setConstraints() {
			store.impose(new Diff2(rect.getRectangles()));
		}

		public void addTask(Task t, int duration) {
			tasks.add(t);
			rect.addRectangle(t.start, duration);
		}

		@Override
		public String toString() {
			return "M" + n;
		}

		public void printTasks() {
			System.out.println(toString() + tasks);
		}

		public void printTasks(int delta) {
			ArrayList<Task> tmpTasks = new ArrayList<Task>();
			for (Task task : tasks) {
				if (task.end.value() <= delta) {
					tmpTasks.add(task);
				}
			}
			System.out.println(toString() + tmpTasks);
		}
	}

	private static class Job {

		ArrayList<Task> tasks = new ArrayList<Task>();
		int id;

		public Job(int job) {
			this.id = job + 1;
		}

		public void addTask(Machine m, int duration, int n) {
			Task t = new Task(duration, n, m, this);
			tasks.add(t);
		}

		public void setConstraints() {
			for (int i = 1; i < tasks.size(); i++) {
				store.impose(new Linear(store, new IntVar[] {
						tasks.get(i).start, tasks.get(i - 1).end }, new int[] {
						1, -1 }, ">=", 0));
			}
		}

		public IntVar getLastEndTime() {
			return tasks.get(tasks.size() - 1).end;
		}

		@Override
		public String toString() {
			return "Job" + id + ": " + tasks.toString();
		}

	}

	private static class Task {
		IntVar start;
		IntVar end;
		int n;
		Machine m;
		Job j;
		int duration;

		public Task(int duration, int n, Machine m, Job j) {
			this.duration = duration;
			this.m = m;
			this.n = n;
			this.j = j;
			start = new IntVar(store, "J" + j.id + " T" + n, 0, BIG_NUMBER);
			end = new IntVar(store, 0, BIG_NUMBER);
			vars.add(start);
			vars.add(end);
			m.addTask(this, duration);
			store.impose(new Linear(store, new IntVar[] { start, end },
					new int[] { -1, 1 }, "=", duration));
		}

		@Override
		public String toString() {
			return "Task" + n + " (" + start.value() + "," + end.value() + ")["
					+ m + ",J" + j.id + "]";
		}
	}

	static class Rectangles {
		private ArrayList<IntVar[]> rectangles;

		public Rectangles() {
			rectangles = new ArrayList<IntVar[]>();
		}

		public void addRectangle(IntVar starts, int length) {
			IntVar y = new IntVar(store, 1, 1);
			IntVar dy = new IntVar(store, 1, 1);
			IntVar dx = new IntVar(store, length, length);
			IntVar[] rect = new IntVar[] { starts, y, dx, dy };
			rectangles.add(rect);
		}

		public IntVar[][] getRectangles() {
			return rectangles.toArray(new IntVar[0][0]);
		}
	}

}
