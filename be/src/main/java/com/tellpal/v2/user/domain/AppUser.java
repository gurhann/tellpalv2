package com.tellpal.v2.user.domain;

import com.tellpal.v2.shared.infrastructure.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "v2_app_users")
public class AppUser extends BaseEntity {

    @Column(name = "firebase_uid", nullable = false, unique = true, columnDefinition = "text")
    private String firebaseUid;

    @Column(name = "is_allow_marketing", nullable = false)
    private boolean isAllowMarketing = false;

    protected AppUser() {
    }

    public AppUser(String firebaseUid) {
        this.firebaseUid = firebaseUid;
        this.isAllowMarketing = false;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public boolean isAllowMarketing() {
        return isAllowMarketing;
    }

    public void setAllowMarketing(boolean allowMarketing) {
        isAllowMarketing = allowMarketing;
    }
}
