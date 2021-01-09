package com.demo.dao.implementation;

import java.util.List;

import org.hibernate.Session;

import com.demo.dao.service.ProductsDAO;
import com.demo.pojo.Products;

public class ProductsDAOImpl implements ProductsDAO{

	public List<Products> getAllProducts(){
		
		Session session = HibernateConnection.doHibernateConnection().openSession();
		
		List<Products> allProducts = session.createQuery("From Products").list();
		session.close();
		
		return allProducts;
	}
	
	public Products getProductById(Integer id){
		
		Session session = HibernateConnection.doHibernateConnection().openSession();
		
		List<Products> products = session.createQuery("From Products where id = "+id).list();
		session.close();
		if(products != null && products.size() == 1){
			return products.get(0);
		}else{
			return null;
		}
	}
}
