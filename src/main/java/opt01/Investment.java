package opt01;


public class Investment implements Comparable<Object>{
    private String accountName;
    private double interestRate;
    private long term;
    private double minInvestmentAmount;
    private double maxInvestmentAmount;
    private double currentInvestmentAmount;
    private boolean active = false;

    public Investment(String accountName, double interestRate, long term,
                       double minInvestment, double maxInvestment){

        this.accountName = accountName;
        this.interestRate = interestRate;
        this.term = term;
        this.minInvestmentAmount = minInvestment;
        this.maxInvestmentAmount = maxInvestment;
        this.active = true;

    }

    public int compareTo(Object otherInvestment){
        if (this.getInterestRate() > ((Investment)otherInvestment).getInterestRate())
            return 1;
        else 
            return -1;
    }
    @Override
    public String toString(){
        return "Name: [" + this.getAccountName() + "] " +
               "Rate: [" + this.getInterestRate() + "] \n";
    }
    
    @Override
    public int hashCode() {
    	final int prime = 31;
    	int result = 1;
    	result = prime * result + accountName.charAt(0);
    	return result;
    }    
    
    @Override
    public boolean equals(Object other){
    	Investment o1 = (Investment)other;
        return accountName.equals(o1.accountName);
    }    

    public long getTerm() { 
    	return this.term; 
    }

    public double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(double interestRate) {
        this.interestRate = interestRate;
    }

    public double getMaxInvestmentAmount() {
        return maxInvestmentAmount;
    }

    public void setMaxInvestmentAmount(double maxInvestmentAmount) {
        this.maxInvestmentAmount = maxInvestmentAmount;
    }

    public double getCurrentInvestmentAmount() {
        return currentInvestmentAmount;
    }

    public void addAmount(double currentInvestmentAmount) {
        this.currentInvestmentAmount += currentInvestmentAmount;
    }

    public void subtractAmount(double currentInvestmentAmount) {
        this.currentInvestmentAmount -= currentInvestmentAmount;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public double getMinInvestmentAmount() {
        return minInvestmentAmount;
    }

    public void setMinInvestmentAmount(double minInvestmentAmount) {
        this.minInvestmentAmount = minInvestmentAmount;
    }

    public boolean isActive(){ return this.active; }
	public void blockInvestment() {
        this.active = false;
	}

    public Investment clone(){
        return new Investment(this.accountName, this.interestRate, this.term, this.minInvestmentAmount, this.maxInvestmentAmount);
    }    

}


