package com.manishrawat.realestate.dao;

import com.manishrawat.realestate.model.Application;
import com.manishrawat.realestate.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ApplicationDAO {
    private static final String SELECT = "SELECT a.*,p.title property_title,u.name tenant_name FROM rental_applications a JOIN properties p ON a.property_id=p.id JOIN users u ON a.tenant_id=u.id";

    public boolean apply(int propertyId, int tenantId, String message) throws Exception {
        String sql = "INSERT INTO rental_applications(property_id,tenant_id,message) SELECT ?,?,? FROM properties WHERE id=? AND status='APPROVED' AND NOT EXISTS (SELECT 1 FROM rental_applications WHERE property_id=? AND tenant_id=?)";
        try (Connection c=DBConnection.getConnection(); PreparedStatement p=c.prepareStatement(sql)) {
            p.setInt(1,propertyId); p.setInt(2,tenantId); p.setString(3,message); p.setInt(4,propertyId); p.setInt(5,propertyId); p.setInt(6,tenantId);
            return p.executeUpdate()>0;
        }
    }
    public List<Application> forTenant(int id) throws Exception { return list(SELECT+" WHERE a.tenant_id=? ORDER BY a.id DESC",id); }
    public List<Application> forManager(int id) throws Exception { return list(SELECT+" JOIN properties owned ON owned.id=a.property_id WHERE owned.manager_id=? ORDER BY a.id DESC",id); }
    public List<Application> approvedWithoutAgreement(int managerId)throws Exception{String sql=SELECT+" JOIN properties owned ON owned.id=a.property_id LEFT JOIN rental_agreements g ON g.application_id=a.id WHERE a.status='APPROVED' AND g.id IS NULL"+(managerId<0?"":" AND owned.manager_id=?");return list(sql,managerId<0?null:managerId);}
    public int managerForProperty(int id)throws Exception{try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT manager_id FROM properties WHERE id=? AND status='APPROVED'")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){return r.next()?r.getInt(1):-1;}}}
    public List<Application> all() throws Exception { return list(SELECT+" ORDER BY a.id DESC",null); }
    public int status(int id,String status,Integer managerId) throws Exception {
        if (!List.of("PENDING","APPROVED","REJECTED").contains(status)) throw new IllegalArgumentException("Invalid application status.");
        String sql="UPDATE rental_applications a JOIN properties p ON p.id=a.property_id SET a.status=? WHERE a.id=?"+(managerId==null?"":" AND p.manager_id=?");
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setString(1,status);p.setInt(2,id);if(managerId!=null)p.setInt(3,managerId);if(p.executeUpdate()==0)throw new IllegalArgumentException("Application not found or access denied.");}
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT tenant_id FROM rental_applications WHERE id=?")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){r.next();return r.getInt(1);}}
    }
    private List<Application> list(String sql,Integer tenantId) throws Exception {
        List<Application> result=new ArrayList<>();
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql)) {
            if(tenantId!=null)p.setInt(1,tenantId);
            try(ResultSet r=p.executeQuery()){while(r.next()){Application a=new Application();a.id=r.getInt("id");a.propertyId=r.getInt("property_id");a.tenantId=r.getInt("tenant_id");a.status=r.getString("status");a.message=r.getString("message");a.propertyTitle=r.getString("property_title");a.tenantName=r.getString("tenant_name");result.add(a);}}
        }
        return result;
    }
}
