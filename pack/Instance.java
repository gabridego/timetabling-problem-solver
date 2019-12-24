package pack;
import java.util.Map;
import java.util.stream.Stream;
import java.util.HashMap;
import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Instance {
	private static Map<Integer,Integer> exams = new HashMap<>();
	private static Integer slots;
	private static Map<Integer,List<Integer>> students = new HashMap<>();
	
	private static void readExams(String fileName) throws IOException {
		try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
	        stream.forEach(s -> {
	        	String[] parts = s.split(" ");
	        	if(parts.length == 2)
	        		exams.put(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]));
	        });
		}
		// System.out.println(exams);
	}
	
	private static void readSlots(String fileName) throws IOException {
		try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
	        stream.forEach(s -> {
	        	slots = Integer.valueOf(s);
	        });
		}
		// System.out.println(slots);
	}
	
	private static void readStudents(String fileName) throws IOException {
		try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
	        stream.forEach(s -> {
	        	String[] parts = s.split(" ");
	        	parts[0] = parts[0].substring(1);
	        	if(parts.length == 2) {
	        		if(!students.containsKey(Integer.valueOf(parts[0])))
	        			students.put(Integer.valueOf(parts[0]), new ArrayList<Integer>());
	        		students.get(Integer.valueOf(parts[0])).add(Integer.valueOf(parts[1]));
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
	}

	public static Map<Integer, Integer> getExams() {
		return exams;
	}

	public static Integer getSlots() {
		return slots;
	}

	public static Map<Integer, List<Integer>> getStudents() {
		return students;
	}

}
