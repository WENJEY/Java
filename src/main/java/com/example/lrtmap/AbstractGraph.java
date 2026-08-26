package com.example.lrtmap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;


public abstract class AbstractGraph implements Graph {

    protected final Map<String, Set<String>> adjacency = new LinkedHashMap<>();
    protected final Map<String, List<String[]>> lines = new LinkedHashMap<>();

    @Override
    public String resolveStationName(String name) {
        if (name == null) return null;
        for (String existing : adjacency.keySet()) {
            if (existing.equalsIgnoreCase(name)) {
                return existing;
            }
        }
        return null;
    }

    @Override
    public boolean addStation(String name) {
        if (resolveStationName(name) != null) {
            return false;
        }
        adjacency.put(name, new LinkedHashSet<>());
        return true;
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public Set<String> getStations() {
        return adjacency.keySet();
    }

    @Override
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

    @Override
    public boolean addLine(String lineName) {
        if (lines.containsKey(lineName)) {
            return false;
        }
        lines.put(lineName, new ArrayList<>());
        return true;
    }

    @Override
    public Set<String> getLines() {
        return lines.keySet();
    }

    @Override
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

    @Override
    public List<String> getLineStations(String lineName) {
        return reconstructOrderFromEdges(lines.getOrDefault(lineName, Collections.emptyList()));
    }

    @Override
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

    @Override
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

    @Override
    public Map<String, Integer> bfsLayers(String start) {
        String actualStart = resolveStationName(start);
        if (actualStart == null) {
            return Collections.emptyMap();
        }

        Map<String, Integer> layers = new LinkedHashMap<>();
        Queue<String> queue = new LinkedList<>();

        layers.put(actualStart, 0);
        queue.add(actualStart);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentLayer = layers.get(current);
            for (String neighbor : adjacency.get(current)) {
                if (!layers.containsKey(neighbor)) {
                    layers.put(neighbor, currentLayer + 1);
                    queue.add(neighbor);
                }
            }
        }
        return layers;
    }

    @Override
    public abstract void saveToFile(String path) throws IOException;

    @Override
    public abstract void loadFromFile(String path) throws IOException;

    protected List<String[]> computeExtraEdges() {
        List<String[]> extraEdges = new ArrayList<>();
        for (String[] edge : getEdges()) {
            if (!isLineInternalEdge(edge[0], edge[1])) {
                extraEdges.add(edge);
            }
        }
        return extraEdges;
    }


    protected void restoreLineEdge(String lineName, String a, String b) {
        addStation(a);
        addStation(b);
        addEdge(a, b);
        lines.get(lineName).add(new String[]{resolveStationName(a), resolveStationName(b)});
    }

    protected static List<String> reconstructOrderFromEdges(List<String[]> edges) {
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
}