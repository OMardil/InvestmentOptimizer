package opt01;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Need{
    private String name;
    private LocalDate dueDate;
    private long daysUntilDue;
    private double amount;
    private boolean active;

    public Need(String name, LocalDate dueDate, double amount){
        this.name = name;
        this.dueDate = dueDate;
        this.daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        this.amount = amount;
        this.active = true;
    }

    public Need(String name, LocalDate dueDate, double amount, boolean active, long daysUntilDue){
        this(name, dueDate, amount);
        this.active = active;
        this.daysUntilDue = daysUntilDue;
    }    
    
    public Need(String name, long daysUntilDue, double amount){
        this.name = name;
        this.daysUntilDue = daysUntilDue;
        this.amount = amount;
    }       

    public Need clone(){
        return new Need(this.name, this.dueDate, this.amount, this.active, this.daysUntilDue);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public double getAmount() {
        return amount;
    }

    public void addAmount(double amount) {
        this.amount += amount;
    }

    public void subtractAmount(double amount) {
        this.amount -= amount;
    }    

    public long getDaysUntilDue() {
        return daysUntilDue;
    }

    public boolean isActive(){ return this.active; }
	public void blockNeed() {
        this.active = false;
	}    

}
