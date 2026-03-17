package com.tellpal.v2;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ApplicationModulesDocumentationIT {

    @Test
    void writesModulithDocumentationDuringVerify() {
        ApplicationModules modules = ApplicationModules.of(TellPalApplication.class);
        new Documenter(modules, Documenter.Options.defaults().withOutputFolder("docs/modulith"))
                .writeDocumentation();
    }
}
