package com.manishrawat.realestate.servlet;
import com.manishrawat.realestate.dao.UserDAO; import com.manishrawat.realestate.model.User; import jakarta.servlet.*; import jakarta.servlet.annotation.WebServlet; import jakarta.servlet.http.*; import java.io.*;
@WebServlet("/profile") public class ProfileServlet extends HttpServlet{
 protected void doGet(HttpServletRequest q,HttpServletResponse s)throws ServletException,IOException{q.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(q,s);}
 protected void doPost(HttpServletRequest q,HttpServletResponse s)throws IOException{User u=(User)q.getSession(false).getAttribute("user");String name=q.getParameter("name"),phone=q.getParameter("phone");if(name==null||name.isBlank()||name.length()>100||phone!=null&&phone.length()>30){s.sendRedirect(q.getContextPath()+"/profile?error=Check+name+and+phone+length");return;}try{new UserDAO().updateProfile(u.getId(),name.trim(),phone==null?"":phone.trim());u.setName(name.trim());u.setPhone(phone==null?"":phone.trim());s.sendRedirect(q.getContextPath()+"/profile?saved=1");}catch(Exception e){throw new IOException("Unable to save profile.",e);}}
}
