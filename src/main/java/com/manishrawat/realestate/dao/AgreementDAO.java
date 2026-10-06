package com.manishrawat.realestate.dao;
import com.manishrawat.realestate.model.User;
import com.manishrawat.realestate.util.DBConnection;
import java.sql.*; import java.util.*;
public class AgreementDAO {
 public List<Map<String,Object>> list(User user)throws Exception{
  String sql="SELECT g.id,g.application_id,g.start_date,g.end_date,g.rent,g.status,p.title,t.name tenant_name,m.name manager_name,g.created_at FROM rental_agreements g JOIN rental_applications a ON a.id=g.application_id JOIN properties p ON p.id=a.property_id JOIN users t ON t.id=a.tenant_id LEFT JOIN users m ON m.id=p.manager_id";
  if("TENANT".equals(user.getRole()))sql+=" WHERE a.tenant_id=?";else if("MANAGER".equals(user.getRole()))sql+=" WHERE p.manager_id=?";
  sql+=" ORDER BY g.id DESC"; List<Map<String,Object>> rows=new ArrayList<>();
  try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){if(!"ADMIN".equals(user.getRole()))p.setInt(1,user.getId());try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object>x=new LinkedHashMap<>();for(String k:List.of("id","application_id","start_date","end_date","rent","status","title","tenant_name","manager_name","created_at"))x.put(k,r.getObject(k));rows.add(x);}}}return rows;
 }
 public int create(int applicationId,java.sql.Date start,java.sql.Date end,java.math.BigDecimal rent,int managerId)throws Exception{
  if(!end.after(start)||rent.signum()<=0)throw new IllegalArgumentException("Enter a valid date range and rent.");
  try(Connection c=DBConnection.getConnection()){
   c.setAutoCommit(false);
   try{
    Map<String,Column> actual=agreementColumns(c);
    List<String> columns=new ArrayList<>(List.of("application_id","start_date","end_date","rent"));
    List<String> expressions=new ArrayList<>(List.of("a.id","?","?","?"));
    addCompatibilityColumn(actual,columns,expressions,"property_id","a.property_id");
    addCompatibilityColumn(actual,columns,expressions,"tenant_id","a.tenant_id");
    addCompatibilityColumn(actual,columns,expressions,"manager_id","p.manager_id");
    int legacyRentAliases=0;
    for(String alias:List.of("monthly_rent","rent_amount","rent_per_month","monthly_amount")){
     if(actual.containsKey(alias)){columns.add(alias);expressions.add("?");legacyRentAliases++;}
    }
    for(Column column:actual.values()){
     if(!column.nullable()&&column.defaultValue()==null&&!column.autoIncrement()&&!columns.contains(column.name()))
      throw new SQLException("Rental agreement table has an unsupported required column: "+column.name());
    }
    String sql="INSERT INTO rental_agreements("+String.join(",",columns)+") SELECT "+String.join(",",expressions)
      +" FROM rental_applications a JOIN properties p ON p.id=a.property_id WHERE a.id=? AND a.status='APPROVED'"
      +" AND (p.manager_id=? OR ?=-1) AND NOT EXISTS(SELECT 1 FROM rental_agreements WHERE application_id=a.id)";
    try(PreparedStatement p=c.prepareStatement(sql)){
     int index=1;p.setDate(index++,start);p.setDate(index++,end);p.setBigDecimal(index++,rent);
     for(int i=0;i<legacyRentAliases;i++)p.setBigDecimal(index++,rent);
     p.setInt(index++,applicationId);p.setInt(index++,managerId);p.setInt(index,managerId);
     if(p.executeUpdate()==0)throw new IllegalArgumentException("Agreement requires an approved application you manage, with no existing agreement.");
    }
    int tenant;
    try(PreparedStatement p=c.prepareStatement("SELECT tenant_id FROM rental_applications WHERE id=?")){
     p.setInt(1,applicationId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("The approved application disappeared during agreement creation.");tenant=r.getInt(1);}
    }
    c.commit();return tenant;
   }catch(Exception e){try{c.rollback();}catch(SQLException rollback){e.addSuppressed(rollback);}throw e;}
  }
 }
 private record Column(String name,boolean nullable,String defaultValue,boolean autoIncrement) { }
 private Map<String,Column> agreementColumns(Connection c)throws SQLException{
  Map<String,Column> columns=new LinkedHashMap<>();
  String sql="SELECT column_name,is_nullable,column_default,extra FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='rental_agreements'";
  try(PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){
   while(r.next()){String name=r.getString("column_name").toLowerCase(Locale.ROOT);columns.put(name,new Column(name,"YES".equalsIgnoreCase(r.getString("is_nullable")),r.getString("column_default"),r.getString("extra").toLowerCase(Locale.ROOT).contains("auto_increment")));}
  }
  for(String required:List.of("application_id","start_date","end_date","rent"))if(!columns.containsKey(required))throw new SQLException("Rental agreement table is missing required column: "+required);
  return columns;
 }
 private void addCompatibilityColumn(Map<String,Column> actual,List<String> columns,List<String> expressions,String column,String expression){
  if(actual.containsKey(column)){columns.add(column);expressions.add(expression);}
 }
 public int updateStatus(int id,String status,User user)throws Exception{if(!List.of("ACTIVE","EXPIRED","TERMINATED").contains(status))throw new IllegalArgumentException("Invalid agreement status.");String sql="UPDATE rental_agreements g JOIN rental_applications a ON a.id=g.application_id JOIN properties p ON p.id=a.property_id SET g.status=? WHERE g.id=?";if("MANAGER".equals(user.getRole()))sql+=" AND p.manager_id=?";try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setString(1,status);p.setInt(2,id);if("MANAGER".equals(user.getRole()))p.setInt(3,user.getId());if(p.executeUpdate()==0)throw new IllegalArgumentException("Agreement not found or access denied.");}try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT a.tenant_id FROM rental_agreements g JOIN rental_applications a ON a.id=g.application_id WHERE g.id=?")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){r.next();return r.getInt(1);}}}
}
