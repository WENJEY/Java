package com.example.lrtmap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Graph {

    private final Map<String, Set<String>> adjacency = new LinkedHashMap<>();
    private final Map<String, List<String[]>> lines = new LinkedHashMap<>();

    public String resolveStationName(String name) {
        if (name == null) return null;
        for (String existing : adjacency.keySet()) {
            if (existing.equalsIgnoreCase(name)) {
                return existing;
            }
        }
        return null;
    }

    public boolean addStation(String name) {
        if (resolveStationName(name) != null) {
            return false;
        }
        adjacency.put(name, new LinkedHashSet<>());
        return true;
    }

    public boolean removeStation(String name) {
        String actual = resolveStationName(name);
        if (actual == null) {
            return false;
        }
        adjacency.remove(actual);
        for (Set<String> neighbors : adjacency.values()) {
            neighbors.remove(actual);
        }
        for (List<String[]> edgesOnLine : lines.values()) {
            edgesOnLine.removeIf(edge -> edge[0].equals(actual) || edge[1].equals(actual));
        }
        return true;
    }

    public boolean addEdge(String a, String b) {
        String actualA = resolveStationName(a);
        String actualB = resolveStationName(b);
        if (actualA == null || actualB == null) {
            return false;
        }
        adjacency.get(actualA).add(actualB);
        adjacency.get(actualB).add(actualA);
        return true;
    }

    public boolean removeEdge(String a, String b) {
        String actualA = resolveStationName(a);
        String actualB = resolveStationName(b);
        if (actualA == null || actualB == null) {
            return false;
        }
        adjacency.get(actualA).remove(actualB);
        adjacency.get(actualB).remove(actualA);
        for (List<String[]> edgesOnLine : lines.values()) {
            edgesOnLine.removeIf(edge ->
                    (edge[0].equals(actualA) && edge[1].equals(actualB)) || (edge[0].equals(actualB) && edge[1].equals(actualA)));
        }
        return true;
    }

    public Set<String> getStations() {
        return adjacency.keySet();
    }


    public List<String[]> getEdges() {
        List<String[]> edges = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String station : adjacency.keySet()) {
            for (String neighbor : adjacency.get(station)) {
                String key = station.compareTo(neighbor) < 0
                        ? station + "|" + neighbor
                        : neighbor + "|" + station;
                if (seen.add(key)) {
                    edges.add(new String[]{station, neighbor});
                }
            }
        }
        return edges;
    }

    public boolean addLine(String lineName) {
        if (lines.containsKey(lineName)) {
            return false;
        }
        lines.put(lineName, new ArrayList<>());
        return true;
    }

    public Set<String> getLines() {
        return lines.keySet();
    }

    public boolean addEdgeToLine(String lineName, String a, String b) {
        List<String[]> edgesOnLine = lines.get(lineName);
        if (edgesOnLine == null) {
            return false;
        }
        String actualA = resolveStationName(a);
        String actualB = resolveStationName(b);
        if (actualA == null || actualB == null) {
            return false;
        }
        for (String[] edge : edgesOnLine) {
            boolean same = (edge[0].equals(actualA) && edge[1].equals(actualB)) || (edge[0].equals(actualB) && edge[1].equals(actualA));
            if (same) {
                return false;
            }
        }

        addEdge(actualA, actualB);
        edgesOnLine.add(new String[]{actualA, actualB});
        return true;
    }

    public List<String> getLineStations(String lineName) {
        return reconstructOrderFromEdges(lines.getOrDefault(lineName, Collections.emptyList()));
    }

    public List<String> getLinesForStation(String station) {
        String actual = resolveStationName(station);
        if (actual == null) return new ArrayList<>();

        List<String> result = new ArrayList<>();
        for (Map.Entry<String, List<String[]>> entry : lines.entrySet()) {
            for (String[] edge : entry.getValue()) {
                if (edge[0].equals(actual) || edge[1].equals(actual)) {
                    result.add(entry.getKey());
                    break;
                }
            }
        }
        return result;
    }

    public boolean isLineInternalEdge(String a, String b) {
        String actualA = resolveStationName(a);
        String actualB = resolveStationName(b);
        if (actualA == null || actualB == null) return false;

        for (List<String[]> edgesOnLine : lines.values()) {
            for (String[] edge : edgesOnLine) {
                if ((edge[0].equals(actualA) && edge[1].equals(actualB)) || (edge[0].equals(actualB) && edge[1].equals(actualA))) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<String> shortestPath(String start, String end) {
        String actualStart = resolveStationName(start);
        String actualEnd = resolveStationName(end);
        if (actualStart == null || actualEnd == null) {
            return Collections.emptyList();
        }
        if (actualStart.equals(actualEnd)) {
            return new ArrayList<>(List.of(actualStart));
        }

        Map<String, String> prev = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        visited.add(actualStart);
        queue.add(actualStart);

        boolean reached = false;
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(actualEnd)) {
                reached = true;
                break;
            }
            for (String neighbor : adjacency.get(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    prev.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        if (!reached) {
            return Collections.emptyList();
        }

        LinkedList<String> path = new LinkedList<>();
        String cur = actualEnd;
        while (cur != null) {
            path.addFirst(cur);
            cur = prev.get(cur);
        }
        return path;
    }


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
        List<String[]> extraEdges = new ArrayList<>();
        for (String[] edge : getEdges()) {
            if (!isLineInternalEdge(edge[0], edge[1])) {
                extraEdges.add(edge);
            }
        }
        for (int i = 0; i < extraEdges.size(); i++) {
            sb.append("    [\"").append(escape(extraEdges.get(i)[0]))
                    .append("\", \"").append(escape(extraEdges.get(i)[1])).append("\"]");
            sb.append(i < extraEdges.size() - 1 ? ",\n" : "\n");
        }
        sb.append("  ]\n");

        sb.append("}\n");

        Files.writeString(Path.of(path), sb.toString(), StandardCharsets.UTF_8);
    }

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
                addStation(edge[0]);
                addStation(edge[1]);
                addEdge(edge[0], edge[1]);
                lines.get(lineName).add(edge);
            }
        }

        for (String[] edge : extractPairArray(content, "extraEdges")) {
            addStation(edge[0]);
            addStation(edge[1]);
            addEdge(edge[0], edge[1]);
        }
    }

    private static List<String> reconstructOrderFromEdges(List<String[]> edges) {
        if (edges.isEmpty()) return new ArrayList<>();

        Map<String, List<String>> localAdj = new LinkedHashMap<>();
        for (String[] edge : edges) {
            localAdj.computeIfAbsent(edge[0], k -> new ArrayList<>()).add(edge[1]);
            localAdj.computeIfAbsent(edge[1], k -> new ArrayList<>()).add(edge[0]);
        }

        String start = edges.get(0)[0];
        for (Map.Entry<String, List<String>> entry : localAdj.entrySet()) {
            if (entry.getValue().size() == 1) {
                start = entry.getKey();
                break;
            }
        }

        List<String> order = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        String current = start;
        String previous = null;
        while (current != null) {
            order.add(current);
            visited.add(current);
            String next = null;
            for (String neighbor : localAdj.getOrDefault(current, Collections.emptyList())) {
                if (!neighbor.equals(previous) && !visited.contains(neighbor)) {
                    next = neighbor;
                    break;
                }
            }
            previous = current;
            current = next;
        }
        return order;
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