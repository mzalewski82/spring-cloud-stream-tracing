package com.example.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HelloController {

  private final StreamBridge streamBridge;
  private final HelloReactiveRepo helloReactiveRepo;
  private final HelloSpan helloSpan;

  @GetMapping("hello")
  public Mono<Hello> hello(@RequestParam String name) {
    return Mono.just(name)
        .doOnNext(it -> log.info("Saying hello to: {}", it))
        .map(it -> {
          String helloString = String.format("Hello %s !!!", it);

          streamBridge.send("hello-out-0", helloString);

          Hello hello = new Hello();
          hello.setText(helloString);
          return hello;
        })
        .doOnNext(helloSpan::logHello)
        .flatMap(helloReactiveRepo::save)
        .flatMap(helloSpan::logHelloReactive);
  }
}
