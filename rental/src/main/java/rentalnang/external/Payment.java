package rentalnang.external;

public class Payment {

    private Long id;
    private Long rentalId;
    private Long price;
    private String status;
    private Long rentHour;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getRentalId() {
        return rentalId;
    }
    public void setRentalId(Long rentalId) {
        this.rentalId = rentalId;
    }
    public Long getPrice() {
        return price;
    }
    public void setPrice(Long price) {
        this.price = price;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public Long getRentHour() {
        return rentHour;
    }
    public void setRentHour(Long rentHour) {
        this.rentHour = rentHour;
    }

}
