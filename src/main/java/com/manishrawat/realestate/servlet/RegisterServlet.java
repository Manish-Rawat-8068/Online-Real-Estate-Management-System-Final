package com.manishrawat.realestate.servlet;
import com.manishrawat.realestate.service.AuthService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
@WebServlet("/register") public class RegisterServlet extends HttpServlet {
 protected void doPost(HttpServletRequest r,HttpServletResponse s)throws IOException{
  try{String name=r.getParameter("name"),email=r.getParameter("email"),password=r.getParameter("password"),phone=r.getParameter("phone");
   if(name==null||name.isBlank()||name.length()>100||email==null||!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")||email.length()>120||password==null||password.length()<8||password.length()>72||phone!=null&&phone.length()>30){s.sendRedirect(r.getContextPath()+"/?error=Enter+valid+details+and+a+password+of+at+least+8+characters");return;}
   new AuthService().register(name.trim(),email.trim().toLowerCase(java.util.Locale.ROOT),password,phone==null?"":phone.trim()); s.sendRedirect(r.getContextPath()+"/?registered=1");
  }catch(Exception e){String msg=e.getMessage()!=null&&e.getMessage().toLowerCase().contains("duplicate")?"Email+already+registered":"Registration+failed";s.sendRedirect(r.getContextPath()+"/?error="+msg);}
 }
}
