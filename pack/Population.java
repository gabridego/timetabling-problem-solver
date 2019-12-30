package pack;

public class Population {
	private static Integer popSize;
	private static Individual[] pop;
	
	public Population(Integer popSize, Instance instance) {
		this.popSize = popSize;
		for(int i = 0; i < popSize; i++) {
			pop[i] = new Individual(instance);
		}
	}
}
