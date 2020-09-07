package opt01;

public class InvestmentLimits {

	String name;
	double minInvestment;
	double maxInvestment;
	
	public InvestmentLimits(String name, double min, double max){
		this.name = name;
		this.minInvestment = min;
		this.maxInvestment = max;
	}
}
