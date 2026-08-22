package com.example.lrtmap;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Result window opened after a CLI search succeeds.
 * Input stays in the console; this page only displays DFS or BFS paths.
 */
public final class SearchView {

    private static final String BG = "#1e1e2e";
    private static final String SURFACE = "#313244";
    private static final String TEXT = "#cdd6f4";
    private static final String ACCENT = "#89b4fa";
    private static final String MUTED = "#a6adc8";
    private static final String SUCCESS = "#a6e3a1";
    private static final String DFS_COLOR = "#cba6f7";
    private static final String BFS_COLOR = "#89b4fa";

    private static Stage currentStage;

    private SearchView() {
    }

    public static final class DfsLineResult {
        final String lineName;
        final String previous;
        final String next;
        final List<String> dfsPath;

        public DfsLineResult(String lineName, String previous, String next, List<String> dfsPath) {
            this.lineName = lineName;
            this.previous = previous;
            this.next = next;
            this.dfsPath = new ArrayList<>(dfsPath);
        }
    }

    public static void showDfs(String station, List<DfsLineResult> lineResults) {
        List<DfsLineResult> copy = new ArrayList<>(lineResults);
        FxSupport.run(() -> open("DFS Result — " + station, buildDfsPane(station, copy)));
    }

    public static void showBfs(String start, String end, List<String> path) {
        List<String> copy = new ArrayList<>(path);
        FxSupport.run(() -> open("BFS Shortest Route", buildBfsPane(start, end, copy)));
    }

    private static void open(String title, Node content) {
        if (currentStage == null) {
            currentStage = new Stage();
            currentStage.setOnCloseRequest(e -> currentStage = null);
        }

        VBox root = new VBox(content);
        root.setStyle("-fx-background-color: " + BG + ";");
        VBox.setVgrow(content, Priority.ALWAYS);

        Scene scene = new Scene(root, 720, 480);
        currentStage.setTitle(title);
        currentStage.setScene(scene);
        currentStage.show();
        currentStage.toFront();
    }

    private static Node buildDfsPane(String station, List<DfsLineResult> lineResults) {
        VBox pane = new VBox(16);
        pane.setPadding(new Insets(20));
        pane.setStyle("-fx-background-color: " + BG + ";");

        pane.getChildren().add(header("DFS", "Depth-First Search", DFS_COLOR));
        pane.getChildren().add(titleLabel("Station: " + station, TEXT, 20, true));
        pane.getChildren().add(muted("Previous / next stations on each line, plus DFS to the last station."));

        for (DfsLineResult result : lineResults) {
            VBox card = card();
            card.getChildren().add(titleLabel("Line: " + result.lineName, DFS_COLOR, 15, true));
            card.getChildren().add(muted("Previous station : " + result.previous));
            card.getChildren().add(muted("Next station     : " + result.next));

            if (result.dfsPath.isEmpty()) {
                card.getChildren().add(muted("No DFS path on this line."));
            } else {
                card.getChildren().add(titleLabel(
                        "DFS to last station (" + result.dfsPath.size() + " stations)",
                        MUTED, 12, false));
                card.getChildren().add(pathStrip(result.dfsPath, DFS_COLOR));
            }
            pane.getChildren().add(card);
        }

        return wrapScroll(pane);
    }

    private static Node buildBfsPane(String start, String end, List<String> path) {
        VBox pane = new VBox(16);
        pane.setPadding(new Insets(20));
        pane.setStyle("-fx-background-color: " + BG + ";");

        int stops = Math.max(path.size() - 1, 0);
        pane.getChildren().add(header("BFS", "Breadth-First Search — shortest route", BFS_COLOR));
        pane.getChildren().add(titleLabel(start + "  →  " + end, TEXT, 20, true));
        pane.getChildren().add(titleLabel(
                stops + " stop" + (stops == 1 ? "" : "s") + "  ·  " + path.size() + " stations",
                SUCCESS, 14, false));
        pane.getChildren().add(new Separator());
        pane.getChildren().add(pathStrip(path, BFS_COLOR));

        return wrapScroll(pane);
    }

    private static HBox header(String badge, String subtitle, String color) {
        Label tag = new Label(badge);
        tag.setTextFill(Color.web(BG));
        tag.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        tag.setPadding(new Insets(4, 10, 4, 10));
        tag.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 6;");

        Label sub = titleLabel(subtitle, MUTED, 13, false);

        HBox row = new HBox(12, tag, sub);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static ScrollPane pathStrip(List<String> path, String color) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 8, 12, 8));

        for (int i = 0; i < path.size(); i++) {
            boolean first = i == 0;
            boolean last = i == path.size() - 1;
            row.getChildren().add(stationChip(i + 1, path.get(i), first, last, color));
            if (!last) {
                Label arrow = new Label("→");
                arrow.setTextFill(Color.web(color));
                arrow.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
                row.getChildren().add(arrow);
            }
        }

        ScrollPane scroll = new ScrollPane(row);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle(
                "-fx-background: " + SURFACE + ";" +
                        "-fx-background-color: " + SURFACE + ";" +
                        "-fx-background-radius: 10;"
        );
        scroll.setMinHeight(120);
        return scroll;
    }

    private static VBox stationChip(int index, String name, boolean first, boolean last, String color) {
        Circle circle = new Circle(14);
        if (first || last) {
            circle.setFill(Color.web(color));
            circle.setStroke(Color.WHITE);
            circle.setStrokeWidth(2);
        } else {
            circle.setFill(Color.web(BG));
            circle.setStroke(Color.web(color));
            circle.setStrokeWidth(2);
        }

        Label num = new Label(String.valueOf(index));
        num.setTextFill((first || last) ? Color.web(BG) : Color.web(color));
        num.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));

        StackPane marker = new StackPane(circle, num);

        Label nameLabel = new Label(name);
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(110);
        nameLabel.setAlignment(Pos.CENTER);

        String role = first ? "Start" : (last ? "End" : "");
        Label roleLabel = new Label(role);
        roleLabel.setTextFill(Color.web(MUTED));
        roleLabel.setFont(Font.font("Segoe UI", 10));

        VBox chip = new VBox(4, marker, nameLabel, roleLabel);
        chip.setAlignment(Pos.TOP_CENTER);
        chip.setMinWidth(90);
        return chip;
    }

    private static VBox card() {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: " + SURFACE + "; -fx-background-radius: 10;");
        return card;
    }

    private static ScrollPane wrapScroll(VBox pane) {
        ScrollPane scroll = new ScrollPane(pane);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + BG + "; -fx-background-color: " + BG + ";");
        return scroll;
    }

    private static Label titleLabel(String text, String color, double size, boolean bold) {
        Label label = new Label(text);
        label.setTextFill(Color.web(color));
        label.setFont(Font.font("Segoe UI", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        label.setWrapText(true);
        return label;
    }

    private static Label muted(String text) {
        return titleLabel(text, MUTED, 13, false);
    }
}
