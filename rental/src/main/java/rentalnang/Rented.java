package rentalnang;

public class Rented extends AbstractEvent {

    private Long id;
    private String carId;
    private Long rentHour;
    private String status;

    public Rented(){
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getRentHour() {
        return rentHour;
    }

    public void setRentHour(Long rentHour) {
        this.rentHour = rentHour;
    }
    public String getCarId() {
        return carId;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
