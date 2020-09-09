package opt01;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.ortools.linearsolver.*;

public class Test {

	static HashMap<String, InvestmentLimits> limits;
	static FileInputStream file;
	static long[] terms;
	static Need[] needsList;
	static Investment[] investmentList;
	static List<Investment> investmentListNoDups;
	static final int DAYS_IN_YEAR = 360;
	static final String DATA_FILE_PATH = "src\\\\main\\\\resources\\\\data2.xlsx";

	static {
		System.loadLibrary("jniortools");
	}

	public static void main(String[] args) throws Exception {

		file = new FileInputStream(DATA_FILE_PATH);
		terms = loadTerms();
		limits = readInvestmentLimitsFromFile();
		needsList = readNeedsFromFile();
		investmentList = readInvestmentsFromFile();
		investmentListNoDups = removeDupsFrom(investmentList);

		double[][] gains = calculateGainsTable();

		printTable(gains);
		System.out.println();
		
		int numNeeds = gains.length;
		int numInvestments = gains[0].length;

		// Create the linear solver with the CBC backend.
		MPSolver solver = MPSolver.createSolver("AssignmentMip", "CBC");
		MPVariable[][] x = initializeVariables(solver, numNeeds, numInvestments);

		setupConstrains(solver, x);

		MPObjective objective = setupObjective(solver, gains, x, numNeeds, numInvestments);

		MPSolver.ResultStatus resultStatus = solver.solve();

		printSolution(gains, numNeeds, numInvestments, x, objective, resultStatus, solver);

	}

	private static MPObjective setupObjective(MPSolver solver, double[][] gains, MPVariable[][] x, int numNeeds,
			int numInvestments) {

		MPObjective objective = solver.objective();
		for (int i = 0; i < numNeeds; ++i) {
			for (int j = 0; j < numInvestments; ++j) {
				objective.setCoefficient(x[i][j], gains[i][j]);
			}
		}

		objective.setMaximization();

		return objective;

	}

	private static List<Investment> removeDupsFrom(Investment[] invListWithDups) {
		return Arrays.asList(invListWithDups).stream().distinct().collect(Collectors.toList());
	}

	private static HashMap<String, InvestmentLimits> readInvestmentLimitsFromFile()
			throws FileNotFoundException, IOException {

		HashMap<String, InvestmentLimits> limits = new HashMap<String, InvestmentLimits>();

		String name = "Supertasas";
		double min = 0;
		double max = 120000;
		limits.put(name, new InvestmentLimits(name, min, max));

		name = "Kubo";
		min = 0;
		max = 120000;
		limits.put(name, new InvestmentLimits(name, min, max));

		name = "Banamex";
		min = 0;
		max = 1000000;
		limits.put(name, new InvestmentLimits(name, min, max));

		return limits;

	}

	private static Investment[] readInvestmentsFromFile() throws FileNotFoundException, IOException {

		List<Investment> investmentList = new ArrayList<Investment>();

		String nombre = "Supertasas";
		long plazo = 30;
		double tasa = 4.5;
		double minInvestment = limits.get(nombre).minInvestment;
		double maxInvestment = limits.get(nombre).maxInvestment;
		investmentList.add(new Investment(nombre, tasa, plazo, minInvestment, maxInvestment));

		nombre = "Kubo";
		plazo = 360;
		tasa = 10;
		minInvestment = limits.get(nombre).minInvestment;
		maxInvestment = limits.get(nombre).maxInvestment;
		investmentList.add(new Investment(nombre, tasa, plazo, minInvestment, maxInvestment));

		nombre = "Banamex";
		plazo = 1;
		tasa = 2;
		minInvestment = limits.get(nombre).minInvestment;
		maxInvestment = limits.get(nombre).maxInvestment;
		investmentList.add(new Investment(nombre, tasa, plazo, minInvestment, maxInvestment));

		nombre = "Banamex";
		plazo = 30;
		tasa = 2.5;
		minInvestment = limits.get(nombre).minInvestment;
		maxInvestment = limits.get(nombre).maxInvestment;
		investmentList.add(new Investment(nombre, tasa, plazo, minInvestment, maxInvestment));

		return investmentList.toArray(new Investment[investmentList.size()]);
	}

	private static Need[] readNeedsFromFile() throws FileNotFoundException, IOException {

		List<Need> needList = new ArrayList<Need>();

		String name = "Carro";
		double amount = 200000;
		long daysUntilDue = 360;
		needList.add(new Need(name, daysUntilDue, amount));

		name = "Navidad";
		amount = 15000;
		daysUntilDue = 180;
		needList.add(new Need(name, daysUntilDue, amount));

		name = "Liquido";
		amount = 70000;
		daysUntilDue = 30;
		needList.add(new Need(name, daysUntilDue, amount));

		return needList.toArray(new Need[needList.size()]);
	}

	private static double[][] calculateGainsTable() {


		double[][] gainsTable = { { .045, .045, .045 }, 
								  { .100, .100, .100 }, 
								  { .020, .020, .020 }, 
								  { .025, .025, .025 } };
		
		for (int c=0; c<gainsTable[0].length; c++) {
			
			double amount = 0;
			switch(c) {
				case 0:
					amount = 200000;
					break;
				case 1:
					amount = 15000;
					break;
				case 2:
					amount = 70000;					
					break;					
			}
				
			
			for(int r=0; r<gainsTable.length; r++) {
				gainsTable[r][c] = gainsTable[r][c]*amount + amount;
			}
		}

		return gainsTable;
	}

	private static double calculateGainInOneYear(double currentAmount, double investmentDailyRate) {
		return currentAmount + currentAmount * investmentDailyRate * DAYS_IN_YEAR;
	}

	private static void printTable(double[][] table) {

		DecimalFormat df = new DecimalFormat("#.##");

		for (double[] row : table) {
			for (double cell : row) {
				System.out.print(df.format(cell) + "\t");
			}
			System.out.println();
		}
	}

	private static double readInterestRate(long term, String accountName) {

		double maxInterestRateForTerm = 0;
		for (int r = 0; r < investmentList.length; r++) {
			if (investmentList[r].getAccountName().equals(accountName) && investmentList[r].getTerm() <= term) {
				maxInterestRateForTerm = Math.max(maxInterestRateForTerm, investmentList[r].getInterestRate());
			}
		}
		return maxInterestRateForTerm;
	}

	private static void setupConstrains(MPSolver solver, MPVariable[][] x) {

		eachInvestmentLimit(solver, x);

		// Each investment provider can only be assigned to at most 1 need
		limitPerInvestment(solver, x);

	}

	private static void limitPerInvestment(MPSolver solver, MPVariable[][] x) {
		
		MPConstraint carro = solver.makeConstraint(200000, 200000, "CARRO");
		carro.setCoefficient(x[0][0], 200000);
		carro.setCoefficient(x[1][0], 200000);
		carro.setCoefficient(x[2][0], 200000);
		carro.setCoefficient(x[3][0], 200000);
		
		MPConstraint navidad = solver.makeConstraint(15000, 15000, "NAVIDAD");
		navidad.setCoefficient(x[0][1], 15000);
		navidad.setCoefficient(x[1][1], 15000);
		navidad.setCoefficient(x[2][1], 15000);
		navidad.setCoefficient(x[3][1], 15000);		
		
		MPConstraint liquido = solver.makeConstraint(70000, 70000, "LIQUIDO");
		liquido.setCoefficient(x[0][2], 70000);
		liquido.setCoefficient(x[1][2], 70000);
		liquido.setCoefficient(x[2][2], 70000);
		liquido.setCoefficient(x[3][2], 70000);				

	}

	private static void eachInvestmentLimit(MPSolver solver, MPVariable[][] x) {
		
		//Supertasas
		MPConstraint c1 = solver.makeConstraint(0, 120000, "SUPERTASAS");
		c1.setCoefficient(x[0][0], 209000);
		c1.setCoefficient(x[0][1], 15675);
		c1.setCoefficient(x[0][2], 73150);

		//Kubo
		MPConstraint n1 = solver.makeConstraint(0, 120000, "KUBO");
		n1.setCoefficient(x[1][0], 220000);
		n1.setCoefficient(x[1][1], 16500);
		n1.setCoefficient(x[1][2], 77000);

		//Banamex
		MPConstraint l1 = solver.makeConstraint(0, 1000000, "BANAMEX");
		l1.setCoefficient(x[2][0], 204000);
		l1.setCoefficient(x[2][1], 15300);
		l1.setCoefficient(x[2][2], 71400);		
		l1.setCoefficient(x[3][0], 205000);		
		l1.setCoefficient(x[3][1], 15375);		
		l1.setCoefficient(x[3][2], 71750);		
		
	}

	private static MPVariable[][] initializeVariables(MPSolver solver, int rows, int columns) {

		MPVariable[][] x = new MPVariable[rows][columns];

		x[0][0] = solver.makeNumVar(0.0, 1.0, "c1");
		x[0][1] = solver.makeNumVar(0.0, 1.0, "n1");
		x[0][2] = solver.makeNumVar(0.0, 1.0, "l1");
		x[1][0] = solver.makeNumVar(0.0, 1.0, "c2");
		x[1][1] = solver.makeNumVar(0.0, 1.0, "n2");
		x[1][2] = solver.makeNumVar(0.0, 1.0, "l2");
		x[2][0] = solver.makeNumVar(0.0, 1.0, "c3");
		x[2][1] = solver.makeNumVar(0.0, 1.0, "n3");
		x[2][2] = solver.makeNumVar(0.0, 1.0, "l3");
		x[3][0] = solver.makeNumVar(0.0, 1.0, "c4");
		x[3][1] = solver.makeNumVar(0.0, 1.0, "n4");
		x[3][2] = solver.makeNumVar(0.0, 1.0, "l4");
		return x;
	}

	private static long[] loadTerms() {
		return new long[] { 1, 30, 180, 360 };
	}

	private static void printSolution(double[][] gains, int numNeeds, int numInvestments, MPVariable[][] x,
			MPObjective objective, MPSolver.ResultStatus resultStatus, MPSolver solver) {

		
		System.out.println("--------------------------");
		System.out.println("Variables:" + solver.numVariables());
		System.out.println("Constrains:" + solver.numConstraints());	
		System.out.println("Model:" + solver.exportModelAsLpFormat());	
		System.out.println("--------------------------");
		
		if (resultStatus == MPSolver.ResultStatus.OPTIMAL || resultStatus == MPSolver.ResultStatus.FEASIBLE) {
			System.out.println("Total gain: " + objective.value() + "\n");
			
			System.out.println("\tCarro\tNavidad\tLiquido");
			for (int r=0; r<x.length; r++) {
				System.out.print(investmentList[r].getAccountName() +"@"+ investmentList[r].getTerm() + "\t");
				for (int c = 0; c<x[0].length; c++) {
					System.out.print(x[r][c].solutionValue()*gains[r][c] +  "\t");
				}
				System.out.println("\t TOTALS=");
			}
		} else {
			System.err.println("No solution found.");
		}
	}	
	
}