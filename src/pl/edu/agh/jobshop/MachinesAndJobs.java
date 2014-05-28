package pl.edu.agh.jobshop;

import java.util.List;

import pl.edu.agh.jobshop.JobshopScheduling.Job;
import pl.edu.agh.jobshop.JobshopScheduling.Machine;

public class MachinesAndJobs {

	private List<Machine> machines;
	private List<Job> jobs;
	public MachinesAndJobs(List<Machine> machines, List<Job> jobs) {
		this.machines = machines;
		this.jobs = jobs;
	}
	public List<Machine> getMachines() {
		return machines;
	}
	public void setMachines(List<Machine> machines) {
		this.machines = machines;
	}
	public List<Job> getJobs() {
		return jobs;
	}
	public void setJobs(List<Job> jobs) {
		this.jobs = jobs;
	}
	
	
	
}
