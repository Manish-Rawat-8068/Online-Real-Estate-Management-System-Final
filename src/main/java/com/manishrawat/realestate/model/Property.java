package com.manishrawat.realestate.model;

import java.math.BigDecimal;

public class Property {
    private int id;
    private int managerId;
    private int bedrooms;
    private String title;
    private String address;
    private String city;
    private String propertyType;
    private String description;
    private String status;
    private BigDecimal rent;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getManagerId() { return managerId; }
    public void setManagerId(int managerId) { this.managerId = managerId; }
    public int getBedrooms() { return bedrooms; }
    public void setBedrooms(int bedrooms) { this.bedrooms = bedrooms; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getRent() { return rent; }
    public void setRent(BigDecimal rent) { this.rent = rent; }

    /** Compatibility aliases for older JSP/DAO code and schemas that call rent "price". */
    @Deprecated
    public BigDecimal getPrice() { return getRent(); }

    @Deprecated
    public void setPrice(BigDecimal price) { setRent(price); }
}
