package com.tellpal.v2;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith boundary verification test.
 *
 * Verifies that all module boundaries are respected:
 * - No module accesses internal classes of another module.
 * - Cross-module communication uses application services, domain events, or IDs only.
 *
 * Also generates module documentation (C4 + PlantUML diagrams) under target/modulith-docs/.
 *
 * NOTE: ApplicationModules.of() performs static classpath analysis — no Spring context needed.
 * The main classes must be on the test classpath, which Maven guarantees via compile scope.
 */
class ApplicationModulesTest {

    @Test
    void verifiesModuleBoundaries() {
        ApplicationModules modules = ApplicationModules.of("com.tellpal.v2");
        modules.verify();
    }

    @Test
    void documentsModules() {
        ApplicationModules modules = ApplicationModules.of("com.tellpal.v2");
        new Documenter(modules)
                .writeDocumentation()
                .writeIndividualModulesAsPlantUml();
    }
}
