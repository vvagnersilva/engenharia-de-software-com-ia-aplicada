package com.songhighlights.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.songhighlights.entity.UserPreferencesEntity;
import com.songhighlights.llm.ConversationSummaryData;
import com.songhighlights.llm.UserPreferencesData;
import com.songhighlights.repository.UserPreferencesRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Porte de src/services/preferencesService.ts. Mantém a mesma semântica de
 * merge (união de gêneros/bandas, nunca sobrescrever com vazio, concatenar em
 * importantContext em vez de substituir).
 */
@Service
public class PreferencesService {

    private final UserPreferencesRepository repository;
    private final ObjectMapper objectMapper;

    public PreferencesService(UserPreferencesRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void mergePreferences(String userId, UserPreferencesData prefs) {
        UserPreferencesEntity entity = repository.findByUserId(userId).orElseGet(UserPreferencesEntity::new);
        entity.setUserId(userId);

        List<String> existingGenres = readList(entity.getFavoriteGenresJson());
        List<String> existingBands = readList(entity.getFavoriteBandsJson());

        List<String> mergedGenres = mergeLists(existingGenres, prefs.favoriteGenres());
        List<String> mergedBands = mergeLists(existingBands, prefs.favoriteBands());

        String mergedContext = Stream.of(
                        entity.getImportantContext(),
                        hasText(prefs.mood()) ? "Mood: " + prefs.mood() : null,
                        hasText(prefs.listeningContext()) ? "Context: " + prefs.listeningContext() : null,
                        prefs.additionalInfo()
                )
                .filter(PreferencesService::hasText)
                .collect(Collectors.joining(". "));

        entity.setName(hasText(prefs.name()) ? prefs.name() : entity.getName());
        entity.setAge(hasValue(prefs.age()) ? prefs.age() : entity.getAge());
        entity.setFavoriteGenresJson(writeList(mergedGenres));
        entity.setFavoriteBandsJson(writeList(mergedBands));
        entity.setImportantContext(hasText(mergedContext) ? mergedContext : null);
        entity.setUpdatedAt(Instant.now());

        repository.save(entity);
    }

    public void storeSummary(String userId, ConversationSummaryData summary) {
        UserPreferencesEntity entity = repository.findByUserId(userId).orElseGet(UserPreferencesEntity::new);
        entity.setUserId(userId);
        entity.setName(hasText(summary.name()) ? summary.name() : null);
        entity.setAge(hasValue(summary.age()) ? summary.age() : null);
        entity.setFavoriteGenresJson(writeList(summary.favoriteGenres()));
        entity.setFavoriteBandsJson(writeList(summary.favoriteBands()));
        entity.setKeyPreferences(summary.keyPreferences());
        entity.setImportantContext(hasText(summary.importantContext()) ? summary.importantContext() : null);
        entity.setUpdatedAt(Instant.now());

        repository.save(entity);
    }

    public Optional<ConversationSummaryData> getSummary(String userId) {
        return repository.findByUserId(userId)
                .map(entity -> new ConversationSummaryData(
                        entity.getName(),
                        entity.getAge(),
                        readList(entity.getFavoriteGenresJson()),
                        readList(entity.getFavoriteBandsJson()),
                        entity.getKeyPreferences(),
                        entity.getImportantContext()
                ));
    }

    public Optional<String> getBasicInfo(String userId) {
        return getSummary(userId).map(summary -> {
            List<String> parts = new ArrayList<>();

            if (summary.name() != null) parts.add("Nome: " + summary.name());
            if (summary.age() != null) parts.add("Idade: " + summary.age());
            if (summary.favoriteGenres() != null && !summary.favoriteGenres().isEmpty()) {
                parts.add("Gêneros Favoritos: " + String.join(", ", summary.favoriteGenres()));
            }
            if (summary.favoriteBands() != null && !summary.favoriteBands().isEmpty()) {
                parts.add("Artistas/Bandas Favoritas: " + String.join(", ", summary.favoriteBands()));
            }
            if (summary.keyPreferences() != null) {
                parts.add("\nPreferências: " + summary.keyPreferences());
            }

            return parts.isEmpty() ? null : String.join("\n", parts);
        }).filter(text -> text != null);
    }

    /**
     * Equivalente à checagem "truthy" do JavaScript ({@code prefs.mood && ...},
     * {@code summary.name || null}) usada pervasivamente no projeto TypeScript
     * original: string nula, vazia ou só com espaços conta como "ausente".
     */
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Mesma ideia de hasText, mas para números: no JS, {@code 0} também é
     * falsy, então {@code prefs.age || existing?.age || null} trata idade 0
     * como "não informada". Preservado aqui para manter paridade de
     * comportamento com o original.
     */
    private static boolean hasValue(Integer value) {
        return value != null && value != 0;
    }

    private List<String> mergeLists(List<String> existing, List<String> incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return existing;
        }
        Set<String> merged = new LinkedHashSet<>(existing != null ? existing : List.of());
        merged.addAll(incoming);
        return new ArrayList<>(merged);
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
