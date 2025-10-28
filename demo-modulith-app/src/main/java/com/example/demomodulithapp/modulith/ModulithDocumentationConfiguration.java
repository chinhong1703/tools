package com.example.demomodulithapp.modulith;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;
import org.springframework.modulith.docs.Documenter.Mode;

@Configuration(proxyBeanMethods = false)
public class ModulithDocumentationConfiguration {

    @Bean
    Documenter documenter(ApplicationModules modules) {
        Documenter documenter = Documenter.of(modules);
        documenter.writeDocumentation();
        documenter.writeModulesAsPlantUml(Mode.Aggregated);
        return documenter;
    }
}
