package com.tellpal.v2.user.domain;

import com.tellpal.v2.shared.infrastructure.persistence.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "v2_user_profiles")
public class UserProfile extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", columnDefinition = "text")
    private String name;

    @Column(name = "age_range")
    private Integer ageRange;

    @Column(name = "avatar_media_id")
    private Long avatarMediaId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "v2_user_profile_favorite_genres",
            joinColumns = @JoinColumn(name = "user_profile_id")
    )
    @Column(name = "genre", columnDefinition = "text")
    @OrderColumn(name = "position")
    private List<String> favoriteGenres = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "v2_user_profile_main_purposes",
            joinColumns = @JoinColumn(name = "user_profile_id")
    )
    @Column(name = "purpose", columnDefinition = "text")
    @OrderColumn(name = "position")
    private List<String> mainPurposes = new ArrayList<>();

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;

    protected UserProfile() {
    }

    public UserProfile(Long userId, boolean isPrimary) {
        this.userId = userId;
        this.isPrimary = isPrimary;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAgeRange() {
        return ageRange;
    }

    public void setAgeRange(Integer ageRange) {
        this.ageRange = ageRange;
    }

    public Long getAvatarMediaId() {
        return avatarMediaId;
    }

    public void setAvatarMediaId(Long avatarMediaId) {
        this.avatarMediaId = avatarMediaId;
    }

    public List<String> getFavoriteGenres() {
        return favoriteGenres;
    }

    public void setFavoriteGenres(List<String> favoriteGenres) {
        this.favoriteGenres = favoriteGenres != null ? favoriteGenres : new ArrayList<>();
    }

    public List<String> getMainPurposes() {
        return mainPurposes;
    }

    public void setMainPurposes(List<String> mainPurposes) {
        this.mainPurposes = mainPurposes != null ? mainPurposes : new ArrayList<>();
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }
}
