package com.example.lrtmap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.*;

public class LRTMapView extends Application {

    private static Graph graphData;
    private static boolean fxStarted = false;

    private static Map<String, Integer> stationLayers = Collections.emptyMap();
    private static String bfsStartStation = null;

    private static Pane currentPane;
    private static Stage currentStage;

    private static final double CANVAS_WIDTH = 1200;
    private static final double CANVAS_HEIGHT = 800;
    private static final double TOP_MARGIN = 100;
    private static final double LEFT_MARGIN = 100;
    private static final double LINE_COLUMN_SPACING = 130;
    private static final double STATION_SPACING = 80;
    private static final double BFS_LINE_COLUMN_SPACING = 200;
    private static final double BFS_STATION_SPACING = 92;

    private static final String[] LINE_COLORS = {
            "#f38ba8", "#a6e3a1", "#89b4fa", "#f9e2af",
            "#cba6f7", "#94e2d5", "#fab387", "#eba0ac"
    };
    private static final String ORPHAN_COLOR = "#9399b2";
    private static final String INTERCHANGE_CONNECTOR_COLOR = "#ffffff";
    private static final String MANUAL_EDGE_COLOR = "#9399b2";
    private static final String CANVAS_BACKGROUND = "#1e1e2e";
    private static final String LAYER_BADGE_TEXT = "#1e1e2e";
    private static final String[] LAYER_COLORS = {
            "#ff3b3b", "#fab387", "#f9e2af", "#a6e3a1",
            "#89b4fa", "#cba6f7", "#94e2d5", "#f38ba8"
    };

    public static void show(Graph graph) {
        graphData = graph;
        stationLayers = Collections.emptyMap();
        bfsStartStation = null;
        display();
    }

    public static void showBfsLayers(Graph graph, Map<String, Integer> layers, String startStation) {
        graphData = graph;
        stationLayers = (layers == null) ? Collections.emptyMap() : new LinkedHashMap<>(layers);
        bfsStartStation = startStation;
        display();
    }

    private static void display() {
        if (!fxStarted) {
            fxStarted = true;
            Thread fxThread = new Thread(() -> Application.launch(LRTMapView.class));
            fxThread.setDaemon(true);
            fxThread.start();
        } else {
            Platform.runLater(LRTMapView::refresh);
        }
    }

    @Override
    public void start(Stage stage) {
        Platform.setImplicitExit(false);
        currentStage = stage;
        currentPane = buildMapPane();

        ScrollPane scrollPane = new ScrollPane(currentPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: " + CANVAS_BACKGROUND + "; -fx-background-color: " + CANVAS_BACKGROUND + ";");

        Scene scene = new Scene(scrollPane, CANVAS_WIDTH, CANVAS_HEIGHT);
        stage.setScene(scene);
        applyStageTitle(stage);
        stage.show();
    }

    private static void refresh() {
        if (currentPane == null) return;
        Pane fresh = new LRTMapView().buildMapPane();
        currentPane.getChildren().setAll(fresh.getChildren());
        currentPane.setPrefSize(fresh.getPrefWidth(), fresh.getPrefHeight());

        if (currentStage != null) {
            applyStageTitle(currentStage);
            currentStage.show();
            currentStage.toFront();
        }
    }

    private static void applyStageTitle(Stage stage) {
        if (bfsStartStation != null && !stationLayers.isEmpty()) {
            stage.setTitle("BFS layers from " + bfsStartStation);
        } else {
            stage.setTitle("LRT Navigation Map");
        }
    }

    private Pane buildMapPane() {
        Pane root = new Pane();
        root.setStyle("-fx-background-color: " + CANVAS_BACKGROUND + ";");

        List<String> stations = new ArrayList<>(graphData.getStations());

        if (stations.isEmpty()) {
            root.setPrefSize(CANVAS_WIDTH, CANVAS_HEIGHT);
            Label empty = new Label("No stations yet. Use 'Create Graph' -> 'Add a Station' first!");
            empty.setTextFill(Color.WHITE);
            empty.setFont(Font.font(16));
            StackPane wrapper = new StackPane(empty);
            wrapper.setPrefSize(CANVAS_WIDTH, CANVAS_HEIGHT);
            wrapper.setAlignment(Pos.CENTER);
            root.getChildren().add(wrapper);
            return root;
        }

        if (bfsStartStation != null && !stationLayers.isEmpty()) {
            return buildBfsUniquePane(root, stations);
        }
        return buildLineMapPane(root, stations);
    }

    private Pane buildLineMapPane(Pane root, List<String> stations) {
        LineLayoutResult layout = computeLinePositions(stations);
        root.setPrefSize(layout.totalWidth, layout.totalHeight);

        for (LineRender lr : layout.lineRenders) {
            for (int i = 0; i < lr.points.size() - 1; i++) {
                OccurrencePoint p1 = lr.points.get(i);
                OccurrencePoint p2 = lr.points.get(i + 1);
                Line segment = new Line(p1.x, p1.y, p2.x, p2.y);
                segment.setStroke(Color.web(lr.color));
                segment.setStrokeWidth(4);
                root.getChildren().add(segment);
            }
            if (!lr.points.isEmpty()) {
                Label lineLabel = new Label(lr.name);
                lineLabel.setTextFill(Color.web(lr.color));
                lineLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
                OccurrencePoint first = lr.points.get(0);
                lineLabel.setLayoutX(first.x - 30);
                lineLabel.setLayoutY(first.y - 40);
                root.getChildren().add(lineLabel);
            }
        }

        for (String[] edge : graphData.getEdges()) {
            if (graphData.isLineInternalEdge(edge[0], edge[1])) {
                continue;
            }
            OccurrencePoint p1 = firstOccurrence(layout, edge[0]);
            OccurrencePoint p2 = firstOccurrence(layout, edge[1]);
            if (p1 == null || p2 == null) continue;
            Line manualEdge = new Line(p1.x, p1.y, p2.x, p2.y);
            manualEdge.setStroke(Color.web(MANUAL_EDGE_COLOR));
            manualEdge.setStrokeWidth(2);
            root.getChildren().add(manualEdge);
        }

        for (Map.Entry<String, List<OccurrencePoint>> entry : layout.occurrences.entrySet()) {
            List<OccurrencePoint> points = entry.getValue();
            if (points.size() < 2) continue;
            for (int i = 0; i < points.size() - 1; i++) {
                OccurrencePoint p1 = points.get(i);
                OccurrencePoint p2 = points.get(i + 1);
                Line connector = new Line(p1.x, p1.y, p2.x, p2.y);
                connector.setStroke(Color.web(INTERCHANGE_CONNECTOR_COLOR));
                connector.setStrokeWidth(2);
                root.getChildren().add(connector);
            }
        }

        for (Map.Entry<String, List<OccurrencePoint>> entry : layout.occurrences.entrySet()) {
            String station = entry.getKey();
            List<OccurrencePoint> points = entry.getValue();
            boolean isInterchange = points.size() > 1;

            for (OccurrencePoint p : points) {
                drawStationCircle(root, p, station, isInterchange, null, false, false);
            }
        }

        return root;
    }

    private Pane buildBfsUniquePane(Pane root, List<String> stations) {
        LineLayoutResult layout = computeLinePositions(stations, BFS_LINE_COLUMN_SPACING, BFS_STATION_SPACING);
        Map<String, OccurrencePoint> unique = mergeDuplicateStations(layout);
        unique = spreadOverlappingStations(unique);

        double maxX = layout.totalWidth;
        double maxY = layout.totalHeight;
        for (OccurrencePoint p : unique.values()) {
            maxX = Math.max(maxX, p.x + 200);
            maxY = Math.max(maxY, p.y + TOP_MARGIN);
        }
        root.setPrefSize(maxX, maxY);

        drawBfsTitle(root, maxX);

        for (LineRender lr : layout.lineRenders) {
            for (int i = 0; i < lr.stationNames.size() - 1; i++) {
                OccurrencePoint p1 = unique.get(lr.stationNames.get(i));
                OccurrencePoint p2 = unique.get(lr.stationNames.get(i + 1));
                if (p1 == null || p2 == null) continue;
                Line segment = new Line(p1.x, p1.y, p2.x, p2.y);
                segment.setStroke(Color.web(lr.color));
                segment.setStrokeWidth(4);
                root.getChildren().add(segment);
            }
            if (!lr.points.isEmpty()) {
                Label lineLabel = new Label(lr.name);
                lineLabel.setTextFill(Color.web(lr.color));
                lineLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
                OccurrencePoint first = lr.points.get(0);
                lineLabel.setLayoutX(first.x - 30);
                lineLabel.setLayoutY(first.y - 54);
                root.getChildren().add(lineLabel);
            }
        }

        for (String[] edge : graphData.getEdges()) {
            if (graphData.isLineInternalEdge(edge[0], edge[1])) {
                continue;
            }
            OccurrencePoint p1 = unique.get(edge[0]);
            OccurrencePoint p2 = unique.get(edge[1]);
            if (p1 == null || p2 == null) continue;
            Line manualEdge = new Line(p1.x, p1.y, p2.x, p2.y);
            manualEdge.setStroke(Color.web(MANUAL_EDGE_COLOR));
            manualEdge.setStrokeWidth(2);
            root.getChildren().add(manualEdge);
        }

        for (Map.Entry<String, OccurrencePoint> entry : unique.entrySet()) {
            String station = entry.getKey();
            OccurrencePoint p = entry.getValue();
            Integer layer = stationLayers.get(station);
            boolean isStart = station.equals(bfsStartStation);
            boolean isInterchange = layout.occurrences.getOrDefault(station, List.of()).size() > 1;
            boolean labelLeft = shouldPlaceLabelLeft(station, p, unique);
            drawStationCircle(root, p, station, isInterchange, layer, isStart, labelLeft);
        }

        return root;
    }

    private Map<String, OccurrencePoint> mergeDuplicateStations(LineLayoutResult layout) {
        Map<String, OccurrencePoint> unique = new LinkedHashMap<>();
        for (Map.Entry<String, List<OccurrencePoint>> entry : layout.occurrences.entrySet()) {
            List<OccurrencePoint> points = entry.getValue();
            if (points.size() == 1) {
                unique.put(entry.getKey(), points.get(0));
                continue;
            }
            double x = 0;
            List<Double> ys = new ArrayList<>();
            for (OccurrencePoint p : points) {
                x += p.x;
                ys.add(p.y);
            }
            Collections.sort(ys);
            double y = ys.get(ys.size() / 2);
            unique.put(entry.getKey(), new OccurrencePoint(
                    x / points.size(),
                    y,
                    points.get(0).color));
        }
        return unique;
    }

    private Map<String, OccurrencePoint> spreadOverlappingStations(Map<String, OccurrencePoint> unique) {
        List<String> names = new ArrayList<>(unique.keySet());
        int n = names.size();
        if (n < 2) {
            return unique;
        }

        double[] xs = new double[n];
        double[] ys = new double[n];
        String[] colors = new String[n];
        for (int i = 0; i < n; i++) {
            OccurrencePoint p = unique.get(names.get(i));
            xs[i] = p.x;
            ys[i] = p.y;
            colors[i] = p.color;
        }

        final double minDist = 78;
        for (int iter = 0; iter < n; iter++) {
            Integer[] order = new Integer[n];
            for (int i = 0; i < n; i++) {
                order[i] = i;
            }
            Arrays.sort(order, Comparator.comparingDouble(i -> ys[i]));

            boolean moved = false;
            for (int a = 1; a < n; a++) {
                int i = order[a];
                for (int b = 0; b < a; b++) {
                    int j = order[b];
                    double dx = xs[i] - xs[j];
                    double dy = ys[i] - ys[j];
                    if (Math.hypot(dx, dy) < minDist) {
                        double needed = ys[j] + minDist;
                        if (needed > ys[i] + 0.5) {
                            ys[i] = needed;
                            moved = true;
                        }
                    }
                }
            }
            if (!moved) {
                break;
            }
        }

        Map<String, OccurrencePoint> spread = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            spread.put(names.get(i), new OccurrencePoint(xs[i], ys[i], colors[i]));
        }
        return spread;
    }

    private boolean shouldPlaceLabelLeft(String station, OccurrencePoint p,
                                         Map<String, OccurrencePoint> unique) {
        for (Map.Entry<String, OccurrencePoint> entry : unique.entrySet()) {
            if (entry.getKey().equals(station)) {
                continue;
            }
            OccurrencePoint other = entry.getValue();
            boolean closeRight = other.x > p.x && other.x - p.x < 150;
            boolean sameRow = Math.abs(other.y - p.y) < 40;
            if (closeRight && sameRow && p.x > LEFT_MARGIN + 40) {
                return true;
            }
        }
        return false;
    }

    private void drawStationCircle(Pane root, OccurrencePoint p, String station,
                                   boolean isInterchange, Integer layer, boolean isStart,
                                   boolean labelLeft) {
        if (layer != null) {
            drawLayerRing(root, p, layer, isStart);
        }

        Circle circle = new Circle(p.x, p.y, isInterchange ? 16 : 14);
        if (isInterchange) {
            circle.setFill(Color.web(CANVAS_BACKGROUND));
            circle.setStroke(Color.web(p.color));
            circle.setStrokeWidth(3);
        } else {
            circle.setFill(Color.web(p.color));
            circle.setStroke(Color.WHITE);
            circle.setStrokeWidth(1.5);
        }
        root.getChildren().add(circle);

        String stationText = (layer == null) ? station : station + "  L" + layer;
        Label label = new Label(stationText);
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font("System", FontWeight.BOLD, 10));
        label.setLayoutY(p.y - 7);
        label.setStyle(
                "-fx-background-color: " + CANVAS_BACKGROUND + ";" +
                        "-fx-background-radius: 3;" +
                        "-fx-padding: 1 4 1 4;"
        );
        if (labelLeft) {
            double estimatedWidth = stationText.length() * 6.4 + 10;
            label.setLayoutX(p.x - 22 - estimatedWidth);
        } else {
            label.setLayoutX(p.x + 22);
        }
        root.getChildren().add(label);

        if (layer != null) {
            drawLayerBadge(root, p, layer);
        }
    }

    private void drawBfsTitle(Pane root, double totalWidth) {
        if (bfsStartStation == null || stationLayers.isEmpty()) {
            return;
        }
        Label title = new Label("BFS from " + bfsStartStation + "  —  same line map, shared stations merged in the middle (L0 = start)");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        title.setLayoutX(LEFT_MARGIN - 20);
        title.setLayoutY(24);
        title.setPrefWidth(Math.max(totalWidth - 40, 400));
        root.getChildren().add(title);
    }

    private void drawLayerRing(Pane root, OccurrencePoint p, int layer, boolean isStart) {
        Circle ring = new Circle(p.x, p.y, isStart ? 24 : 20);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.web(layerColor(layer)));
        ring.setStrokeWidth(isStart ? 4 : 3);
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(layerColor(layer)));
        glow.setRadius(isStart ? 14 : 8);
        ring.setEffect(glow);
        root.getChildren().add(ring);
    }

    private void drawLayerBadge(Pane root, OccurrencePoint p, int layer) {
        Label badge = new Label("L" + layer);
        badge.setTextFill(Color.web(LAYER_BADGE_TEXT));
        badge.setFont(Font.font("System", FontWeight.BOLD, 11));
        badge.setStyle(
                "-fx-background-color: " + layerColor(layer) + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 1 5 1 5;"
        );
        badge.setLayoutX(p.x - 12);
        badge.setLayoutY(p.y - 34);
        root.getChildren().add(badge);
    }

    private static String layerColor(int layer) {
        return LAYER_COLORS[Math.floorMod(layer, LAYER_COLORS.length)];
    }

    private OccurrencePoint firstOccurrence(LineLayoutResult layout, String station) {
        List<OccurrencePoint> points = layout.occurrences.get(station);
        return (points == null || points.isEmpty()) ? null : points.get(0);
    }

    private LineLayoutResult computeLinePositions(List<String> allStations) {
        return computeLinePositions(allStations, LINE_COLUMN_SPACING, STATION_SPACING);
    }

    private LineLayoutResult computeLinePositions(List<String> allStations,
                                                  double columnSpacing, double rowSpacing) {
        Map<String, List<OccurrencePoint>> occurrences = new LinkedHashMap<>();
        List<String> lineNames = new ArrayList<>(graphData.getLines());
        List<LineRender> lineRenders = new ArrayList<>();

        double x = LEFT_MARGIN;
        double maxY = TOP_MARGIN;

        for (int li = 0; li < lineNames.size(); li++) {
            String lineName = lineNames.get(li);
            List<String> stationsOnLine = graphData.getLineStations(lineName);
            String color = LINE_COLORS[li % LINE_COLORS.length];

            List<OccurrencePoint> linePoints = new ArrayList<>();
            for (int i = 0; i < stationsOnLine.size(); i++) {
                double y = TOP_MARGIN + i * rowSpacing;
                OccurrencePoint point = new OccurrencePoint(x, y, color);
                linePoints.add(point);
                occurrences.computeIfAbsent(stationsOnLine.get(i), k -> new ArrayList<>()).add(point);
                maxY = Math.max(maxY, y);
            }
            lineRenders.add(new LineRender(lineName, color, linePoints, stationsOnLine));
            x += columnSpacing;
        }

        Set<String> placedStations = occurrences.keySet();
        List<String> orphanStations = new ArrayList<>();
        for (String station : allStations) {
            if (!placedStations.contains(station)) {
                orphanStations.add(station);
            }
        }
        for (int i = 0; i < orphanStations.size(); i++) {
            double y = TOP_MARGIN + i * rowSpacing;
            OccurrencePoint point = new OccurrencePoint(x, y, ORPHAN_COLOR);
            occurrences.computeIfAbsent(orphanStations.get(i), k -> new ArrayList<>()).add(point);
            maxY = Math.max(maxY, y);
        }
        if (!orphanStations.isEmpty()) {
            x += columnSpacing;
        }

        double totalWidth = Math.max(x + LEFT_MARGIN, CANVAS_WIDTH);
        double totalHeight = Math.max(maxY + TOP_MARGIN, CANVAS_HEIGHT);

        return new LineLayoutResult(lineRenders, occurrences, totalWidth, totalHeight);
    }

    private static class OccurrencePoint {
        final double x;
        final double y;
        final String color;

        OccurrencePoint(double x, double y, String color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }

    private static class LineRender {
        final String name;
        final String color;
        final List<OccurrencePoint> points;
        final List<String> stationNames;

        LineRender(String name, String color, List<OccurrencePoint> points, List<String> stationNames) {
            this.name = name;
            this.color = color;
            this.points = points;
            this.stationNames = stationNames;
        }
    }

    private static class LineLayoutResult {
        final List<LineRender> lineRenders;
        final Map<String, List<OccurrencePoint>> occurrences;
        final double totalWidth;
        final double totalHeight;

        LineLayoutResult(List<LineRender> lineRenders, Map<String, List<OccurrencePoint>> occurrences,
                         double totalWidth, double totalHeight) {
            this.lineRenders = lineRenders;
            this.occurrences = occurrences;
            this.totalWidth = totalWidth;
            this.totalHeight = totalHeight;
        }
    }
}