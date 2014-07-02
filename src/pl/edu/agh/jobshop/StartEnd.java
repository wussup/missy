package pl.edu.agh.jobshop;

/**
 * Helper class which represents start and end of one task on machine
 */
public class StartEnd {

	private int start;
	private int end;

	public StartEnd(int start, int end) {
		this.start = start;
		this.end = end;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}

}
