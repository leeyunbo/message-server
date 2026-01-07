# Message Server 

## Netty 기반 대량 트래픽 서버 설계

---

## 목차

1. [프로젝트 배경](#프로젝트-배경)
2. [아키텍처 비교](#아키텍처-비교)

---

## 프로젝트 배경

---

## 아키텍처 비교

### 4가지 버전

| 버전 | 모드 | 포트 | 핵심 특징                         |
|------|------|------|-------------------------------|
| **V1** | Blocking | 8082 | EventLoop에서 직접 블로킹 (현재 아키텍처)  |
| **V2** | Non-blocking | 8083 | ThreadPool 오프로드 + Backpressure |
| **V3** | Reactive | 8084 | reactor-rabbitmq NIO 기반       |
| **V4** | Virtual Thread | 8085 | Java 21 Virtual Thread        |

### V1 Blocking (현재 아키텍처)
![Blocking Architecture](docs/images/v1.png)
- EventLoop Thread에서 RabbitMQ 동기 호출하는 형태
- Publish 후 RabbitMQ로부터 응답을 받을 때까지 EventLoop가 블로킹됨

### V2 Non-blocking (ThreadPool 오프로드)
![Non-blocking Architecture](docs/images/v2.png)
- EventLoop에서 RabbitMQ Publish를 ThreadPool로 오프로드하여 블로킹 회피
- 동시성 제한(Backpressure) 추가로 과부하 방지
- 만약 Backpressure가 설정이 안되어있으면 대량 트래픽이 들어올 경우 Thread가 고갈되어 처리 지연 및 장애 발생 가능
- 1 Request Per 1 Thread 모델이기 때문에 메모리 소모가 큼

### V3 Reactive (NIO 기반)
![Reactive Architecture](docs/images/v3.png)
- reactor-rabbitmq 라이브러리를 사용하여 NIO 기반 비동기 호출
- reactor-rabbitmq는 RabbitMQ Java Client를 Reactive하게 감싼 라이브러리
- 반응형 패러다임으로, RabbitMQ I/O 작업이 완료되면 Publisher가 Stream에 이벤트를 발행하고 Subscriber인 EventLoop가 이를 처리
- I/O 작업 대기 시에도 스레드를 점유하지 않기 때문에 V2와 비교하여 메모리 소모가 훨씬 적음
- 동시에 컨텍스트 스위칭 오버헤드가 거의 존재하지 않기 때문에 CPU 사용률도 낮음

### V4 Virtual Thread
![Reactive Architecture](docs/images/v4.png)
- Java 21의 Virtual Thread를 사용한 방식으로 V2와 유사하게 ThreadPool 오프로드 구조
- Virtual Thread는 경량 스레드로, 수천 개의 Virtual Thread를 생성해도 메모리 소모가 적음
- Virtual Thread는 OS 스레드와 1:1 매핑되지 않고, 필요에 따라 OS 스레드에서 실행되므로 많은 수의 동시 작업을 효율적으로 처리 가능
- V2와 비교하여 메모리 소모가 훨씬 적음
- RabbitTemplate에 synchronized 블록이 다수 포함되어 있어 Carrier Thread에 고정 되는 현상 발생 (사용 불가)

---

## 부하 테스트 결과

---

### 테스트 방식

#### K6 부하 테스트 스크립트

```javascript
```

---

### 결론 

