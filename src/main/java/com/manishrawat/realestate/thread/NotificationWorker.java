package com.manishrawat.realestate.thread;
import com.manishrawat.realestate.dao.NotificationDAO;
import java.util.concurrent.*;
import java.util.logging.*;

/** Bounded single-consumer queue for non-blocking in-app notification writes. */
public final class NotificationWorker implements Runnable {
 private static final BlockingQueue<Notice> QUEUE=new ArrayBlockingQueue<>(500);
 private static final Logger LOG=Logger.getLogger(NotificationWorker.class.getName());
 public record Notice(int userId,String message) { }
 public static boolean submit(int userId,String message){return QUEUE.offer(new Notice(userId,message));}
 public static synchronized int pending(){return QUEUE.size();}
 @Override public void run(){NotificationDAO dao=new NotificationDAO();while(!Thread.currentThread().isInterrupted()){try{Notice n=QUEUE.take();dao.create(n.userId(),n.message());}catch(InterruptedException e){Thread.currentThread().interrupt();}catch(Exception e){LOG.log(Level.WARNING,"Could not persist queued notification",e);}}}
}
