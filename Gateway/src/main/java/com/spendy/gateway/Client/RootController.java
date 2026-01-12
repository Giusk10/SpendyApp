package com.spendy.gateway.Client;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class RootController {

    @GetMapping("/root")
    public Mono<String> home() {
        return Mono.just("🟢 Spendy Gateway è attivo e funzionante!");
    }
}
