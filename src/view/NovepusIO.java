package view;

import model.Comment;
import model.Message;
import model.Post;
import model.User;

import java.util.ArrayList;
import java.util.Scanner;

public final class NovepusIO {
    private final Scanner scanner;
    private String username;

    public NovepusIO() {
        scanner = new Scanner(System.in);
        systemPrintln(this + " Initialized");
    }

    public String readPassword() {
        String password;
        do {
            System.out.print(username + " % ");
            if (System.console() != null)
                password = String.valueOf(System.console().readPassword());
            else
                password = scanner.nextLine();
        } while (password.isBlank());
        return password;
    }

    public String readLine() {
        String line;
        do {
            System.out.print(username + " % ");
            line = scanner.nextLine();
        } while (line.isBlank());
        return line;
    }

    public String readOptional() {
        String string;
        System.out.println(username + " % ");
        string = scanner.nextLine();
        return string;
    }

    public String readText() {
        StringBuilder text = new StringBuilder();
        String line;
        systemPrintln("Reading multiple lines, double 'Enter' to finish");
        do {
            System.out.print(username + "[continue]:");
            line = scanner.nextLine();
            text.append(line).append('\n');
        } while (!line.isEmpty());
        systemPrintln("Finished!");
        return String.valueOf(text);
    }

    public void systemPrintln(Object o) {
        System.out.println("Novepus >>> " + o);
    }


    public void printUser(User user) {
        System.out.println(user);
    }

    public void printPost(Post post) {
        System.out.println(post);
    }

    public void printComment(Comment comment) {
        System.out.println(comment);
    }

    public void printMessage(Message message) {
        System.out.println(message);
    }

    public void printUserList(ArrayList<User> userList) {
        System.out.println("________NAME_____________________EMAIL______________________");
        for (User user : userList) {
            System.out.printf("|    %15s    ||%28s|%n",
                    user.userName(), user.userEmail().isEmpty() ? "NOT SET" : user.userEmail());
        }
        System.out.println("------------------------------------------------------------");
    }

    public void showMainMenu() {
        System.out.println("""
                _______________________________________________
                        [        Main Menu            ]
                        |    'r'    to     New User   |
                        |    'l'    to      Log In    |
                        |    'w'    to      Forum     |
                        |    'q'    to      Exit      |
                -----------------------------------------------""");
    }

    public void showUserMenu() {
        System.out.printf("""
                        _______________________________________________
                                         User Center  [%s]
                                |    'i'     to    Basic Info |
                                |    'e'     to      Edit     |
                                |    'p'     to      Post     |
                                |    'w'     to      Forum    |
                                |    's'     to     Follows   |
                                |    'm'     to     MailBox   |
                                |    'q'     to     Log Out   |
                        -----------------------------------------------%n""",
                username);
    }

    public void showFollowMenu(){
        System.out.printf("""
                        _______________________________________________
                                       Social Option [%s]
                                |    'v'    to    View Recent |
                                |    'r'    to    Recommends  |
                                |    's'    to      Search    |
                                |    'i'    to    User Center |
                                |    'p'    to       Post     |
                                |    'q'    to      Go Back   |
                        -----------------------------------------------%n""",
                username);
    }

    public void showForumMenu() {
        System.out.printf("""
                        _______________________________________________
                                        World Forum  [%s]
                                |    'v'    to    View Recent |
                                |    'r'    to    Recommends  |
                                |    's'    to      Search    |
                                |    'i'    to    User Center |
                                |    'p'    to       Post     |
                                |    'q'    to      Go Back   |
                        -----------------------------------------------%n""",
                username);
    }

    public void showPostDetailMenu() {
        System.out.println("""
                _______________________________________________
                        |    'l'    to     Like       |
                        |    'c'    to    Comment     |
                        |    'q'    to    Go Back     |
                -----------------------------------------------""");
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}