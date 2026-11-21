package pharmatrack.json;

import java.util.*;

public class JsonNode implements Iterable<JsonNode> {
    private final Object value;
    private final Map<String, JsonNode> fields;
    private final List<JsonNode> elements;

    public JsonNode(Object value) {
        this.value = value;
        this.fields = null;
        this.elements = null;
    }

    public JsonNode(Map<String, JsonNode> fields) {
        this.value = null;
        this.fields = fields != null ? fields : new LinkedHashMap<>();
        this.elements = null;
    }

    public JsonNode(List<JsonNode> elements) {
        this.value = null;
        this.fields = null;
        this.elements = elements != null ? elements : new ArrayList<>();
    }

    public static JsonNode missingNode() {
        return new JsonNode((Object) null);
    }

    public JsonNode path(String fieldName) {
        if (fields != null && fields.containsKey(fieldName)) {
            return fields.get(fieldName);
        }
        return missingNode();
    }

    public JsonNode get(String fieldName) {
        if (fields != null) {
            return fields.get(fieldName);
        }
        return null;
    }

    public JsonNode get(int index) {
        if (elements != null && index >= 0 && index < elements.size()) {
            return elements.get(index);
        }
        return null;
    }

    public boolean isArray() {
        return elements != null;
    }

    public boolean isEmpty() {
        if (elements != null) return elements.isEmpty();
        if (fields != null) return fields.isEmpty();
        return value == null || value.toString().isEmpty();
    }

    public int size() {
        if (elements != null) return elements.size();
        if (fields != null) return fields.size();
        return value != null ? 1 : 0;
    }

    public String asText() {
        return value != null ? value.toString() : "";
    }

    public double asDouble() {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(asText());
        } catch (Exception e) {
            return 0.0;
        }
    }

    public int asInt() {
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(asText());
        } catch (Exception e) {
            return 0;
        }
    }

    public long asLong() {
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(asText());
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override
    public Iterator<JsonNode> iterator() {
        if (elements != null) {
            return elements.iterator();
        }
        if (fields != null) {
            return fields.values().iterator();
        }
        return Collections.emptyIterator();
    }

    @Override
    public String toString() {
        if (elements != null) {
            return elements.toString();
        }
        if (fields != null) {
            return fields.toString();
        }
        return String.valueOf(value);
    }
}