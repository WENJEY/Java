package com.example.lrtmap;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX window for Search features that used to be console-only:
 * station lookup (previous / next / DFS) and shortest route (BFS).
 */
public final class SearchView {

    private static final String BG = "#1e1e2e";
    private static final String SURFACE = "#313244";
    private static final String TEXT = "#cdd6f4";
    private static final String ACCENT = "#89b4fa";
    private static final String MUTED = "#a6adc8";
    private static final String SUCCESS = "#a6e3a1";
    private static final String WARN = "#f38ba8";

    private static Graph graphData;
    private static Stage currentStage;

    private SearchView() {
    }

    public static void show(Graph graph) {
        graphData = graph;
        FxSupport.run(SearchView::openOrFocus);
    }

    private static void openOrFocus() {
        if (currentStage != null) {
            refreshStationLists();
            currentStage.show();
            currentStage.toFront();
            return;
        }

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-background-color: " + BG + ";");

        Tab stationTab = new Tab("Search Station", buildStationSearchPane());
        Tab routeTab = new Tab("Shortest Route", buildRouteSearchPane());
        tabs.getTabs().addAll(stationTab, routeTab);

        VBox root = new VBox(12);
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: " + BG + ";");

        Label title = new Label("LRT Search");
        title.setTextFill(Color.web(ACCENT));
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));

        Label subtitle = new Label("Look up a station or find the shortest route between two stops.");
        subtitle.setTextFill(Color.web(MUTED));
        subtitle.setFont(Font.font("Segoe UI", 13));
        subtitle.setWrapText(true);

        root.getChildren().addAll(title, subtitle, tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        Scene scene = new Scene(root, 560, 520);
        currentStage = new Stage();
        currentStage.setTitle("LRT Search");
        currentStage.setScene(scene);
        currentStage.setOnCloseRequest(e -> currentStage = null);
        currentStage.show();
    }

    private static VBox buildStationSearchPane() {
        VBox pane = new VBox(12);
        pane.setPadding(new Insets(14, 4, 4, 4));
        pane.setStyle("-fx-background-color: " + BG + ";");

        Label hint = styledLabel("Type a station name or pick one from the list:", MUTED, 12);

        TextField searchField = new TextField();
        searchField.setPromptText("Station name...");
        styleField(searchField);

        ComboBox<String> stationBox = new ComboBox<>();
        stationBox.setPromptText("Select station");
        stationBox.setMaxWidth(Double.MAX_VALUE);
        styleCombo(stationBox);
        stationBox.setItems(FXCollections.observableArrayList(sortedStations()));

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String q = newVal == null ? "" : newVal.trim().toLowerCase();
            List<String> filtered = new ArrayList<>();
            for (String name : sortedStations()) {
                if (q.isEmpty() || name.toLowerCase().contains(q)) {
                    filtered.add(name);
                }
            }
            stationBox.setItems(FXCollections.observableArrayList(filtered));
            if (filtered.size() == 1) {
                stationBox.getSelectionModel().select(0);
            }
        });

        Button searchBtn = accentButton("Search");
        TextArea resultArea = resultArea();

        searchBtn.setOnAction(e -> {
            String selected = stationBox.getSelectionModel().getSelectedItem();
            if (selected == null || selected.isBlank()) {
                selected = searchField.getText();
            }
            resultArea.setText(formatStationInfo(selected));
        });

        searchField.setOnAction(e -> searchBtn.fire());

        HBox row = new HBox(10, searchField, searchBtn);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        row.setAlignment(Pos.CENTER_LEFT);

        pane.getChildren().addAll(hint, row, stationBox, new Separator(), resultArea);
        VBox.setVgrow(resultArea, Priority.ALWAYS);
        return pane;
    }

    private static VBox buildRouteSearchPane() {
        VBox pane = new VBox(12);
        pane.setPadding(new Insets(14, 4, 4, 4));
        pane.setStyle("-fx-background-color: " + BG + ";");

        Label startLabel = styledLabel("Starting station", MUTED, 12);
        ComboBox<String> startBox = new ComboBox<>();
        startBox.setPromptText("From...");
        startBox.setMaxWidth(Double.MAX_VALUE);
        styleCombo(startBox);

        Label endLabel = styledLabel("Destination station", MUTED, 12);
        ComboBox<String> endBox = new ComboBox<>();
        endBox.setPromptText("To...");
        endBox.setMaxWidth(Double.MAX_VALUE);
        styleCombo(endBox);

        List<String> stations = sortedStations();
        startBox.setItems(FXCollections.observableArrayList(stations));
        endBox.setItems(FXCollections.observableArrayList(stations));

        Button findBtn = accentButton("Find Shortest Route");
        ListView<String> pathList = new ListView<>();
        pathList.setStyle(
                "-fx-control-inner-background: " + SURFACE + ";" +
                        "-fx-background-color: " + SURFACE + ";" +
                        "-fx-text-fill: " + TEXT + ";" +
                        "-fx-background-radius: 8;"
        );
        Label summary = styledLabel("", MUTED, 13);

        findBtn.setOnAction(e -> {
            String start = startBox.getSelectionModel().getSelectedItem();
            String end = endBox.getSelectionModel().getSelectedItem();
            pathList.getItems().clear();

            if (start == null || end == null) {
                summary.setTextFill(Color.web(WARN));
                summary.setText("Please select both a start and a destination station.");
                return;
            }
            if (start.equals(end)) {
                summary.setTextFill(Color.web(WARN));
                summary.setText("Start and destination must be different stations.");
                return;
            }

            List<String> path = graphData.bfsForShortestPath(start, end);
            if (path.isEmpty()) {
                summary.setTextFill(Color.web(WARN));
                summary.setText("No route found between '" + start + "' and '" + end + "'.");
                return;
            }

            int stops = path.size() - 1;
            summary.setTextFill(Color.web(SUCCESS));
            summary.setText("Route found (" + stops + " stop" + (stops == 1 ? "" : "s") + "):");
            for (int i = 0; i < path.size(); i++) {
                pathList.getItems().add((i + 1) + ". " + path.get(i));
            }
        });

        pane.getChildren().addAll(
                startLabel, startBox,
                endLabel, endBox,
                findBtn,
                summary,
                pathList
        );
        VBox.setVgrow(pathList, Priority.ALWAYS);
        return pane;
    }

    private static String formatStationInfo(String input) {
        if (input == null || input.isBlank()) {
            return "Please enter or select a station name.";
        }

        String actualName = graphData.resolveStationName(input.trim());
        if (actualName == null) {
            return "Station '" + input.trim() + "' does not exist in the system.";
        }

        List<String> stationLines = graphData.getLinesForStation(actualName);
        StringBuilder sb = new StringBuilder();
        sb.append("Station: ").append(actualName).append("\n");

        if (stationLines.isEmpty()) {
            sb.append("\nThis station exists but is not connected to any line yet.");
            return sb.toString();
        }

        sb.append("Found on ").append(stationLines.size())
                .append(" line(s):\n");

        for (String lineName : stationLines) {
            List<String> stationsOnLine = graphData.getLineStations(lineName);
            int idx = stationsOnLine.indexOf(actualName);
            String previous = (idx > 0) ? stationsOnLine.get(idx - 1) : "None (start of the line)";
            String next = (idx < stationsOnLine.size() - 1)
                    ? stationsOnLine.get(idx + 1)
                    : "None (end of the line)";

            sb.append("\n──────── ").append(lineName).append(" ────────\n");
            sb.append("Previous station : ").append(previous).append("\n");
            sb.append("Next station     : ").append(next).append("\n");

            List<String> dfsPath = graphData.dfsToLastStation(lineName, actualName);
            if (!dfsPath.isEmpty()) {
                sb.append("DFS to last station: ").append(String.join(" → ", dfsPath)).append("\n");
            }
        }
        return sb.toString();
    }

    private static void refreshStationLists() {
        // Stage already open: recreate scene content next open is enough;
        // if still open, rebuild by closing null path — user can reopen from menu.
        if (currentStage != null) {
            currentStage.close();
            currentStage = null;
            openOrFocus();
        }
    }

    private static List<String> sortedStations() {
        List<String> stations = new ArrayList<>(graphData.getStations());
        stations.sort(String.CASE_INSENSITIVE_ORDER);
        return stations;
    }

    private static Label styledLabel(String text, String color, double size) {
        Label label = new Label(text);
        label.setTextFill(Color.web(color));
        label.setFont(Font.font("Segoe UI", size));
        label.setWrapText(true);
        return label;
    }

    private static TextArea resultArea() {
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setWrapText(true);
        area.setPromptText("Search results will appear here...");
        area.setStyle(
                "-fx-control-inner-background: " + SURFACE + ";" +
                        "-fx-text-fill: " + TEXT + ";" +
                        "-fx-prompt-text-fill: " + MUTED + ";" +
                        "-fx-background-color: " + SURFACE + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-family: 'Consolas', 'Courier New', monospace;" +
                        "-fx-font-size: 13;"
        );
        return area;
    }

    private static void styleField(TextField field) {
        field.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                        "-fx-text-fill: " + TEXT + ";" +
                        "-fx-prompt-text-fill: " + MUTED + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 8 12 8 12;"
        );
    }

    private static void styleCombo(ComboBox<String> combo) {
        combo.setStyle(
                "-fx-background-color: " + SURFACE + ";" +
                        "-fx-text-fill: " + TEXT + ";" +
                        "-fx-background-radius: 8;"
        );
    }

    private static Button accentButton(String text) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: " + ACCENT + ";" +
                        "-fx-text-fill: " + BG + ";" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 8 16 8 16;" +
                        "-fx-cursor: hand;"
        );
        return button;
    }
}
