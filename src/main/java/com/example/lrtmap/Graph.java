package com.example.lrtmap;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;


public interface Graph {

    String resolveStationName(String name);

    boolean addStation(String name);

    boolean removeStation(String name);

    boolean addEdge(String a, String b);

    boolean removeEdge(String a, String b);

    Set<String> getStations();

    List<String[]> getEdges();

    boolean addLine(String lineName);

    boolean removeLine(String lineName);

    Set<String> getLines();

    boolean addEdgeToLine(String lineName, String a, String b);

    List<String> getLineStations(String lineName);

    List<String> getLinesForStation(String station);

    boolean isLineInternalEdge(String a, String b);

    Map<String, Integer> bfsLayers(String start);

    void saveToFile(String path) throws IOException;

    void loadFromFile(String path) throws IOException;
}