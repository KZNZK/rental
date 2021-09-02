



# rental nang

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
http localhost:8088/payments rentalId=1 rentHour='3'

# delivery 서비스의 배송처리
http localhost:8088/deliveries rentalId=1 status="delivered"

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
http localhost:8081/mypages/1

HTTP/1.1 200 
Content-Type: application/hal+json;charset=UTF-8
Date: Thu, 26 Aug 2021 12:18:11 GMT
Transfer-Encoding: chunked

{
    "_links": {
        "mypage": {
            "href": "http://localhost:8084/mypages/1"
        },
        "self": {
            "href": "http://localhost:8084/mypages/1"
        }
    },
    "rentalId": 1,
    "carId": "bbb",
    "rentHour": 4,
    "status": "Car Reservation OK!"
}
```
![rental](https://user-images.githubusercontent.com/87048557/131713297-b506b626-11bf-47ad-b86b-b24d3efa7582.jpeg)
![rental mypage](https://user-images.githubusercontent.com/87048557/131713349-b6ab6162-ebc9-42a0-8fa1-22d6c88f7ed0.jpeg)
![rental mypage cancel](https://user-images.githubusercontent.com/87048557/131713318-52f892f2-3b2b-4127-a4d9-f31e9510789e.jpeg)


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
                "status": "Car Reservation OK!"
            }
```

# 운영

## Deploy

아래와 같은 순서로 AWS 사전 설정을 진행한다.
```
1) AWS IAM 설정
2) EKC Cluster 생성	
3) AWS 클러스터 토큰 가져오기
4) Docker Start/Login 
```
이후 사전 설정이 완료된 상태에서 아래 배포 수행한다.
```
(1) rental build/push
mvn package
docker build -t 184714207239.dkr.ecr.ap-northeast-2.amazonaws.com/jnrental:v1 .
docker push 184714207239.dkr.ecr.ap-northeast-2.amazonaws.com/jnrental:v1

(2) delivery build/push
mvn package
docker build -t 184714207239.dkr.ecr.ap-northeast-2.amazonaws.com/jndelivery:v1 .
docker push 184714207239.dkr.ecr.ap-northeast-2.amazonaws.com/jndelivery:v1

(3) payment build/push
mvn package
docker build -t 184714207239.dkr.ecr.ap-northeast-2.amazonaws.com/jnpayment:v1 .
docker push 184714207239.dkr.ecr.ap-northeast-2.amazonaws.com/jnpayment:v1

(4) mypage build/push
mvn package
docker build -t 184714207239.dkr.ecr.ap-northeast-2.amazonaws.com/jnmypage:v1 .
docker push 184714207239.dkr.ecr.ap-northeast-2.amazonaws.com/jnmypage:v1

(5) gateway build/push
mvn package
docker build -t 184714207239.dkr.ecr.ap-northeast-2.amazonaws.com/jngateway:v1 .
docker push 184714207239.dkr.ecr.ap-northeast-2.amazonaws.com/jngateway:v1

(6) 배포
kubectl create deploy mypage --image=184714207239.dkr.ecr.ap-northeast-2.amazonaws.com/jnmypage:v1
kubectl create deploy gateway --image=184714207239.dkr.ecr.ap-northeast-2.amazonaws.com/jngateway:v1
kubectl create deploy rental --image=184714207239.dkr.ecr.ap-northeast-2.amazonaws.com/jnrental:v1
kubectl create deploy payment --image=184714207239.dkr.ecr.ap-northeast-2.amazonaws.com/jnpayment:v1
kubectl create deploy delivery --image=184714207239.dkr.ecr.ap-northeast-2.amazonaws.com/jndelivery:v1

kubectl expose deploy mypage --type=ClusterIP --port=8080
kubectl expose deploy rental --type=ClusterIP --port=8080
kubectl expose deploy payment --type=ClusterIP --port=8080
kubectl expose deploy delivery --type=ClusterIP --port=8080
kubectl expose deploy gateway --type=LoadBalancer --port=8080
Gateway는 LoadBalancer type으로 설정하고, 결과는 아래와 같다.
```

![로드밸런서](https://user-images.githubusercontent.com/87048557/131713104-2d1156f2-e19e-462d-a5fb-9db8471fe3e1.jpeg)


## 동기식 호출 / 서킷 브레이킹 / 장애격리

## Circuit Breaker

* Circuit Breaker 프레임워크의 선택: istio 사용하여 구현.

시나리오는 주문(order) → 결제(payment) 시의 연결이 Request/Response 로 연동하여 구현이 되어있고, 주문 요청이 과도할 경우 CB 를 통하여 장애격리.

- DestinationRule 를 생성하여 circuit break 가 발생할 수 있도록 설정 최소 connection pool 설정
```
# destination-rule.yaml

apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: order
spec:
  host: order
  trafficPolicy:
    connectionPool:
      http:
        http1MaxPendingRequests: 1
        maxRequestsPerConnection: 1

```

- istio-injection 활성화

![istio](https://user-images.githubusercontent.com/87048557/131777863-a9f4a2b8-6665-4ca8-b0ce-3917772b180a.jpg)
![istio2](https://user-images.githubusercontent.com/87048557/131777879-9e303c54-72bd-4e78-b452-2130779f58dc.jpg)

- 1명이 10초간 부하 발생하여 100% 정상처리 확인

![CB_load_st_be](https://user-images.githubusercontent.com/3106233/130160213-a083edb3-b40b-4626-8f0d-5c5ff1956cba.jpg)


- 10명이 10초간 부하 발생하여 82.05% 정상처리, 168건 실패 확인

![CB_load_rs_af](https://user-images.githubusercontent.com/3106233/130160265-cc77b0de-1e8a-4713-af89-81011941c93d.jpg)


운영시스템은 죽지 않고 지속적으로 CB 에 의하여 적절히 회로가 열림과 닫힘이 벌어지면서 자원을 보호하고 있음을 보여줌.



### 오토스케일 아웃
mypage에 대한 조회증가 시 replica 를 동적으로 늘려주도록 오토스케일아웃을 설정한다.

- autoscaleout_mypage.yaml에 resources 설정을 추가한다

![hpa5](https://user-images.githubusercontent.com/87048557/131786254-3b68cf2f-00b4-45db-a0b9-557f89fe5def.jpg)

- mypage 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 10프로를 넘어서면 replica 를 5개까지 늘려준다.

![hpa3](https://user-images.githubusercontent.com/87048557/131786140-94ad00fa-1d28-4caa-8183-20be139d1314.jpg)

- 부하를 동시사용자 100명으로 걸어준다.

![hpa1](https://user-images.githubusercontent.com/87048557/131786162-66ea00e0-e77a-478a-8306-2057fffe1d0d.jpg)

- 모니터링 결과 스케일 아웃 정상작동을 확인할 수 있다.

![hpa4](https://user-images.githubusercontent.com/87048557/131786183-7b87da45-b8ac-4745-ad7f-fd06364d65fd.jpg)

## 무정지 재배포 (Readiness)

* 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 CB 설정을 제거함
- mypage microservice v2 이미지를 생성해 deploy
- 새 터미널에서 seige 로 배포작업 직전에 워크로드를 모니터링 함.
- 새버전으로 배포

```
kubectl apply -f /home/zn/rental/kubernetes/deployment_readiness_v1.yml
```

- seige에서  Availability 가 100% 미만으로 떨어졌는지 확인

![레디니스 100미만1](https://user-images.githubusercontent.com/87048557/131712796-d9265475-ec5c-489b-8e77-83ce87bb86a1.jpeg)
![레디니스 100미만2](https://user-images.githubusercontent.com/87048557/131712942-cf51d926-7975-426e-b1a1-498fef89241d.jpeg)

배포기간중 Availability 가 평소 100%에서 80%대로 떨어지는 것을 확인. Kubernetes가 신규로 Deploy된 Microservice를 준비 상태로 인식해 서비스 수행했기 때문임.
방지를 위해 Readiness Probe 를 설정함:

```
# deployment.yaml 의 readiness probe 의 설정:
kubectl apply -f kubernetes/deployment.yaml
```

- 동일한 시나리오로 재배포 한 후 Availability 확인:

![레디니스 100](https://user-images.githubusercontent.com/87048557/131713004-48fe5eac-669f-4ba7-b6e3-a3e963053cc7.jpeg)

배포기간 동안 Availability 가 변화없기 때문에 무정지 재배포가 성공한 것으로 확인됨.


## Liveness

임의로 Pod의 Health check에 문제를 발생시키고, Liveness Probe가 Pod를 재기동하는지 확인

```
          args:
          - /bin/sh
          - -c
          - touch /tmp/healthy; sleep 90; rm -rf /tmp/healthy; sleep 600
          ports:
            - containerPort: 8080
          livenessProbe:
            exec:
              command:
              - cat
              - /tmp/healthy
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5
```


RESTARTS 회수가 증가함.

![리브니스](https://user-images.githubusercontent.com/87048557/131712664-38388ef2-4fac-4d4a-94e2-d5500dcc0930.jpeg)!

## Persistence Volume
신규로 생성한 EFS Storage에 Pod가 접근할 수 있도록 권한 및 서비스 설정.

1. EFS 생성: ClusterSharedNodeSecurityGroup 선택
![efs2](https://user-images.githubusercontent.com/87048557/131711600-bb9f420e-55ed-468b-ad8e-acb073137297.jpg)
![efs3](https://user-images.githubusercontent.com/87048557/131712185-f341ad69-cfc2-4fc8-9c6d-f542bfb55470.jpg)

2. EFS계정 생성 및 Role 바인딩
```
- ServerAccount 생성
kubectl apply -f efs-sa.yml
kubectl get ServiceAccount efs-provisioner -n rental


-SA(efs-provisioner)에 권한(rbac) 설정
kubectl apply -f efs-rbac.yaml

# efs-provisioner-deploy.yml 파일 수정
value: fs-941997f4
value: ap-northeast-2
server: fs-941997f4.efs.ap-northeast-2.amazonaws.com
```

3. EFS provisioner 설치
```
kubectl apply -f efs-provisioner-deploy.yml
kubectl get Deployment efs-provisioner -n rental
```

4. EFS storageclass 생성
```
kubectl apply -f efs-storageclass.yaml
kubectl get sc aws-efs -n rental
```

5. PVC 생성
```
kubectl apply -f volume-pvc.yml
kubectl get pvc -n rental
```

6. Create Pod with PersistentVolumeClaim
```
kubectl apply -f pod-with-pvc.yaml
```
- df-k로 EFS에 접근 가능
![efs1](https://user-images.githubusercontent.com/87048557/131712284-c7623cd7-6f30-4bef-b813-2351dc8e7a50.jpeg)
