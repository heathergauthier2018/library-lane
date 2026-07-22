package com.librarylane.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.librarylane.catalog.CatalogBookResult;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class WikidataCatalogService {

    private static final String API = "https://www.wikidata.org/w/api.php";
    private final RestClient restClient;

    public WikidataCatalogService(RestClient.Builder builder) {
        this.restClient = builder
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "LibraryLane/1.0 (book metadata resolver)")
                .build();
    }

    /**
     * Adds only structured work facts: series (P179), series ordinal (P1545),
     * and original publication date (P577). A candidate must have an exact
     * normalized title and, when Wikidata names an author, a matching author.
     */
    public CatalogBookResult enrich(CatalogBookResult selected) {
        if (selected == null || !hasText(selected.title())) return selected;

        Set<String> searchedIds = new LinkedHashSet<>(searchIds(selected.title()));
        if (!safe(selected.authors()).isEmpty()) {
            searchedIds.addAll(searchIds(selected.title() + " "
                    + selected.authors().get(0)));
        }
        List<String> candidateIds = new ArrayList<>(searchedIds);
        if (candidateIds.isEmpty()) return selected;

        JsonNode initialEntities = getEntities(candidateIds).path("entities");
        Set<String> allCandidateIds = new LinkedHashSet<>(candidateIds);
        for (String id : candidateIds) {
            // Search may return a paperback/e-book item. Follow P629 to the
            // canonical work, where series and original-publication facts
            // normally belong.
            allCandidateIds.addAll(entityIds(initialEntities.path(id), "P629"));
        }
        candidateIds = new ArrayList<>(allCandidateIds);
        JsonNode entities = getEntities(candidateIds).path("entities");
        Set<String> relatedIds = new LinkedHashSet<>();
        for (String id : candidateIds) {
            JsonNode entity = entities.path(id);
            relatedIds.addAll(entityIds(entity, "P50"));
            relatedIds.addAll(entityIds(entity, "P179"));
            relatedIds.addAll(entityIds(entity, "P136"));
        }
        Map<String, String> relatedLabels = labels(relatedIds);

        String wantedTitle = normalize(selected.title());
        Set<String> wantedAuthors = new LinkedHashSet<>();
        for (String author : safe(selected.authors())) {
            if (hasText(author)) wantedAuthors.add(canonicalPerson(author));
        }

        List<Match> matches = new ArrayList<>();
        for (String id : candidateIds) {
            JsonNode entity = entities.path(id);
            String title = label(entity);
            if (!normalize(title).equals(wantedTitle)) continue;

            List<String> authorIds = entityIds(entity, "P50");
            List<String> verifiedAuthors = authorIds.stream()
                    .map(relatedLabels::get)
                    .filter(WikidataCatalogService::hasText)
                    .toList();
            boolean authorMatch = authorIds.stream()
                    .map(relatedLabels::get)
                    .filter(WikidataCatalogService::hasText)
                    .map(WikidataCatalogService::canonicalPerson)
                    .anyMatch(wantedAuthors::contains);

            String description = description(entity);
            boolean describedAsBook = normalize(description).matches(
                    ".*\\b(book|novel|novella|literary work|fiction)\\b.*");
            if (!authorIds.isEmpty() && !wantedAuthors.isEmpty() && !authorMatch) continue;
            if (authorIds.isEmpty() && !describedAsBook) continue;

            SeriesFact series = seriesFact(entity, relatedLabels);
            List<String> genres = entityIds(entity, "P136").stream()
                    .map(relatedLabels::get)
                    .filter(WikidataCatalogService::hasText)
                    .toList();
            Integer year = publicationYear(entity);
            int score = (authorMatch ? 100 : 0)
                    + (describedAsBook ? 20 : 0)
                    + (series.name() != null ? 15 : 0)
                    + (series.number() != null ? 15 : 0)
                    + (year != null ? 5 : 0);
            matches.add(new Match(score, series, year, genres, verifiedAuthors));
        }

        Match best = matches.stream()
                .max(Comparator.comparingInt(Match::score))
                .orElse(null);
        if (best == null) return selected;

        String seriesName = hasText(best.series().name())
                ? best.series().name()
                : selected.seriesName();
        Double seriesNumber = best.series().number() != null
                ? best.series().number()
                : selected.seriesNumber();
        String publication = best.year() != null
                ? String.valueOf(best.year())
                : selected.publicationDate();

        return new CatalogBookResult(
                combinedProvider(selected.provider(), "WIKIDATA"),
                selected.providerId(),
                selected.title(),
                selected.subtitle(),
                preferVerifiedAuthors(selected.authors(), best.authors()),
                union(selected.genres(), best.genres()),
                selected.description(),
                selected.publisher(),
                publication,
                selected.pageCount(),
                selected.audiobookLengthSeconds(),
                safe(selected.narrators()),
                selected.coverImageUrl(),
                selected.language(),
                selected.isbn10(),
                selected.isbn13(),
                seriesName,
                seriesNumber,
                selected.editionFormat(),
                selected.format()
        );
    }

    private List<String> searchIds(String title) {
        URI uri = UriComponentsBuilder.fromUriString(API)
                .queryParam("action", "wbsearchentities")
                .queryParam("search", title)
                .queryParam("language", "en")
                .queryParam("uselang", "en")
                .queryParam("type", "item")
                .queryParam("limit", 25)
                .queryParam("format", "json")
                .build().encode().toUri();
        JsonNode response = restClient.get().uri(uri).retrieve().body(JsonNode.class);
        List<String> ids = new ArrayList<>();
        if (response != null && response.path("search").isArray()) {
            response.path("search").forEach(item -> {
                String id = text(item.path("id"));
                if (hasText(id)) ids.add(id);
            });
        }
        return ids;
    }

    private JsonNode getEntities(Iterable<String> ids) {
        List<String> values = new ArrayList<>();
        ids.forEach(values::add);
        if (values.isEmpty()) return com.fasterxml.jackson.databind.node.NullNode.instance;
        URI uri = UriComponentsBuilder.fromUriString(API)
                .queryParam("action", "wbgetentities")
                .queryParam("ids", String.join("|", values))
                .queryParam("props", "labels|descriptions|claims")
                .queryParam("languages", "en")
                .queryParam("format", "json")
                .build().encode().toUri();
        JsonNode response = restClient.get().uri(uri).retrieve().body(JsonNode.class);
        return response == null
                ? com.fasterxml.jackson.databind.node.NullNode.instance
                : response;
    }

    private Map<String, String> labels(Set<String> ids) {
        Map<String, String> labels = new LinkedHashMap<>();
        if (ids.isEmpty()) return labels;
        JsonNode entities = getEntities(ids).path("entities");
        for (String id : ids) {
            String label = label(entities.path(id));
            if (hasText(label)) labels.put(id, label);
        }
        return labels;
    }

    private static SeriesFact seriesFact(
            JsonNode entity,
            Map<String, String> labels) {
        JsonNode statements = entity.path("claims").path("P179");
        if (!statements.isArray()) return new SeriesFact(null, null);
        for (JsonNode statement : statements) {
            String seriesId = entityId(statement.path("mainsnak"));
            if (!hasText(seriesId)) continue;
            Double ordinal = qualifierNumber(statement.path("qualifiers").path("P1545"));
            return new SeriesFact(labels.get(seriesId), ordinal);
        }
        return new SeriesFact(null, null);
    }

    private static Double qualifierNumber(JsonNode qualifiers) {
        if (!qualifiers.isArray()) return null;
        for (JsonNode qualifier : qualifiers) {
            JsonNode value = qualifier.path("datavalue").path("value");
            String raw = value.isTextual() ? value.asText() : null;
            if (!hasText(raw)) continue;
            var matcher = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)")
                    .matcher(raw);
            if (matcher.find()) return Double.valueOf(matcher.group(1));
        }
        return null;
    }

    private static Integer publicationYear(JsonNode entity) {
        JsonNode statements = entity.path("claims").path("P577");
        if (!statements.isArray()) return null;
        Integer earliest = null;
        for (JsonNode statement : statements) {
            String time = text(statement.path("mainsnak").path("datavalue")
                    .path("value").path("time"));
            if (!hasText(time)) continue;
            var matcher = java.util.regex.Pattern.compile("[+-](\\d{4})-").matcher(time);
            if (matcher.find()) {
                int year = Integer.parseInt(matcher.group(1));
                earliest = earliest == null ? year : Math.min(earliest, year);
            }
        }
        return earliest;
    }

    private static List<String> entityIds(JsonNode entity, String property) {
        List<String> ids = new ArrayList<>();
        JsonNode statements = entity.path("claims").path(property);
        if (!statements.isArray()) return ids;
        for (JsonNode statement : statements) {
            String id = entityId(statement.path("mainsnak"));
            if (hasText(id)) ids.add(id);
        }
        return ids;
    }

    private static String entityId(JsonNode snak) {
        return text(snak.path("datavalue").path("value").path("id"));
    }

    private static String label(JsonNode entity) {
        return text(entity.path("labels").path("en").path("value"));
    }

    private static String description(JsonNode entity) {
        return text(entity.path("descriptions").path("en").path("value"));
    }

    private static String combinedProvider(String first, String second) {
        if (!hasText(first)) return second;
        if (first.contains(second)) return first;
        return first + "+" + second;
    }

    private static String canonicalPerson(String value) {
        return java.util.Arrays.stream(normalize(value).split(" "))
                .filter(token -> !token.isBlank())
                .sorted()
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim().replaceAll("\\s+", " ");
    }

    private static String text(JsonNode node) {
        return node != null && node.isValueNode() && !node.isNull()
                ? node.asText(null)
                : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static List<String> union(List<String> first, List<String> second) {
        Set<String> values = new LinkedHashSet<>();
        values.addAll(safe(first));
        values.addAll(safe(second));
        return values.stream().filter(WikidataCatalogService::hasText).limit(12).toList();
    }

    private static List<String> preferVerifiedAuthors(
            List<String> selected,
            List<String> verified) {
        List<String> trusted = safe(verified).stream()
                .filter(WikidataCatalogService::hasText)
                .distinct()
                .limit(4)
                .toList();
        return trusted.isEmpty() ? safe(selected) : trusted;
    }

    private record SeriesFact(String name, Double number) {}
    private record Match(
            int score,
            SeriesFact series,
            Integer year,
            List<String> genres,
            List<String> authors) {}
}
