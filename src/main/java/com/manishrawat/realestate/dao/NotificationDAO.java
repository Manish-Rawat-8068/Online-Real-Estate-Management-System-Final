package com.manishrawat.realestate.dao;
import com.manishrawat.realestate.util.DBConnection; import java.sql.*; import java.util.*;
public class NotificationDAO {
 public void create(int uid,String message)throws Exception{
  Map<String,Column> actual=columns();List<String> names=new ArrayList<>(List.of("user_id"));List<String> values=new ArrayList<>(List.of("?"));
  if(actual.containsKey("title")){names.add("title");values.add("?");}
  names.add("message");values.add("?");
  for(Column column:actual.values())if(!column.nullable()&&column.defaultValue()==null&&!column.autoIncrement()&&!names.contains(column.name()))throw new SQLException("Notifications table has an unsupported required column: "+column.name());
  try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO notifications("+String.join(",",names)+") VALUES("+String.join(",",values)+")")){
   int i=1;p.setInt(i++,uid);if(actual.containsKey("title"))p.setString(i++,"Account update");p.setString(i,message);p.executeUpdate();
  }
 }
 public List<Map<String,Object>> list(int uid)throws Exception{Map<String,Column> actual=columns();String title=actual.containsKey("title")?"title":"''";List<Map<String,Object>> rows=new ArrayList<>();try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT id,"+title+" AS title,message,is_read,created_at FROM notifications WHERE user_id=? ORDER BY id DESC LIMIT 100")){p.setInt(1,uid);try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object>x=new LinkedHashMap<>();for(String k:List.of("id","title","message","is_read","created_at"))x.put(k,r.getObject(k));rows.add(x);}}}return rows;}
 public int unreadCount(int uid)throws Exception{try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM notifications WHERE user_id=? AND is_read=FALSE")){p.setInt(1,uid);try(ResultSet r=p.executeQuery()){r.next();return r.getInt(1);}}}
 public void markRead(int id,int uid)throws Exception{try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("UPDATE notifications SET is_read=TRUE WHERE id=? AND user_id=?")){p.setInt(1,id);p.setInt(2,uid);p.executeUpdate();}}
 public void markAllRead(int uid)throws Exception{try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("UPDATE notifications SET is_read=TRUE WHERE user_id=?")){p.setInt(1,uid);p.executeUpdate();}}
 private record Column(String name,boolean nullable,String defaultValue,boolean autoIncrement) { }
 private Map<String,Column> columns()throws Exception{Map<String,Column> result=new LinkedHashMap<>();String sql="SELECT column_name,is_nullable,column_default,extra FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='notifications'";try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){while(r.next()){String name=r.getString("column_name").toLowerCase(Locale.ROOT);result.put(name,new Column(name,"YES".equalsIgnoreCase(r.getString("is_nullable")),r.getString("column_default"),r.getString("extra").toLowerCase(Locale.ROOT).contains("auto_increment")));}}for(String required:List.of("user_id","message","id","is_read","created_at"))if(!result.containsKey(required))throw new SQLException("Notifications table is missing required column: "+required);return result;}
}
