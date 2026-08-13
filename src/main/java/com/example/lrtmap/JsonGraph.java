package com.example.lrtmap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonGraph extends AbstractGraph {

    @Override
    public void saveToFile(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        sb.append("  \"stations\": [\n");
        List<String> stations = new ArrayList<>(adjacency.keySet());
        for (int i = 0; i < stations.size(); i++) {
            sb.append("    \"").append(escape(stations.get(i))).append("\"");
            sb.append(i < stations.size() - 1 ? ",\n" : "\n");
        }
        sb.append("  ],\n");

        sb.append("  \"lines\": {\n");
        List<String> lineNames = new ArrayList<>(lines.keySet());
        for (int i = 0; i < lineNames.size(); i++) {
            String lineName = lineNames.get(i);
            List<String[]> edgesOnLine = lines.get(lineName);

            sb.append("    \"").append(escape(lineName)).append("\": {\n");
            sb.append("      \"edges\": [\n");
            for (int j = 0; j < edgesOnLine.size(); j++) {
                sb.append("        [\"").append(escape(edgesOnLine.get(j)[0]))
                        .append("\", \"").append(escape(edgesOnLine.get(j)[1])).append("\"]");
                sb.append(j < edgesOnLine.size() - 1 ? ",\n" : "\n");
            }
            sb.append("      ]\n");
            sb.append("    }");
            sb.append(i < lineNames.size() - 1 ? ",\n" : "\n");
        }
        sb.append("  },\n");

        sb.append("  \"extraEdges\": [\n");
        List<String[]> extraEdges = computeExtraEdges();
        for (int i = 0; i < extraEdges.size(); i++) {
            sb.append("    [\"").append(escape(extraEdges.get(i)[0]))
                    .append("\", \"").append(escape(extraEdges.get(i)[1])).append("\"]");
            sb.append(i < extraEdges.size() - 1 ? ",\n" : "\n");
        }
        sb.append("  ]\n");

        sb.append("}\n");

        Files.writeString(Path.of(path), sb.toString(), StandardCharsets.UTF_8);
    }

    @Override
    public void loadFromFile(String path) throws IOException {
        Path p = Path.of(path);
        if (!Files.exists(p)) {
            return;
        }

        String content = Files.readString(p, StandardCharsets.UTF_8);
        adjacency.clear();
        lines.clear();

        for (String station : extractStringArray(content, "stations")) {
            addStation(station);
        }

        for (Map.Entry<String, String> lineEntry : extractObjectOfObjects(content, "lines").entrySet()) {
            String lineName = lineEntry.getKey();
            String lineBody = lineEntry.getValue();

            addLine(lineName);
            for (String[] edge : extractPairArray(lineBody, "edges")) {
                restoreLineEdge(lineName, edge[0], edge[1]);
            }
        }

        for (String[] edge : extractPairArray(content, "extraEdges")) {
            addStation(edge[0]);
            addStation(edge[1]);
            addEdge(edge[0], edge[1]);
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String unescape(String s) {
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static List<String> extractStringArray(String json, String key) {
        List<String> result = new ArrayList<>();
        String body = extractArrayBody(json, key);
        if (body == null) return result;

        Matcher m = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(body);
        while (m.find()) {
            result.add(unescape(m.group(1)));
        }
        return result;
    }

    private static List<String[]> extractPairArray(String json, String key) {
        List<String[]> result = new ArrayList<>();
        String body = extractArrayBody(json, key);
        if (body == null) return result;

        Matcher m = Pattern.compile(
                "\\[\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*,\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*\\]"
        ).matcher(body);
        while (m.find()) {
            result.add(new String[]{unescape(m.group(1)), unescape(m.group(2))});
        }
        return result;
    }

    private static String extractArrayBody(String json, String key) {
        int keyIdx = json.indexOf("\"" + key + "\"");
        if (keyIdx == -1) return null;
        int arrStart = json.indexOf('[', keyIdx);
        if (arrStart == -1) return null;

        int depth = 0;
        for (int i = arrStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return json.substring(arrStart + 1, i);
                }
            }
        }
        return json.substring(arrStart + 1);
    }

    private static Map<String, String> extractObjectOfObjects(String json, String key) {
        Map<String, String> result = new LinkedHashMap<>();
        int keyIdx = json.indexOf("\"" + key + "\"");
        if (keyIdx == -1) return result;
        int objStart = json.indexOf('{', keyIdx);
        if (objStart == -1) return result;

        int outerEnd = findMatchingBrace(json, objStart);
        if (outerEnd == -1) return result;
        String body = json.substring(objStart + 1, outerEnd);

        Matcher keyMatcher = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"\\s*:\\s*\\{").matcher(body);
        while (keyMatcher.find()) {
            String name = unescape(keyMatcher.group(1));
            int innerStart = keyMatcher.end() - 1;
            int innerEnd = findMatchingBrace(body, innerStart);
            if (innerEnd == -1) continue;
            result.put(name, body.substring(innerStart + 1, innerEnd));
        }
        return result;
    }

    private static int findMatchingBrace(String text, int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }
}