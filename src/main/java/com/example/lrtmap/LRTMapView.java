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

    private static List<String> highlightedStations = Collections.emptyList();

    private static Pane currentPane;
    private static Stage currentStage;

    private static final double CANVAS_WIDTH = 900;
    private static final double CANVAS_HEIGHT = 900;
    private static final double TOP_MARGIN = 100;
    private static final double LEFT_MARGIN = 100;
    private static final double LINE_COLUMN_SPACING = 130;
    private static final double STATION_SPACING = 80;

    private static final String[] LINE_COLORS = {
            "#f38ba8", "#a6e3a1", "#89b4fa", "#f9e2af",
            "#cba6f7", "#94e2d5", "#fab387", "#eba0ac"
    };
    private static final String ORPHAN_COLOR = "#9399b2";
    private static final String INTERCHANGE_CONNECTOR_COLOR = "#ffffff";
    private static final String MANUAL_EDGE_COLOR = "#9399b2";
    private static final String CANVAS_BACKGROUND = "#1e1e2e";
    private static final String HIGHLIGHT_COLOR = "#ff3b3b";

    public static void show(Graph graph) {
        graphData = graph;
        highlightedStations = Collections.emptyList();
        display();
    }
    public static void highlightRoute(Graph graph, List<String> path) {
        graphData = graph;
        highlightedStations = (path == null) ? Collections.emptyList() : new ArrayList<>(path);
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
        stage.setTitle("LRT Navigation Map");
        stage.show();
    }

    private static void refresh() {
        if (currentPane == null) return;
        Pane fresh = new LRTMapView().buildMapPane();
        currentPane.getChildren().setAll(fresh.getChildren());
        currentPane.setPrefSize(fresh.getPrefWidth(), fresh.getPrefHeight());

        if (currentStage != null) {
            currentStage.show();
            currentStage.toFront();
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

                Label label = new Label(station);
                label.setTextFill(Color.WHITE);
                label.setFont(Font.font("System", FontWeight.BOLD, 10));
                label.setLayoutX(p.x + 18);
                label.setLayoutY(p.y - 7);
                label.setStyle(
                        "-fx-background-color: " + CANVAS_BACKGROUND + ";" +
                                "-fx-background-radius: 3;" +
                                "-fx-padding: 1 4 1 4;"
                );
                root.getChildren().add(label);
            }
        }

        drawHighlightOverlay(root, layout);

        return root;
    }

    private void drawHighlightOverlay(Pane root, LineLayoutResult layout) {
        if (highlightedStations == null || highlightedStations.isEmpty()) return;

        List<String> path = highlightedStations;
        int n = path.size();

        if (n == 1) {
            List<OccurrencePoint> points = layout.occurrences.get(path.get(0));
            if (points != null) {
                for (OccurrencePoint p : points) {
                    drawHighlightRing(root, p);
                }
            }
            return;
        }

        String[] edgeLine = new String[n - 1];
        for (int i = 0; i < n - 1; i++) {
            String a = path.get(i);
            String b = path.get(i + 1);
            List<String> candidates = commonAdjacentLines(a, b);
            String prevLine = (i > 0) ? edgeLine[i - 1] : null;
            if (prevLine != null && candidates.contains(prevLine)) {
                edgeLine[i] = prevLine;
            } else if (!candidates.isEmpty()) {
                edgeLine[i] = candidates.get(0);
            } else {
                edgeLine[i] = null;
            }
        }

        List<OccurrencePoint[]> resolved = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String station = path.get(i);
            String enterLine = (i > 0) ? edgeLine[i - 1] : null;
            String exitLine = (i < n - 1) ? edgeLine[i] : null;

            OccurrencePoint enterPt = pointOnLine(layout, enterLine, station);
            OccurrencePoint exitPt = pointOnLine(layout, exitLine, station);

            if (enterPt != null && exitPt != null) {
                resolved.add(enterPt == exitPt ? new OccurrencePoint[]{enterPt}
                        : new OccurrencePoint[]{enterPt, exitPt});
            } else if (enterPt != null) {
                resolved.add(new OccurrencePoint[]{enterPt});
            } else if (exitPt != null) {
                resolved.add(new OccurrencePoint[]{exitPt});
            } else {
                OccurrencePoint fb = firstOccurrence(layout, station);
                resolved.add(fb == null ? new OccurrencePoint[0] : new OccurrencePoint[]{fb});
            }
        }

        for (int i = 0; i < n - 1; i++) {
            OccurrencePoint[] fromPts = resolved.get(i);
            OccurrencePoint[] toPts = resolved.get(i + 1);
            if (fromPts.length == 0 || toPts.length == 0) continue;
            drawHighlightLine(root, fromPts[fromPts.length - 1], toPts[0]);
        }

        for (OccurrencePoint[] pts : resolved) {
            if (pts.length == 2) {
                drawHighlightLine(root, pts[0], pts[1]);
            }
        }

        for (int i = 0; i < n; i++) {
            for (OccurrencePoint p : resolved.get(i)) {
                drawHighlightRing(root, p);
            }
            if (resolved.get(i).length > 0) {
                drawOrderBadge(root, resolved.get(i)[0], i + 1);
            }
        }
    }

    private List<String> commonAdjacentLines(String a, String b) {
        List<String> result = new ArrayList<>();
        for (String lineName : graphData.getLines()) {
            List<String> stationsOnLine = graphData.getLineStations(lineName);
            int ia = stationsOnLine.indexOf(a);
            int ib = stationsOnLine.indexOf(b);
            if (ia != -1 && ib != -1 && Math.abs(ia - ib) == 1) {
                result.add(lineName);
            }
        }
        return result;
    }

    private OccurrencePoint pointOnLine(LineLayoutResult layout, String lineName, String station) {
        if (lineName == null) return null;
        for (LineRender lr : layout.lineRenders) {
            if (lr.name.equals(lineName)) {
                List<String> stationsOnLine = graphData.getLineStations(lineName);
                int idx = stationsOnLine.indexOf(station);
                if (idx == -1) return null;
                return lr.points.get(idx);
            }
        }
        return null;
    }

    private void drawHighlightLine(Pane root, OccurrencePoint p1, OccurrencePoint p2) {
        Line highlightLine = new Line(p1.x, p1.y, p2.x, p2.y);
        highlightLine.setStroke(Color.web(HIGHLIGHT_COLOR));
        highlightLine.setStrokeWidth(6);
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(HIGHLIGHT_COLOR));
        glow.setRadius(14);
        highlightLine.setEffect(glow);
        root.getChildren().add(highlightLine);
    }

    private void drawHighlightRing(Pane root, OccurrencePoint p) {
        Circle ring = new Circle(p.x, p.y, 20);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.web(HIGHLIGHT_COLOR));
        ring.setStrokeWidth(3);
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(HIGHLIGHT_COLOR));
        glow.setRadius(10);
        ring.setEffect(glow);
        root.getChildren().add(ring);
    }

    private void drawOrderBadge(Pane root, OccurrencePoint p, int order) {
        Label label = new Label(String.valueOf(order));
        label.setTextFill(Color.web(CANVAS_BACKGROUND));
        label.setFont(Font.font("System", FontWeight.BOLD, 11));
        label.setStyle(
                "-fx-background-color: " + HIGHLIGHT_COLOR + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 1 5 1 5;"
        );
        label.setLayoutX(p.x - 8);
        label.setLayoutY(p.y - 32);
        root.getChildren().add(label);
    }

    private OccurrencePoint firstOccurrence(LineLayoutResult layout, String station) {
        List<OccurrencePoint> points = layout.occurrences.get(station);
        return (points == null || points.isEmpty()) ? null : points.get(0);
    }

    private LineLayoutResult computeLinePositions(List<String> allStations) {
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
                double y = TOP_MARGIN + i * STATION_SPACING;
                OccurrencePoint point = new OccurrencePoint(x, y, color);
                linePoints.add(point);
                occurrences.computeIfAbsent(stationsOnLine.get(i), k -> new ArrayList<>()).add(point);
                maxY = Math.max(maxY, y);
            }
            lineRenders.add(new LineRender(lineName, color, linePoints));
            x += LINE_COLUMN_SPACING;
        }

        Set<String> placedStations = occurrences.keySet();
        List<String> orphanStations = new ArrayList<>();
        for (String station : allStations) {
            if (!placedStations.contains(station)) {
                orphanStations.add(station);
            }
        }
        for (int i = 0; i < orphanStations.size(); i++) {
            double y = TOP_MARGIN + i * STATION_SPACING;
            OccurrencePoint point = new OccurrencePoint(x, y, ORPHAN_COLOR);
            occurrences.computeIfAbsent(orphanStations.get(i), k -> new ArrayList<>()).add(point);
            maxY = Math.max(maxY, y);
        }
        if (!orphanStations.isEmpty()) {
            x += LINE_COLUMN_SPACING;
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

        LineRender(String name, String color, List<OccurrencePoint> points) {
            this.name = name;
            this.color = color;
            this.points = points;
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