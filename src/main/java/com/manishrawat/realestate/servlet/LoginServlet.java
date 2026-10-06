package com.manishrawat.realestate.servlet;
import com.manishrawat.realestate.model.User;
import com.manishrawat.realestate.service.AuthService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
@WebServlet("/login") public class LoginServlet extends HttpServlet {
 private static final Logger LOG=Logger.getLogger(LoginServlet.class.getName());
 private final AuthService auth=new AuthService();
 protected void doPost(HttpServletRequest r,HttpServletResponse s)throws IOException{
  try{String email=r.getParameter("email"), password=r.getParameter("password");
   if(email==null||email.isBlank()||password==null||password.isBlank()){s.sendRedirect(r.getContextPath()+"/?error=Please+fill+all+fields");return;}
   User u=auth.login(email.trim(),password); if(u==null){s.sendRedirect(r.getContextPath()+"/?error=Invalid+email+or+password");return;}
   HttpSession session=r.getSession(); session.invalidate(); session=r.getSession(true); session.setAttribute("user",u); session.setMaxInactiveInterval(30*60); s.sendRedirect(r.getContextPath()+"/dashboard");
  }catch(Exception e){LOG.log(Level.SEVERE,"Login database operation failed (credentials are not logged).",e);s.sendRedirect(r.getContextPath()+"/?error=Database+connection+error");}
 }
}
