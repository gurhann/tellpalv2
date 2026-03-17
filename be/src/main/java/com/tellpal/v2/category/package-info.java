@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "asset::api",
                "content::api",
                "shared::domain",
                "shared::persistence",
                "shared::admin-web"
        })
package com.tellpal.v2.category;
