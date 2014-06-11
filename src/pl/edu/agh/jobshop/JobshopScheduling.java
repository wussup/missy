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
	private static final int MACHINES_NUMBER = 8;
	private static final int JOBS_NUMBER = 3;
	private static final int JOBS_MIN = 10;
	private static final int DURATIONS_NUMBER = 3;
	private static final int MIN_DURATION = 3;
	private static final int DELTA = 30;
	private static final int WINDOW_SIZE = DELTA * MACHINES_NUMBER;
	private static final int[] JOBS_ARR = new int[] { 1, 2 /* , 3 */};
	private static final int[] DUR_ARR = new int[] { 2, 3 /* 10, 15 */};
	private static int it = 0;
	private static int lastJobId;
	private static List<Machine> machines;
	private static List<Job> jobs;
	private static Store store;
	private static List<Job> prevJobs;
	private static ConcurrentHashMap<ConcurrentHashMap<Integer, List<Integer>>, List<Machine>> generatedMachines;
	private static ConcurrentHashMap<ConcurrentHashMap<Integer, List<Integer>>, List<Job>> generatedJobs;
	private static ArrayList<IntVar> vars = new ArrayList<IntVar>();
	private static boolean shouldWork;
	private static FileWriter fileWriter = new FileWriter("results.txt");
	private static float allSum = 0;

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

	private static MachinesAndJobs getRandomMachines(
			Map<Integer, List<Integer>> numOfJobsPerMachine) {
		boolean bool = true;
		int minimumTime = 999999;
		Map<Integer, List<Integer>> identTemp = null;
		Map<Integer, List<Integer>> ident = null;
		for (Map<Integer, List<Integer>> id : generatedMachines.keySet()) {
			bool = true;
			int time = 0;
			for (int j = 0; j < MACHINES_NUMBER; j++) {
				if (numOfJobsPerMachine.get(j).size() <= id.get(j).size()){
					for (int k = 0; k < numOfJobsPerMachine.get(j).size(); k++) {
						if (numOfJobsPerMachine.get(j).get(k) <= id.get(j).get(k)) {
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
					ident = identTemp;
				}
			}

		}
		if (ident != null) {
			List<Machine> value = generatedMachines.get(ident);
			return new MachinesAndJobs(value, generatedJobs.get(ident));
		}
		return null;
	}

	private static MachinesAndJobs getMachine(
			Map<Integer, List<Integer>> numOfJobsPerMachine) {
		for (Map<Integer, List<Integer>> conf : generatedMachines.keySet())
			if (isEqualsConfs(conf, numOfJobsPerMachine))
				return new MachinesAndJobs(generatedMachines.get(conf),
						generatedJobs.get(conf));
		return null;
	}

	private static boolean isEqualsConfs(Map<Integer, List<Integer>> conf,
			Map<Integer, List<Integer>> numOfJobsPerMachine) {
		int i = 0;
		for (List<Integer> list : conf.values()) {
			int j = 0;
			List<Integer> list2 = numOfJobsPerMachine.get(i);
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

	private static void runWorkers() {
		setShouldWork(true);
		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				generatedMachines = new ConcurrentHashMap<ConcurrentHashMap<Integer, List<Integer>>, List<Machine>>();
				generatedJobs = new ConcurrentHashMap<ConcurrentHashMap<Integer, List<Integer>>, List<Job>>();
				for (int num0 = JOBS_ARR.length - 1; num0 >= 0
						&& isShouldWork(); num0--) {
					int numOfJobs0 = JOBS_ARR[num0];
					for (int num1 = JOBS_ARR.length - 1; num1 >= 0
							&& isShouldWork(); num1--) {
						int numOfJobs1 = JOBS_ARR[num1];
						for (int num2 = JOBS_ARR.length - 1; num2 >= 0
								&& isShouldWork(); num2--) {
							int numOfJobs2 = JOBS_ARR[num2];
							for (int num3 = JOBS_ARR.length - 1; num3 >= 0
									&& isShouldWork(); num3--) {
								int numOfJobs3 = JOBS_ARR[num3];
								for (int num4 = JOBS_ARR.length - 1; num4 >= 0
										&& isShouldWork(); num4--) {
									int numOfJobs4 = JOBS_ARR[num4];
									for (int num5 = JOBS_ARR.length - 1; num5 >= 0
											&& isShouldWork(); num5--) {
										int numOfJobs5 = JOBS_ARR[num5];
										for (int num6 = JOBS_ARR.length - 1; num6 >= 0
												&& isShouldWork(); num6--) {
											int numOfJobs6 = JOBS_ARR[num6];
											for (int num7 = JOBS_ARR.length - 1; num7 >= 0
													&& isShouldWork(); num7--) {
												int numOfJobs7 = JOBS_ARR[num7];
												// for (int num8 = 0; num8 <
												// JOBS_ARR.length
												// && isShouldWork(); num8++) {
												// int numOfJobs8 =
												// JOBS_ARR[num8];
												// for (int num9 = 0; num9 <
												// JOBS_ARR.length
												// && isShouldWork(); num9++) {
												// int numOfJobs9 =
												// JOBS_ARR[num9];
												// for (int num10 = 0; num10 <
												// JOBS_ARR.length
												// && isShouldWork(); num10++) {
												// int numOfJobs10 =
												// JOBS_ARR[num10];
												// for (int num11 = 0; num11 <
												// JOBS_ARR.length
												// && isShouldWork(); num11++) {
												// int numOfJobs11 =
												// JOBS_ARR[num11];
												// for (int num12 = 0; num12 <
												// JOBS_ARR.length
												// && isShouldWork(); num12++) {
												// int numOfJobs12 =
												// JOBS_ARR[num12];
												// for (int num13 = 0; num13 <
												// JOBS_ARR.length
												// && isShouldWork(); num13++) {
												// int numOfJobs13 =
												// JOBS_ARR[num13];
												// for (int num14 = 0; num14 <
												// JOBS_ARR.length
												// && isShouldWork(); num14++) {
												// int numOfJobs14 =
												// JOBS_ARR[num14];
												for (long worker = 0; worker < DUR_ARR.length
														* DUR_ARR.length
														* DUR_ARR.length
														* DUR_ARR.length
														* DUR_ARR.length
														* DUR_ARR.length
														* DUR_ARR.length
														* DUR_ARR.length // numOfJobs0
												// * numOfJobs1
												// * numOfJobs2
												// * numOfJobs3
												// * numOfJobs4
												// * numOfJobs5
												// * numOfJobs6
												// * numOfJobs7
												// * numOfJobs8
												// * numOfJobs9
												// * numOfJobs10
												// * numOfJobs11
												// * numOfJobs12
												// * numOfJobs13
												// * numOfJobs14
														&& shouldWork; worker++) {
													Random rand = new Random();
													boolean work = false;
													ConcurrentHashMap<Integer, List<Integer>> numOfJobsPerMachine = new ConcurrentHashMap<Integer, List<Integer>>();
													// while
													// (work)
													// {
													numOfJobsPerMachine = new ConcurrentHashMap<Integer, List<Integer>>();
													List<Integer> list = new ArrayList<Integer>();
													for (int k = 0; k < numOfJobs0; k++) {
														list.add(DUR_ARR[rand
																.nextInt(DUR_ARR.length)]);
													}
													numOfJobsPerMachine.put(0,
															list);
													list = new ArrayList<Integer>();
													for (int k = 0; k < numOfJobs1; k++) {
														list.add(DUR_ARR[rand
																.nextInt(DUR_ARR.length)]);
													}
													numOfJobsPerMachine.put(1,
															list);
													list = new ArrayList<Integer>();
													for (int k = 0; k < numOfJobs2; k++) {
														list.add(DUR_ARR[rand
																.nextInt(DUR_ARR.length)]);
													}
													numOfJobsPerMachine.put(2,
															list);
													list = new ArrayList<Integer>();
													for (int k = 0; k < numOfJobs3; k++) {
														list.add(DUR_ARR[rand
																.nextInt(DUR_ARR.length)]);
													}
													numOfJobsPerMachine.put(3,
															list);
													list = new ArrayList<Integer>();
													for (int k = 0; k < numOfJobs4; k++) {
														list.add(DUR_ARR[rand
																.nextInt(DUR_ARR.length)]);
													}
													numOfJobsPerMachine.put(4,
															list);
													list = new ArrayList<Integer>();
													for (int k = 0; k < numOfJobs5; k++) {
														list.add(DUR_ARR[rand
																.nextInt(DUR_ARR.length)]);
													}
													numOfJobsPerMachine.put(5,
															list);
													list = new ArrayList<Integer>();
													for (int k = 0; k < numOfJobs6; k++) {
														list.add(DUR_ARR[rand
																.nextInt(DUR_ARR.length)]);
													}
													numOfJobsPerMachine.put(6,
															list);
													list = new ArrayList<Integer>();
													for (int k = 0; k < numOfJobs7; k++) {
														list.add(DUR_ARR[rand
																.nextInt(DUR_ARR.length)]);
													}
													numOfJobsPerMachine.put(7,
															list);
													// list = new
													// ArrayList<Integer>();
													// for (int k = 0; k <
													// numOfJobs8; k++) {
													// list.add(DUR_ARR[rand
													// .nextInt(DUR_ARR.length)]);
													// }
													// numOfJobsPerMachine
													// .put(8,
													// list);
													// list = new
													// ArrayList<Integer>();
													// for (int k = 0; k <
													// numOfJobs9; k++) {
													// list.add(DUR_ARR[rand
													// .nextInt(DUR_ARR.length)]);
													// }
													// numOfJobsPerMachine
													// .put(9,
													// list);
													// list = new
													// ArrayList<Integer>();
													// for (int k = 0; k <
													// numOfJobs10; k++) {
													// list.add(DUR_ARR[rand
													// .nextInt(DUR_ARR.length)]);
													// }
													// numOfJobsPerMachine
													// .put(10,
													// list);
													// list = new
													// ArrayList<Integer>();
													// for (int k = 0; k <
													// numOfJobs11; k++) {
													// list.add(DUR_ARR[rand
													// .nextInt(DUR_ARR.length)]);
													// }
													// numOfJobsPerMachine
													// .put(11,
													// list);
													// list = new
													// ArrayList<Integer>();
													// for (int k = 0; k <
													// numOfJobs12; k++) {
													// list.add(DUR_ARR[rand
													// .nextInt(DUR_ARR.length)]);
													// }
													// numOfJobsPerMachine
													// .put(12,
													// list);
													// list = new
													// ArrayList<Integer>();
													// for (int k = 0; k <
													// numOfJobs13; k++) {
													// list.add(DUR_ARR[rand
													// .nextInt(DUR_ARR.length)]);
													// }
													// numOfJobsPerMachine
													// .put(13,
													// list);
													// list = new
													// ArrayList<Integer>();
													// for (int k = 0; k <
													// numOfJobs14; k++) {
													// list.add(DUR_ARR[rand
													// .nextInt(DUR_ARR.length)]);
													// }
													// numOfJobsPerMachine
													// .put(14,
													// list);
													if (getMachine(numOfJobsPerMachine) == null)
														work = true;
													else
														System.err
																.println("JUZ JEST W BAZIE!!!");
													// }
													if (work) {
														boolean result = makeAllJob(
																numOfJobsPerMachine,
																false);
														if (result) {
															generatedMachines
																	.put(numOfJobsPerMachine,
																			machines);
															generatedJobs
																	.put(numOfJobsPerMachine,
																			jobs);
														}
														store = new Store();

														machines = new ArrayList<Machine>();

														for (int i = 0; i < MACHINES_NUMBER; i++) {
															machines.add(new Machine(
																	i));
														}

														List<Job> tmpPrevJobs = new ArrayList<JobshopScheduling.Job>();
														tmpPrevJobs
																.addAll(prevJobs);

														prevJobs = new ArrayList<Job>();
														for (Job job : tmpPrevJobs) {
															Job tmpJob = new Job(
																	job.id - 1);
															for (Task task : job.tasks) {
																tmpJob.addTask(
																		machines.get(task.m.n),
																		task.duration,
																		task.n);
															}
															if (!tmpJob.tasks
																	.isEmpty()) {
																tmpJob.setConstraints();
																prevJobs.add(tmpJob);
															}
														}
														jobs = new ArrayList<JobshopScheduling.Job>();
														vars = new ArrayList<IntVar>();
													}
												}
											}
										}
									}
								}
							}
						}
						// }
					}
					// }
					// }
					// }
					// }
					// }
					// }
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

	public static boolean checkStartEnds(ArrayList<StartEnd> startEnds,
			int start, int end) {
		for (StartEnd startEnd : startEnds)
			if ((start >= startEnd.getStart() && start < startEnd.getEnd())
					|| (end > startEnd.getStart() && end <= startEnd.getEnd()))
				return false;
		return true;
	}

	public static class Job {

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

	public synchronized static boolean isShouldWork() {
		return shouldWork;
	}

	public synchronized static void setShouldWork(boolean shouldWork) {
		JobshopScheduling.shouldWork = shouldWork;
	}

}
