@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "content::api",
                "event::api",
                "shared::persistence",
                "user::api"
        })
package com.tellpal.v2.purchase;
