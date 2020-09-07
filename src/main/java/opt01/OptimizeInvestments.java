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

public class OptimizeInvestments {

	static HashMap<String, InvestmentLimits> limits;
	static FileInputStream file;
	static long[] terms;
	static Need[] needsList;
	static Investment[] investmentList;
	static List<Investment> investmentListNoDups;
	static final int DAYS_IN_YEAR = 360;
	static final String DATA_FILE_PATH = "src\\\\main\\\\resources\\\\data.xlsx";

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

		int numNeeds = gains.length;
		int numInvestments = gains[0].length;

		// Create the linear solver with the CBC backend.
		MPSolver solver = MPSolver.createSolver("AssignmentMip", "CBC");
		MPVariable[][] x = initializeVariables(solver, numNeeds, numInvestments);

		setupConstrains(solver, x);

		MPObjective objective = setupObjective(solver, gains, x, numNeeds, numInvestments);

		MPSolver.ResultStatus resultStatus = solver.solve();

		printSolution(gains, numNeeds, numInvestments, x, objective, resultStatus);

	}

	private static void printSolution(double[][] gains, int numNeeds, int numInvestments, MPVariable[][] x,
			MPObjective objective, MPSolver.ResultStatus resultStatus) {

		if (resultStatus == MPSolver.ResultStatus.OPTIMAL || resultStatus == MPSolver.ResultStatus.FEASIBLE) {
			System.out.println("Total cost: " + objective.value() + "\n");
			for (int i = 0; i < numNeeds; ++i) {
				for (int j = 0; j < numInvestments; ++j) {
					if (x[i][j].solutionValue() > 0.5) {
						System.out.println(investmentListNoDups.get(j % investmentListNoDups.size()).getAccountName()
								+ " " + gains[i][j] + " @ " + terms[i]);
					}
				}
			}
		} else {
			System.err.println("No solution found.");
		}
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
		file = new FileInputStream(DATA_FILE_PATH);
		XSSFWorkbook myExcelBook = new XSSFWorkbook(file);
		XSSFSheet myExcelSheet = myExcelBook.getSheet("Limits");

		for (Row excelRow : myExcelSheet) {
			String name = excelRow.getCell(0).getStringCellValue();
			if (excelRow.getRowNum() != 0) {
				double min = excelRow.getCell(1).getNumericCellValue();
				double max = excelRow.getCell(2).getNumericCellValue();
				limits.put(name, new InvestmentLimits(name, min, max));
			}
		}

		myExcelBook.close();
		return limits;

	}

	private static Investment[] readInvestmentsFromFile() throws FileNotFoundException, IOException {

		file = new FileInputStream(DATA_FILE_PATH);
		XSSFWorkbook myExcelBook = new XSSFWorkbook(file);
		XSSFSheet myExcelSheet = myExcelBook.getSheet("Investments");

		List<Investment> investmentList = new ArrayList<Investment>();

		for (Row excelRow : myExcelSheet) {
			if (excelRow.getRowNum() != 0) {
				String nombre = excelRow.getCell(0).getStringCellValue();
				long plazo = (long) excelRow.getCell(1).getNumericCellValue();
				double tasa = excelRow.getCell(2).getNumericCellValue();
				double minInvestment = limits.get(nombre).minInvestment;
				double maxInvestment = limits.get(nombre).maxInvestment;

				investmentList.add(new Investment(nombre, tasa, plazo, minInvestment, maxInvestment));
			}
		}

		myExcelBook.close();
		return investmentList.toArray(new Investment[investmentList.size()]);
	}

	private static Need[] readNeedsFromFile() throws FileNotFoundException, IOException {
		file = new FileInputStream(DATA_FILE_PATH);
		XSSFWorkbook myExcelBook = new XSSFWorkbook(file);
		XSSFSheet myExcelSheet = myExcelBook.getSheet("Needs");

		List<Need> needList = new ArrayList<Need>();

		for (Row excelRow : myExcelSheet) {
			if (excelRow.getRowNum() != 0) {

				String name = excelRow.getCell(0).getStringCellValue();
				double amount = excelRow.getCell(1).getNumericCellValue();
				long daysUntilDue = (long) excelRow.getCell(2).getNumericCellValue();

				needList.add(new Need(name, daysUntilDue, amount));
			}
		}

		return needList.toArray(new Need[needList.size()]);
	}

	private static double[][] calculateGainsTable() {

		// Table should be of size
		// rows = terms size
		// columns = need * investment options
		double[][] gainsTable = new double[terms.length][needsList.length * investmentListNoDups.size()];

		for (int n1 = 0; n1 < needsList.length; n1++) {
			for (int i1 = n1 * investmentListNoDups.size(), count = 0; count < investmentListNoDups
					.size(); i1++, count++) {
				for (int t1 = 0; t1 < terms.length; t1++) {
					long currentTerm = terms[t1];
					String currentInvestmentOption = investmentListNoDups.get(count).getAccountName();
					double currentAmount = needsList[n1].getAmount();
					
					if (needsList[n1].getDaysUntilDue() <= terms[t1]) {
						double dailyRate = readInterestRate(currentTerm, currentInvestmentOption) / 360;
						gainsTable[t1][i1] = calculateGainInOneYear(currentAmount, dailyRate);
					}
				}
			}
		}

		// printTable(gainsTable);
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

		// Each need is assigned to at most one inversion.
		// oneInvestmentPerNeed(solver, x, numNeeds, numInvestments);
		// Each
		oneInvestmentPerOptionsForNeed(solver, x);

		limitPerInvestment(solver, x);

	}

	private static void limitPerInvestment(MPSolver solver, MPVariable[][] x) {

		for (int needsCount = 0; needsCount < needsList.length; needsCount++) {
			MPConstraint c0 = solver.makeConstraint(0, 1, "");
			for (int c = needsCount; c < x[0].length; c = c + investmentListNoDups.size()) {
				for (int r = 0; r < x.length; r++) {
					c0.setCoefficient(x[r][c], 1);
				}
			}

		}
	}

	private static void oneInvestmentPerOptionsForNeed(MPSolver solver, MPVariable[][] x) {

		for (int needsCount = 0; needsCount < needsList.length; needsCount++) {

			MPConstraint c0 = solver.makeConstraint(0, 1, "");
			for (int r = 0; r < terms.length; r++) {
				for (int c = 0; c < investmentListNoDups.size(); c++) {
					int offset = investmentListNoDups.size() * needsCount;
					c0.setCoefficient(x[r][c + offset], 1);
				}
			}
		}
	}

	private static void oneInvestmentPerNeed(MPSolver solver, MPVariable[][] x, int numNeeds, int numInvestments) {
		for (int i = 0; i < numNeeds; ++i) {
			MPConstraint constraint = solver.makeConstraint(0, 1, "");
			for (int j = 0; j < numInvestments; ++j) {
				constraint.setCoefficient(x[i][j], 1);
			}
		}
	}

	private static MPVariable[][] initializeVariables(MPSolver solver, int rows, int columns) {

		MPVariable[][] x = new MPVariable[rows][columns];
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < columns; c++) {
				x[r][c] = solver.makeIntVar(0, 1, "");
			}
		}

		return x;
	}

	private static long[] loadTerms() {
		return new long[] { 1, 7, 14, 28, 60, 90, 120, 150, 180, 210, 240, 300, 330, 360 };
	}

}