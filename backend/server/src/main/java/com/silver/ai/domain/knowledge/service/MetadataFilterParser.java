package com.silver.ai.domain.knowledge.service;

import com.silver.ai.domain.knowledge.model.VectorMetadataKeys;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析并合并受限的元数据过滤表达式。
 */
public final class MetadataFilterParser {

    private static final Set<String> ALLOWED_FIELDS = Set.of(
            VectorMetadataKeys.DOCUMENT_ID,
            VectorMetadataKeys.FILE_TYPE,
            VectorMetadataKeys.FILE_NAME
    );

    private static final Pattern IN_PATTERN = Pattern.compile("(?i)^([a-z_]+)\\s+IN\\s*\\((.+)\\)$");
    private static final Pattern EQ_PATTERN = Pattern.compile("(?i)^([a-z_]+)\\s*=\\s*(.+)$");

    public Map<String, Object> parse(String expression) {
        if (expression == null || expression.isBlank()) {
            return Map.of();
        }

        Map<String, Object> filters = new LinkedHashMap<>();
        for (String rawClause : expression.split("(?i)\\s+AND\\s+")) {
            String clause = rawClause == null ? "" : rawClause.trim();
            if (clause.isBlank()) {
                continue;
            }
            Matcher inMatcher = IN_PATTERN.matcher(clause);
            if (inMatcher.matches()) {
                mergeField(filters, normalizeField(inMatcher.group(1)), parseList(inMatcher.group(2)));
                continue;
            }

            Matcher eqMatcher = EQ_PATTERN.matcher(clause);
            if (eqMatcher.matches()) {
                mergeField(filters, normalizeField(eqMatcher.group(1)), normalizeValue(eqMatcher.group(2)));
                continue;
            }

            throw new IllegalArgumentException("Unsupported metadata filter clause: " + clause);
        }
        return filters;
    }

    public Map<String, Object> merge(Map<String, Object> baseFilter, String expression) {
        return merge(baseFilter, parse(expression));
    }

    public Map<String, Object> merge(Map<String, Object> baseFilter, Map<String, Object> extraFilter) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (baseFilter != null) {
            merged.putAll(baseFilter);
        }
        if (extraFilter == null || extraFilter.isEmpty()) {
            return merged;
        }

        extraFilter.forEach((field, value) -> mergeField(merged, field, value));
        return merged;
    }

    public boolean isContradictory(Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {
            return false;
        }
        return filter.values().stream().anyMatch(value -> value instanceof List<?> list && list.isEmpty());
    }

    private void mergeField(Map<String, Object> target, String field, Object incoming) {
        validateField(field);
        Object normalizedIncoming = normalizeMergedValue(incoming);
        Object existing = target.get(field);
        if (existing == null) {
            target.put(field, normalizedIncoming);
            return;
        }
        target.put(field, intersect(existing, normalizedIncoming));
    }

    private Object intersect(Object left, Object right) {
        if (left instanceof List<?> leftList && right instanceof List<?> rightList) {
            List<String> intersection = leftList.stream()
                    .map(String::valueOf)
                    .filter(item -> rightList.stream().map(String::valueOf).anyMatch(item::equals))
                    .distinct()
                    .toList();
            return intersection;
        }
        if (left instanceof List<?> leftList) {
            String rightValue = String.valueOf(right);
            return leftList.stream().map(String::valueOf).anyMatch(rightValue::equals)
                    ? rightValue
                    : List.of();
        }
        if (right instanceof List<?> rightList) {
            String leftValue = String.valueOf(left);
            return rightList.stream().map(String::valueOf).anyMatch(leftValue::equals)
                    ? leftValue
                    : List.of();
        }
        return String.valueOf(left).equals(String.valueOf(right)) ? String.valueOf(left) : List.of();
    }

    private String normalizeField(String field) {
        return field == null ? "" : field.trim().toLowerCase();
    }

    private void validateField(String field) {
        if (!ALLOWED_FIELDS.contains(field)) {
            throw new IllegalArgumentException("Unsupported metadata filter field: " + field);
        }
    }

    private List<String> parseList(String rawList) {
        String[] parts = rawList.split(",");
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String part : parts) {
            String normalized = normalizeValue(part);
            if (!normalized.isBlank()) {
                values.add(normalized);
            }
        }
        return new ArrayList<>(values);
    }

    private Object normalizeMergedValue(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).map(this::normalizeValue).filter(item -> !item.isBlank()).distinct().toList();
        }
        return normalizeValue(String.valueOf(value));
    }

    private String normalizeValue(String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim();
        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1).trim();
        }
        return value;
    }
}