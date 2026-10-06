package com.manishrawat.realestate.servlet;
import com.manishrawat.realestate.dao.NotificationDAO; import com.manishrawat.realestate.model.User; import jakarta.servlet.*; import jakarta.servlet.annotation.WebServlet; import jakarta.servlet.http.*; import java.io.*;
@WebServlet("/notifications") public class NotificationServlet extends HttpServlet{
 protected void doGet(HttpServletRequest q,HttpServletResponse s)throws ServletException,IOException{try{User u=(User)q.getSession(false).getAttribute("user");NotificationDAO d=new NotificationDAO();q.setAttribute("notifications",d.list(u.getId()));q.getRequestDispatcher("/WEB-INF/views/notifications.jsp").forward(q,s);}catch(Exception e){throw new ServletException("Unable to load notifications.",e);}}
 protected void doPost(HttpServletRequest q,HttpServletResponse s)throws IOException{try{User u=(User)q.getSession(false).getAttribute("user");new NotificationDAO().markRead(Integer.parseInt(q.getParameter("id")),u.getId());s.sendRedirect(q.getContextPath()+"/notifications");}catch(Exception e){s.sendError(400,"Invalid notification request.");}}
}
