package com.example.lrtmap;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BfsLayersTest {

    @Test
    void bfsAssignsLayerZeroToStartAndIncrementsByOneHop() {
        JsonGraph graph = new JsonGraph();
        graph.addStation("A");
        graph.addStation("B");
        graph.addStation("C");
        graph.addStation("D");
        graph.addStation("Isolated");
        graph.addEdge("A", "B");
        graph.addEdge("B", "C");
        graph.addEdge("A", "D");

        Map<String, Integer> layers = graph.bfsLayers("A");

        assertEquals(0, layers.get("A"));
        assertEquals(1, layers.get("B"));
        assertEquals(1, layers.get("D"));
        assertEquals(2, layers.get("C"));
        assertEquals(4, layers.size());
        assertFalse(layers.containsKey("Isolated"));
    }

    @Test
    void bfsIsCaseInsensitiveAndEmptyWhenStationMissing() {
        JsonGraph graph = new JsonGraph();
        graph.addStation("KLCC");
        graph.addStation("Damai");
        graph.addEdge("KLCC", "Damai");

        Map<String, Integer> layers = graph.bfsLayers("klcc");
        assertEquals(0, layers.get("KLCC"));
        assertEquals(1, layers.get("Damai"));
        assertTrue(graph.bfsLayers("missing").isEmpty());
    }
}
