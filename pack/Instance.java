package pack;
import java.util.Map;
import java.util.stream.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

public class Instance {
	private Map<Integer,Integer> numberOfStudentsPerExam = new LinkedHashMap<>();
	private Integer numberOfSlots;
	private Map<Integer,List<Integer>> listOfExamsPerStudent = new LinkedHashMap<>();
	private Map<Integer,List<Integer>> listOfStudentsPerExam = new LinkedHashMap<>();
	private Integer numberOfExams;
	private Integer numberOfStudents;
	private Integer maxExam;
	private Integer[][] conflictMatrix;
	private Map<Integer,Integer> conflictingStudents = new LinkedHashMap<>();
	
	private void readExams(String fileName) throws IOException {
		try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
	        stream.forEach(s -> {
	        	String[] parts = s.split(" ");
	        	if(parts.length == 2)
	        		numberOfStudentsPerExam.put(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]));
	        });
		}
		// System.out.println(exams);
	}
	
	private void readSlots(String fileName) throws IOException {
		try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
	        stream.forEach(s -> {
	        	numberOfSlots = Integer.valueOf(s);
	        });
		}
		// System.out.println(slots);
	}
	
	private void readStudents(String fileName) throws IOException {
		try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
	        stream.forEach(s -> {
	        	String[] parts = s.split(" ");
	        	parts[0] = parts[0].substring(1);
	        	if(parts.length == 2) {
	        		if(!listOfExamsPerStudent.containsKey(Integer.valueOf(parts[0])))
	        			listOfExamsPerStudent.put(Integer.valueOf(parts[0]), new ArrayList<Integer>());
	        		listOfExamsPerStudent.get(Integer.valueOf(parts[0])).add(Integer.valueOf(parts[1]));
	        		
	        		if(!listOfStudentsPerExam.containsKey(Integer.valueOf(parts[1])))
	        			listOfStudentsPerExam.put(Integer.valueOf(parts[1]), new ArrayList<Integer>());
	        		listOfStudentsPerExam.get(Integer.valueOf(parts[1])).add(Integer.valueOf(parts[0]));
	        	}	        		
	        });
		}
		// System.out.println(students);
	}
	
	public Instance(String instanceName) {
		try {
			readExams(instanceName + ".exm");
		} catch (IOException e) {
			System.out.println("Error reading " + instanceName + ".exm");
			System.exit(-1);
		}
		this.numberOfExams = this.numberOfStudentsPerExam.size();
		this.maxExam = Collections.max(this.numberOfStudentsPerExam.keySet());
		this.conflictMatrix = new Integer[this.maxExam+1][this.maxExam+1];
		for(int i = 0; i <= this.maxExam; i++)
			for(int j = 0; j <= this.maxExam; j++)
				this.conflictMatrix[i][j] = 0;
		
		try {
			readSlots(instanceName + ".slo");
		} catch (IOException e) {
			System.out.println("Error reading " + instanceName + ".slo");
			System.exit(-1);
		}
		
		try {
			readStudents(instanceName + ".stu");
		} catch (IOException e) {
			System.out.println("Error reading " + instanceName + ".slo");
			System.exit(-1);
		}
		this.numberOfStudents = this.listOfExamsPerStudent.keySet().size();
		
		// Feed conflict matrix with number of conflicting students
		for(int i : this.listOfStudentsPerExam.keySet())
			for(int j : this.listOfStudentsPerExam.keySet()) {
				if(j > i) {
					List<Integer> common = new ArrayList<Integer>(listOfStudentsPerExam.get(i));
					common.retainAll(listOfStudentsPerExam.get(j));
					if(!common.isEmpty())
						this.conflictMatrix[i][j] = this.conflictMatrix[j][i] = common.size();
				}
			}
		
		// Map exams to number of conflicting students and sort
		for(int i : this.listOfStudentsPerExam.keySet()) {
			Integer sum = 0;
			for(int x : this.conflictMatrix[i])
				sum += x;
			this.conflictingStudents.put(i, sum);
		}
		this.conflictingStudents = this.conflictingStudents.entrySet().stream().sorted(Map.Entry.<Integer,Integer>comparingByValue()
			.reversed()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		//System.out.println(this.conflictingStudents);
	}
	
	public void printConflictMatrix(String fileName) throws IOException {
		FileWriter fw = new FileWriter(fileName);
		PrintWriter pw = new PrintWriter(fw);
		pw.print("\t");
		for(int exam = 0; exam <= this.maxExam; exam++)
			pw.print(exam + "\t");
		pw.print("\n");
		
		for(int i = 0; i <= this.maxExam; i++) {
			pw.print(i + "\t");
			for(int j = 0; j <= this.maxExam; j++)
				pw.print(this.conflictMatrix[i][j] + "\t");
			pw.print("\n");
		}
		pw.close();
		//fw.close();
	}

	public Integer getMaxExam() {
		return maxExam;
	}

	public Integer[][] getConflictMatrix() {
		return conflictMatrix;
	}

	public Map<Integer, Integer> getConflictingStudents() {
		return conflictingStudents;
	}

	public Map<Integer, Integer> getNumberOfStudentsPerExam() {
		return numberOfStudentsPerExam;
	}

	public Integer getNumberOfSlots() {
		return numberOfSlots;
	}
	
	public Integer getNumberOfStudents() {
		return numberOfStudents;
	}

	public Map<Integer, List<Integer>> getListOfExamsPerStudent() {
		return listOfExamsPerStudent;
	}

	public Map<Integer, List<Integer>> getListOfStudentsPerExam() {
		return listOfStudentsPerExam;
	}

}
