package com.example.demomodulithapp.modulith;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;
import org.springframework.modulith.test.ApplicationModuleTest;

@SpringBootTest
@ApplicationModuleTest
class ModulithDocumentationTest {

    private final ApplicationModules modules;

    ModulithDocumentationTest(ApplicationModules modules) {
        this.modules = modules;
    }

    @Test
    void verifyModuleStructure() {
        modules.verify();
    }

    @Test
    void writeDocumentation() {
        Documenter.of(modules).writeDocumentation();
    }
}
