package rentalnang;

public class Rented extends AbstractEvent {

    private Long id;
    private String carId;
    private Integer rentHour;
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getCarId() {
        return carId;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }
    public Integer getRentHour() {
        return rentHour;
    }

    public void setRentHour(Integer rentHour) {
        this.rentHour = rentHour;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}