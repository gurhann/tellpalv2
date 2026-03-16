package com.tellpal.v2.content.domain;

import com.tellpal.v2.shared.infrastructure.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "v2_contributors")
public class Contributor extends BaseEntity {

    @Column(name = "display_name", nullable = false)
    private String displayName;

    protected Contributor() {
    }

    public Contributor(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
