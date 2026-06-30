package com.example.lrtmap;
import java.util.Scanner;
import java.util.HashMap;
public class Main {

    static HashMap<String,String> Staff = new HashMap<String , String>();
    static Scanner s = new Scanner(System.in);

    public static void Login(){
        Staff.put("S001","wenjey01");
        System.out.println("Welcome to Rapid KL");
        System.out.println("-------------------------------------------------");

        System.out.print("Please enter your staff id : ");
        String id = s.nextLine();
        System.out.print("Please enter your password : ");
        String password = s.nextLine();

        if (Staff.containsKey(id)){

            String passwordChecking = Staff.get(id);

            if(password.equals(passwordChecking)){
                System.out.println("Login Successfully\uD83C\uDF89");
            }else
                System.out.println("Invalid Staff ID or password");
        }else
            System.out.println("Invalid Staff ID or password");

    }
    public static void main(String[] args) {
        Login();
    }
}
