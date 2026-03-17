@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "shared::domain",
                "shared::persistence",
                "shared::admin-web"
        })
package com.tellpal.v2.asset;
