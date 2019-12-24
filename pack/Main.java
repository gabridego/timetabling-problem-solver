package pack;

public class Main {

	public static void main(String[] args) {
		if(args.length != 3 || !args[1].equals("-t")) {
			System.out.println("Arguments error!");
			System.exit(-1);
		}
		Instance instance = new Instance(args[0]);
		System.out.println(":)");
	}

}
