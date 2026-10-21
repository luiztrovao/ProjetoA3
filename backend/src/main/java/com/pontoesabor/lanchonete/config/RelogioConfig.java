package com.pontoesabor.lanchonete.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Relógio da aplicação: injetado nos services para que os testes possam fixar a data e hora. */
@Configuration
public class RelogioConfig {

    @Bean
    Clock relogio() {
        return Clock.systemDefaultZone();
    }
}
