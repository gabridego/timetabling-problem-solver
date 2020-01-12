package pack;

import java.util.concurrent.TimeUnit;

public class Main {

	public static void main(String[] args) {
		long start = System.nanoTime();
		if(args.length != 3 || !args[1].equals("-t")) {
			System.out.println("Arguments error!");
			System.exit(-1);
		}
		long dur = TimeUnit.SECONDS.toNanos(Integer.parseInt(args[2]));
		//Load instance
		Instance instance = new Instance(args[0]);
		
		//Generate initial population
		Population pop = new Population(10,instance, 90, start, dur, args[0]); //(popSize, instance, %popSubstituted, startTime, algorithmDuration)
		
		//Starting evolutionary process:
		pop.evolve();
		System.out.println("Terminating");
		System.out.println("");
		System.out.println("--------------------");
		System.out.println("");
	}

}
