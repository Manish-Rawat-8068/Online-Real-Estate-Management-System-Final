package com.manishrawat.realestate.dao; import java.util.*; public interface GenericRepository<T> { List<T> findAll() throws Exception; T findById(int id) throws Exception; }
