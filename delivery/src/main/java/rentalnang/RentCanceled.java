package rentalnang;

public class RentCanceled extends AbstractEvent {

    private Long id;
    private String carId;
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return carId;
    }

    public void setName(String carId) {
        this.carId = carId;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}