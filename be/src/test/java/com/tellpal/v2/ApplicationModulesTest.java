package com.tellpal.v2;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ApplicationModulesTest {

    @Test
    void verifiesModuleBoundaries() {
        ApplicationModules.of(TellPalApplication.class).verify();
    }
}
