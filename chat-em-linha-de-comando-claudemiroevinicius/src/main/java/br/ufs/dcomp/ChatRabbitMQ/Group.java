package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import java.io.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Group { // comando utilizados nos grupos

  private Channel channel;
  private String user;
  
  
  // criar grupo: "!addGroup grupo"
  public static void addGroup( Channel channel, String group, String user ) {
    try {
  
      channel.exchangeDeclare( ( group + "Text" ), "fanout" ); // cria fila do grupo no chat
      channel.exchangeDeclare( ( group + "File" ), "fanout" ); // cria fila de arquivos 
      channel.queueBind( ( user +"Text" ), ( group + "Text" ), "" ); // inclui usuario logado na fila
      channel.queueBind( ( user + "File" ), ( group + "File" ), "" ); // inclui usuario logado na fila de arquivos
  
    } catch ( Exception e ) {
			e.printStackTrace();
		}
  }
  
  // apagar grupo: "!removeGroup grupo"
  public static void removeGroup( Channel channel, String group ) {
    try {
  
      channel.exchangeDelete( group + "Text" ); // exclui a fila do grupo
      channel.exchangeDelete( group + "File" ); // exclui a fila de arquivos do grupo
      System.out.println( "Grupo " + group + " foi finalizado !" );
      
    } catch ( Exception e ) {
			e.printStackTrace();
		}
  }

  // adicionar usuario ao grupo: "!addUser marcos grupo"
  public static void addUser( Channel channel, String sendTo, String group ) {
    try {
      
      channel.queueDeclare( ( sendTo + "Text" ), false, false, false, null ); // cria a fila do usuario, caso nao exista
      channel.queueBind( ( sendTo + "Text" ), ( group + "Text" ), "" ); // inclui usuario na exchange 
      channel.queueDeclare( ( sendTo + "File" ), false, false, false, null ); // cria a fila de arquivos do usuario, caso nao exista
      channel.queueBind( ( sendTo + "File" ), ( group + "File" ), "" ); // inclui usuario da exchange de arquivos
    } catch ( Exception e ) {
			e.printStackTrace();
		}
  }

  // excluir usuario do grupo: "!delFromGroup marcos grupo"
  public static void delFromGroup( Channel channel, String sendTo, String group ) {
    try {

      channel.queueUnbind( ( sendTo + "Text" ), ( group + "Text" ), "" ); // exclui usuario da fila do grupo
      channel.queueUnbind( ( sendTo + "File" ), ( group + "File"), "" ); // exclui usuario da fila de arquivos do grupo
      System.out.println( "Usuário " + sendTo + " foi removido do grupo " + group + " !" );
   
    } catch ( Exception e ) {
			e.printStackTrace();
		}
  }
  
    // Listar todos os usuários em grupo: "!listUsers ufs"
  public static void listUsers( String usernameAndPassword, String restResource, String group ) {
    try {
      
      String authorizationHeaderName = "Authorization";
      String authorizationHeaderValue = "Basic " + java.util.Base64.getEncoder().encodeToString( usernameAndPassword.getBytes() );
      
      // Perform a request
      Client client = ClientBuilder.newClient();
      Response answer = client.target( restResource )
        .path( "/api/exchanges/%2F/" + group + "Text/bindings/source" )
    	  .request( MediaType.APPLICATION_JSON )
        .header( authorizationHeaderName, authorizationHeaderValue ) // The basic authentication header goes here
        .get(); // Perform a post with the form values
   
      if (answer.getStatus() == 200) {
      	String json = answer.readEntity( String.class );
        
        String groupUsers = "";
        Pattern pattern = Pattern.compile( "(destination\":\")(.*?)(\",\"destination_type)" ); //extraindo das filas usando expressao regular
        Matcher queue = pattern.matcher(json);
        
        List<String> listQueues = new ArrayList<String>();

        while( queue.find() ) {
          listQueues.add( queue.group(2) );
        }

        for( String identifier : listQueues ) {
          groupUsers += identifier.replace( "Text", "" ) + ", "; //extraindo nome do usuario do nome da fila
        }
        System.out.println( groupUsers.substring( 0, groupUsers.length() - 2 ) );
      }

    } catch ( Exception e ) {
			e.printStackTrace();
		}
    
  }
  
  // Listar todos os grupos:"!listGroups"
  public static void listGroups( String usernameAndPassword, String restResource, String user ) {
    try {
      
      String authorizationHeaderName = "Authorization";
      String authorizationHeaderValue = "Basic " + java.util.Base64.getEncoder().encodeToString( usernameAndPassword.getBytes() );
      
      // Perform a request
      Client client = ClientBuilder.newClient();
      Response answer = client.target( restResource )
        .path( "/api/queues/%2F/" + user + "Text/bindings" )
    	  .request( MediaType.APPLICATION_JSON )
        .header( authorizationHeaderName, authorizationHeaderValue ) // The basic authentication header goes here
        .get(); // Perform a post with the form values
        
      if (answer.getStatus() == 200) {
      	String json = answer.readEntity( String.class );
        
        String groups = "";
        Pattern pattern = Pattern.compile( "(source\":\")(.*?)(\",)" ); //extraindo das filas usando expressao regular
        Matcher queue = pattern.matcher(json);
        
        List<String> listQueues = new ArrayList<String>();

        while( queue.find() ) {
          listQueues.add( queue.group(2) );
        }
        
        for( String identifier : listQueues ) {
          if ( identifier.endsWith( "Text" ) ) groups += identifier.replace( "Text", "" ) + ", "; //extraindo nome do grupo do nome da fila
        }
        
        System.out.println( groups.substring( 0, groups.length() - 2 ) );
        
      }

    } catch ( Exception e ) {
		  e.printStackTrace();
		}
    
  }
                  
}