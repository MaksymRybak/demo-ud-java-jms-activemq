package com.demo.dao.implementation;

import java.util.List;

import org.hibernate.Session;

import com.demo.pojo.User;

public class UserDAOImpl implements com.demo.dao.service.UserDAO{
	
	public User getUserByUsername(String username){
		try{
			Session session = HibernateConnection.doHibernateConnection().openSession();
			List<User> users = session.createQuery("From User where username = :username")
								.setParameter("username", username).list();
			session.close();
			if(users != null && users.size() == 1){
				return users.get(0);
			}else{
				return null;
			}
			
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
}
