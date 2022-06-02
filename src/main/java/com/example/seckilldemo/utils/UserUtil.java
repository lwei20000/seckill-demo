package com.example.seckilldemo.utils;

import com.example.seckilldemo.entity.TUser;
import com.example.seckilldemo.vo.RespBean;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 生成用户工具类
 *
 * @author: LC
 * @date 2022/3/4 3:29 下午
 * @ClassName: UserUtil
 */
public class UserUtil {

    private static void createUser(int count) throws Exception {
        List<TUser> users = new ArrayList<TUser>();
        for (int i = 0; i < count; i++) {
            TUser tUser = new TUser();
            tUser.setId(1300000000L + i);
            tUser.setNickname("user" + i);
            tUser.setSalt("1a2b3c");
            tUser.setPassword(MD5Util.inputPassToDBPass("123456", tUser.getSalt()));
            //tUser.setHead();
            tUser.setRegisterDate(new Date());
            //tUser.setLastLoginDate();
            tUser.setLoginCount(1);
            users.add(tUser);
        }
        System.out.println("create user");


        // 插入数据库
        //insertToDB(users);

        // 登陆，生成token
        createToken(users);


        // 遗留问题点：文件中ticket是null
        // TUserServiceImpl.doLongin的 return RespBean.success(ticket); 没有括号中加入ticket
    }

    /**
     * 插入数据库
     * @param users
     * @throws Exception
     */
    private static void insertToDB(List<TUser> users) throws Exception {
        // 插入数据库
        Connection conn = getConn();
        String sql = "insert into t_user(login_count,nickname,register_date,salt,password,id) value (?,?,?,?,?,?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        for (int i = 0; i < users.size(); i++) {
            TUser tUser = users.get(i);
            pstmt.setInt(1, tUser.getLoginCount());
            pstmt.setString(2, tUser.getNickname());
            pstmt.setTimestamp(3, new Timestamp(tUser.getRegisterDate().getTime()));
            pstmt.setString(4, tUser.getSalt());
            pstmt.setString(5, tUser.getPassword());
            pstmt.setLong(6, tUser.getId());
            pstmt.addBatch();
        }
        pstmt.executeBatch();
        pstmt.clearParameters();
        conn.close();
        System.out.println("insert to DB");
    }

    /**
     * 登陆，生成token
     * @param users
     */
    private static void createToken(List<TUser> users) throws Exception {
        String urlString = "http://127.0.0.1:8080/login/doLogin";
        File file = new File("/Users/weiliang/Desktop/config.txt");
        if (file.exists()) {
            file.delete();
        }
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.seek(0);
        for (int i = 0; i < users.size(); i++) {
            TUser tUser = users.get(i);
            URL url = new URL(urlString);
            HttpURLConnection co = (HttpURLConnection) url.openConnection();
            co.setRequestMethod("POST");
            co.setDoOutput(true);
            OutputStream out = co.getOutputStream();
            String params = "mobile=" + tUser.getId() + "&password=" + MD5Util.inputPassToFromPass("123456");
            out.write(params.getBytes());
            out.flush();
            InputStream inputStream = co.getInputStream();
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            byte[] buff = new byte[1024];
            int len = 0;
            while ((len = inputStream.read(buff)) >= 0) {
                bout.write(buff, 0, len);
            }
            inputStream.close();
            bout.close();
            String response = new String(bout.toByteArray());
            ObjectMapper mapper = new ObjectMapper();
            RespBean respBean = mapper.readValue(response, RespBean.class);
            String userTicket = (String) respBean.getObject();
            System.out.println("create userTicket:" + tUser.getId() + "， " + userTicket);
            String row = tUser.getId() + "," + userTicket;
            raf.seek(raf.length());
            raf.write(row.getBytes());
            raf.write("\r\n".getBytes());
            System.out.println("write to file •" + tUser.getId());
        }
        raf.close();
        System.out.println("over");
    }

    private static Connection getConn() throws Exception {
        String url = "jdbc:mysql://127.0.0.1:3306/seckill?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai";
        String username = "root";
        String password = "111111.qq";
        String driver = "com.mysql.cj.jdbc.Driver";
        Class.forName(driver);
        return DriverManager.getConnection(url, username, password);
    }

    public static void main(String[] args) throws Exception {
        createUser(5000);
    }

}
