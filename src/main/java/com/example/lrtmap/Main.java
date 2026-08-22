package com.example.lrtmap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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

    public static void SearchMenu(Scanner s) {
        clearScreen();
        int choice = -1;
        while (true) {
            System.out.println("\n--------------------------------------------");
            System.out.printf("%25s", "Search for a LRT Station\n");
            System.out.println("--------------------------------------------");
            System.out.println("[1] Search a Station (view previous/next station)");
            System.out.println("[2] Find Shortest Route (between two stations)");
            System.out.println("[3] Return to the main menu");
            System.out.print("Enter your choice : ");

            if (s.hasNextInt()) {
                choice = s.nextInt();
                s.nextLine();

                if (choice >= 1 && choice <= 3) {
                    break;
                } else {
                    System.out.println("Invalid selection! Please enter a number from 1 to 3.");
                }
            } else {
                System.out.println("Invalid input! Please enter numbers only.");
                s.next();
            }
        }

        switch (choice) {
            case 1:
                SearchStationInfo(s);
                break;
            case 2:
                FindShortestRoute(s);
                break;
            case 3:
                MainPage(s);
                break;
            default:
                System.out.println("Please only enter number between 1 - 3.");
        }
    }

    public static void SearchStationInfo(Scanner s) {
        clearScreen();
        char choice;
        System.out.println("\n--------------------------------------------");
        System.out.printf("%25s", "Search a Station\n");
        System.out.println("--------------------------------------------");
        System.out.println("Press '0' to back to previous page");
        System.out.print("Enter the station name to search : ");
        String stationName = s.nextLine().trim();

        if (stationName.equals("0")) {
            SearchMenu(s);
            return;
        }

        String actualName = graph.resolveStationName(stationName);

        if (actualName == null) {
            System.out.println("\nStation '" + stationName + "' does not exist in the system.");
        } else {
            List<String> stationLines = graph.getLinesForStation(actualName);
            if (stationLines.isEmpty()) {
                System.out.println("\nStation '" + actualName + "' exists but is not connected to any line yet.");
            } else {
                System.out.println("\nStation '" + actualName + "' found on the following line(s):");
                for (String lineName : stationLines) {
                    List<String> stationsOnLine = graph.getLineStations(lineName);
                    int idx = stationsOnLine.indexOf(actualName);
                    String previous = (idx > 0) ? stationsOnLine.get(idx - 1) : "None (start of the line)";
                    String next = (idx < stationsOnLine.size() - 1) ? stationsOnLine.get(idx + 1) : "None (end of the line)";

                    System.out.println("\nLine: " + lineName);
                    System.out.println("Previous station : " + previous);
                    System.out.println("Next station     : " + next);

                    List<String> dfsPath = graph.dfsToLastStation(lineName, actualName);
                    if (!dfsPath.isEmpty()) {
                        System.out.println("DFS to last station on this line : " + String.join(" -> ", dfsPath));
                    }
                }
            }
        }

        System.out.print("\nSearch again? (Y/N) : ");
        choice = s.next().charAt(0);
        s.nextLine();
        if (choice == 'Y' || choice == 'y') {
            SearchStationInfo(s);
        } else {
            SearchMenu(s);
        }
    }

    public static void FindShortestRoute(Scanner s) {
        clearScreen();
        char choice;
        System.out.println("\n--------------------------------------------");
        System.out.printf("%22s", "Find Shortest Route\n");
        System.out.println("--------------------------------------------");
        System.out.println("Press '0' to back to previous page");

        System.out.println("\nSelect the starting station:");
        String start = selectStation(s, "Please enter the starting station number (Enter 0 to return to Main Page)\n");
        if (start == null) {
            SearchMenu(s);
            return;
        }

        System.out.println("\nSelect the destination station:");
        String end = selectStation(s, "Please enter the destination station number (Enter 0 to return to Main Page)\n");
        if (end == null) {
            SearchMenu(s);
            return;
        }

        List<String> path = graph.bfsForShortestPath(start, end); // BFS shortest path

        if (path.isEmpty()) {
            System.out.println("\nNo route found between '" + start + "' and '" + end + "'.");
        } else {
            int stops = path.size() - 1;
            System.out.println("\nRoute found (" + stops + " stop" + (stops == 1 ? "" : "s") + "):");
            for (int i = 0; i < path.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + path.get(i));
            }
        }

        System.out.print("\nSearch again? (Y/N) : ");
        choice = s.next().charAt(0);
        s.nextLine();
        if (choice == 'Y' || choice == 'y') {
            FindShortestRoute(s);
        } else {
            SearchMenu(s);
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
            System.out.println("[2] Add a Station");
            System.out.println("[3] Remove a Station");
            System.out.println("[4] Add a Edge");
            System.out.println("[5] Remove a Edge");
            System.out.println("[6] Return to the main menu");
            System.out.print("Enter your choice : ");

            if (s.hasNextInt()) {
                choice = s.nextInt();
                s.nextLine();

                if (choice >= 1 && choice <= 6) {
                    break;
                } else {
                    System.out.println("Invalid selection! Please enter a number from 1 to 6.");
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
                AddStation(s);
                break;
            case 3:
                RemoveStation(s);
                break;
            case 4:
                AddEdge(s);
                break;
            case 5:
                RemoveEdge(s);
                break;
            case 6:
                MainPage(s);
                break;
            default:
                System.out.println("Please only enter number between 1 - 6.");
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
            System.out.println("[2] Search for a LRT station");
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
                } else {
                    System.out.println("Opening LRT search window...");
                    SearchView.show(graph);
                }
                MainPage(s);
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