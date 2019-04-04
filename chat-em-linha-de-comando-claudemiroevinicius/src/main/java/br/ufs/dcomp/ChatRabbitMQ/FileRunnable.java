package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import java.io.*;
import java.nio.file.*;

public class FileRunnable /*implements Runnable */ extends Thread {

  private Channel channel;
  private String destinationPrefix;
  private String sendTo;
  private String user;
  private String typeMime;
  private String fileName;
  private Path source;

  public FileRunnable( Channel channel, String destinationPrefix, String sendTo, String user, String typeMime, String fileName, Path source)
    throws Exception {
    
    this.channel = channel;
    this.destinationPrefix = destinationPrefix;
    this.sendTo = sendTo;
    this.user = user;
    this.typeMime = typeMime;
    this.fileName = fileName;
    this.source = source;
    
  }

  public void run() {
    try {
      
      System.out.println( "\n Enviando \"" + fileName + "\" para " + destinationPrefix + sendTo + ".");
      Chat.toWait();
      
      if ( destinationPrefix.equals("@") ) {
        channel.basicPublish( "", ( sendTo + "File" ), null, Message.serialize( user , ( destinationPrefix + sendTo ), typeMime, fileName, source.getFileName().toString() ) );
      } else {
        channel.basicPublish( ( sendTo + "File" ), "", null, Message.serialize( user, (destinationPrefix + sendTo), typeMime, fileName, source.getFileName().toString() ) );
      }
      
      System.out.println( "\n Arquivo \""  + fileName +  "\" foi enviado para " + destinationPrefix + sendTo + " !" );
      Chat.toWait();
      
    } catch ( Exception e ) {
      System.out.println( e );
    }
  }


}


