package com.songhighlights.infrastructure.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.songhighlights.domain.model.ConversationSummaryData;
import com.songhighlights.domain.port.UserPreferencesRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementação de UserPreferencesRepository sobre Spring Data JPA. Esconde
 * da aplicação o detalhe de que gêneros/bandas são persistidos como JSON em
 * texto - a aplicação só lida com ConversationSummaryData já desserializado.
 */
@Component
public class UserPreferencesRepositoryAdapter implements UserPreferencesRepository {

    private final UserPreferencesJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public UserPreferencesRepositoryAdapter(UserPreferencesJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<ConversationSummaryData> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public void save(String userId, ConversationSummaryData preferences) {
        UserPreferencesJpaEntity entity = jpaRepository.findByUserId(userId).orElseGet(UserPreferencesJpaEntity::new);
        entity.setUserId(userId);
        entity.setName(preferences.name());
        entity.setAge(preferences.age());
        entity.setFavoriteGenresJson(writeList(preferences.favoriteGenres()));
        entity.setFavoriteBandsJson(writeList(preferences.favoriteBands()));
        entity.setKeyPreferences(preferences.keyPreferences());
        entity.setImportantContext(preferences.importantContext());
        entity.setUpdatedAt(Instant.now());

        jpaRepository.save(entity);
    }

    private ConversationSummaryData toDomain(UserPreferencesJpaEntity entity) {
        return new ConversationSummaryData(
                entity.getName(),
                entity.getAge(),
                readList(entity.getFavoriteGenresJson()),
                readList(entity.getFavoriteBandsJson()),
                entity.getKeyPreferences(),
                entity.getImportantContext()
        );
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String writeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar lista para JSON", e);
        }
    }
}
