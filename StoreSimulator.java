import java.util.Scanner;
import java.util.Random;
/**
 * This class represents the actual simulator for the store itself, including an array of
 * checkout queues, the main simulator including all of its helper methods, variables that
 * calculate and display performance metrics, and constants that represent the time that 
 * common customer actions take as well as worker wages and overhead costs.
 */
public class StoreSimulator {
	private Checkout[] checkouts;
	private static final Random RANDOM = new Random();
	
	private double arrivalProb; 
	private double gross;
	private double profit;
	private int totalCustomers;
	private int totalCustomersServed;
	private double totalWaitTime;
	private double averageWaitTime;
	private int totalItems;
	private int numWorkers;
	private int totalNumWorkers;
	private int duration;
	private int maxCustPerMin;
	private double totalItemCost;
	
	private static final double INIT_TIME = 0.5;
	private static final double TIME_PER_ITEM = 0.1;
	private static final double FIX_TIME = 2;
	private static final double PAYMENT_TIME = 1;
	private static final double WORKER_WAGE = 16.5;
	private static final double OVERHEAD_COST_PERCENTAGE = 0.3;
	
	public StoreSimulator() {}
	
	/**
	 * Constructor for the store simulator object
	 * @param numberOfCheckouts
	 * 	represents the size of the checkout array
	 * @param arrivalProb
	 * 	represents the probability of a customer arriving at a given minute
	 * @param numWorkers
	 * 	represents the number of workers for the checkout array
	 * @param duration
	 * 	represents the amount of minutes the simulation will run for
	 * @param maxCustPerMin
	 * 	represents the maximum amount of customers that can enter in a given minute
	 * @throws IllegalArgumentException
	 * 	throws this exception if any of the above variables except for maxCustPerMin
	 *	is outside of the proper range in order to ensure the simulator runs properly
	 */
	public StoreSimulator(int numberOfCheckouts, double arrivalProb, 
	  int numWorkers, int duration, int maxCustPerMin) 
	{
		if (numberOfCheckouts <= 0) {
			throw new IllegalArgumentException("Error: Number of checkouts must be at least 1.");
		}
		
		if (arrivalProb <= 0 || arrivalProb > 1) {
			throw new IllegalArgumentException("Error: Arrival probability must be between 0 and 1.");
		}
		
		if (numWorkers < 0) {
			throw new IllegalArgumentException("Error: Number of workers cannot be negative.");
		}
		
		if (duration < 0) {
			throw new IllegalArgumentException("Error: Duration must be positive.");
		}
		
		checkouts = new Checkout[numberOfCheckouts];
		this.arrivalProb = arrivalProb;
		this.numWorkers = numWorkers;
		this.duration = duration;
		this.maxCustPerMin = maxCustPerMin;
		
		for (int i = 0; i < numberOfCheckouts; i++) {
			checkouts[i] = new Checkout();
		}
	}
	
	/**
	 * Simulates the arrival of customers to the checkout array in a given minute, and
	 * enqueues them to the least busy checkout in the array
	 */
	private void simulateArrivals() {
		int customerArrivals = RANDOM.nextInt(maxCustPerMin + 1);
		
		for (int i = 0; i < customerArrivals; i++) {
			if (RANDOM.nextDouble() <= arrivalProb) {
				Customer newCustomer = new Customer();
				
				Checkout leastBusy = checkouts[0];
				
				for (Checkout checkout : checkouts) {
					if (checkout.getSize() < leastBusy.getSize()) {
						leastBusy = checkout;
					}
				}
				
				leastBusy.enqueue(newCustomer);
				totalCustomers++;
				System.out.println("Customer #" + newCustomer.getNumber() 
				  + " has joined the checkout queue with " + newCustomer.getNumberOfItems()
				  + " item(s).");
			}
		}
	}
	
	/**
	 * Largely assisted by PingPong
	 * Helper method for dequeueing customers who are finished to free up space
	 * for new customers in the checkout queue
	 * @param index
	 * 	represents the specific checkout in the array the customer is in
	 * @param remainingTime
	 * 	represents the array that stores the remaining time of customers
	 */
	private void handleCompletedCustomers(int index, double[] remainingTime) {
		Customer currentCustomer = checkouts[index].peek();
		
		if (currentCustomer != null && remainingTime[index] <= 0.001) {
			totalCustomersServed++;
			gross += currentCustomer.getPriceOfItems();
			totalItems += currentCustomer.getNumberOfItems();
			totalWaitTime += currentCustomer.totalTimeSpent(
			  INIT_TIME, TIME_PER_ITEM, FIX_TIME, PAYMENT_TIME);
			
			if (currentCustomer.hasIssue()) {
				numWorkers++;
			}
			
			remainingTime[index] = 0;
			System.out.println("Customer #" + currentCustomer.getNumber() 
			  + " has finished using self-checkout #" + (index + 1) + ".");
			checkouts[index].dequeue();
		}
	}
	
	/**
	 * Assisted by PingPong
	 * Helper method for checking the minute by minute progress of customers, while 
	 * also making sure to allocate workers as needed to customers with issues. 
	 * If no workers are available, progress at the checkout is halted until a worker is
	 * free to help the customer.
	 * @param index
	 * 	represents the specific checkout in the array the customer is in
	 * @param remainingTime
	 * 	represents the array that stores the remaining time of customers
	 */
	private void processCheckout(int index, double[] remainingTime) {
		Customer currentCustomer = checkouts[index].peek();
		if (currentCustomer != null) {
			if (remainingTime[index] == 0) {											
				double delay = 0;
				
				if (currentCustomer.hasIssue()) {
					if (numWorkers > 0) {
						delay = handleIssue(currentCustomer);
						System.out.println("Customer #" 
						  + currentCustomer.getNumber() 
						  + " has an issue. A worker has been assigned.");
						remainingTime[index] = currentCustomer.totalTimeSpent(
						  INIT_TIME, TIME_PER_ITEM, delay, PAYMENT_TIME);
						remainingTime[index]--;
					} else {
						System.out.println("Customer #" 
						  + currentCustomer.getNumber() 
						  + " has an issue. No workers are available.");
						remainingTime[index] = Double.POSITIVE_INFINITY;
						return;
					}
				} else {
					remainingTime[index] = currentCustomer.totalTimeSpent(
					  INIT_TIME, TIME_PER_ITEM, delay, PAYMENT_TIME);
				}		
				remainingTime[index] = currentCustomer.totalTimeSpent(
				  INIT_TIME, TIME_PER_ITEM, delay, PAYMENT_TIME);
			}
			
			if (remainingTime[index] > 0 && !Double.isInfinite(remainingTime[index])) {
				System.out.println("Checkout #" + (index + 1) + ": Customer #" 
				  + currentCustomer.getNumber() + " is currently at the self-checkout. "
				  + "(" + String.format("%.2f", remainingTime[index]) + " minutes remaining).");
				remainingTime[index]--;		
			} else {
				System.out.println("Checkout #" + (index + 1) + ": Customer #" 
				  + currentCustomer.getNumber() + " currently has an issue. "
				  + "Waiting for a free worker.");
			}
		}
	}
	
	/**
	 * Helper method for assigning a worker to a customer if they have an issue and
	 * temporarily stopping the worker from assisting elsewhere
	 * @param customer
	 * 	represents a customer who has an issue
	 * @return
	 * 	the delay taken by fixing the issue
	 */
	private double handleIssue(Customer customer) {
		double delay = FIX_TIME;
		numWorkers--;
		return delay;
	}
	
	/**
	 * Helper method for adding up the total time spent by all customers both waiting
	 * on line and also scanning
	 * @param remainingTime
	 * 	the array that represents the remaining time of all customers
	 */
	private void accumalateTotalTime(double[] remainingTime) {
		for (int i = 0; i < checkouts.length; i++) {
			Checkout checkout = checkouts[i];
			double waitThisMinute = 0.0;
			
			for (Customer customer : checkout.getCustomers()) {
				if (customer != null) {
					waitThisMinute += 1.0;
				}
			}
			totalWaitTime += waitThisMinute;
		}
	}
	
	/**
	 * Helper method that prints a log of each checkout in the checkout array per minute, 
	 * including whether or not the queue is empty and toString representations of each 
	 * checkout object, which includes toString representations of each customer object.
	 */
	public void checkoutStatus() {
		System.out.println();
		for (int i = 0; i < checkouts.length; i++) {
			System.out.println("Status of Checkout #" + (i + 1) + ": " 
			  + (checkouts[i].peek() != null ? "OCCUPIED" : "EMPTY"));
			System.out.println("+-----------------+------------+----------------------+");
			System.out.print(checkouts[i].toString());
			System.out.println("+-----------------+------------+----------------------+\n");
		}
	}
	
	/**
	 * Helper method for displaying all performance metrics once the simulation is done
	 * including calculating the total wait time, profit, and average wait time, and also
	 * displaying gross income, amount of items sold, amount of customers served, efficiency
	 * of serving customers, wait time for all customers including customers who were not
	 * served yet, and total amount of customers.
	 */
	public void displayResults() {	
		totalItemCost = 0.0;
		profit = 0.0;
		if (totalCustomers > 0) {
			averageWaitTime = totalWaitTime / totalCustomers;
		} else {
			averageWaitTime = 0;
		}
		
		for (int i = 0; i < totalItems; i++) {
			totalItemCost += RANDOM.nextDouble() * 5;
		}
		
		double overheadCost = gross * OVERHEAD_COST_PERCENTAGE;
		double efficiency = ((double)totalCustomersServed / totalCustomers) * 100;
		
		if (Double.isNaN(efficiency)) {
			efficiency = 0;
		}
		
		double totalWaitTimeHours = totalWaitTime / 60;
		double workerCosts = ((double) totalNumWorkers * WORKER_WAGE) * ((double) duration / 60.0);
		profit = gross - workerCosts - totalItemCost - overheadCost;
		
		System.out.println("\nSimulation Results:");
		System.out.println("\tGross Amount: $" + String.format("%.2f", gross));
		System.out.println("\tWorker Costs: $" + String.format("%.2f", workerCosts));
		System.out.println("\tItem Costs: $" + String.format("%.2f", totalItemCost));
		System.out.println("\tOverhead Costs: $" + String.format("%.2f", overheadCost));
		System.out.println("\tTotal Profit: $" + String.format("%.2f", profit));
		System.out.println("\tTotal Items Sold: " + totalItems);
		System.out.println("\tTotal Customers: " + totalCustomers);
		System.out.println("\tTotal Customers Served: " + totalCustomersServed);
		System.out.println("\tCustomer Serving Efficiency: " + String.format("%.2f", efficiency) + "%");
		System.out.println("\tAggregate Wait Time: " + String.format("%.2f", totalWaitTimeHours) + " hours");
		System.out.println("\tAverage Wait Time per Customer: " 
		  + String.format("%.2f", averageWaitTime) + " minutes");
	}
	
	/**
	 * Assisted by PingPong
	 * Main logic for running the simulation, utilizes the previous helper methods to
	 * correctly add customers to the checkout array, remove them when they are done, keep
	 * track of running time, print out minute by minute logs of the simumlation.
	 */
	public void simulate() {	
		gross = 0;
		totalCustomersServed = 0;
		totalItems = 0;
		averageWaitTime = 0;
		totalNumWorkers = numWorkers;
		
		double[] remainingTime = new double[checkouts.length];
		
		for (int i = 1; i <= duration; i++) {
			System.out.println("\nMinute " + i + ":\n");
			
			for (int j = 0; j < checkouts.length; j++) {
				handleCompletedCustomers(j, remainingTime);
			}

			simulateArrivals();
			
			for (int j = 0; j < checkouts.length; j++) {
				processCheckout(j, remainingTime);
			}
			
	
			accumalateTotalTime(remainingTime);
			checkoutStatus();
		}		
		displayResults();
	}
	
	/**
	 * Main method that instantiates a store object using user provided inputs and then
	 * runs a simulation on that store object. 
	 * @param args
	 */
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		
		System.out.println("Starting store simulator...\n");
		
		try {
			System.out.print("Enter the number of checkouts: ");
			int numCheckouts = sc.nextInt();
			
			System.out.print("Enter the probability of customer arrival: ");
			double arrivalProb = sc.nextDouble();
			
			System.out.print("Enter the amount of workers: ");
			int numWorkers = sc.nextInt();
			
			System.out.print("Enter the duration of the simulation: ");
			int duration = sc.nextInt();
			
			System.out.print("Enter the maximum amount of customers that can enter each minute: ");
			int maxCustPerMin = sc.nextInt();
			
			StoreSimulator store = new StoreSimulator(numCheckouts, 
			  arrivalProb, numWorkers, duration, maxCustPerMin);
			store.simulate();
		} catch (IllegalArgumentException e) {
			System.out.println("\n" + e.getMessage());
		}
		sc.close();
	}	
}
