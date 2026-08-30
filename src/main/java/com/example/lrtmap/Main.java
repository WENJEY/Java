package com.example.lrtmap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class Main {

    private static final Graph graph = new JsonGraph();
    private static final String DATA_FILE = "lrt_data.json";
    private static final int STATION_PER_ROW = 5;
    private static final int EDGE_PER_ROW = 3;

    private static void saveGraph(){
        try{
            graph.saveToFile(DATA_FILE);
        }catch (IOException e){
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void loadGraph(){
        try{
            graph.loadFromFile(DATA_FILE);
            if(!graph.getStations().isEmpty()){
                System.out.println("Station loading...");
            }
        }catch (IOException e){
            System.out.println("Error: "+ e.getMessage());
        }
    }

    private static void clearScreen() {
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
    }

    private static void printInRows(List<String> items, int itemsPerRow) {
        int columnWidth = 0;
        for (int i = 0; i < items.size(); i++) {
            String cell = "[" + (i + 1) + "] " + items.get(i);
            columnWidth = Math.max(columnWidth, cell.length());
        }
        columnWidth += 2;

        for (int i = 0; i < items.size(); i++) {
            String cell = "[" + (i + 1) + "] " + items.get(i);
            System.out.printf("%-" + columnWidth + "s", cell);
            if ((i + 1) % itemsPerRow == 0 || i == items.size() - 1) {
                System.out.println();
            }
        }
    }


    private static String selectStation(Scanner s, String promptLabel) {
        List<String> stations = new ArrayList<>(graph.getStations());
        if (stations.isEmpty()) {
            System.out.println("No stations yet. Use 'Create Graph' -> 'Add a Station' first!");
            return null;
        }

        System.out.println("\nStation：");
        printInRows(stations, STATION_PER_ROW);
        System.out.println("[0] Return to Main Page");

        while (true) {
            System.out.print(promptLabel);
            String input = s.nextLine().trim();

            if (input.equals("0")) {
                return null;
            }

            try {
                int idx = Integer.parseInt(input);
                if (idx >= 1 && idx <= stations.size()) {
                    return stations.get(idx - 1);
                }
                System.out.println("Invalid selection! Please enter between 1 to " + stations.size() + " number or Enter 0 to return");
            } catch (NumberFormatException e) {
                String resolved = graph.resolveStationName(input);
                if (resolved != null) {
                    return resolved; // 用系统里实际存在的大小写版本，不是使用者打的原始输入
                }
                System.out.println("Invalid input! Please enter the correct station index");
            }
        }
    }

    private static String[] selectEdge(Scanner s) {
        List<String[]> edges = graph.getEdges();
        if (edges.isEmpty()) {
            System.out.println("No edge yet. Use 'Create Graph' -> 'Add an Edge' first!");
            return null;
        }

        List<String> edgeLabels = new ArrayList<>();
        for (String[] edge : edges) {
            edgeLabels.add(edge[0] + " - " + edge[1]);
        }

        System.out.println("\nEdge：");
        printInRows(edgeLabels, EDGE_PER_ROW);
        System.out.println("[0] Return to Main Page");

        while (true) {
            System.out.print("Enter the wanted delete edge：");
            String input = s.nextLine().trim();

            if (input.equals("0")) {
                return null;
            }

            try {
                int idx = Integer.parseInt(input);
                if (idx >= 1 && idx <= edges.size()) {
                    return edges.get(idx - 1);
                }
                System.out.println("Invalid selection! Please enter between 1 to " + edges.size() + " number or Enter 0 to return\n");
            } catch (NumberFormatException e) {
                System.out.println(" Please enter the correct edge index\n");
            }
        }
    }

    private static String selectLine(Scanner s) {
        List<String> lineNames = new ArrayList<>(graph.getLines());
        if (lineNames.isEmpty()) {
            System.out.println("No LRT line yet. Use 'Create Graph' -> 'Add a LRT Line' first!");
            return null;
        }

        System.out.println("\nLRT Line：");
        for (int i = 0; i < lineNames.size(); i++) {
            System.out.printf("[%d] %s%n", i + 1, lineNames.get(i));
        }
        System.out.println("[0] Return to Main Page");

        while (true) {
            System.out.print("Select the LRT line to remove : ");
            String input = s.nextLine().trim();

            if (input.equals("0")) {
                return null;
            }

            try {
                int idx = Integer.parseInt(input);
                if (idx >= 1 && idx <= lineNames.size()) {
                    return lineNames.get(idx - 1);
                }
                System.out.println("Invalid selection! Please enter between 1 to " + lineNames.size() + " number or Enter 0 to return");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter the correct line index");
            }
        }
    }

    private static String selectOrCreateLine(Scanner s) {
        List<String> lineNames = new ArrayList<>(graph.getLines());

        if (lineNames.isEmpty()) {
            System.out.println("No LRT line yet. Let's create one first.");
            System.out.print("Enter the new LRT line name : ");
            String newLine = s.nextLine().trim();
            if (newLine.isEmpty()) {
                System.out.println("Line name cannot be empty.");
                return null;
            }
            graph.addLine(newLine);
            System.out.println("LRT line '" + newLine + "' created.");
            return newLine;
        }

        System.out.println("\nLRT Line：");
        for (int i = 0; i < lineNames.size(); i++) {
            System.out.printf("[%d] %s%n", i + 1, lineNames.get(i));
        }
        System.out.println("[N] Create a New Line");
        System.out.println("[0] Return to Main Page");

        while (true) {
            System.out.print("Select a line (or N to create new) : ");
            String input = s.nextLine().trim();

            if (input.equals("0")) {
                return null;
            }
            if (input.equalsIgnoreCase("N")) {
                System.out.print("Enter the new LRT line name : ");
                String newLine = s.nextLine().trim();
                if (newLine.isEmpty()) {
                    System.out.println("Line name cannot be empty.");
                    continue;
                }
                if (graph.addLine(newLine)) {
                    System.out.println("LRT line '" + newLine + "' created.");
                } else {
                    System.out.println("Line '" + newLine + "' already exists, selecting it.");
                }
                return newLine;
            }

            try {
                int idx = Integer.parseInt(input);
                if (idx >= 1 && idx <= lineNames.size()) {
                    return lineNames.get(idx - 1);
                }
                System.out.println("Invalid selection! Please enter between 1 to " + lineNames.size());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input!");
            }
        }
    }

    public static void RemoveEdge(Scanner s){
        clearScreen();
        char choice;
        System.out.println("\n--------------------------------------------");
        System.out.printf("%26s", "Remove Edge\n");
        System.out.println("--------------------------------------------");
        System.out.println("Press '0' to back to previous page");

        String[] edge = selectEdge(s);

        if (edge == null){
            CreateGraph(s);
            return;
        }

        if (graph.removeEdge(edge[0], edge[1])) {
            System.out.printf("LRT line between %s and %s has been removed.", edge[0], edge[1]);
            saveGraph();
        } else {
            System.out.printf("Could not remove edge: make sure both '%s' and '%s' exist.", edge[0], edge[1]);
        }

        System.out.print("\nContinue? (Y/N) : ");
        choice = s.next().charAt(0);
        s.nextLine();
        if (choice == 'Y' || choice == 'y') {
            RemoveEdge(s);
        } else {
            CreateGraph(s);
        }
    }

    public static void AddEdge(Scanner s){
        clearScreen();
        char choice;
        System.out.println("\n--------------------------------------------");
        System.out.printf("%23s", "Add Edge\n");
        System.out.println("--------------------------------------------");
        System.out.println("Press '0' to back to previous page");

        String lineName = selectOrCreateLine(s);
        if (lineName == null) {
            CreateGraph(s);
            return;
        }

        String station1 = selectStation(s,"Please enter the first station number (Enter 0 to return to Main Page)\n");
        if (station1== null){
            CreateGraph(s);
            return;
        }

        String station2 = selectStation(s,"Please enter the second station number (Enter 0 to return to Main Page)\n");
        if (station2== null){
            CreateGraph(s);
            return;
        }

        if (station1.equals(station2)){
            System.out.println("Station should not connect itself!Please enter again.");
        }else if (graph.addEdgeToLine(lineName, station1, station2)) {
            System.out.printf("There is now a LRT connection between %s and %s on line '%s'%n", station1, station2, lineName);
            saveGraph();
        } else {
            System.out.printf("Could not add edge: make sure both '%s' and '%s' exist as stations, and this edge isn't already on '%s'.%n", station1, station2, lineName);
        }

        System.out.print("Continue? (Y/N) : ");
        choice = s.next().charAt(0);
        s.nextLine();
        if (choice == 'Y' || choice == 'y') {
            AddEdge(s);
        } else {
            CreateGraph(s);
        }
    }

    public static void RemoveStation(Scanner s){
        clearScreen();
        char choice;
        System.out.println("\n--------------------------------------------");
        System.out.printf("%28s", "Remove Station\n");
        System.out.println("--------------------------------------------");
        System.out.println("Press '0' to back to previous page");
        System.out.print("Enter the name of the station to be removed : ");

        String stationRemoved = selectStation(s, "Please enter the wanted delete station index (Enter 0 to return to Main Page)\n");

        if (stationRemoved== null){
            CreateGraph(s);
            return;
        }

        System.out.print("Confirm to remove '"+ stationRemoved + "'? (Y/N) : ");
        choice = s.next().charAt(0);
        s.nextLine();
        if (choice == 'Y' || choice == 'y') {
            if (graph.removeStation(stationRemoved)) {
                System.out.println("Station '" + stationRemoved + "' removed.");
                saveGraph();
            } else {
                System.out.println("Station '" + stationRemoved + "' does not exist.");
            }
            RemoveStation(s);
        } else {
            CreateGraph(s);
        }
    }

    public static void AddStation(Scanner s){
        clearScreen();
        String station;
        char choice;
        System.out.println("\n--------------------------------------------");
        System.out.printf("%27s", "Add Station\n");
        System.out.println("--------------------------------------------");
        System.out.println("Press '0' to back to previous page");
        System.out.print("Enter the name of the station : ");
        station = s.nextLine();

        if (station.equals("0")){
            CreateGraph(s);
            return;
        }

        if (graph.addStation(station)) {
            System.out.println("Station '" + station + "' added.");
            saveGraph();
        } else {
            System.out.println("Station '" + station + "' already exists.");
        }

        System.out.print("Continue? (Y/N) : ");
        choice = s.next().charAt(0);
        s.nextLine();
        if (choice == 'Y' || choice == 'y'){
            AddStation(s);
        }else if (choice == 'N' || choice == 'n'){
            CreateGraph(s);
        }
    }

    public static void AddLRTLine(Scanner s) {
        clearScreen();
        char choice;
        System.out.println("\n--------------------------------------------");
        System.out.printf("%27s", "Add LRT Line\n");
        System.out.println("--------------------------------------------");
        System.out.println("Press '0' to back to previous page");
        System.out.print("Enter the name of the LRT line : ");
        String lineName = s.nextLine();

        if (lineName.equals("0")) {
            CreateGraph(s);
            return;
        }

        if (graph.addLine(lineName)) {
            System.out.println("LRT line '" + lineName + "' added.");
            saveGraph();
        } else {
            System.out.println("LRT line '" + lineName + "' already exists.");
        }

        System.out.print("Continue? (Y/N) : ");
        choice = s.next().charAt(0);
        s.nextLine();
        if (choice == 'Y' || choice == 'y') {
            AddLRTLine(s);
        } else {
            CreateGraph(s);
        }
    }

    public static void RemoveLRTLine(Scanner s) {
        clearScreen();
        char choice;
        System.out.println("\n--------------------------------------------");
        System.out.printf("%27s", "Remove LRT Line\n");
        System.out.println("--------------------------------------------");
        System.out.println("Press '0' to back to previous page");

        String lineName = selectLine(s);
        if (lineName == null) {
            CreateGraph(s);
            return;
        }

        System.out.print("Confirm to remove LRT line '" + lineName + "'? (Y/N) : ");
        choice = s.next().charAt(0);
        s.nextLine();
        if (choice == 'Y' || choice == 'y') {
            if (graph.removeLine(lineName)) {
                System.out.println("LRT line '" + lineName + "' removed.");
                saveGraph();
            } else {
                System.out.println("LRT line '" + lineName + "' does not exist.");
            }
            RemoveLRTLine(s);
        } else {
            CreateGraph(s);
        }
    }

    public static void BfsTraversal(Scanner s) {
        clearScreen();
        char choice;
        System.out.println("\n--------------------------------------------");
        System.out.printf("%24s", "BFS Traversal\n");
        System.out.println("--------------------------------------------");
        System.out.println("Press '0' to back to previous page");

        String start = selectStation(s, "Please enter the starting station number (Enter 0 to return to Main Page)\n");
        if (start == null) {
            MainPage(s);
            return;
        }

        Map<String, Integer> layers = graph.bfsLayers(start);
        if (layers.isEmpty()) {
            System.out.println("\nStation '" + start + "' was not found.");
        } else {
            Map<Integer, List<String>> byLayer = new TreeMap<>();
            for (Map.Entry<String, Integer> entry : layers.entrySet()) {
                byLayer.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
            }

            System.out.println("\nBFS from '" + start + "' (layer 0) to the end of the graph:");
            for (Map.Entry<Integer, List<String>> entry : byLayer.entrySet()) {
                System.out.println("  Layer " + entry.getKey() + " : " + String.join(", ", entry.getValue()));
            }

            int unreachable = graph.getStations().size() - layers.size();
            if (unreachable > 0) {
                System.out.println("  Unreachable : " + unreachable + " station(s) not connected to '" + start + "'.");
            }

            System.out.println("\nOpening LRT map window with BFS layers...");
            LRTMapView.showBfsLayers(graph, layers, start);
        }

        System.out.print("\nTraverse again? (Y/N) : ");
        choice = s.next().charAt(0);
        s.nextLine();
        if (choice == 'Y' || choice == 'y') {
            BfsTraversal(s);
        } else {
            MainPage(s);
        }
    }

    public static void CreateGraph(Scanner s) {
        clearScreen();
        int choice = -1;
        while (true) {
            System.out.println("\n--------------------------------------------");
            System.out.printf("%27s", "Create Graph\n");
            System.out.println("--------------------------------------------");
            System.out.println("[1] Add a LRT Line");
            System.out.println("[2] Remove a LRT Line");
            System.out.println("[3] Add a Station");
            System.out.println("[4] Remove a Station");
            System.out.println("[5] Add a Edge");
            System.out.println("[6] Remove a Edge");
            System.out.println("[7] Return to the main menu");
            System.out.print("Enter your choice : ");

            if (s.hasNextInt()) {
                choice = s.nextInt();
                s.nextLine();

                if (choice >= 1 && choice <= 7) {
                    break;
                } else {
                    System.out.println("Invalid selection! Please enter a number from 1 to 7.");
                }
            } else {
                System.out.println("Invalid input! Please enter numbers only.");
                s.next();
            }
        }

        switch(choice){
            case 1:
                AddLRTLine(s);
                break;
            case 2:
                RemoveLRTLine(s);
                break;
            case 3:
                AddStation(s);
                break;
            case 4:
                RemoveStation(s);
                break;
            case 5:
                AddEdge(s);
                break;
            case 6:
                RemoveEdge(s);
                break;
            case 7:
                MainPage(s);
                break;
            default:
                System.out.println("Please only enter number between 1 - 7.");
        }
    }

    public static void MainPage (Scanner s){
        clearScreen();
        int answer = -1;
        while(true) {
            System.out.printf("\n%35s", "Welcome to the Rapid KL\n");
            System.out.println("--------------------------------------------");
            System.out.println("\n    \uD83D\uDE86 Welcome to LRT Navigation System\n");
            System.out.println("--------------------------------------------");
            System.out.println("[1] Create Graph");
            System.out.println("[2] BFS traversal from a station");
            System.out.println("[3] View the LRT Map");
            System.out.println("[4] Exit");
            System.out.print("Enter your choice : ");

            if (s.hasNextInt()) {
                answer = s.nextInt();
                s.nextLine();

                if (answer >= 1 && answer <= 4) {
                    break;
                } else {
                    System.out.println("Invalid selection! Please enter a number from 1 to 4.");
                }
            } else {
                System.out.println("Invalid input! Please enter numbers only.");
                s.next();
            }
        }

        switch (answer) {
            case 1:
                CreateGraph(s);
                break;

            case 2:
                if (graph.getStations().isEmpty()) {
                    System.out.println("No stations yet — add some via 'Create Graph' first.");
                    MainPage(s);
                } else {
                    BfsTraversal(s);
                }
                break;

            case 3:
                if (graph.getStations().isEmpty()) {
                    System.out.println("No stations yet — add some via 'Create Graph' first.");
                } else {
                    System.out.println("Opening LRT map window...");
                }
                LRTMapView.show(graph);
                MainPage(s);
                break;

            case 4:
                System.out.println("Thank you for using our app!!!");
                return;

            default:
                System.out.println("Please only enter number between 1 - 4.");
        }
    }

    public static void main(String[] args) {
        loadGraph();
        Scanner s = new Scanner(System.in);
        MainPage(s);
    }
}