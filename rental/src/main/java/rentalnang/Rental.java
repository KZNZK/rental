package rentalnang;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Rental_table")
public class Rental {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String carId;
    private Long rentHour;
    private String status;

    @PostPersist
    public void onPostPersist(){
        Rented rented = new Rented();
        BeanUtils.copyProperties(this, rented);
        rented.publishAfterCommit();

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        rentalnang.external.Payment payment = new rentalnang.external.Payment();
        // mappings goes here
        payment.setRentalId(this.id);
        payment.setPrice(this.rentHour * 1000);
        payment.setRentHour(this.rentHour);
        payment.setStatus("Pay rental bill");
        RentalApplication.applicationContext.getBean(rentalnang.external.PaymentService.class)
            .pay(payment);

    }
    @PostUpdate
    public void onPostUpdate(){
        RentCanceled rentCanceled = new RentCanceled();
        BeanUtils.copyProperties(this, rentCanceled);
        rentCanceled.publishAfterCommit();

    }
    @PrePersist
    public void onPrePersist(){
    }

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
    public Long getRentHour() {
        return rentHour;
    }

    public void setRentHour(Long rentHour) {
        this.rentHour = rentHour;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }




}