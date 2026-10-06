package com.manishrawat.realestate.service;

import com.manishrawat.realestate.dao.PropertyDAO;
import com.manishrawat.realestate.model.Property;

import java.util.List;

public class PropertyService {
    private final PropertyDAO dao = new PropertyDAO();

    public int create(Property property) throws Exception { return dao.save(property); }
    public void update(Property property, int managerId) throws Exception { dao.update(property, managerId); }
    public void delete(int propertyId, int managerId) throws Exception { dao.delete(propertyId, managerId); }
    public List<Property> search(String city, String type) throws Exception { return dao.approved(city, type); }
    public List<Property> all() throws Exception { return dao.findAll(); }
    public void approve(int id, boolean approved) throws Exception {
        dao.setStatus(id, approved ? "APPROVED" : "REJECTED");
    }
    public List<Property> manager(int id) throws Exception { return dao.findByManager(id); }
}
