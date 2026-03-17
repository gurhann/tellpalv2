@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "content::api",
                "shared::domain",
                "user::api"
        })
package com.tellpal.v2.event;
