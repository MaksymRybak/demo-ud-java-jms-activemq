package com.demo.dao.service;

import java.util.List;

import com.demo.pojo.Products;

public interface ProductsDAO {
	
	public List<Products> getAllProducts();
	public Products getProductById(Integer id);
}
