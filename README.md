# rentalnang

# 목차

- [rentalnang](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [분석 설계](#분석-설계)
  - [구현](#구현)
    - [DDD 의 적용](#ddd의-적용)
    - [Polyglot Persistence](#Polyglot-Persistence)
    - [CQRS](#CQRS)
    - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
    - [비동기식 호출 과 Eventual Consistency](#비동기식-호출-과-Eventual-Consistency)
  - [운영](#운영)
    - [CI/CD 설정](#cicd설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-서킷-브레이킹-장애격리)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [무정지 재배포](#무정지-재배포)
    - [Liveness](#Liveness)
    - [Persistence Volume](#Persistence-Volume)
    
# 서비스 시나리오

차량 예약 시스템인 rentalnang의 기능적, 비기능적 요구사항은 다음과 같습니다. 사용자가 원하는 차량을 예약한 후 결제를 완료합니다. 담당자는 예약 내역을 확인한 후 확정합니다. 사용자는 예약 현황을 확인할 수 있습니다.

기능적 요구사항

1. 사용자는 원하는 차량을 예약한다.
2. 사용자가 결제를 완료하면 예약이 완료된다.
3. 사용자가 결제를 완료하지 못하면 예약이 취소된다.
4. 예약이 완료되면 담당자는 예약 내역을 확인하고 확정한다.
5. 사용자는 예약 현황을 조회할 수 있다.
6. 사용자는 중간에 예약을 취소할 수 있으며, 해당 예약은 삭제된다.

비기능적 요구사항

1. 담당자 부재 등으로 예약 내역을 확정할 수 없더라도 사용자는 중단 없이 차량을 예약할 수 있다. (Event Pub/Sub)
2. 결제가 완료되어야 예약이 완료된다. (Req/Res)
3. 부하 발생 시 자동으로 처리 프로세스를 증가시킨다. (Autoscale)

# 분석 설계
## Event Storming

- MSAEZ에서 Event Storming 수행
- Event 도출


- Actor, Command 부착


- Policy 부착


- Aggregate 부착


- View 추가 및 Bounded Context 묶기


- 완성 모형: Pub/Sub, Req/Res 추가


기능적 요구사항 커버 여부 검증

1. 사용자는 원하는 차량을 예약한다. (O)
2. 사용자가 결제를 완료하면 예약이 완료된다. (O)
3. 사용자가 결제를 완료하지 못하면 예약이 취소된다. (O)
4. 예약이 완료되면 담당자는 내역을 확인하고 확정한다. (O)
5. 사용자는 예약 현황을 조회할 수 있다. (O)
6. 사용자는 중간에 예약을 취소할 수 있으며, 해당 예약은 삭제된다. (O)

비기능적 요구사항 커버 여부 검증

1. 담당자 부재 등으로 예약 내역을 확정할 수 없더라도 사용자는 중단 없이 차량을 예약할 수 있다. (Event Pub/Sub) (O)
2. 결제가 완료되어야 예약이 완료된다. (Req/Res) (O)
3. 부하 발생 시 자동으로 처리 프로세스를 증가시킨다. (Autoscale) (O)

## Hexagonal Architecture Diagram


- Inbound adaptor와 Outbound adaptor를 구분함
- 호출관계에서 PubSub 과 Req/Resp 를 구분함
- 서브 도메인과 바운디드 컨텍스트를 분리함

# 구현
4개의 Microservice를 Springboot로 구현했으며, 다음과 같이 실행해 Local test를 진행했다. Port number는 8081~8084이다.

```
cd mypage
mvn spring-boot:run

cd rental
mvn spring-boot:run

cd payment
mvn spring-boot:run

cd delivery
mvn spring-boot:run
```

## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다. 

```
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

```
- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
package rentalnang;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="paymentHistories", path="paymentHistories")
public interface PaymentHistoryRepository extends PagingAndSortingRepository<PaymentHistory, Long>{

}
```
- 적용 후 REST API 의 테스트
```
# rental 서비스의 주문처리
http localhost:8081/rentals carId="aaa" rentHour='3' status="rented"

# payment 서비스의 결제처리
http localhost:8088/payments Id=1 rentHour='3'

# delivery 서비스의 배송처리
http localhost:8088/deliveries Id=1 status="delivered"

# 주문 상태 확인    
http localhost:8081/rentals/1
HTTP/1.1 200
Content-Type: application/hal+json;charset=UTF-8
Date: Thu, 26 Aug 2021 12:13:53 GMT
Transfer-Encoding: chunked

{
    "_links": {
        "rental": {
            "href": "http://localhost:8081/rentals/1"
        },
        "self": {
            "href": "http://localhost:8081/rentals/1"
        }
    },
    "rentHour": 3,
    "carId": "aaa",
    "status": "rented"
}

```

## Polyglot Persistence

Polyglot Persistence를 위해 h2datase를 hsqldb로 변경

```
		<dependency>
			<groupId>org.hsqldb</groupId>
			<artifactId>hsqldb</artifactId>
			<scope>runtime</scope>
		</dependency>
<!--
		<dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<scope>runtime</scope>
		</dependency>
-->

# 변경/재기동 후 예약 주문
http localhost:8081/rentals carId="bbb" rentHour='4' status="rented"

HTTP/1.1 201 
Content-Type: application/json;charset=UTF-8
Date: Thu, 26 Aug 2021 12:16:09 GMT
Location: http://localhost:8081/rentals/1
Transfer-Encoding: chunked

{
    "_links": {
        "rental": {
            "href": "http://localhost:8081/rentals/1"
        },
        "self": {
            "href": "http://localhost:8081/rentals/1"
        }
    },
    "rentHour": 4,
    "carId": "bbb",
    "status": "rented"
}


# 저장이 잘 되었는지 조회
http localhost:8081/rentals/1

HTTP/1.1 200
Content-Type: application/hal+json;charset=UTF-8    
Date: Thu, 26 Aug 2021 12:17:01 GMT
Transfer-Encoding: chunked

{
    "_links": {
        "rental": {
            "href": "http://localhost:8081/rentals/1"
        },
        "self": {
            "href": "http://localhost:8081/rentals/1"
        }
    },
    "rentHour": 4,
    "carId": "bbb",
    "status": "rented"
}
```

## CQRS

CQRS 구현을 위해 고객의 예약 상황을 확인할 수 있는 Mypage를 구성.

```
# mypage 호출 
http localhost:8081/mypages/12

HTTP/1.1 200 
Content-Type: application/hal+json;charset=UTF-8
Date: Wed, 18 Aug 2021 09:46:13 GMT
Transfer-Encoding: chunked

{
    "_links": {
        "mypage": {
            "href": "http://localhost:8084/mypages/2"
        },
        "self": {
            "href": "http://localhost:8084/mypages/2"
        }
    },
    "cancellationId": null,
    "name": "kim",
    "orderId": 2,
    "reservationId": 2,
    "status": "Reservation Complete"
}
```

## 동기식 호출 과 Fallback 처리

주문(Rental)->결제(Payment) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다.

- 결제서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 

```
#PaymentHistoryService.java

package rental.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="payment", url="${api.payment.url}")
public interface PaymentHistoryService {
    @RequestMapping(method= RequestMethod.POST, path="/paymentHistories")
    public void pay(@RequestBody PaymentHistory paymentHistory);

}
```

- 주문을 받은 직후(@PostPersist) 결제를 요청하도록 처리
```
# Rental.java (Entity)

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
```

- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 결제 시스템이 장애가 나면 주문도 못받는다는 것을 확인:


```
# 결제 (payment) 서비스를 잠시 내려놓음 (ctrl+c)

# 주문요청
http localhost:8081/rentals carId="ccc" rentHour='1' status="rented"
 
HTTP/1.1 500 Internal Server Error
Content-Type: application/json;charset=UTF-8
Date: Fri, 25 Aug 2021 11:28:44 GMT
transfer-encoding: chunked

{
    "error": "Internal Server Error",
    "message": "Could not commit JPA transaction; nested exception is javax.persistence.RollbackException: Error while committing the transaction",
    "path": "/rentals",
    "status": 500,
    "timestamp": "2021-08-25T11:28:44.229+0000"
}

# 결제 (payment) 재기동
mvn spring-boot:run

#주문처리
http localhost:8081/rentals carId="ccc" rentHour='1' status="rented"

HTTP/1.1 201 Created
Content-Type: application/json;charset=UTF-8
Date: Fri, 25 Aug 2021 11:28:55 GMT
Location: http://localhost:8081/rentals/3    
transfer-encoding: chunked

{
    "_links": {
        "rental": {
            "href": "http://localhost:8081/rentals/3"
        },
        "self": {
            "href": "http://localhost:8081/rentals/3"
        }
    },
    "rentHour": 1,
    "carId": "ccc",
    "status": "rented"
}
```

- 또한 과도한 요청시에 서비스 장애가 도미노 처럼 벌어질 수 있다. (서킷브레이커, 폴백 처리는 운영단계에서 설명한다.)


## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트


결제가 이루어진 후에 배송 시스템으로 이를 알려주는 행위는 동기식이 아니라 비 동기식으로 처리하여 배송 시스템의 처리를 위하여 결제주문이 블로킹 되지 않아도록 처리한다.
 
- 이를 위하여 결제이력에 기록을 남긴 후에 곧바로 결제승인이 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
 
```
package rentalnang;

 ...
    @PostPersist
    public void onPostPersist(){
        PaymentApproved paymentApproved = new PaymentApproved();
        
	paymentApproved.setStatus("Pay OK");
        
	BeanUtils.copyProperties(this, paymentApproved);
        paymentApproved.publishAfterCommit();

    }
```
- 배송 서비스에서는 결제승인 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다:

```
package rentalnang;

...

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
       
```

배송 시스템은 주문/결제와 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 배송시스템이 유지보수로 인해 잠시 내려간 상태라도 주문을 받는데 문제가 없다:
```
# 배송 서비스 (delivery) 를 잠시 내려놓음 (ctrl+c)

#주문처리
http localhost:8081/rentals carId="ddd" rentHour='1' status="rented"

#주문상태 확인
http localhost:8081/rentals/4      # 주문정상

{
    "_links": {
        "rental": {
            "href": "http://localhost:8081/rentals/4"
        },
        "self": {
            "href": "http://localhost:8081/rentals/4"
        }
    },
    "rentHour": 1,
    "carId": "ddd",
    "status": "rented"
}
	    
#배송 서비스 기동
cd delivery
mvn spring-boot:run

#주문상태 확인
http localhost:8084/mypages     # 예약 상태가 "Car Reservation OK!"으로 확인

 {
                "_links": {
                    "mypage": {
                        "href": "http://localhost:8084/mypages/4"
                    },
                    "self": {
                        "href": "http://localhost:8084/mypages/4"
                    }
                },
                "rentalId": 4,
                "carId": "ddd",
                "rentHour": 1,
                "reservationId": null,
                "status": "Car Reservation OK!"
            }
```

