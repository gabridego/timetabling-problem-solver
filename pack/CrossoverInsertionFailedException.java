package pack;

@SuppressWarnings("serial")
public class CrossoverInsertionFailedException extends Exception {
	private Integer failedReinsertedExam = -1;

	public CrossoverInsertionFailedException(Integer failedReinsertedExam) {
		super();
		this.failedReinsertedExam = failedReinsertedExam;
	}

	public Integer getFailedReinsertedExam() {
		return failedReinsertedExam;
	}

	public CrossoverInsertionFailedException() {
		// TODO Auto-generated constructor stub
	}
	
	
}
