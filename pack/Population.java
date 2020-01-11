package pack;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class Population {
	private Integer popSize;
	private Individual[] pop;
	private int individualsToUpdatePerIteration;
	private long start;
	private long duration;
	private float[] genOpProbabilities;
	private String outputFile;
	
	//Arbitrary parameters (NOT BAD at 0.9 0.1 0.7)
	final private float crossover = (float) 0.9;
	final private float mutation = (float) 0.1;
	final private float maxMovingProbability = (float) 0.7;
	
	public Population(Integer popSize, Instance instance, float percentage, long start, long duration, String outputFile) {
		this.popSize = popSize;
		this.outputFile = outputFile+"_DMOgroup07.sol";
		
		this.individualsToUpdatePerIteration=(int) (this.popSize*(percentage/100));
		if (this.individualsToUpdatePerIteration<1) {
			this.individualsToUpdatePerIteration=1;
		} else if(this.individualsToUpdatePerIteration>=this.popSize) {
			this.individualsToUpdatePerIteration=this.popSize-1;
		}
		
		this.start = start;
		this.duration=duration;
		
		this.genOpProbabilities = new float[2];
		this.genOpProbabilities[0]=crossover;
		this.genOpProbabilities[1]=mutation;
		
		pop = new Individual[popSize];
		for(int i = 0; i < popSize; i++) {
			pop[i] = new Individual(instance);
			System.out.println(i + ": " + pop[i].getAssignment());
			System.out.println(i + ": " + 1/pop[i].getFitness());
			if(!pop[i].isFeasible()) {
				System.out.println("Non feasible individual " + i);
			}
		}
	}

	public Integer getPopSize() {
		return popSize;
	}

	public Individual[] getPopulation() {
		return pop;
	}
	
	private void adjustProbabilities() {
		//TODO: possibly do something more in case crossover/mutation failed at previous iteration
		float passedTimePercentage = (float) 100/( (float) duration / ( (float) (System.nanoTime()-this.start) ) );	//compute percentage of elapsed time
		System.out.println(passedTimePercentage+"% of the available time has passed");
																													//Starting from a fixed max amount of movable probability
		float movingProbability = (float) this.maxMovingProbability*passedTimePercentage/100;						//move a portion of that amount proportionally to elapsed time
		
		this.genOpProbabilities[0] = this.crossover - movingProbability;											//from the crossover (most probable at beginning)
		this.genOpProbabilities[1] = this.mutation + movingProbability;												//to mutation (most probable at end)
		
		System.out.println("");
		System.out.println("New probabilities:");
		System.out.println("	crossover: "+this.genOpProbabilities[0]);
		System.out.println("	mutation: "+this.genOpProbabilities[1]);
		System.out.println("");
		/*
		try {																										//TODO: remove this waiting block
			Thread.sleep(250);
		} catch (InterruptedException e) {}
		*/
	}
	
	private Individual[] hybridization(Individual[] offsprings) {
		Individual[] hybridizedOffsprings = new Individual[individualsToUpdatePerIteration];						//new array with same size as offspings
		int c = 0;
		for(Individual i : offsprings) {
			hybridizedOffsprings[c] = i.hybridize();																//new offsprings are the improved ones
			c++;
		}
		return hybridizedOffsprings;
	}
	
	private int getSum(Map<Integer, Float> m) {
		float sum = m.values().stream().reduce((float)0, (a, b) -> a+b);
		return (int)(sum*1000);
	}
	
	public List<Integer> selectNbyFitness(Map<Integer, Float> m, int n) {
		
		Map<Integer, Float> copy = new HashMap<>();							//create a copy that can be modified
		for (Map.Entry<Integer,Float> entry : m.entrySet()) {
			copy.put(entry.getKey(), entry.getValue());
		}
		
		Map<Integer, Float> orderedCopy =									//sorted version (descending value) --> give precedence to highest fitness ones
			    copy.entrySet().stream()											
			       .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
			       .collect(Collectors.toMap(
			    		   Map.Entry::getKey, Map.Entry::getValue, (e1,e2) -> e1, LinkedHashMap::new));
		
		List<Integer> res = new LinkedList<Integer>();						//result list with all selected elements
		Random generator = new Random();									//random number generator
		int i;
		for(i=0; i<n; i++) {												//do the following N times:
			int incremental = 0;											//accumulator for finding the key
			int random = generator.nextInt(getSum(orderedCopy));			//get an int in range [0, sumOfFitnesses*1000-1]
			/*
			System.out.println("");
			System.out.println(getSum(orderedCopy));
			System.out.println(random);
			*/
			int j=0;
			for(Map.Entry<Integer, Float> e : orderedCopy.entrySet()) {		//loop on the ordered entries
				j++;
				incremental+=(int)(e.getValue()*1000);						//increase by value until reach random number
				if(incremental>random) {									//when found
					res.add(e.getKey());									//add element to result list
					orderedCopy.remove(e.getKey());							//remove element from map to avoid repetitions
					break;
				}
				if(j==orderedCopy.size()) {									//it means it was picked the last element
					res.add(e.getKey());									//add element to result list
					orderedCopy.remove(e.getKey());							//remove element from map to avoid repetitions (might be useless)
					break;
				}
			}
		}
		/*
		System.out.println("");
		System.out.println(copy);
		System.out.println(orderedCopy);
		System.out.println(res);
		*/
		/*
		if (res.size() != n) {
			System.out.println("---THERES A PROBLEM IN YOUR FUNTION---");
			System.out.println("n: "+n);
			System.out.println("m: "+m);
			System.out.println("copy: "+copy);
			System.out.println("orderedCopy: "+orderedCopy);
			System.out.println("res: "+res);
			System.exit(1);
		}
		*/
		return res;
	}
	
	public void evolve() {
		//Method to be called on the Population object to start the evolutionary process after initialization
		/*
		 * STEPS:
		 * 0. Allocate a structure for storing fitness of each population element 					--> DONE: map
		 * 1. Select individuals for reproduction 													--> DONE
		 * 	1.1 Decide how many must reproduce 															--> DONE: percentage passed as Population argument
		 * 	1.2 Find the ones with best fitness 														--> W.I.P. TODO may randomize better
		 * 2. Reproduction through some genetic operators											--> W.I.P.
		 * 	2.1 Manage probabilistic aspect of genetic operator selection 								--> DONE: custom probability distribution system
		 * 		(possibly with varying probabilities at runtime, this may exploit the time arg)
		 *   2.1.1 Bigger use of crossover at beginning													--> DONE
		 *   2.1.2 Mutation to diversify (Comparison of best fitness with average fitness)				--> DONE
		 * 	2.2 Apply one of the three genetic operators: 												--> DONE
		 * 		crossover (standard/order/partiallyMapped), mutation, inversion
		 *  2.3 Decide whether to improve offsprings or not through local search 						--> W.I.P. TODO implement local search
		 *  	(hybridization --> memetic algorithm) 
		 * 3. Population updating																	--> W.I.P.
		 * 	3.1 select whether to use Population replacement or Steady state and with which parameters 	--> DONE
		 * 	(elitist approach / % of pop subtituted) ( keep constant total population)
		 * 	3.2 select weakest elements of the population to be substituted								--> W.I.P.
		 * 4. Save the result (write the file)														--> DONE
		 * 	4.1 find new best solution in the population												--> DONE
		 * 	4.2 write the file (pay attention to the format) with the new best solution					--> DONE
		 */
		
		
		System.out.println("");
		System.out.println("--------------------");
		System.out.println("Beginning evolution:");
		

		
		int iteratCnt = 1;
		Random rand = new Random();
		while(iteratCnt>0 && (System.nanoTime()-start)<duration) {							//Endless loop - TODO: remove time constraint
			System.out.println("Iteration: "+iteratCnt);
			System.out.println("");
			
			// 0. Data structure allocation:
			float avgFit1=(float) 0.0, bestFit1=(float) 0.0;
			Map<Integer,Float> fitnessMap = new HashMap<>(); 								// build a map to store couples: individualId - fitness
			for (Individual i : pop) {
				fitnessMap.put(i.getId(), i.getFitness());
				avgFit1 += i.getFitness();
				if (i.getFitness()>bestFit1) {
					bestFit1=i.getFitness();
				}
			}
			avgFit1/=popSize;
			System.out.println("Beginning statistics:");
			System.out.println("	best fitness: "+bestFit1);
			System.out.println("	average fitness: "+avgFit1);
			System.out.println("");
			//System.out.println(fitnessMap);
			
			
			//1. Select individuals for reproduction
			List<Integer> parents = selectNbyFitness(fitnessMap, individualsToUpdatePerIteration);
			
			//2. Reproduction
			this.adjustProbabilities();													//Rebalance probabilities according to elapsed time
			int r = rand.nextInt(100) + 1;												//Generate random number in range [1,100]
			Individual[] offsprings = new Individual[individualsToUpdatePerIteration];	//the amount of generated offsprings is the same of the substituted ones
			
			int reproducedElem = 0;														//keep count of how many reproduced up to now
			boolean crossoverFlag = false;												//crossover takes two elements --> this is needed to skip an element
			int tmpElem = -1;															//to store temporarily an element before crossover
			
			System.out.println("Reproducing by: ");
			for (int i : parents){									//loop on the IDs of the individuals to reproduce
				//System.out.println("CURRENT POPULATION: ");
				//for (Individual k : pop) {
				//	System.out.println(k.getId());
				//}
				if (crossoverFlag == true) {											//intercept this loop if crossover must be done
					Individual A=null;
					Individual B=null;
					for (Individual ind : pop) {										//find the two individuals
						//System.out.println("Looking for "+i+" and "+tmpElem+" and found "+ind.getId());
						if (ind.getId()==i) {
							A = ind;
							//System.out.println("found A at "+i);
						}
						if (ind.getId()==tmpElem) {
							B = ind;
							//System.out.println("found B at "+tmpElem);
						}
					}
					
					List<Individual> l = A.crossover(B, (float)(rand.nextInt(10) + 1)/10);
					
					reproducedElem++;													//mark this element as reproduced
					tmpElem=0;															//reused just as a counter
					for(Individual off : l) {											//copy returned list into the array of the offsprings
						offsprings[reproducedElem-1-tmpElem] = off;
						tmpElem++;
					}
					
					crossoverFlag=false;												//mark crossover as happened
					System.out.println("	crossover (end)");
					continue;															//go to next element
				}
																						//Pick gen. op according to generated number and probabilities
				if (r<=genOpProbabilities[0]*100 && (individualsToUpdatePerIteration-reproducedElem)>1) { //crossover can be one if there are at least 2 elements to reproduce
					System.out.println("	crossover (start)");
					crossoverFlag = true;												//flag that crossover is picked, setting up and ready to happen
					tmpElem=i;															//store the ID of this individual
					reproducedElem++;													//mark it as reproduced
					continue;															//go to next element
				} else {
					System.out.println("	mutation");
					Individual A=null;
					for (Individual ind : pop) {										//find the two individuals
						//System.out.println("Looking for "+i+" and found "+ind.getId());
						if (ind.getId()==i) {
							A = ind;
							//System.out.println("found A at "+i);
							break;
						}
					}
					int m = rand.nextInt(3);
					switch (m) {
					case 0:
						offsprings[reproducedElem]=A.mutate();
						break;
					case 1:
						offsprings[reproducedElem]=A.swapSlots();
						break;
					case 2:
						offsprings[reproducedElem]=A.desrupt();
						break;
					}
					if(!offsprings[reproducedElem].isFeasible()) {
						System.out.println("Warning: non feasible solution!");
						System.out.print(offsprings[reproducedElem-1].getAssignment());
						return;
					}
					reproducedElem++;
				}
			}
					
			//Hybridization step:
			offsprings = this.hybridization(offsprings);
			
			
			//3. Population updating
			Map<Integer, Float> weakestFitnessMap =										//Map with the elements to substitute
				    fitnessMap.entrySet().stream()
				       .sorted(Map.Entry.comparingByValue()) 	
				       .limit(individualsToUpdatePerIteration)
				       .collect(Collectors.toMap(
				    		   Map.Entry::getKey, Map.Entry::getValue, (e1,e2) -> e1, LinkedHashMap::new));
			//System.out.println(weakestFitnessMap);
			//TODO: add elitist approach to use in case individualsToUpdatePerIteration = popSize

			int substituted = 0;																		//count how many inserted
			for (int i : weakestFitnessMap.keySet()) {													//loop on the IDs of elements to substitute (to remove)
				int counter = 0;																		//count how many checked
				for (Individual ind : pop) {															//look for the individuals to remove
					if(ind.getId()==i) {																//if the current one has to be removed
						pop[counter]=offsprings[substituted];											//remove it and substitute it with an offspring
						substituted++;
						break;
					}	
					counter++;
				}
			}

			
			float avgFit2=(float) 0.0, bestFit2=(float) 0.0;
			for (Individual i : pop) {
				avgFit2 += i.getFitness();
				if (i.getFitness()>bestFit2) {
					bestFit2=i.getFitness();
				}
			}
			avgFit2/=popSize;
			System.out.println("Ending statistics:");
			System.out.println("	best fitness improvement: "+(bestFit2-bestFit1));
			System.out.println("	average fitness improvement: "+(avgFit2-avgFit1));
			System.out.println("");
			
			
			//4. Save results
			int keyOfBestSol = fitnessMap.entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();	//Find optimal solution

			for (Individual ind : pop) {
				if (ind.getId()==keyOfBestSol) {
					try {
						System.out.println("Lowest penalty: " + 1/ind.getFitness());
						System.out.println("Printing results to: "+this.outputFile);
						ind.printIndividual(this.outputFile);																							//print it
					} catch (IOException e) {
						System.out.println("FAILED PRINTING RESULTS! R.I.P.");
						e.printStackTrace();
					}
					break;
				}
			}
			
			iteratCnt++;
			System.out.println("");
			System.out.println("--------------------");
			System.out.println("");
		}
		
	
	}
}
