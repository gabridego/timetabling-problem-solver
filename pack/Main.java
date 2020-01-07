package pack;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Main {

	public static void main(String[] args) {
		long start = System.nanoTime();
		if(args.length != 3 || !args[1].equals("-t")) {
			System.out.println("Arguments error!");
			System.exit(-1);
		}
		long dur = TimeUnit.SECONDS.toNanos(Integer.parseInt(args[2]));
		Instance instance = new Instance(args[0]);
		//System.out.println(instance.getListOfStudentsPerExam().keySet());
		/*for(Integer[] v : instance.getConflictMatrix()) {
			for(Integer x : v)
				System.out.print(x + " ");
			System.out.println();
		}*/
		try {
			instance.printConflictMatrix("conflict.txt");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//System.out.println(instance.getConflictingStudents());
		/*Individual ind = new Individual(instance);
		try {
			ind.printIndividual(args[0] + "_test.sol");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(ind.getFitness());*/
		Population pop = new Population(5,instance, 40, start, dur); //(popSize, instance, %popSubstituted, startTime, algorithmDuration)
		System.out.println(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
		/*int i=0;
		for(Individual ind : pop.getPopulation())
			try {
				//System.out.println(ind.getFitness());
				ind.printIndividual(args[0] + "_" + i++ + "test.sol");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
		/*while(System.nanoTime() < start + dur)
			System.out.println("a");*/
		
		//Starting evolutionary process:
		pop.evolve();
	}

}
