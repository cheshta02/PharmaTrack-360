package pharmatrack.json;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
public class ObjectMapper {

    public ObjectMapper() {
    }

    public ObjectMapper registerModule(Object module) {
        return this;
    }

    public ObjectMapper disable(SerializationFeature feature) {
        return this;
    }

    public ObjectMapper enable(SerializationFeature feature) {
        return this;
    }

    public void writeValue(File file, Object value) throws IOException {
        String json = toJson(value, 0);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        Files.writeString(file.toPath(), json);
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(File file, TypeReference<T> typeReference) throws IOException {
        if (!file.exists() || file.length() == 0) {
            return null;
        }
        String json = Files.readString(file.toPath()).trim();
        return readValue(json, typeReference);
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(String json, TypeReference<T> typeReference) throws IOException {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        JsonNode root = readTree(json);
        Type type = typeReference.getType();
        return (T) convertNode(root, type);
    }

    public JsonNode readTree(String json) throws IOException {
        if (json == null) return JsonNode.missingNode();
        json = json.trim();
        return parseJson(new JsonTokenizer(json));
    }

    private JsonNode parseJson(JsonTokenizer tok) throws IOException {
        String token = tok.nextToken();
        if (token == null) return JsonNode.missingNode();

        if ("{".equals(token)) {
            Map<String, JsonNode> map = new LinkedHashMap<>();
            while (true) {
                String key = tok.nextToken();
                if ("}".equals(key)) break;
                if (key == null) throw new IOException("Unexpected end of JSON object");
                String colon = tok.nextToken();
                if (!":".equals(colon)) throw new IOException("Expected ':' but found " + colon);
                JsonNode val = parseJson(tok);
                map.put(key, val);
                String commaOrEnd = tok.nextToken();
                if ("}".equals(commaOrEnd)) break;
                if (!",".equals(commaOrEnd)) throw new IOException("Expected ',' or '}' but found " + commaOrEnd);
            }
            return new JsonNode(map);
        } else if ("[".equals(token)) {
            List<JsonNode> list = new ArrayList<>();
            while (true) {
                tok.mark();
                String next = tok.nextToken();
                if ("]".equals(next)) break;
                tok.reset();
                JsonNode element = parseJson(tok);
                list.add(element);
                String commaOrEnd = tok.nextToken();
                if ("]".equals(commaOrEnd)) break;
                if (!",".equals(commaOrEnd)) throw new IOException("Expected ',' or ']' but found " + commaOrEnd);
            }
            return new JsonNode(list);
        } else {
            if ("null".equals(token)) return new JsonNode((Object) null);
            if ("true".equalsIgnoreCase(token)) return new JsonNode(Boolean.TRUE);
            if ("false".equalsIgnoreCase(token)) return new JsonNode(Boolean.FALSE);
            try {
                if (token.contains(".")) {
                    return new JsonNode(Double.parseDouble(token));
                } else {
                    return new JsonNode(Long.parseLong(token));
                }
            } catch (NumberFormatException e) {
                return new JsonNode(token);
            }
        }
    }

    private static class JsonTokenizer {
        private final String input;
        private int pos = 0;
        private int markedPos = 0;

        public JsonTokenizer(String input) {
            this.input = input;
        }

        public void mark() {
            markedPos = pos;
        }

        public void reset() {
            pos = markedPos;
        }

        public String nextToken() {
            skipWhitespace();
            if (pos >= input.length()) return null;
            char c = input.charAt(pos);

            if (c == '{' || c == '}' || c == '[' || c == ']' || c == ':' || c == ',') {
                pos++;
                return String.valueOf(c);
            }

            if (c == '"' || c == '\'') {
                char quote = c;
                pos++;
                StringBuilder sb = new StringBuilder();
                while (pos < input.length()) {
                    char ch = input.charAt(pos++);
                    if (ch == '\\' && pos < input.length()) {
                        char esc = input.charAt(pos++);
                        if (esc == 'n') sb.append('\n');
                        else if (esc == 'r') sb.append('\r');
                        else if (esc == 't') sb.append('\t');
                        else sb.append(esc);
                    } else if (ch == quote) {
                        break;
                    } else {
                        sb.append(ch);
                    }
                }
                return sb.toString();
            }

            // Word / number
            StringBuilder sb = new StringBuilder();
            while (pos < input.length()) {
                char ch = input.charAt(pos);
                if (Character.isWhitespace(ch) || ch == ',' || ch == '}' || ch == ']' || ch == ':') {
                    break;
                }
                sb.append(ch);
                pos++;
            }
            return sb.toString();
        }

        private void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
        }
    }

    private Object convertNode(JsonNode node, Type targetType) {
        if (node == null || (node.isEmpty() && !node.isArray())) {
            if (targetType instanceof Class<?> c && c.isPrimitive()) {
                if (c.equals(int.class)) return 0;
                if (c.equals(long.class)) return 0L;
                if (c.equals(double.class)) return 0.0;
                if (c.equals(boolean.class)) return false;
            }
            return null;
        }

        if (targetType instanceof Class<?> clazz && clazz.isArray()) {
            Class<?> componentType = clazz.getComponentType();
            if (node.isArray()) {
                int size = node.size();
                Object array = Array.newInstance(componentType, size);
                for (int i = 0; i < size; i++) {
                    Object val = convertNode(node.get(i), componentType);
                    Array.set(array, i, val);
                }
                return array;
            }
            return null;
        }

        if (targetType instanceof ParameterizedType pt) {
            Type rawType = pt.getRawType();
            if (rawType instanceof Class<?> rawClass && List.class.isAssignableFrom(rawClass)) {
                Type itemType = pt.getActualTypeArguments()[0];
                List<Object> result = new ArrayList<>();
                for (JsonNode child : node) {
                    result.add(convertNode(child, itemType));
                }
                return result;
            }
        }

        if (targetType instanceof Class<?> clazz) {
            if (clazz.equals(String.class)) return node.asText();
            if (clazz.equals(int.class) || clazz.equals(Integer.class)) return node.asInt();
            if (clazz.equals(long.class) || clazz.equals(Long.class)) return node.asLong();
            if (clazz.equals(double.class) || clazz.equals(Double.class)) return node.asDouble();
            if (clazz.equals(boolean.class) || clazz.equals(Boolean.class)) return Boolean.parseBoolean(node.asText());
            if (clazz.equals(LocalDate.class)) {
                String txt = node.asText();
                return txt.isEmpty() ? null : LocalDate.parse(txt);
            }
            if (clazz.equals(LocalDateTime.class)) {
                String txt = node.asText();
                return txt.isEmpty() ? null : LocalDateTime.parse(txt);
            }
            if (clazz.isEnum()) {
                for (Object constant : clazz.getEnumConstants()) {
                    if (constant.toString().equalsIgnoreCase(node.asText())) {
                        return constant;
                    }
                }
                return null;
            }
            for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
                if (ctor.isAnnotationPresent(JsonCreator.class) || clazz.getDeclaredConstructors().length == 1) {
                    ctor.setAccessible(true);
                    Parameter[] params = ctor.getParameters();
                    Object[] args = new Object[params.length];
                    for (int i = 0; i < params.length; i++) {
                        Parameter p = params[i];
                        String propName = p.isAnnotationPresent(JsonProperty.class) ?
                                p.getAnnotation(JsonProperty.class).value() : p.getName();
                        JsonNode child = node.path(propName);
                        args[i] = convertNode(child, p.getParameterizedType());
                    }
                    try {
                        return ctor.newInstance(args);
                    } catch (Exception e) {
                        // fallback to next constructor
                    }
                }
            }
        }

        return null;
    }

    public String toJson(Object obj, int indent) {
        if (obj == null) return "null";
        if (obj instanceof String s) return "\"" + escapeJson(s) + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj instanceof LocalDate || obj instanceof LocalDateTime || obj instanceof Enum<?>)
            return "\"" + obj + "\"";

        if (obj.getClass().isArray()) {
            int len = Array.getLength(obj);
            if (len == 0) return "[]";
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < len; i++) {
                sb.append(toJson(Array.get(obj, i), indent));
                if (i < len - 1) sb.append(", ");
            }
            sb.append("]");
            return sb.toString();
        }

        if (obj instanceof List<?> list) {
            if (list.isEmpty()) return "[]";
            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < list.size(); i++) {
                sb.append("  ".repeat(indent + 1)).append(toJson(list.get(i), indent + 1));
                if (i < list.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ".repeat(indent)).append("]");
            return sb.toString();
        }

        if (obj instanceof Map<?, ?> map) {
            if (map.isEmpty()) return "{}";
            StringBuilder sb = new StringBuilder("{\n");
            int count = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sb.append("  ".repeat(indent + 1))
                        .append("\"").append(escapeJson(String.valueOf(entry.getKey()))).append("\": ")
                        .append(toJson(entry.getValue(), indent + 1));
                if (++count < map.size()) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ".repeat(indent)).append("}");
            return sb.toString();
        }
        Class<?> clazz = obj.getClass();
        Map<String, Object> props = new LinkedHashMap<>();
        for (Method m : clazz.getMethods()) {
            if (m.getParameterCount() == 0 && !m.getName().equals("getClass")) {
                String name = m.getName();
                String propName = null;
                if (name.startsWith("get") && name.length() > 3) {
                    propName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                } else if (name.startsWith("is") && name.length() > 2) {
                    propName = Character.toLowerCase(name.charAt(2)) + name.substring(3);
                }
                if (propName != null) {
                    try {
                        props.put(propName, m.invoke(obj));
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return toJson(props, indent);
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}