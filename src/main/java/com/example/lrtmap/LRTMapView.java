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

    private static final double CANVAS_WIDTH = 900;
    private static final double CANVAS_HEIGHT = 900;
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
            manualEdge.getStrokeDashArray().addAll(6.0, 6.0);
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
                connector.getStrokeDashArray().addAll(3.0, 4.0);
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
        unique = unstickStationsFromForeignEdges(unique, layout);

        List<DrawnEdge> drawnEdges = collectBfsEdges(layout, unique);
        markCrossingEdges(drawnEdges);

        int bottomSlots = 0;
        for (DrawnEdge edge : drawnEdges) {
            if (edge.goBottom) {
                edge.bottomSlot = bottomSlots++;
            }
        }

        double maxX = layout.totalWidth;
        double maxY = layout.totalHeight;
        for (OccurrencePoint p : unique.values()) {
            maxX = Math.max(maxX, p.x + 200);
            maxY = Math.max(maxY, p.y + TOP_MARGIN);
        }
        double bottomBase = maxY + 36;
        if (bottomSlots > 0) {
            maxY = bottomBase + bottomSlots * 22 + 40;
        }
        root.setPrefSize(maxX, maxY);

        drawBfsTitle(root, maxX);

        for (DrawnEdge edge : drawnEdges) {
            if (!edge.goBottom) {
                continue;
            }
            double bottomY = bottomBase + edge.bottomSlot * 22;
            drawBottomDetour(root, edge.p1, edge.p2, edge.color, edge.width, edge.dashed, bottomY);
        }
        for (DrawnEdge edge : drawnEdges) {
            if (edge.goBottom) {
                continue;
            }
            addLineSeg(root, edge.p1.x, edge.p1.y, edge.p2.x, edge.p2.y,
                    edge.color, edge.width, edge.dashed);
        }

        for (LineRender lr : layout.lineRenders) {
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

    private List<DrawnEdge> collectBfsEdges(LineLayoutResult layout, Map<String, OccurrencePoint> unique) {
        List<DrawnEdge> edges = new ArrayList<>();
        for (LineRender lr : layout.lineRenders) {
            for (int i = 0; i < lr.stationNames.size() - 1; i++) {
                String a = lr.stationNames.get(i);
                String b = lr.stationNames.get(i + 1);
                OccurrencePoint p1 = unique.get(a);
                OccurrencePoint p2 = unique.get(b);
                if (p1 == null || p2 == null) {
                    continue;
                }
                edges.add(new DrawnEdge(a, b, p1, p2, lr.color, 4, false));
            }
        }
        for (String[] edge : graphData.getEdges()) {
            if (graphData.isLineInternalEdge(edge[0], edge[1])) {
                continue;
            }
            OccurrencePoint p1 = unique.get(edge[0]);
            OccurrencePoint p2 = unique.get(edge[1]);
            if (p1 == null || p2 == null) {
                continue;
            }
            edges.add(new DrawnEdge(edge[0], edge[1], p1, p2, MANUAL_EDGE_COLOR, 2, true));
        }
        return edges;
    }

    private void markCrossingEdges(List<DrawnEdge> edges) {
        for (int i = 0; i < edges.size(); i++) {
            DrawnEdge e1 = edges.get(i);
            boolean spansColumns = Math.abs(e1.p1.x - e1.p2.x) > BFS_LINE_COLUMN_SPACING * 0.65;
            if (spansColumns) {
                e1.goBottom = true;
            }
            for (int j = i + 1; j < edges.size(); j++) {
                DrawnEdge e2 = edges.get(j);
                if (!properIntersect(e1, e2)) {
                    continue;
                }
                e1.goBottom = true;
                e2.goBottom = true;
            }
        }
    }

    private static boolean properIntersect(DrawnEdge e1, DrawnEdge e2) {
        if (e1.a.equals(e2.a) || e1.a.equals(e2.b) || e1.b.equals(e2.a) || e1.b.equals(e2.b)) {
            return false;
        }
        return segmentsIntersect(
                e1.p1.x, e1.p1.y, e1.p2.x, e1.p2.y,
                e2.p1.x, e2.p1.y, e2.p2.x, e2.p2.y);
    }

    private static boolean segmentsIntersect(
            double ax, double ay, double bx, double by,
            double cx, double cy, double dx, double dy) {
        double d1 = cross(bx - ax, by - ay, cx - ax, cy - ay);
        double d2 = cross(bx - ax, by - ay, dx - ax, dy - ay);
        double d3 = cross(dx - cx, dy - cy, ax - cx, ay - cy);
        double d4 = cross(dx - cx, dy - cy, bx - cx, by - cy);
        return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0))
                && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0));
    }

    private static double cross(double x1, double y1, double x2, double y2) {
        return x1 * y2 - y1 * x2;
    }

    private void drawBottomDetour(Pane root, OccurrencePoint p1, OccurrencePoint p2,
                                  String color, double width, boolean dashed, double bottomY) {
        double outward1 = (p1.x <= p2.x) ? -18 : 18;
        double outward2 = (p2.x <= p1.x) ? -18 : 18;
        double ox1 = p1.x + outward1;
        double ox2 = p2.x + outward2;
        addLineSeg(root, p1.x, p1.y, ox1, p1.y, color, width, dashed);
        addLineSeg(root, ox1, p1.y, ox1, bottomY, color, width, dashed);
        addLineSeg(root, ox1, bottomY, ox2, bottomY, color, width, dashed);
        addLineSeg(root, ox2, bottomY, ox2, p2.y, color, width, dashed);
        addLineSeg(root, ox2, p2.y, p2.x, p2.y, color, width, dashed);
    }

    private void addLineSeg(Pane root, double x1, double y1, double x2, double y2,
                            String color, double width, boolean dashed) {
        Line segment = new Line(x1, y1, x2, y2);
        segment.setStroke(Color.web(color));
        segment.setStrokeWidth(width);
        if (dashed) {
            segment.getStrokeDashArray().addAll(6.0, 6.0);
        }
        root.getChildren().add(segment);
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

    private Map<String, OccurrencePoint> unstickStationsFromForeignEdges(
            Map<String, OccurrencePoint> unique, LineLayoutResult layout) {
        List<String> names = new ArrayList<>(unique.keySet());
        int n = names.size();
        if (n < 3) {
            return unique;
        }

        double[] xs = new double[n];
        double[] ys = new double[n];
        String[] colors = new String[n];
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < n; i++) {
            OccurrencePoint p = unique.get(names.get(i));
            xs[i] = p.x;
            ys[i] = p.y;
            colors[i] = p.color;
            index.put(names.get(i), i);
        }

        List<int[]> edges = new ArrayList<>();
        for (LineRender lr : layout.lineRenders) {
            for (int i = 0; i < lr.stationNames.size() - 1; i++) {
                Integer a = index.get(lr.stationNames.get(i));
                Integer b = index.get(lr.stationNames.get(i + 1));
                if (a != null && b != null && !a.equals(b)) {
                    edges.add(new int[]{a, b});
                }
            }
        }
        for (String[] edge : graphData.getEdges()) {
            if (graphData.isLineInternalEdge(edge[0], edge[1])) {
                continue;
            }
            Integer a = index.get(edge[0]);
            Integer b = index.get(edge[1]);
            if (a != null && b != null && !a.equals(b)) {
                edges.add(new int[]{a, b});
            }
        }

        final double clearance = 40;
        final double minStationDist = 72;
        for (int iter = 0; iter < 80; iter++) {
            boolean moved = false;

            for (int[] edge : edges) {
                int a = edge[0];
                int b = edge[1];
                double x1 = xs[a];
                double y1 = ys[a];
                double x2 = xs[b];
                double y2 = ys[b];
                double edx = x2 - x1;
                double edy = y2 - y1;
                double len = Math.hypot(edx, edy);
                if (len < 1) {
                    continue;
                }
                double nx = -edy / len;
                double ny = edx / len;

                for (int c = 0; c < n; c++) {
                    if (c == a || c == b) {
                        continue;
                    }
                    double t = ((xs[c] - x1) * edx + (ys[c] - y1) * edy) / (len * len);
                    if (t <= 0.1 || t >= 0.9) {
                        continue;
                    }
                    double qx = x1 + t * edx;
                    double qy = y1 + t * edy;
                    double dist = Math.hypot(xs[c] - qx, ys[c] - qy);
                    if (dist >= clearance) {
                        continue;
                    }
                    double side = (xs[c] - x1) * nx + (ys[c] - y1) * ny;
                    if (Math.abs(side) < 0.0001) {
                        side = 1;
                    }
                    double push = clearance - dist + 3;
                    xs[c] += nx * Math.signum(side) * push;
                    ys[c] += ny * Math.signum(side) * push;
                    moved = true;
                }
            }

            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    double dx = xs[j] - xs[i];
                    double dy = ys[j] - ys[i];
                    double dist = Math.hypot(dx, dy);
                    if (dist < 0.5) {
                        dx = 1;
                        dist = 1;
                    }
                    if (dist < minStationDist) {
                        double push = (minStationDist - dist) / 2;
                        xs[i] -= dx / dist * push;
                        ys[i] -= dy / dist * push;
                        xs[j] += dx / dist * push;
                        ys[j] += dy / dist * push;
                        moved = true;
                    }
                }
            }

            for (int i = 0; i < n; i++) {
                xs[i] = Math.max(LEFT_MARGIN - 20, xs[i]);
                ys[i] = Math.max(TOP_MARGIN, ys[i]);
            }
            if (!moved) {
                break;
            }
        }

        Map<String, OccurrencePoint> result = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            result.put(names.get(i), new OccurrencePoint(xs[i], ys[i], colors[i]));
        }
        return result;
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

    private static class DrawnEdge {
        final String a;
        final String b;
        final OccurrencePoint p1;
        final OccurrencePoint p2;
        final String color;
        final double width;
        final boolean dashed;
        boolean goBottom;
        int bottomSlot;

        DrawnEdge(String a, String b, OccurrencePoint p1, OccurrencePoint p2,
                  String color, double width, boolean dashed) {
            this.a = a;
            this.b = b;
            this.p1 = p1;
            this.p2 = p2;
            this.color = color;
            this.width = width;
            this.dashed = dashed;
        }
    }
}