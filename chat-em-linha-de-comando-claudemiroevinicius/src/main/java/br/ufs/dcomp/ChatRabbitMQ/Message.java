package br.ufs.dcomp.ChatRabbitMQ;


import com.rabbitmq.client.*;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import com.google.protobuf.*;
import br.ufs.dcomp.ChatRabbitMQ.*;


public class Message { 

                              //(        remetente,      destino,        tipo,        corpo,       nome )
  public static byte[] serialize( String emitter, String destiny, String type, String body, String name )    throws Exception {
    
    // pegando a data e hora do sistema
    SimpleDateFormat dateFormat = new SimpleDateFormat( "dd/MM/yyyy HH:mm" );
    Date currentDate = new Date();
    String dateNow[] = dateFormat.format( currentDate ).split( " " );
                
    // agrupando dados do conteudo
    MessageProto.Content.Builder newContent = MessageProto.Content.newBuilder();
    newContent.setType( type );
    
    if ( type.equals( "text/message" ) ) {
      newContent.setBody( ByteString.copyFrom( body.getBytes( "UTF-8" ) ) ); // mensagem de texto
    } else {
      newContent.setBody( ByteString.copyFrom( fileToByte( body ) ) ); // no caso de arquivo, o caminho eh passado no corpo
    }
    
    
    newContent.setName( name );
    
    // agrupando dados da mensagem
    MessageProto.Message.Builder newMessage = MessageProto.Message.newBuilder();
    newMessage.setEmitter( emitter );
    newMessage.setDate( dateNow[0] );
    newMessage.setHour( dateNow[1] );
    newMessage.setGroup( destiny );
    newMessage.setContent( newContent );
    
    // obtendo a mensagem
    MessageProto.Message sendMessage = newMessage.build();
    
    // serializando mensagem
    return sendMessage.toByteArray();

  }
  
               
  public static String deserialize( byte[] buffer )      throws IOException {
    
    Chat newChat = new Chat();
    
    // mapeando bytes para mensagem protobuf
    MessageProto.Message messageReceived = MessageProto.Message.parseFrom( buffer );
    String messageToPrint = "";
    
    // extraindo dados da mensagem
    if ( !messageReceived.getEmitter().equals( newChat.user ) ) {
      String type = messageReceived.getContent().getType();
      
      switch ( type ) {
        
        case "text/message":
          messageToPrint = "(" + messageReceived.getDate() + " às " + messageReceived.getHour() + ") " + messageReceived.getEmitter() + 
                              ( ( messageReceived.getGroup().charAt(0) == '#' ) ? ( messageReceived.getGroup() ) : "") + // verificando existência de grupo
                              " diz: " + messageReceived.getContent().getBody().toStringUtf8();
          break;
          
        default:
          SimpleDateFormat dateFormat = new SimpleDateFormat( "dd/MM/yyyy HH:mm" );
          Date currentDate = new Date();
          String dateNow[] = dateFormat.format( currentDate ).split( " " );
          String dateFile = "(" + dateNow[0] + " às " + dateNow[1] + ") "; // utilizando data e hora de sistema
          String dateFileName = dateNow[0].replaceAll( "/", "-" ) + "_" + dateNow[1].replaceAll( ":", "h" ); //
          String fileName = dateFileName + "_from" + "@" + messageReceived.getEmitter() + "_" + messageReceived.getContent().getName();
          byteToFile( ( messageReceived.getContent().getBody().toByteArray() ), fileName );
          messageToPrint = dateFile + "Arquivo \"" + messageReceived.getContent().getName() + "\" recebido de @" + messageReceived.getEmitter() + " !"; // (21/09/2016 às 20:55) Arquivo "aula1.pdf" recebido de @tarcisio !

          break;
      }
    }
    return messageToPrint;
    
  }
  
  public static byte[] fileToByte( String path )         throws FileNotFoundException, IOException {
    File fileToSend = new File( path );
    FileInputStream fileStream = new FileInputStream( fileToSend );
    byte[] byteFile = new byte[(int)fileToSend.length()];
    for( int i = 0; i < fileToSend.length(); i++ ) {
      byteFile[i] = ( byte ) fileStream.read();
    }
    return byteFile;
  }
  
   public static void byteToFile( byte[] bytes, String fileName )          throws FileNotFoundException, IOException {
    FileOutputStream fileReceived = new FileOutputStream( new File( "downloads/" + fileName ) );
    fileReceived.write( bytes );
  }
  
}