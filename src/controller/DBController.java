package controller;

import model.Post;
import model.User;
import oracle.jdbc.driver.OracleConnection;
import oracle.jdbc.driver.OracleDriver;

import java.sql.*;
import java.util.ArrayList;

public class DBController {
    static OracleConnection conn;

    private static ResultSet exc(String s) throws SQLException {
        Statement stmt = conn.createStatement();
        return stmt.executeQuery(s);

    }

    private static Date curTime() {
        return new Date(new java.util.Date().getTime());
    }

    // about user
    public static void createUser(User user) throws SQLException {
        String s = String.format("INSERT INTO \"user\" VALUES (%s,'%s','%s','%s','%s','%s','%s',%s)", 0, user.userName(), user.userPassword(), user.userEmail(), "haha", curTime(), curTime(), 0);
        exc(s);
    }

    public static void setUserPassword(User user, String newPassword) throws SQLException {
        String s = String.format("update \"user\" set \"password\" = '%s' where \"username\"= '%s'",newPassword,user.userName());
        exc(s);
    }

    public static User retrieveUserByName(String userName) throws SQLException {
        // get user
        String s1 = String.format("SELECT * FROM \"user\" WHERE \"username\" = '%s'", userName);
        ResultSet r1 = exc(s1);
        r1.next();

        // get interest id
        ArrayList<Integer> interestIdList = new ArrayList<>();
        String s2 = String.format("SELECT * FROM \"interest_user\" where \"user_id\" = %s",  r1.getInt(1));
        ResultSet r2 = exc(s2);
        while (r2.next()) {
            interestIdList.add(r2.getInt(2));
        }

        // get post id
        ArrayList<Integer> postIdList = new ArrayList<>();
        String s3 = String.format("SELECT * FROM \"post\" where \"create_user_id\" = %s", r1.getInt(1));
        ResultSet r3 = exc(s3);
        while (r3.next()) {
            postIdList.add(r3.getInt(1));
        }

        // get following id
        ArrayList<Integer> followingsIdList = new ArrayList<>();
        String s4 = String.format("SELECT * FROM \"follow_user\" where \"user_id\" = %s", r1.getInt(1));
        ResultSet r4 = exc(s4);
        while (r4.next()) {
            followingsIdList.add(r4.getInt(2));
        }

        // get follower id
        ArrayList<Integer> followerIdList = new ArrayList<>();
        String s5 = String.format("SELECT * FROM \"follow_user\" where \"user_befollowed_id\" = %s", r1.getInt(1));
        ResultSet r5 = exc(s5);
        while (r5.next()) {
            followerIdList.add(r5.getInt(1));
        }

        return new User(
                r1.getInt(1),
                r1.getString(2),
                r1.getString(3),
                r1.getString(4),
                r1.getBoolean(8),
                r1.getDate(6).toString(),
                r1.getDate(7).toString(),
                interestIdList,
                postIdList,
                followingsIdList,
                followerIdList
        );
    }

    public static User retrieveUserById(int userId) throws SQLException {
        return null;
    }

    public static void setUserStatus (String username,boolean status) throws SQLException{
        String s = String.format("update \"user\" set \"isonline\" = '%s' where \"username\"= '%s'",status?"1":"0",username);
        exc(s);
    }

    public static void setPostStatus (int postId,boolean status) throws SQLException{
        String s = String.format("update \"post\" set \"isdelete\" = '%s' where \"id\"= %s",status?"1":"0",postId);
        exc(s);
    }

    public static ArrayList<String> getUserInterest (int userId) {
        try {
            ArrayList<Integer> interestId = new ArrayList<>();
            String s2 = String.format("SELECT * FROM \"interest_user\" where \"user_id\" = '%s'",  userId);
            ResultSet r2 = exc(s2);
            while (r2.next()) {
                interestId.add(r2.getInt(2));
            }
            ArrayList<String> interestString = new ArrayList<>();
            for (Integer integer : interestId) {
                interestString.add(getLabelById(integer));
            }
            return interestString;
        } catch (SQLException e){
            System.out.println("ERROR");
        }
        return null;
    }

    public static String getLabelById (int id){
        try {
            String s= String.format("SELECT * FROM \"interest\" where \"id\" = '%s'",id);
            ResultSet r= exc(s);
            if (r.next()){
                return r.getString(2);
            }
        }catch (SQLException e){
            System.out.println("ERROR");
        }
        return null;
    }

    public static boolean userExist(String username) throws SQLException {
        String s = String.format("SELECT * FROM \"user\" WHERE \"username\" = '%s'", username);
        ResultSet r = exc(s);
        return r.next();
    }

    // about post
    public static void createPost(Post post) throws SQLException {
        String s = String.format("INSERT INTO \"post\" VALUES (%s, '%s', '%s', '%s', %s, '%s')",0,post.postAuthor(),post.postDate(),post.content(),0,post.postTitle());
        ResultSet r = exc(s);
    }

    public static Post retrievePostById(int postId) throws SQLException {
        // get post
        String s = String.format("SELECT * FROM \"post\" WHERE \"id\" = %s",postId);
        ResultSet r = exc(s);

        // get interest id
        ArrayList<Integer> interestIdList = new ArrayList<>();
        String s2 = String.format("SELECT * FROM \"interest_user\" where \"id\" = %s", postId);
        ResultSet r2 = exc(s2);
        while (r2.next()) {
            interestIdList.add(r2.getInt(2));
        }

        r.next();
        return new Post(
                r.getInt(1),
                r.getString(6),
                r.getString(2),
                r.getString(4),
                r.getBoolean(5),
                r.getString(3),
                interestIdList);
    }

    public static boolean postExist(int id) throws SQLException {
        String s = String.format("SELECT * FROM \"post\" WHERE \"id\" = %s", id);
        ResultSet r = exc(s);
        return r.next();
    }

    // -------------Need to achieve---------------

    // about post

    public static void addUserInterest(String username, String labelName) throws SQLException {
        String s = String.format("SELECT id FROM \"user\" WHERE \"username\" = '%s'", username);
        ResultSet r = exc(s);
        String str = String.format("INSERT INTO \"interest\" VALUES (%d,'%s')", r.getInt(1), labelName);
    }

    public static ArrayList<Post> searchPostByKey(String keyword) throws SQLException {
        ArrayList<Integer> postIdlist = null;
        String s = "SELECT id FROM \"post\" WHERE \"content\" LIKE \'%" + keyword + "%\'";
        ResultSet r = exc(s);

        ArrayList<Post> postList = null;
        while (r.next()) {
            postList.add(retrievePostById(r.getInt(1)));
        }
        return postList;
    }

    public static ArrayList<String> getPostLabel(int postId) {
        ArrayList<String> postLableList = null;
        try{
            String s = String.format("SELECT I.lable_name FROM \"interest\" I, \"interest_post\" P WHERE \"I.id\" = \"P.interest_id\" AND \"P.post_id\"= %d", postId);
            ResultSet r = exc(s);
            while (r.next()) {
                postLableList.add(r.getString(1));
            }
        }
        catch (SQLException e){
            System.out.println("Select failed.");
        }
        return postLableList;
    }

    public static ArrayList<Integer> getAllPostId(){
        try {
            String s= "SELECT * FROM \"post\"";
            ResultSet r = exc(s);
        }


    }


    // --------------For test --------------------------

    public static void main(String[] args) {
        try {
            DriverManager.registerDriver(new OracleDriver());
            conn = (OracleConnection) DriverManager.getConnection("jdbc:oracle:thin:@studora.comp.polyu.edu.hk:1521:dbms", "20075519d", "viukiyec");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}