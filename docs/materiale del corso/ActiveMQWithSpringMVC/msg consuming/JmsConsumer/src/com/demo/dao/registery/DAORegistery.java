package com.demo.dao.registery;

import com.demo.dao.implementation.ProductsDAOImpl;
import com.demo.dao.implementation.UserDAOImpl;

public class DAORegistery {

	public static com.demo.dao.service.ProductsDAO productsDAO;
	public static com.demo.dao.service.UserDAO userDAO;
	
	static{
		userDAO = new UserDAOImpl();
		productsDAO = new ProductsDAOImpl();
	}

	public static com.demo.dao.service.ProductsDAO getProductsDAO() {
		return productsDAO;
	}

	public static void setProductsDAO(com.demo.dao.service.ProductsDAO productsDAO) {
		DAORegistery.productsDAO = productsDAO;
	}

	public static com.demo.dao.service.UserDAO getUserDAO() {
		return userDAO;
	}

	public static void setUserDAO(com.demo.dao.service.UserDAO userDAO) {
		DAORegistery.userDAO = userDAO;
	}
	
}
