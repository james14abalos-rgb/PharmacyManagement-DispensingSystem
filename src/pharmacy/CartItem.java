package pharmacy;

/** One line of the dispensing cart: a medicine and the quantity requested. */
public class CartItem {
    private final Medicine medicine;
    private int quantity;

    public CartItem(Medicine medicine, int quantity) {
        this.medicine = medicine;
        this.quantity = quantity;
    }

    public Medicine getMedicine() { return medicine; }
    public int getQuantity() { return quantity; }
    public void addQuantity(int extra) { quantity += extra; }
    public double getSubtotal() { return medicine.getPrice() * quantity; }
}
