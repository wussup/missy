package pl.edu.agh.jobshop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

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

	/**
	 * Amount of machines
	 */
	private static final int MACHINES_NUMBER = 8;

	/**
	 * Time delta
	 */
	private static final int DELTA = 20;

	/**
	 * Window size
	 */
	private static final int WINDOW_SIZE = DELTA * MACHINES_NUMBER;

	/**
	 * Array of amount of jobs
	 */
	private static final int[] JOBS_ARR = new int[] { 1, 2, 3 };

	/**
	 * Array of job durations
	 */
	private static final int[] DUR_ARR = new int[] { 5, 10, 15 };

	/**
	 * Number of iteration
	 */
	private static int it = 0;

	/**
	 * Last executed job Id
	 */
	private static int lastJobId;

	/**
	 * List of using machnies
	 */
	private static List<Machine> machines;

	/**
	 * List of current jobs
	 */
	private static List<Job> jobs;

	/**
	 * Store that contains all constrains for DFS algorithm
	 */
	private static Store store;
	/**
	 * Jobs that was not executed at previous iteration
	 */
	private static List<Job> prevJobs;

	/**
	 * Map which contains configuration of tasks as a key and generated plan for
	 * these tasks as a value
	 */
	private static ConcurrentHashMap<ConcurrentHashMap<Integer, List<Integer>>, List<Machine>> generatedMachines;

	/**
	 * Map which contains configuration of tasks as a key and jobs as a value
	 */
	private static ConcurrentHashMap<ConcurrentHashMap<Integer, List<Integer>>, List<Job>> generatedJobs;
	/**
	 * List of constrains
	 */
	private static ArrayList<IntVar> vars = new ArrayList<IntVar>();
	/**
	 * Does thread should work
	 */
	private static boolean shouldWork;
	/**
	 * File writer results
	 */
	private static FileWriter fileWriter = new FileWriter("results.txt");
	/**
	 * File writer metric
	 */
	private static FileWriter fw = new FileWriter("metric.txt");
	/**
	 * All sum of tasks executed at one iteration
	 */
	private static float allSum = 0;

	/**
	 * Metric of tasks executed at one iteration
	 */
	private static float metric = 0;
	/**
	 * Metric number of tasks executed at one iteration
	 */
	private static float metricNumber = 0;

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
		System.out.println("\n\t*** Execution time = " + T + " ms\n");

	}

	/**
	 * Function draw new tasks and decide, what program should do with drawn
	 * tasks: generate plan for it or get plan from generated plans
	 */
	private static void jobshopScheduling() {
		Random rand = new Random();
		Map<Integer, List<Integer>> numOfJobsPerMachine = new LinkedHashMap<Integer, List<Integer>>();
		for (int i = 0; i < MACHINES_NUMBER; i++) {
			int numOfJobs = JOBS_ARR[rand.nextInt(JOBS_ARR.length)];
			List<Integer> durations = new ArrayList<Integer>();
			for (int j = 0; j < numOfJobs; j++) {
				durations.add(DUR_ARR[rand.nextInt(DUR_ARR.length)]);
			}
			numOfJobsPerMachine.put(i, durations);
		}
		if (generatedMachines == null) {
			boolean result = makeAllJob(numOfJobsPerMachine, true);

			if (result) {
				new Graph(machines, DELTA);
				System.out.println("Solution:");
				int sum = 0;
				for (int i = 0; i < machines.size(); i++) {
					sum += machines.get(i).printTasks(DELTA);
				}
				fileWriter.write("Efficiency: " + ((float) sum / WINDOW_SIZE)
						* 100 + "%\n");
				allSum += ((float) sum / WINDOW_SIZE) * 100;
				clearJobs(null);
			} else
				System.out.println("No solution");
		} else {
			MachinesAndJobs machinesAndJobs = getMachine(numOfJobsPerMachine);
			if (machinesAndJobs == null) {
				fileWriter.write("Wybralem randomowo z listy planow\n");
				machinesAndJobs = getRandomMachines(numOfJobsPerMachine);
			} else {
				fileWriter.write("Wybralem z listy planow\n");
			}
			machines = machinesAndJobs.getMachines();
			lastJobId += MACHINES_NUMBER;
			new Graph(machines, DELTA);
			System.out.println("Solution:");
			int sum = 0;
			for (int i = 0; i < machines.size(); i++) {
				sum += machines.get(i).printTasks(DELTA);
			}
			fileWriter.write("Efficiency: " + ((float) sum / WINDOW_SIZE) * 100
					+ "%\n");
			allSum += ((float) sum / WINDOW_SIZE) * 100;
			clearJobs(machinesAndJobs.getJobs());
		}

	}

	/**
	 * Search for best plan for drawn tasks
	 * 
	 * @param numOfJobsPerMachine
	 *            tasks
	 * @return best plan
	 */
	private static MachinesAndJobs getRandomMachines(
			Map<Integer, List<Integer>> numOfJobsPerMachine) {
		boolean bool = true;
		int minimumTime = 999999;
		Map<Integer, List<Integer>> identTemp = null;
		Map<Integer, List<Integer>> ident = null;
		for (Map<Integer, List<Integer>> id : generatedMachines.keySet()) {
			bool = true;
			int time = 0;
			for (int j = 0; j < MACHINES_NUMBER && bool; j++) {
				if (numOfJobsPerMachine.get(j).size() <= id.get(j).size()) {
					for (int k = 0; k < numOfJobsPerMachine.get(j).size()
							&& bool; k++) {
						if (numOfJobsPerMachine.get(j).get(k) <= id.get(j).get(
								k)) {
							time += id.get(j).get(k);
							identTemp = id;
						} else {
							bool = false;
						}

					}
				} else {
					bool = false;
				}
			}

			if (bool) {
				if (time < minimumTime) {
					minimumTime = time;
					ident = identTemp;
				}
			}

		}
		if (ident != null) {
			List<Machine> value = generatedMachines.get(ident);
			int generadetPlanTime = 0;
			int selectedPlanTime = 0;
			float percent;
			for (int j = 0; j < MACHINES_NUMBER; j++) {
				for (int i = 0; i < numOfJobsPerMachine.get(j).size(); i++) {
					selectedPlanTime += numOfJobsPerMachine.get(j).get(i);
				}
				for (int i = 0; i < ident.get(j).size(); i++) {
					generadetPlanTime += ident.get(j).get(i);
				}
			}

			percent = (float) (generadetPlanTime - selectedPlanTime)
					/ generadetPlanTime;
			fw.write("Iteration " + it + ": " + "gen: " + generadetPlanTime
					+ "; sel: " + selectedPlanTime + "; metric: " + percent
					* 100 + " %\n\n");
			metricNumber += (generadetPlanTime - selectedPlanTime);
			metric += percent * 100;
			if (it == 4) {
				metric /= 4;
				metricNumber /= 4;
				fw.write("\nSumaric metric: " + metric + " %.");
				fw.write("\nNumber of metric: " + metricNumber + "\n");
			}
			return new MachinesAndJobs(value, generatedJobs.get(ident));
		}
		return null;
	}

	/**
	 * Search for identical tasks in generated plans and return plan if found
	 * 
	 * @param numOfJobsPerMachine
	 *            tasks
	 * @return plan
	 */
	private static MachinesAndJobs getMachine(
			Map<Integer, List<Integer>> numOfJobsPerMachine) {
		for (Map<Integer, List<Integer>> conf : generatedMachines.keySet())
			if (isEqualsConfs(conf, numOfJobsPerMachine))
				return new MachinesAndJobs(generatedMachines.get(conf),
						generatedJobs.get(conf));
		return null;
	}

	/**
	 * Check if tasks are identical
	 * 
	 * @param conf
	 *            first configuration of tasks
	 * @param conf2
	 *            second configuration of tasks
	 * @return true if equals
	 */
	private static boolean isEqualsConfs(Map<Integer, List<Integer>> conf,
			Map<Integer, List<Integer>> conf2) {
		int i = 0;
		for (List<Integer> list : conf.values()) {
			int j = 0;
			List<Integer> list2 = conf2.get(i);
			if (list.size() != list2.size())
				return false;
			for (Integer it : list) {
				Integer it2 = list2.get(j);
				if (it != it2)
					return false;
				j++;
			}
			i++;
		}
		return true;
	}

	/**
	 * Generate one plan for one configuration of tasks
	 * 
	 * @param numOfJobsPerMachine
	 *            tasks
	 * @param shouldChangeLastJobId
	 *            should or not
	 * @return info about good conclusion
	 */
	private static boolean makeAllJob(
			Map<Integer, List<Integer>> numOfJobsPerMachine,
			boolean shouldChangeLastJobId) {
		int tmpLastId = 0;
		if (!shouldChangeLastJobId)
			tmpLastId = lastJobId;
		for (int i = 0; i < MACHINES_NUMBER; i++) {
			Machine m = machines.get(i);
			int numOfTasks = numOfJobsPerMachine.get(i).size();
			int id = shouldChangeLastJobId ? lastJobId : tmpLastId;
			Job tmpJob = new Job(id);
			for (int j = 0; j < numOfTasks; j++) {
				int duration = numOfJobsPerMachine.get(i).get(j);
				tmpJob.addTask(m, duration, j);
			}
			tmpJob.setConstraints();
			jobs.add(tmpJob);
			if (shouldChangeLastJobId)
				lastJobId++;
			else
				tmpLastId++;
		}

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

		IntVar lastJob = new IntVar(store, "lastJob", 0, 10000);

		store.impose(new Max(lastTimes, lastJob));

		Search<IntVar> search = new DepthFirstSearch<IntVar>();

		SelectChoicePoint<IntVar> select = new SimpleSelect<IntVar>(v,
				new SmallestDomain<IntVar>(), new IndomainMin<IntVar>());

		boolean result = search.labeling(store, select, lastJob);
		return result;
	}

	/**
	 * Clear all used info about tasks on iteration end, save tasks that was not
	 * executed
	 * 
	 * @param js
	 *            list of jobs
	 */
	private static void clearJobs(List<Job> js) {
		List<Job> searchInJobs;
		if (js != null)
			searchInJobs = js;
		else
			searchInJobs = jobs;
		store = new Store();

		machines = new ArrayList<Machine>();

		for (int i = 0; i < MACHINES_NUMBER; i++) {
			machines.add(new Machine(i));
		}
		prevJobs = new ArrayList<Job>();
		for (Job job : searchInJobs) {
			Job tmpJob = new Job(job.id - 1);
			ArrayList<StartEnd> startEnds = new ArrayList<StartEnd>();
			for (Task task : job.tasks) {
				if (!(task.end.value() <= DELTA && checkStartEnds(startEnds,
						task.start.value(), task.end.value()))) {
					tmpJob.addTask(machines.get(task.m.n), task.duration,
							task.n);
				} else {
					startEnds.add(new StartEnd(task.start.value(), task.end
							.value()));
				}
			}
			if (!tmpJob.tasks.isEmpty()) {
				tmpJob.setConstraints();
				prevJobs.add(tmpJob);
			}
		}
		jobs = new ArrayList<JobshopScheduling.Job>();
		vars = new ArrayList<IntVar>();

		it++;
		if (it < 5) {
			runWorkers();
			jobshopScheduling();
		} else {
			fileWriter.write("Average efficiency: " + allSum / 5 + "%\n");
		}
	}

	/**
	 * Run thread which generate plans
	 */
	private static void runWorkers() {
		setShouldWork(true);
		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				generatedMachines = new ConcurrentHashMap<ConcurrentHashMap<Integer, List<Integer>>, List<Machine>>();
				generatedJobs = new ConcurrentHashMap<ConcurrentHashMap<Integer, List<Integer>>, List<Job>>();
				generateTheMostBiggerPlan();
				while (isShouldWork()) {
					Random rand = new Random();
					boolean work = false;
					ConcurrentHashMap<Integer, List<Integer>> numOfJobsPerMachine = new ConcurrentHashMap<Integer, List<Integer>>();
					numOfJobsPerMachine = new ConcurrentHashMap<Integer, List<Integer>>();
					List<Integer> list = new ArrayList<Integer>();
					int numOfJobs0 = JOBS_ARR[rand.nextInt(JOBS_ARR.length)];
					int numOfJobs1 = JOBS_ARR[rand.nextInt(JOBS_ARR.length)];
					int numOfJobs2 = JOBS_ARR[rand.nextInt(JOBS_ARR.length)];
					int numOfJobs3 = JOBS_ARR[rand.nextInt(JOBS_ARR.length)];
					int numOfJobs4 = JOBS_ARR[rand.nextInt(JOBS_ARR.length)];
					int numOfJobs5 = JOBS_ARR[rand.nextInt(JOBS_ARR.length)];
					int numOfJobs6 = JOBS_ARR[rand.nextInt(JOBS_ARR.length)];
					int numOfJobs7 = JOBS_ARR[rand.nextInt(JOBS_ARR.length)];
					for (int k = 0; k < numOfJobs0; k++) {
						list.add(DUR_ARR[rand.nextInt(DUR_ARR.length)]);
					}
					numOfJobsPerMachine.put(0, list);
					list = new ArrayList<Integer>();
					for (int k = 0; k < numOfJobs1; k++) {
						list.add(DUR_ARR[rand.nextInt(DUR_ARR.length)]);
					}
					numOfJobsPerMachine.put(1, list);
					list = new ArrayList<Integer>();
					for (int k = 0; k < numOfJobs2; k++) {
						list.add(DUR_ARR[rand.nextInt(DUR_ARR.length)]);
					}
					numOfJobsPerMachine.put(2, list);
					list = new ArrayList<Integer>();
					for (int k = 0; k < numOfJobs3; k++) {
						list.add(DUR_ARR[rand.nextInt(DUR_ARR.length)]);
					}
					numOfJobsPerMachine.put(3, list);
					list = new ArrayList<Integer>();
					for (int k = 0; k < numOfJobs4; k++) {
						list.add(DUR_ARR[rand.nextInt(DUR_ARR.length)]);
					}
					numOfJobsPerMachine.put(4, list);
					list = new ArrayList<Integer>();
					for (int k = 0; k < numOfJobs5; k++) {
						list.add(DUR_ARR[rand.nextInt(DUR_ARR.length)]);
					}
					numOfJobsPerMachine.put(5, list);
					list = new ArrayList<Integer>();
					for (int k = 0; k < numOfJobs6; k++) {
						list.add(DUR_ARR[rand.nextInt(DUR_ARR.length)]);
					}
					numOfJobsPerMachine.put(6, list);
					list = new ArrayList<Integer>();
					for (int k = 0; k < numOfJobs7; k++) {
						list.add(DUR_ARR[rand.nextInt(DUR_ARR.length)]);
					}
					numOfJobsPerMachine.put(7, list);

					if (getMachine(numOfJobsPerMachine) == null)
						work = true;
					else
						System.err.println("JUZ JEST W BAZIE!!!");
					if (work) {
						boolean result = makeAllJob(numOfJobsPerMachine, false);
						if (result) {
							generatedMachines
									.put(numOfJobsPerMachine, machines);
							generatedJobs.put(numOfJobsPerMachine, jobs);
						}
						store = new Store();

						machines = new ArrayList<Machine>();

						for (int i = 0; i < MACHINES_NUMBER; i++) {
							machines.add(new Machine(i));
						}

						List<Job> tmpPrevJobs = new ArrayList<JobshopScheduling.Job>();
						tmpPrevJobs.addAll(prevJobs);

						prevJobs = new ArrayList<Job>();
						for (Job job : tmpPrevJobs) {
							Job tmpJob = new Job(job.id - 1);
							for (Task task : job.tasks) {
								tmpJob.addTask(machines.get(task.m.n),
										task.duration, task.n);
							}
							if (!tmpJob.tasks.isEmpty()) {
								tmpJob.setConstraints();
								prevJobs.add(tmpJob);
							}
						}
						jobs = new ArrayList<JobshopScheduling.Job>();
						vars = new ArrayList<IntVar>();
					}
				}

			}
		});
		thread.start();
		try {
			Thread.sleep(DELTA * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		setShouldWork(false);
		while (thread.isAlive())
			;
	}

	/**
	 * Generate the most bigger plan
	 */
	protected static void generateTheMostBiggerPlan() {
		ConcurrentHashMap<Integer, List<Integer>> numOfJobsPerMachine = new ConcurrentHashMap<Integer, List<Integer>>();

		numOfJobsPerMachine = new ConcurrentHashMap<Integer, List<Integer>>();
		List<Integer> list = new ArrayList<Integer>();
		int numOfJobs = JOBS_ARR[JOBS_ARR.length - 1];
		int duration = DUR_ARR[DUR_ARR.length - 1];
		for (int k = 0; k < numOfJobs; k++) {
			list.add(duration);
		}
		numOfJobsPerMachine.put(0, list);
		list = new ArrayList<Integer>();
		for (int k = 0; k < numOfJobs; k++) {
			list.add(duration);
		}
		numOfJobsPerMachine.put(1, list);
		list = new ArrayList<Integer>();
		for (int k = 0; k < numOfJobs; k++) {
			list.add(duration);
		}
		numOfJobsPerMachine.put(2, list);
		list = new ArrayList<Integer>();
		for (int k = 0; k < numOfJobs; k++) {
			list.add(duration);
		}
		numOfJobsPerMachine.put(3, list);
		list = new ArrayList<Integer>();
		for (int k = 0; k < numOfJobs; k++) {
			list.add(duration);
		}
		numOfJobsPerMachine.put(4, list);
		list = new ArrayList<Integer>();
		for (int k = 0; k < numOfJobs; k++) {
			list.add(duration);
		}
		numOfJobsPerMachine.put(5, list);
		list = new ArrayList<Integer>();
		for (int k = 0; k < numOfJobs; k++) {
			list.add(duration);
		}
		numOfJobsPerMachine.put(6, list);
		list = new ArrayList<Integer>();
		for (int k = 0; k < numOfJobs; k++) {
			list.add(duration);
		}
		numOfJobsPerMachine.put(7, list);
		boolean result = makeAllJob(numOfJobsPerMachine, false);
		if (result) {
			generatedMachines.put(numOfJobsPerMachine, machines);
			generatedJobs.put(numOfJobsPerMachine, jobs);
		}
		store = new Store();

		machines = new ArrayList<Machine>();

		for (int i = 0; i < MACHINES_NUMBER; i++) {
			machines.add(new Machine(i));
		}

		List<Job> tmpPrevJobs = new ArrayList<JobshopScheduling.Job>();
		tmpPrevJobs.addAll(prevJobs);

		prevJobs = new ArrayList<Job>();
		for (Job job : tmpPrevJobs) {
			Job tmpJob = new Job(job.id - 1);
			for (Task task : job.tasks) {
				tmpJob.addTask(machines.get(task.m.n), task.duration, task.n);
			}
			if (!tmpJob.tasks.isEmpty()) {
				tmpJob.setConstraints();
				prevJobs.add(tmpJob);
			}
		}
		jobs = new ArrayList<JobshopScheduling.Job>();
		vars = new ArrayList<IntVar>();
	}

	/**
	 * Machine
	 */
	static class Machine {
		/**
		 * Number
		 */
		int n;
		/**
		 * Tasks on machine
		 */
		ArrayList<Task> tasks = new ArrayList<Task>();
		/**
		 * Graphic representation of tasks
		 */
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

		public int printTasks(int delta) {
			ArrayList<Task> tmpTasks = new ArrayList<Task>();
			ArrayList<StartEnd> startEnds = new ArrayList<StartEnd>();
			int sum = 0;
			for (Task task : tasks) {
				if (task.end.value() <= delta
						&& checkStartEnds(startEnds, task.start.value(),
								task.end.value())) {
					tmpTasks.add(task);
					startEnds.add(new StartEnd(task.start.value(), task.end
							.value()));
					sum += task.duration;
				}
			}
			System.out.println(toString() + tmpTasks);
			return sum;
		}

	}

	/**
	 * Check tasks for conclusions
	 * 
	 * @param startEnds
	 *            table of starts and ends of tasks
	 * @param start
	 *            start of one task
	 * @param end
	 *            end of one task
	 * @return true if conclusion exists
	 */
	public static boolean checkStartEnds(ArrayList<StartEnd> startEnds,
			int start, int end) {
		for (StartEnd startEnd : startEnds)
			if ((start >= startEnd.getStart() && start < startEnd.getEnd())
					|| (end > startEnd.getStart() && end <= startEnd.getEnd()))
				return false;
		return true;
	}

	/**
	 * Job
	 */
	public static class Job {

		/**
		 * List of tasks
		 */
		ArrayList<Task> tasks = new ArrayList<Task>();
		/**
		 * Id
		 */
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

	/**
	 * Task
	 */
	private static class Task {
		/**
		 * Start of task
		 */
		IntVar start;
		/**
		 * End of task
		 */
		IntVar end;
		/**
		 * Number
		 */
		int n;
		/**
		 * Machine
		 */
		Machine m;
		/**
		 * Job
		 */
		Job j;
		/**
		 * Duration
		 */
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

	/**
	 * Graphical representation of tasks
	 */
	static class Rectangles {
		/**
		 * Rectangles
		 */
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

	public synchronized static boolean isShouldWork() {
		return shouldWork;
	}

	public synchronized static void setShouldWork(boolean shouldWork) {
		JobshopScheduling.shouldWork = shouldWork;
	}

}
