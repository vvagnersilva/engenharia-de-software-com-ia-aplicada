package com.songhighlights.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Equivalente à tabela user_preferences criada por PreferencesService (Knex)
 * no projeto TypeScript original. favoriteGenres/favoriteBands são
 * armazenados como JSON serializado em texto, igual ao comportamento
 * table.json(...) + JSON.stringify/JSON.parse do Knex.
 */
@Entity
@Table(name = "user_preferences")
public class UserPreferencesJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false)
    private String userId;

    private String name;

    private Integer age;

    @Column(name = "favorite_genres", columnDefinition = "text")
    private String favoriteGenresJson;

    @Column(name = "favorite_bands", columnDefinition = "text")
    private String favoriteBandsJson;

    @Column(name = "key_preferences", columnDefinition = "text")
    private String keyPreferences;

    @Column(name = "important_context", columnDefinition = "text")
    private String importantContext;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getFavoriteGenresJson() {
        return favoriteGenresJson;
    }

    public void setFavoriteGenresJson(String favoriteGenresJson) {
        this.favoriteGenresJson = favoriteGenresJson;
    }

    public String getFavoriteBandsJson() {
        return favoriteBandsJson;
    }

    public void setFavoriteBandsJson(String favoriteBandsJson) {
        this.favoriteBandsJson = favoriteBandsJson;
    }

    public String getKeyPreferences() {
        return keyPreferences;
    }

    public void setKeyPreferences(String keyPreferences) {
        this.keyPreferences = keyPreferences;
    }

    public String getImportantContext() {
        return importantContext;
    }

    public void setImportantContext(String importantContext) {
        this.importantContext = importantContext;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
