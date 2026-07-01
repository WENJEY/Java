package com.example.lrtmap;
import java.util.Scanner;
import java.util.HashMap;
public class Main {

    public static void RemoveEdge(Scanner s){
        String station1,station2;
        char choice;
        System.out.println("\n--------------------------------------------");
        System.out.printf("%26s", "Remove Edge\n");
        System.out.println("--------------------------------------------");
        System.out.println("Press '0' to back to previous page");
        System.out.print("Enter the 1st station  : ");
        station1 = s.nextLine();

        if (station1.equals("0")){
            CreateGraph(s);
        }
        System.out.print("Enter the 2st station  : ");
        station2 = s.nextLine();

        if (station2.equals("0")){
            CreateGraph(s);
        }

        System.out.printf("LRT line between %s and %s has been removed.",station1,station2);

        System.out.print("\nContinue? (Y/N) : ");
        choice = s.next().charAt(0);
        s.nextLine();
        if (choice == 'Y' || choice == 'y'){
            RemoveEdge(s);
        }else if (choice == 'N' || choice == 'n'){
            CreateGraph(s);
        }
    }

    public static void AddEdge(Scanner s){
        String station1,station2;
        char choice;
        System.out.println("\n--------------------------------------------");
        System.out.printf("%23s", "Add Edge\n");
        System.out.println("--------------------------------------------");
        System.out.println("Press '0' to back to previous page");
        System.out.print("Enter the 1st station  : ");
        station1 = s.nextLine();

        if (station1.equals("0")){
            CreateGraph(s);
        }
        System.out.print("Enter the 2st station  : ");
        station2 = s.nextLine();

        if (station2.equals("0")){
            CreateGraph(s);
        }

        System.out.printf("There is now a LRT line between %s and %s",station1,station2);

        System.out.print("Continue? (Y/N) : ");
        choice = s.next().charAt(0);
        s.nextLine();
        if (choice == 'Y' || choice == 'y'){
            AddEdge(s);
        }else if (choice == 'N' || choice == 'n'){
            CreateGraph(s);
        }
    }

    public static void RemoveStation(Scanner s){
        String stationRemoved;
        char choice;
        System.out.println("\n--------------------------------------------");
        System.out.printf("%28s", "Remove Station\n");
        System.out.println("--------------------------------------------");
        System.out.println("Press '0' to back to previous page");
        System.out.print("Enter the name of the station to be removed : ");
        stationRemoved = s.nextLine();

        if (stationRemoved.equals("0")){
            CreateGraph(s);
        }

        System.out.print("Confirm to removed the selected station? (Y/N) : ");
        choice = s.next().charAt(0);
        s.nextLine();
        if (choice == 'Y' || choice == 'y'){
            RemoveStation(s);
        }else if (choice == 'N' || choice == 'n'){
            CreateGraph(s);
        }
    }

    public static void AddStation(Scanner s){
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

    public static void CreateGraph(Scanner s) {
        int choice = -1;
        while (true) {
            System.out.println("\n--------------------------------------------");
            System.out.printf("%27s", "Create Graph\n");
            System.out.println("--------------------------------------------");
            System.out.println("[1] Add a Station");
            System.out.println("[2] Remove a Station");
            System.out.println("[3] Add a Edge");
            System.out.println("[4] Remove a Edge");
            System.out.println("[5] Return to the main menu");
            System.out.print("Enter your choice : ");

            if (s.hasNextInt()) {
                choice = s.nextInt();
                s.nextLine();

                if (choice >= 1 && choice <= 5) {
                    break;
                } else {
                    System.out.println("Invalid selection! Please enter a number from 1 to 5.");
                }
            } else {
                System.out.println("Invalid input! Please enter numbers only.");
                s.next(); // Remove the invalid input
            }

    }
            switch(choice){
        case 1:
            AddStation(s);
            break;

        case 2:
            RemoveStation(s);
            break;

        case 3:
            AddEdge(s);
            break;

        case 4 :
            RemoveEdge(s);
            break;

        case 5:
            MainPage(s);
            break;

        default:
            System.out.println("Please only enter number between 1 - 5.");

    }
}

    public static void MainPage (Scanner s){
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
                s.next(); // Remove the invalid input
            }
        }

            switch (answer) {
                case 1:
                    CreateGraph(s);
                    break;

                case 2:
                    System.out.println("2");
                    break;

                case 3:
                    System.out.println("3");
                    break;

                case 4:
                    System.out.println("Thank you for using our app!!!");
                    return;

                default:
                    System.out.println("Please only enter number between 1 - 4.");

        }
    }

    public static void main(String[] args) {
        Scanner s = new Scanner(System.in);
        RemoveEdge(s);
    }
}
