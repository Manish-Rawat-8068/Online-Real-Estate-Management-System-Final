package com.manishrawat.realestate.thread;
import jakarta.servlet.*; import jakarta.servlet.annotation.WebListener;
@WebListener public class NotificationLifecycle implements ServletContextListener {
 private Thread worker;
 @Override public void contextInitialized(ServletContextEvent event){worker=new Thread(new NotificationWorker(),"real-estate-notification-worker");worker.setDaemon(true);worker.start();}
 @Override public void contextDestroyed(ServletContextEvent event){if(worker!=null){worker.interrupt();try{worker.join(3000);}catch(InterruptedException e){Thread.currentThread().interrupt();}}}
}
