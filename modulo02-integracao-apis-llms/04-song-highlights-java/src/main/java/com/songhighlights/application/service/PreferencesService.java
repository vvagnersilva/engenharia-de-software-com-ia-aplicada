package com.songhighlights.application.service;

import com.songhighlights.domain.model.ConversationSummaryData;
import com.songhighlights.domain.model.UserPreferencesData;
import com.songhighlights.domain.port.UserPreferencesRepository;
import org.springframework.stereotype.Service;

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
 * importantContext em vez de substituir). A serialização em JSON das listas
 * é detalhe da infraestrutura (UserPreferencesRepository); aqui só se lida
 * com o modelo de domínio já desserializado.
 */
@Service
public class PreferencesService {

    private static final ConversationSummaryData EMPTY_SUMMARY =
            new ConversationSummaryData(null, null, List.of(), List.of(), null, null);

    private final UserPreferencesRepository repository;

    public PreferencesService(UserPreferencesRepository repository) {
        this.repository = repository;
    }

    public void mergePreferences(String userId, UserPreferencesData prefs) {
        ConversationSummaryData existing = repository.findByUserId(userId).orElse(EMPTY_SUMMARY);

        List<String> mergedGenres = mergeLists(existing.favoriteGenres(), prefs.favoriteGenres());
        List<String> mergedBands = mergeLists(existing.favoriteBands(), prefs.favoriteBands());

        String mergedContext = Stream.of(
                        existing.importantContext(),
                        hasText(prefs.mood()) ? "Mood: " + prefs.mood() : null,
                        hasText(prefs.listeningContext()) ? "Context: " + prefs.listeningContext() : null,
                        prefs.additionalInfo()
                )
                .filter(PreferencesService::hasText)
                .collect(Collectors.joining(". "));

        ConversationSummaryData merged = new ConversationSummaryData(
                hasText(prefs.name()) ? prefs.name() : existing.name(),
                hasValue(prefs.age()) ? prefs.age() : existing.age(),
                mergedGenres,
                mergedBands,
                existing.keyPreferences(),
                hasText(mergedContext) ? mergedContext : null
        );

        repository.save(userId, merged);
    }

    public void storeSummary(String userId, ConversationSummaryData summary) {
        ConversationSummaryData normalized = new ConversationSummaryData(
                hasText(summary.name()) ? summary.name() : null,
                hasValue(summary.age()) ? summary.age() : null,
                summary.favoriteGenres(),
                summary.favoriteBands(),
                summary.keyPreferences(),
                hasText(summary.importantContext()) ? summary.importantContext() : null
        );
        repository.save(userId, normalized);
    }

    public Optional<ConversationSummaryData> getSummary(String userId) {
        return repository.findByUserId(userId);
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
}
