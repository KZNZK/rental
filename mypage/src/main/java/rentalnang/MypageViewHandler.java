package rentalnang;

import rentalnang.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class MypageViewHandler {


    @Autowired
    private MypageRepository mypageRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenRented_then_CREATE_1 (@Payload Rented rented) {
        try {

            if (!rented.validate()) return;

            // view 객체 생성
            Mypage mypage = new Mypage();
            // view 객체에 이벤트의 Value 를 set 함
            mypage.setRentalId(rented.getId());
            mypage.setRentHour(rented.getRentHour());
            mypage.setCarId(rented.getCarId());
            mypage.setStatus(rented.getStatus());
            // view 레파지 토리에 save
            mypageRepository.save(mypage);

        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whenDeliveryApproved_then_UPDATE_1(@Payload DeliveryApproved deliveryApproved) {
        try {
            if (!deliveryApproved.validate()) return;
                // view 객체 조회

                    List<Mypage> mypageList = mypageRepository.findByRentalId(deliveryApproved.getRentalId());
                    for(Mypage mypage : mypageList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    mypage.setRentalId(deliveryApproved.getRentalId());
                    mypage.setStatus(deliveryApproved.getStatus());
                // view 레파지 토리에 save
                mypageRepository.save(mypage);
                }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenPaymentApproved_then_UPDATE_2(@Payload PaymentApproved paymentApproved) {
        try {
            if (!paymentApproved.validate()) return;
                // view 객체 조회

                    List<Mypage> mypageList = mypageRepository.findByRentalId(paymentApproved.getRentalId());
                    for(Mypage mypage : mypageList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    mypage.setRentalId(paymentApproved.getRentalId());
                    mypage.setStatus(paymentApproved.getStatus());
                // view 레파지 토리에 save
                mypageRepository.save(mypage);
                }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenDeliveryCanceled_then_UPDATE_3(@Payload DeliveryCanceled deliveryCanceled) {
        try {
            if (!deliveryCanceled.validate()) return;
                // view 객체 조회

                    List<Mypage> mypageList = mypageRepository.findByRentalId(deliveryCanceled.getRentalId());
                    for(Mypage mypage : mypageList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    mypage.setRentalId(deliveryCanceled.getRentalId());
                    mypage.setStatus(deliveryCanceled.getStatus());
                // view 레파지 토리에 save
                mypageRepository.save(mypage);
                }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenPaymentCanceled_then_UPDATE_4(@Payload PaymentCanceled paymentCanceled) {
        try {
            if (!paymentCanceled.validate()) return;
                // view 객체 조회

                    List<Mypage> mypageList = mypageRepository.findByRentalId(paymentCanceled.getRentalId());
                    for(Mypage mypage : mypageList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    mypage.setRentalId(paymentCanceled.getRentalId());
                    mypage.setStatus(paymentCanceled.getStatus());
                // view 레파지 토리에 save
                mypageRepository.save(mypage);
                }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenRentCanceled_then_UPDATE_5(@Payload RentCanceled rentCanceled) {
        try {
            if (!rentCanceled.validate()) return;
                // view 객체 조회

                    List<Mypage> mypageList = mypageRepository.findByRentalId(rentCanceled.getId());
                    for(Mypage mypage : mypageList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    mypage.setRentalId(rentCanceled.getId());
                    mypage.setStatus(rentCanceled.getStatus());
                // view 레파지 토리에 save
                mypageRepository.save(mypage);
                }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

}

