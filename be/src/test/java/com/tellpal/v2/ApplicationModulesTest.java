package com.tellpal.v2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

class ApplicationModulesTest {

    private final ApplicationModules modules = ApplicationModules.of(TellPalApplication.class);

    @Test
    void verifiesModuleBoundaries() {
        modules.verify();
    }

    @Test
    void exposesExpectedModuleGraph() {
        assertThat(modules.stream()
                .map(module -> module.getIdentifier().toString())
                .sorted()
                .toList())
                .containsExactly("admin", "asset", "category", "content", "event", "purchase", "shared", "user");
        assertThat(directDependenciesOf("admin")).containsExactly("shared");
        assertThat(directDependenciesOf("asset")).containsExactly("shared");
        assertThat(directDependenciesOf("category")).containsExactly("asset", "content", "shared");
        assertThat(directDependenciesOf("content")).containsExactly("asset", "shared");
        assertThat(directDependenciesOf("event")).containsExactly("content", "shared", "user");
        assertThat(directDependenciesOf("purchase")).containsExactly("content", "event", "shared", "user");
        assertThat(directDependenciesOf("shared")).isEmpty();
        assertThat(directDependenciesOf("user")).containsExactly("asset", "shared");
    }

    private List<String> directDependenciesOf(String moduleName) {
        ApplicationModule module = modules.stream()
                .filter(candidate -> candidate.getIdentifier().toString().equals(moduleName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown module: " + moduleName));
        return module.getDirectDependencies(modules)
                .uniqueModules()
                .map(candidate -> candidate.getIdentifier().toString())
                .sorted()
                .toList();
    }
}
