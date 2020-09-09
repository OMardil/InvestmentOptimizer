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

import org.apache.commons.compress.utils.Lists;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.ortools.linearsolver.*;

public class OptimizeInvestments {

	static HashMap<String, InvestmentLimits> limits;
	static double[][] gains;
	static FileInputStream file;
	static long[] terms;
	static Need[] needsList;
	static Investment[] investmentList;
	static List<Investment> investmentListNoDups;
	static final int DAYS_IN_YEAR = 360;
	static final String DATA_FILE_PATH = "src\\\\main\\\\resources\\\\data.xlsx";
	static MPSolver solver;	
	static MPVariable[][] x;
	static MPObjective objective;
	static MPSolver.ResultStatus resultStatus;
	
	
	static {
		System.loadLibrary("jniortools");
	}

	public static void main(String[] args) throws Exception {
		
		loadProgramVariables();
		
		calculateGainsTable();
		printTable(gains);

		initializeSolver();
		initializeVariables();
		
		setupConstrains();
		setupObjective();

		printSolution();

	}

	private static void initializeSolver() {
		solver = MPSolver.createSolver("AssignmentMip", "CBC");	
	}

	private static void printDebugInfo(MPSolver solver) {
		System.out.println("--------------------------");
		System.out.println("Variables:" + solver.numVariables());
		System.out.println("Constrains:" + solver.numConstraints());	
		System.out.println("Model:" + solver.exportModelAsLpFormat());	
		System.out.println("--------------------------");
	}

	private static void loadProgramVariables() throws FileNotFoundException, IOException {

		XSSFWorkbook excelBook = new XSSFWorkbook(new FileInputStream(DATA_FILE_PATH));
		
		limits = readInvestmentLimitsFromFile(excelBook);
		terms = loadTerms();
		needsList = readNeedsFromFile(excelBook);
		investmentList = readInvestmentsFromFile(excelBook);
		investmentListNoDups = removeDupsFrom(investmentList);		
		excelBook.close();
	}

	private static void calculateGainsTable() {

		gains = new double[investmentList.length][needsList.length];

		for(int r=0; r<gains.length; r++) {
			for(int c=0; c<gains[0].length; c++) {
				if (getNeedTermFromCell(r,c) >= getInvestmentTermFromCell(r, c)){
					gains[r][c] = calculateGainInOneYear(needsList[c].getAmount(), investmentList[r].getInterestRate() / DAYS_IN_YEAR);
				}
			}
		}
	}
	
	
	private static void printSolution() {

		if (resultStatus == MPSolver.ResultStatus.OPTIMAL || resultStatus == MPSolver.ResultStatus.FEASIBLE) {
			System.out.println("Total cost: " + objective.value() + "\n");
			
			System.out.print("\t");
			Arrays.asList(needsList).forEach(result -> System.out.print(result.getName() + "\t"));
			System.out.println();
			
			for (int r = 0; r < x.length; ++r) {
				System.out.print(investmentList[r].getAccountName() +"_"+ investmentList[r].getTerm() + "\t");
				for (int c = 0; c < x[0].length; ++c) {
					System.out.print(x[r][c].solutionValue()*gains[r][c] + "\t");
				}
				System.out.println();
			}
		
		} else {
			System.err.println("No solution found.");
		}
	}

	private static void setupObjective() {

		objective = solver.objective();
		for (int i = 0; i < gains.length; ++i) {
			for (int j = 0; j < gains[0].length; ++j) {
				objective.setCoefficient(x[i][j], gains[i][j]);
			}
		}

		objective.setMaximization();
		
		resultStatus = solver.solve();		
	}

	private static List<Investment> removeDupsFrom(Investment[] invListWithDups) {
		return Arrays.asList(invListWithDups).stream().distinct().collect(Collectors.toList());
	}

	private static HashMap<String, InvestmentLimits> readInvestmentLimitsFromFile(XSSFWorkbook excelBook)
			throws FileNotFoundException, IOException {

		HashMap<String, InvestmentLimits> limits = new HashMap<String, InvestmentLimits>();		
		XSSFSheet excelSheet = excelBook.getSheet("Limits");

		for (Row excelRow : excelSheet) {
			String name = excelRow.getCell(0).getStringCellValue();
			if (excelRow.getRowNum() != 0) {
				double min = excelRow.getCell(1).getNumericCellValue();
				double max = excelRow.getCell(2).getNumericCellValue();
				limits.put(name, new InvestmentLimits(name, min, max));
			}
		}

		return limits;

	}

	private static Investment[] readInvestmentsFromFile(XSSFWorkbook excelBook) throws FileNotFoundException, IOException {

		XSSFSheet excelSheet = excelBook.getSheet("Investments");

		List<Investment> investmentList = new ArrayList<Investment>();

		for (Row excelRow : excelSheet) {
			if (excelRow.getRowNum() != 0) {
				String nombre = excelRow.getCell(0).getStringCellValue();
				long plazo = (long) excelRow.getCell(1).getNumericCellValue();
				double tasa = excelRow.getCell(2).getNumericCellValue();
				double minInvestment = limits.get(nombre).minInvestment;
				double maxInvestment = limits.get(nombre).maxInvestment;

				investmentList.add(new Investment(nombre, tasa, plazo, minInvestment, maxInvestment));
			}
		}

		return investmentList.toArray(new Investment[investmentList.size()]);
	}

	private static Need[] readNeedsFromFile(XSSFWorkbook excelBook) throws FileNotFoundException, IOException {
		file = new FileInputStream(DATA_FILE_PATH);
		XSSFSheet excelSheet = excelBook.getSheet("Needs");

		List<Need> needList = new ArrayList<Need>();

		for (Row excelRow : excelSheet) {
			if (excelRow.getRowNum() != 0) {

				String name = excelRow.getCell(0).getStringCellValue();
				double amount = excelRow.getCell(1).getNumericCellValue();
				long daysUntilDue = (long) excelRow.getCell(2).getNumericCellValue();

				needList.add(new Need(name, daysUntilDue, amount));
			}
		}

		return needList.toArray(new Need[needList.size()]);
	}


	private static double calculateGainInOneYear(double currentAmount, double investmentDailyRate) {
		return currentAmount + currentAmount * investmentDailyRate * DAYS_IN_YEAR;
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

	private static void setupConstrains() {

		// Each need is assigned to at most one inversion.
		limitsPerInvestmentOption();

		//Each investment provider can only be assigned to at most 1 need
		limitPerNeed();

	}
	
	private static void limitsPerInvestmentOption() {

		String lastInvestmentName = "";
		MPConstraint c0 = null;
		boolean isMultiRowConstraint = false;
		
		for(int r=0; r<gains.length; r++) {

			InvestmentLimits investmentLimit = limits.get(investmentList[r].getAccountName());
			double max = investmentLimit.maxInvestment;
			double min = investmentLimit.minInvestment;
				
			isMultiRowConstraint = lastInvestmentName.equals(getInvestmentNameFromCell(r, -1));
			
			if (!isMultiRowConstraint) {
				c0 = solver.makeConstraint(min, max, getInvestmentNameFromCell(r, -1));
			}
		
			for(int c=0; c<gains[0].length; c++) {
				c0.setCoefficient(x[r][c], gains[r][c]);
			}
			
			lastInvestmentName = getInvestmentNameFromCell(r, -1);
		}
	}
	
	private static void limitPerNeed() {
		
		for (int c = 0; c< gains[0].length; c++) {
			MPConstraint c0 = solver.makeConstraint(getNeedAmountFromCell(-1, c), getNeedAmountFromCell(-1, c), getNeedFromCell(-1, c));
			for (int r = 0; r < gains.length; r++) {
				c0.setCoefficient(x[r][c], getNeedAmountFromCell(r, c));
			}
		}

	}
	
	private static String getInvestmentNameFromCell(int row, int column) {
		return investmentList[row].getAccountName();
	}

	private static double getRateFromCell(int row, int column) {
		return investmentList[row].getInterestRate();
	}
	
	private static long getInvestmentTermFromCell(int row, int column) {
		return investmentList[row].getTerm();
	}	

	private static long getNeedTermFromCell(int row, int column) {
		return needsList[column].getDaysUntilDue();
	}	
	
	private static String getNeedFromCell(int row, int column) {
		return needsList[column].getName();
	}
	
	private static double getNeedAmountFromCell(int row, int column) {
		return needsList[column].getAmount();
	}
	
	private static double getDaysDueFromCell(int row, int column) {
		return needsList[column].getDaysUntilDue();	
	}
	
	private static void initializeVariables() {

		x = new MPVariable[gains.length][gains[0].length];
		
		for (int r = 0; r < gains.length; r++) {
			for (int c = 0; c < gains[0].length; c++) {
				String varname = String.format("x[%s][%s]",r,c);
				x[r][c] = solver.makeNumVar(0, 1.0, varname);
			}
		}
	}

	private static long[] loadTerms() {
		return new long[] { 1, 7, 14, 28, 60, 90, 120, 150, 180, 210, 240, 300, 330, 360 };
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

}