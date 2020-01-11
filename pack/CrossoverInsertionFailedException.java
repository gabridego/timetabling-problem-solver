package pack;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class CrossoverInsertionFailedException extends Exception {
	private List<Integer> failedReinsertedExam = new ArrayList<>();

	public CrossoverInsertionFailedException(Integer failed) {
		super();
		this.failedReinsertedExam.add(failed);
	}
	
	public CrossoverInsertionFailedException(List<Integer> failed) {
		super();
		this.failedReinsertedExam = failed;
	}

	public List<Integer> getFailedReinsertedExam() {
		return failedReinsertedExam;
	}

	public CrossoverInsertionFailedException() {
		// TODO Auto-generated constructor stub
	}
}
