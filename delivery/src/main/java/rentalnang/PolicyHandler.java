package rentalnang;

import rentalnang.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired DeliveryRepository deliveryRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentApproved_Accept(@Payload PaymentApproved paymentApproved){

        if(!paymentApproved.validate()) return;

        System.out.println("\n\n##### listener Accept : " + paymentApproved.toJson() + "\n\n");



        // Sample Logic //
        Delivery delivery = new Delivery();
        delivery.setRentalId(paymentApproved.getRentalId());
        delivery.setStatus("Car Reservation OK!");
        deliveryRepository.save(delivery);

    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverRentCanceled_Cancel(@Payload RentCanceled rentCanceled){

        if(!rentCanceled.validate()) return;

        System.out.println("\n\n##### listener Cancel : " + rentCanceled.toJson() + "\n\n");



        // Sample Logic //
        Delivery delivery = new Delivery();
        delivery.setRentalId(rentCanceled.getId());
        delivery.setStatus("Cancel Rental OK!!");

        deliveryRepository.save(delivery);

    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
