package com.example.lrtmap;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddEdgeToLineTest {

    private JsonGraph lineWithABC() {
        JsonGraph graph = new JsonGraph();
        graph.addStation("A");
        graph.addStation("B");
        graph.addStation("C");
        graph.addStation("D");
        graph.addLine("Kelana Jaya");
        assertTrue(graph.addEdgeToLine("Kelana Jaya", "A", "B"));
        assertTrue(graph.addEdgeToLine("Kelana Jaya", "B", "C"));
        return graph;
    }

    @Test
    void firstEdgeCanConnectAnyTwoStations() {
        JsonGraph graph = new JsonGraph();
        graph.addStation("A");
        graph.addStation("B");
        graph.addLine("Kelana Jaya");

        assertTrue(graph.addEdgeToLine("Kelana Jaya", "A", "B"));
        assertEquals(List.of("A", "B"), graph.getLineStations("Kelana Jaya"));
    }

    @Test
    void rejectsChordBetweenStationsAlreadyOnTheLine() {
        JsonGraph graph = lineWithABC();

        assertFalse(graph.addEdgeToLine("Kelana Jaya", "A", "C"));
        assertEquals(List.of("A", "B", "C"), graph.getLineStations("Kelana Jaya"));
    }

    @Test
    void rejectsBranchFromAMiddleStation() {
        JsonGraph graph = lineWithABC();

        assertFalse(graph.addEdgeToLine("Kelana Jaya", "B", "D"));
        assertEquals(List.of("A", "B", "C"), graph.getLineStations("Kelana Jaya"));
    }

    @Test
    void allowsGrowthFromAnEndpoint() {
        JsonGraph graph = lineWithABC();

        assertTrue(graph.addEdgeToLine("Kelana Jaya", "C", "D"));
        assertEquals(List.of("A", "B", "C", "D"), graph.getLineStations("Kelana Jaya"));
    }
}
