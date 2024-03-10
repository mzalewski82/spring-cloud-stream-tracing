package com.example.kafkaclient;

import io.micrometer.observation.ObservationRegistry;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.observability.micrometer.Micrometer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class KafkaClient {

  private final HelloReactiveRepo helloReactiveRepo;
  private final HelloSpan helloSpan;
  private final ObservationRegistry observationRegistry;

  @Bean
  public Function<Flux<Message<String>>, Mono<Void>> helloReactive() {
    return flux -> processMessages(flux, "reactive");
  }

  @Bean
  public Function<Flux<Message<String>>, Mono<Void>> helloReactor() {
    return flux -> processMessages(flux, "reactor");
  }

  @NotNull
  private Mono<Void> processMessages(Flux<Message<String>> flux, String logName) {
    return flux
        .flatMap(msg ->
            Mono.just(msg)
                .map(message -> {
                  log.info("Received {} message {}", logName, message);
                  Hello hello = new Hello();
                  hello.setText(message.getPayload());
                  return hello;
                })
                .flatMap(helloSpan::logHelloBefore)
                .flatMap(helloReactiveRepo::save)
                .flatMap(helloSpan::logHelloAfter)
                .doOnNext(message -> log.info("Saved {} message {}", logName, message))
                .tap(Micrometer.observation(observationRegistry))
        )
        .then();
  }
}
