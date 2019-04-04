package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import java.io.*;
import java.util.Scanner;
import com.google.protobuf.*;
import java.nio.file.*;

public class Chat {
  
  static String user = ""; // usuario logado
  static String sentBy = ""; // origem da mensagem, usuario ou grupo
  static String sendTo = ""; // destinatario da mensagem, usuario ou grupo
  static String destinationPrefix = ""; // prefixo indicador do destinatario
  static String commandPrefix = ""; // prefixo indicador da operacao
  static String typeMime; // identificador do tipo de arquivo
  static Path source; // origem do arquivo para upload
  static String username = "maasoares"; // usuario administrador do servidor Rabbit
  static String password = "abc123"; // senha de acesso ao servidor Rabbit
  static String hostrmq = "ec2-3-88-66-6.compute-1.amazonaws.com"; // IP da instancia no servidor Rabbit
  
  public static void toWait() { // retorna para espera de entrada do usuario logado 
    System.out.print( ( !sendTo.isEmpty() ? ( destinationPrefix + sendTo ) : "" )  + ">> " );
  }
                    
  public static void main(String[] argv)      throws Exception  {
    
    ConnectionFactory factory = new ConnectionFactory();
    
    factory.setHost( hostrmq ); // IP da instancia ChatRabbitMQ-0 //("ip-da-instancia-da-aws");
    factory.setUsername(username); //("usuário-do-rabbitmq-server");
    factory.setPassword(password); //("senha-do-rabbitmq-server");
    factory.setVirtualHost("/");
    
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    
    // iniciando o chat, logando o usuario
    Scanner scan = new Scanner(System.in);
    
    while ( user.isEmpty() ) {
      System.out.print( "User: " );
      user = scan.nextLine();
      if ( user.isEmpty() ) // verificando se foi digitado o nome do usuario
        System.out.println( "Por favor forneça o nome do usuário !" );
    }

    // criando a fila 
                      //(  queue-name,     durable, exclusive, auto-delete, params); 
    channel.queueDeclare( (user + "Text"), false,   false,     false,       null); // cria fila 
    channel.queueDeclare( (user + "File"), false,   false,     false,       null); // cria filas de arquivos

    // aguardando entrada do usuario logado
    toWait();

    // monitorando o recebimento de mensagens
    Consumer consumer = new DefaultConsumer(channel) {
      
      public void handleDelivery( String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body )    throws IOException {
        String messageReceived = Message.deserialize( body );
        if ( !messageReceived.isEmpty() ) {
        System.out.println( "\n" + messageReceived ); // imprime a mensagem recebida
        toWait();
        }
      }
    };
    
    // consome a fila
                      //(   queue-name,      autoAck, consumer);    
    channel.basicConsume( ( user + "Text" ), true,    consumer ); // consome a fila
    channel.basicConsume( ( user + "File" ), true,    consumer ); // consome a fila de arquivos
    
    while ( scan.hasNextLine() ) {
        
        String messageText = scan.nextLine(); // recebe a entrada do usuario logado
        
        if ( !messageText.isEmpty() ) {
          
          switch ( messageText.charAt(0) ) {
              
            case '@': // identificando usuario destino
              sendTo = messageText.substring(1); // indicando o destinatario
              destinationPrefix = "@";
                                //( queue-name,        durable, exclusive, auto-delete, params);               
              channel.queueDeclare( (sendTo + "Text"), false,   false,     false,       null ); // cria a fila 
              channel.queueDeclare( (sendTo + "File"), false,   false,     false,       null ); // cria a fila de arquivos
              toWait();
              break;
            
            case '#': // identificando grupo destino
              sendTo = messageText.substring(1); // indicando o destinatario
              destinationPrefix = "#";
              toWait();
              break;
              
            case '!': // gera uma lista de comandos
              String command[] = messageText.substring(1).split(" ");
              commandPrefix = "!";
              
              switch ( command[0] ) {
                
                case "addGroup": // criar grupo: "!addGroup grupo"
                  if ( command.length == 2 ) {
                    Group.addGroup( channel, command[1], user );
                  } else {
                    System.out.println( "Nome do grupo não informado !" );
                  }
                  toWait();
                  break;
                      
                case "addUser": // adicionar usuario ao grupo: "!addUser marcos grupo"
                  if ( command.length == 3 ) {
                    Group.addUser( channel, command[1], command[2] );
                  } else {
                    System.out.println( "ERROR - Favor verifique o comando novamente" );
                  }
                  toWait();
                  break;
                  
                case "delFromGroup": // excluir usuario do grupo: "!delFromGroup marcos grupo"
                  if ( command.length == 3 ) {
                    Group.delFromGroup( channel, command[1], command[2] );
                  } else {
                    System.out.println( "ERROR - Favor verifique o comando novamente" );
                  }
                  toWait();
                  break;
                
                case "removeGroup": // apagar grupo: "!removeGroup grupo"
                  if ( command.length == 2 ) {
                    Group.removeGroup( channel, command[1] );
                  } else {
                    System.out.println( "Nome do grupo não informado !" );
                  }
                  toWait();
                  break;
                  
                case "listUsers": // Listar todos os usuários em um grupo: "!listUsers ufs"
                  if ( command.length == 2 ) {
                    Group.listUsers( username + ":" + password,  "http://" + hostrmq + ":15672", command[1] );
                  } else {
                    System.out.println( "Nome do grupo não foi informado!" );
                  }
                  toWait();
                  break;
    
                case "listGroups": // Listar todos usuarios amigos grupos:"!listGroups"
                  Group.listGroups( username + ":" + password,  "http://" + hostrmq + ":15672", user );
                  toWait();
                  break;
                  
                case "upload": // enviar arquivo: "!upload /home/tarcisio/aula1.pdf"
                  source = Paths.get( command[1] );
                  typeMime = Files.probeContentType( source );
                  if ( !sendTo.isEmpty() ) { // enviando arquivo em thread
                    Runnable fileRunnable = new FileRunnable( channel, destinationPrefix, sendTo, user, typeMime, command[1], source);
                    Thread sendFile = new Thread( fileRunnable );
                    sendFile.start();
                  } else {
                    System.out.println( "Atenção não há destinatário definido!" );
                  }
                  break;
                  
                default:
                  System.out.println( "Comando não encontrado !" );
                  break;               
              }
              break;
              
            default: 
              if ( !sendTo.isEmpty() ) {
                if ( destinationPrefix.equals("@") ) {
                // escrevendo mensagem na fila do rabitmq - usuario
                channel.basicPublish( "", ( sendTo + "Text" ), null, Message.serialize( user , ( destinationPrefix + sendTo ), "text/message", messageText, "" ) );
                } else {
                  // escrevendo mensagem no exchange do rabitmq - grupo
                  channel.basicPublish( ( sendTo + "Text" ), "", null, Message.serialize( user, ( destinationPrefix + sendTo ), "text/message", messageText, "" ) );
                }
              } else {
                System.out.println( "Destinatário não definido para envio da mensagem !");
              }
              toWait();
              break;
          }
        }

    }
  }

}