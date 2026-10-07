package com.manishrawat.realestate.dao;
import com.manishrawat.realestate.model.*; import com.manishrawat.realestate.util.DBConnection; import java.sql.*; import java.util.*;
public class UserDAO implements GenericRepository<User>{
 public User login(String email,String pass)throws Exception{String q="SELECT * FROM users WHERE email=? AND password=?";try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(q)){p.setString(1,email);p.setString(2,pass);ResultSet r=p.executeQuery();if(r.next())return map(r); }return null;}
 public boolean register(String n,String e,String p,String phone)throws Exception{try(Connection c=DBConnection.getConnection();PreparedStatement s=c.prepareStatement("INSERT INTO users(name,email,password,role,phone) VALUES(?,?,?,?,?)")){s.setString(1,n);s.setString(2,e);s.setString(3,p);s.setString(4,"TENANT");s.setString(5,phone);return s.executeUpdate()>0;}}
 public List<User> findAll()throws Exception{List<User> l=new ArrayList<>();try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT * FROM users ORDER BY id DESC")){ResultSet r=p.executeQuery();while(r.next())l.add(map(r));}return l;}
 public User findById(int id)throws Exception{try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT * FROM users WHERE id=?")){p.setInt(1,id);ResultSet r=p.executeQuery();return r.next()?map(r):null;}}
 public int count()throws Exception{try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM users")){ResultSet r=p.executeQuery();r.next();return r.getInt(1);}}
 private User map(ResultSet r)throws Exception{int i=r.getInt("id");String n=r.getString("name"),e=r.getString("email"),pw=r.getString("password"),role=r.getString("role"),ph=r.getString("phone");return switch(role){case "ADMIN"->new Admin(i,n,e,pw,ph);case "MANAGER"->new Manager(i,n,e,pw,ph);default->new Tenant(i,n,e,pw,ph);};}
}
