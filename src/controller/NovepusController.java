package controller;

import controller.data.OracleData;
import model.Comment;
import model.Message;
import model.Post;
import model.User;
import oracle.jdbc.driver.OracleConnection;
import oracle.jdbc.driver.OracleDriver;
import view.NovepusIO;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;


public final class NovepusController {
    private static final String GUEST_USER_NAME = "_guest_user_";
    private final NovepusIO io;
    private OracleConnection connection;
    private String currentUser;

    public NovepusController() {
        this.io = new NovepusIO();
        connectToOracle();
        setCurrentUser(GUEST_USER_NAME);
        io.novepusPrintln(this + " Initialized");
    }

    private void mainMenu() throws SQLException {
        if (!Objects.equals(currentUser, GUEST_USER_NAME)) {
            userMenu();
            return;
        }
        String cmd;
        do {
            io.showMainMenu();
            cmd = io.readLine().strip().toLowerCase();
            switch (cmd) {
                case "r" -> registerGuide();
                case "l" -> {
                    loginGuide();
                    if (!Objects.equals(currentUser, GUEST_USER_NAME))
                        userMenu();
                }
                case "w" -> worldForum();
                case "q" -> io.novepusPrintln("Quit session");
                default -> io.novepusPrintln("Unrecognized Command " + cmd);
            }
        } while (!Objects.equals(cmd, "q"));
    }

    private void userMenu() throws SQLException {
        String cmd;
        do {
            io.showUserMenu();
            cmd = io.readLine().strip().toLowerCase();
            switch (cmd) {
                case "i" -> displayUserDetails();
                case "e" -> editUserDetails();
                case "p" -> postMenu();
                case "w" -> worldForum();
                case "s" -> manageFollows();
                case "m" -> mailBox();
                case "q" -> {
                    io.novepusPrintln("Logging out...");
                    DBController.setUserStatus(currentUser, false);
                    setCurrentUser(GUEST_USER_NAME);
                }
                default -> io.novepusPrintln("Unrecognized Command " + cmd);
            }
        } while (!Objects.equals(cmd, "q"));
    }

    private void postMenu() throws SQLException {
        String cmd;
        do {
            io.showPostMenu();
            cmd = io.readLine().strip().toLowerCase();
            switch (cmd) {
                case "p" -> postGuide();
                case "v" -> displayMyPosts();
                case "w" -> worldForum();
                case "d" -> deletePost();
                case "q" -> io.novepusPrintln("Going Back");
                default -> io.novepusPrintln("Unrecognized Command " + cmd);
            }
        } while (!Objects.equals(cmd, "q"));
    }

    private void manageFollows() throws SQLException {
        User user = DBController.retrieveUserByName(currentUser);
        io.novepusPrintln(String.format("User '%s' follows %s users and has %d followers!",
                user.userName(), user.followingsIdList().size(), user.followersIdList().size()));
        String cmd;
        do {
            io.showFollowMenu();
            cmd = io.readLine().strip().toLowerCase();
            switch (cmd) {
                case "v" -> displayFollowDetails();
                case "f" -> addFollowing();
                case "d" -> deleteFollowing();
                case "p" -> sendMessage();
                case "q" -> io.novepusPrintln("Going Back");
                default -> io.novepusPrintln("Unrecognized Command " + cmd);
            }
        } while (!Objects.equals(cmd, "q"));
    }

    private void worldForum() throws SQLException {
        String cmd;
        do {
            io.showForumMenu();
            cmd = io.readLine().strip().toLowerCase();
            switch (cmd) {
                case "v" -> displayAllPosts();
                case "r" -> displayInterestPosts();
                case "s" -> selectPost();
                case "a" -> displayAllUsers();
                case "p" -> postGuide();
                case "q" -> io.novepusPrintln("Going Back");
                default -> io.novepusPrintln("Unrecognized Command " + cmd);
            }
        } while (!Objects.equals(cmd, "q"));
    }

    private void mailBox() throws SQLException {
        String cmd;
        do {
            ArrayList<Message> inbox = new ArrayList<>();
            ArrayList<Message> sent = new ArrayList<>();
            for (int id : DBController.getUserInbox(currentUser)) {
                Message message = DBController.retrieveMessageById(id);
                if (!message.deleted())
                    inbox.add(message);
            }
            for (int id : DBController.getUserSent(currentUser)) {
                Message message = DBController.retrieveMessageById(id);
                if (!message.deleted())
                    sent.add(message);
            }
            io.novepusPrintln("Displaying User Inbox");
            io.printMessageList(inbox);
            io.novepusPrintln("Display User Inbox Finished!");
            io.novepusPrintln("Displaying User Sent");
            io.printMessageList(sent);
            io.novepusPrintln("Display User Sent Finished!");
            io.showMailBoxMenu();
            cmd = io.readLine().strip().toLowerCase();
            switch (cmd) {
                case "p" -> sendMessage();
                case "d" -> deleteMessage();
                case "q" -> io.novepusPrintln("Going Back");
                default -> io.novepusPrintln("Unrecognized Command " + cmd);
            }
        } while (!cmd.equals("q"));
    }

    private void sendMessage() throws SQLException {
        String receiver;
        String content;
        do {
            io.novepusPrintln("Input the username of the receiver ('~' to quit)");
            receiver = io.readLine();
            if (receiver.equals("~"))
                return;
            if (!DBController.userExist(receiver))
                io.novepusPrintln(String.format("User '%s' does not exist!", receiver));
        } while (!DBController.userExist(receiver));
        io.novepusPrintln("You may input your Message content now");
        content = io.readText();
        DBController.createMessage(new Message(currentUser, receiver, content));
        io.novepusPrintln("Sent!");
    }

    private void deleteMessage() throws SQLException {
        String mids;
        int mid;
        do {
            io.novepusPrintln("Input the 'mid' to delete ('~' to quit)");
            mids = io.readLine();
            if (mids.equals("~"))
                return;
            try {
                mid = Integer.parseInt(mids);
            } catch (NumberFormatException numberFormatException) {
                io.novepusPrintln("Invalid pid value!");
                mid = 0;
                continue;
            }
            if (DBController.messageNotExist(mid) || DBController.retrieveMessageById(mid).deleted()) {
                io.novepusPrintln(String.format("Message (mid=%s) does not exist! Cannot delete!", mid));
                continue;
            }
            if (!DBController.getUserInbox(currentUser).contains(mid) &&
                    !DBController.getUserSent(currentUser).contains(mid))
                io.novepusPrintln(String.format("Message (mid=%s) is not yours! Cannot delete!", mid));
        } while (DBController.messageNotExist(mid) ||
                (!DBController.getUserInbox(currentUser).contains(mid) &&
                        !DBController.getUserSent(currentUser).contains(mid)));
        io.printMessage(DBController.retrieveMessageById(mid));
        String cmd;
        io.novepusPrintln("Are you sure to delete?");
        io.novepusPrintln("'w' to confirm, otherwise quit");
        cmd = io.readLine().strip().toLowerCase();
        if (!cmd.equals("w")) {
            io.novepusPrintln("Canceled");
            return;
        }
        DBController.setMessageStatus(mid, true);
        io.novepusPrintln(String.format("Successfully delete Message at %s", new Date()));
    }

    private void registerGuide() throws SQLException {
        String username;
        String password;
        String confirm;
        String email;
        do {
            io.novepusPrintln("Input your Username ('~' to quit)");
            username = io.readLine();
            if (username.equals("~"))
                return;
            if (DBController.userExist(username))
                io.novepusPrintln(username + " has been taken!");
            if (username.length() > 15)
                io.novepusPrintln("Username oversize!");
        } while (DBController.userExist(username) || username.length() > 15);
        do {
            io.novepusPrintln("Input your Password");
            password = io.readPassword();
            io.novepusPrintln("Confirm your Password (Repeat)");
            confirm = io.readPassword();
            if (!Objects.equals(password, confirm))
                io.novepusPrintln("Confirmation Failure!");
            if (password.length() > 15)
                io.novepusPrintln("Password oversize!");
        } while (!Objects.equals(password, confirm) || password.length() > 15);
        do {
            io.novepusPrintln("Your email (optional)");
            email = io.readOptional();
            if (email.length() > 28)
                io.novepusPrintln("Email oversize!");
        } while (email.length() > 28);
        DBController.createUser(new User(username, password, email));
        io.novepusPrintln(String.format("New User '%s' finished registration at %s",
                username, new Date()));
    }

    private void loginGuide() throws SQLException {
        String username;
        String password;
        do {
            do {
                io.novepusPrintln("Input your Username ('~' to quit)");
                username = io.readLine();
                if (username.equals("~"))
                    return;
                if (!DBController.userExist(username))
                    io.novepusPrintln(String.format("User '%s' does not exist!", username));
            } while (!DBController.userExist(username));
            io.novepusPrintln("Input Password for " + username);
            password = io.readPassword();
            if (!DBController.retrieveUserByName(username).userPassword().equals(password))
                io.novepusPrintln("Incorrect Password!");
        } while (!DBController.retrieveUserByName(username).userPassword().equals(password));
        setCurrentUser(username);
        DBController.setUserStatus(currentUser, true);
        io.novepusPrintln("Successfully Log In As " + username);
        io.novepusPrintln("Welcome!");
    }

    private void postGuide() throws SQLException {
        String title;
        String content;
        String confirm;
        if (Objects.equals(currentUser, GUEST_USER_NAME)) {
            io.novepusPrintln("You must Log In before posting");
            loginGuide();
            if (Objects.equals(currentUser, GUEST_USER_NAME))
                return;
        }
        do {
            io.novepusPrintln("Input the title ('~' to quit)");
            title = io.readLine();
            if (title.equals("~"))
                return;
            if (title.length() > 30)
                io.novepusPrintln("Title oversize!");
        } while (title.length() > 30);
        io.novepusPrintln("You may input the content now");
        content = io.readText();
        io.novepusPrintln("'w' to confirm, otherwise quit");
        confirm = io.readLine().strip().toLowerCase();
        if (!confirm.equals("w")) {
            System.out.println("Leaving");
            return;
        }
        String label;
        ArrayList<String> labelList = new ArrayList<>();
        do {
            io.novepusPrintln("You may add several label to your Post ('~' to finish)");
            label = io.readLine();
            if (!Objects.equals(label, "~")) {
                labelList.add(label);
                io.novepusPrintln(String.format("'%s' added!", label));
            }
        } while (!Objects.equals(label, "~"));
        DBController.createPost(new Post(title, currentUser, content, labelList));
        io.novepusPrintln(String.format("User '%s' creates a new Post '%s' at %s",
                currentUser, title, new Date()));
    }

    private void displayFollowDetails() throws SQLException {
        User user = DBController.retrieveUserByName(currentUser);
        ArrayList<User> followings = new ArrayList<>();
        ArrayList<User> followers = new ArrayList<>();
        for (int id : user.followingsIdList())
            followings.add(DBController.retrieveUserById(id));
        for (int id : user.followersIdList())
            followers.add(DBController.retrieveUserById(id));
        io.novepusPrintln(followings.size() + " followings in total!");
        io.printUserList(followings);
        io.novepusPrintln("Display followings finished!");
        io.novepusPrintln(followers.size() + " followers in total!");
        io.printUserList(followers);
        io.novepusPrintln("Display followers finished!");
        String userName;
        do {
            io.novepusPrintln("You may input the username to view details ('~' to quit)");
            userName = io.readLine();
            if (userName.equals("~"))
                return;
            if (!DBController.userExist(userName))
                io.novepusPrintln(String.format("User '%s' does not exist!", userName));
        } while (!DBController.userExist(userName));
        io.printUser(DBController.retrieveUserByName(userName));
    }

    private void addFollowing() throws SQLException {
        String userName;
        do {
            io.novepusPrintln("Input the username of the user you want to follow ('~' to quit)");
            userName = io.readLine();
            if (userName.equals("~"))
                return;
            if (!DBController.userExist(userName))
                io.novepusPrintln(String.format("User '%s' does not exist!", userName));
            if (DBController.retrieveUserByName(currentUser).followingsIdList().
                    contains(DBController.retrieveUserByName(userName).userId()))
                io.novepusPrintln(String.format("You have already followed '%s'!", userName));
        } while (!DBController.userExist(userName) ||
                DBController.retrieveUserByName(currentUser).followingsIdList().
                        contains(DBController.retrieveUserByName(userName).userId()));
        io.printUser(DBController.retrieveUserByName(userName));
        String cmd;
        io.novepusPrintln(String.format("Are you sure to follow User '%s'?", userName));
        io.novepusPrintln("'w' to confirm, otherwise quit");
        cmd = io.readLine().strip().toLowerCase();
        if (!cmd.equals("w")) {
            io.novepusPrintln("Canceled");
            return;
        }
        DBController.userFollow(currentUser, userName);
        io.novepusPrintln("Followed");
    }

    private void deleteFollowing() throws SQLException {
        String userName;
        do {
            io.novepusPrintln("Input the username of the user you want to unfollow ('~' to quit)");
            userName = io.readLine();
            if (userName.equals("~"))
                return;
            if (!DBController.userExist(userName))
                io.novepusPrintln(String.format("User '%s' does not exist!", userName));
            if (!DBController.retrieveUserByName(currentUser).followingsIdList().
                    contains(DBController.retrieveUserByName(userName).userId()))
                io.novepusPrintln(String.format("You have not followed '%s' yet!", userName));
        } while (!DBController.userExist(userName) ||
                !DBController.retrieveUserByName(currentUser).followingsIdList().
                        contains(DBController.retrieveUserByName(userName).userId()));
        String cmd;
        io.novepusPrintln(String.format("Are you sure to unfollow User '%s'?", userName));
        io.novepusPrintln("'w' to confirm, otherwise quit");
        cmd = io.readLine().strip().toLowerCase();
        if (!cmd.equals("w")) {
            io.novepusPrintln("Canceled");
            return;
        }
        DBController.userUnfollow(currentUser, userName);
        io.novepusPrintln("Unfollowed");
    }

    private void displayAllUsers() throws SQLException {
        ArrayList<User> allUsers = new ArrayList<>();
        for (int id : DBController.getAllUserId()) {
            User user = DBController.retrieveUserById(id);
            allUsers.add(user);
        }
        io.novepusPrintln(String.format("Displaying all Users, %d in total!", allUsers.size()));
        io.printUserList(allUsers);
        io.novepusPrintln("Display all Users finished!");
    }

    private void displayUserDetails() throws SQLException {
        User user = DBController.retrieveUserByName(currentUser);
        io.printUser(user);
    }

    private void editUserDetails() throws SQLException {
        displayUserDetails();
        String cmd;
        do {
            io.showUserDetailMenu();
            cmd = io.readLine().strip().toLowerCase();
            switch (cmd) {
                case "p" -> resetUserPassword();
                case "e" -> resetUserEmail();
                case "i" -> addUserInterest();
                case "q" -> io.novepusPrintln("Going Back");
                default -> io.novepusPrintln("Unrecognized Command " + cmd);
            }
        } while (!Objects.equals(cmd, "q"));
    }

    private void resetUserPassword() throws SQLException {
        String oldPassword;
        String newPassword;
        String confirm;
        io.novepusPrintln("You have to input your old Password first");
        oldPassword = io.readPassword();
        if (!Objects.equals(oldPassword, DBController.retrieveUserByName(currentUser).userPassword())) {
            io.novepusPrintln("Incorrect Password! Going Back");
            return;
        }
        do {
            io.novepusPrintln("Input you new Password now");
            newPassword = io.readPassword();
            io.novepusPrintln("Confirm your new Password (Repeat)");
            confirm = io.readPassword();
            if (newPassword.length() > 15)
                io.novepusPrintln("New Password oversize!");
            if (!newPassword.equals(confirm))
                io.novepusPrintln("Confirmation Failure!");
        } while (!newPassword.equals(confirm) | newPassword.length() > 15);
        DBController.setUserPassword(currentUser, newPassword);
        io.novepusPrintln("Password Reset!");
        DBController.createMessage(new Message("Admin", currentUser, "Reset Password."));

    }

    private void resetUserEmail() throws SQLException {
        String newEmail;
        do {
            io.novepusPrintln("Your new email");
            newEmail = io.readOptional();
            if (newEmail.length() > 28)
                io.novepusPrintln("Email oversize!");
        } while (newEmail.length() > 28);
        DBController.setUserEmail(currentUser, newEmail);
        io.novepusPrintln("Email Reset!");
        DBController.createMessage(new Message("Admin", currentUser, "Reset Email."));

    }

    private void addUserInterest() throws SQLException {
        String interest;
        do {
            io.novepusPrintln("Input the interest word ('~' to quit)");
            interest = io.readLine();
            if (!Objects.equals(interest, "~")) {
                DBController.addUserInterest(currentUser, interest);
                io.novepusPrintln(String.format("'%s' added!", interest));
            }
        } while (!Objects.equals(interest, "~"));
        io.printUser(DBController.retrieveUserByName(currentUser));
    }

    private void deletePost() throws SQLException {
        displayMyPosts();
        String pids;
        int pid;
        do {
            io.novepusPrintln("Input the 'pid' to delete ('~' to quit)");
            pids = io.readLine();
            if (pids.equals("~"))
                return;
            try {
                pid = Integer.parseInt(pids);
            } catch (NumberFormatException numberFormatException) {
                io.novepusPrintln("Invalid pid value!");
                pid = 0;
                continue;
            }
            if (DBController.postNotExist(pid) || DBController.retrievePostById(pid).deleted()) {
                io.novepusPrintln(String.format("Post (pid=%s) does not exist! Cannot delete!", pid));
                continue;
            }
            if (!Objects.equals(DBController.retrievePostById(pid).postAuthor(), currentUser))
                io.novepusPrintln(String.format("Post (pid=%s) is not yours! Cannot delete!", pid));
        } while (DBController.postNotExist(pid) || DBController.retrievePostById(pid).deleted() ||
                !Objects.equals(DBController.retrievePostById(pid).postAuthor(), currentUser));
        DBController.setPostStatus(pid, true);
        io.novepusPrintln(String.format("Successfully delete Post '%s' at %s",
                DBController.retrievePostById(pid).postTitle(), new Date()));
    }

    private void displayMyPosts() throws SQLException {
        User user = DBController.retrieveUserByName(currentUser);
        ArrayList<Post> userPosts = new ArrayList<>();
        for (int id : user.postIdList()) {
            Post post = DBController.retrievePostById(id);
            if (!post.deleted())
                userPosts.add(post);
        }
        io.novepusPrintln(userPosts.size() + " Posts in total!");
        io.printPostList(userPosts);
        io.novepusPrintln("Display posts finished!");
    }

    private void displayAllPosts() throws SQLException {
        ArrayList<Post> allPosts = new ArrayList<>();
        for (int id : DBController.getAllPostId()) {
            Post post = DBController.retrievePostById(id);
            if (!post.deleted())
                allPosts.add(post);
        }
        io.novepusPrintln(String.format("Displaying all Posts, %d in total!", allPosts.size()));
        io.printPostList(allPosts);
        io.novepusPrintln("Display posts finished!");
    }

    private void displayInterestPosts() throws SQLException {
        io.novepusPrintln("You are interested in " +
                DBController.getUserInterest(currentUser));
        ArrayList<Post> posts = new ArrayList<>();
        for (int id : DBController.getUserInterestPost(currentUser)) {
            Post post = DBController.retrievePostById(id);
            if (!post.deleted())
                posts.add(post);
        }
        io.novepusPrintln(String.format("Displaying interesting Posts, %d in total!", posts.size()));
        io.printPostList(posts);
        io.novepusPrintln("Display interesting posts finished!");
    }

    private void selectPost() throws SQLException {
        displayAllPosts();
        String pids;
        int pid;
        if (Objects.equals(currentUser, GUEST_USER_NAME)) {
            io.novepusPrintln("You must Log In first!");
            loginGuide();
            if (Objects.equals(currentUser, GUEST_USER_NAME))
                return;
        }
        do {
            io.novepusPrintln("Input the 'pid' ('~' to quit)");
            pids = io.readLine();
            if (pids.equals("~"))
                return;
            try {
                pid = Integer.parseInt(pids);
            } catch (NumberFormatException numberFormatException) {
                io.novepusPrintln("Invalid pid value!");
                pid = 0;
                continue;
            }
            if (DBController.postNotExist(pid) || DBController.retrievePostById(pid).deleted())
                io.novepusPrintln(String.format("Post (pid=%s) does not exist! Cannot select!", pid));
        } while (DBController.postNotExist(pid) || DBController.retrievePostById(pid).deleted());
        String cmd;
        do {
            displayPostDetails(pid);
            io.showPostDetailMenu();
            cmd = io.readLine().strip().toLowerCase();
            switch (cmd) {
                case "l" -> {
                    DBController.userLikePost(currentUser, pid);
                    io.novepusPrintln("Liked");
                }
                case "c" -> {
                    String content;
                    io.novepusPrintln("You may make comment to this Post");
                    content = io.readText();
                    DBController.createComment(new Comment(pid, currentUser, content));
                    io.novepusPrintln(String.format("Successfully comment on Post (pid=%d)", pid));
                }
                case "q" -> io.novepusPrintln("Going Back");
                default -> io.novepusPrintln("Unrecognized Command " + cmd);
            }
        } while (!Objects.equals(cmd, "q"));
    }

    private void displayPostDetails(int postId) throws SQLException {
        Post post = DBController.retrievePostById(postId);
        io.printPost(post);
    }

    public void run() throws SQLException {
        io.showWelcomePage();
        mainMenu();
    }

    public void connectToOracle() {
        try {
            DriverManager.registerDriver(new OracleDriver());
            connection = (OracleConnection) DriverManager.getConnection(OracleData.URL.getData(),
                    OracleData.USERNAME.getData(), OracleData.PASSWORD.getData());
            DBController.conn = connection;
            io.novepusPrintln("Successfully connect to Oracle -> " + connection);
        } catch (SQLException sqlException) {
            connection = null;
            sqlException.printStackTrace();
            io.novepusPrintln("System Failure! Cannot connect to Oracle! Exit");
            System.exit(0);
        }
    }

    public NovepusIO getIo() {
        return io;
    }

    public OracleConnection getConnection() {
        return connection;
    }

    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
        io.setUsername(currentUser);
    }
}
