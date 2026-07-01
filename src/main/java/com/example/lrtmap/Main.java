package com.example.lrtmap;
import java.util.Scanner;
import java.util.HashMap;
public class Main {

    static Scanner s = new Scanner(System.in);
    static HashMap<String,String> Staff = new HashMap<String , String>();

    public static void CreateGraph(){
        System.out.println("--------------------------------------------");
        System.out.printf("%20s","Create Graph");
        System.out.println("--------------------------------------------");


    }
    public static void MainPage (){
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
                    CreateGraph();
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
                    System.out.println("Please only enter number between 1 - 4.u");

        }
    }
    public static void main(String[] args) {
        MainPage();
    }
}
