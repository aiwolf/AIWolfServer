package org.aiwolf.server.net;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//import net.arnx.jsonic.JSON;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Request;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.net.DataConverter;
import org.aiwolf.common.net.GameSettingEntity;
import org.aiwolf.common.net.Packet;
import org.aiwolf.common.util.AiWolfLoggerFactory;
import org.aiwolf.common.util.BidiMap;
import org.aiwolf.server.GameData;
import org.aiwolf.server.LostClientException;

/**
 * 
 * @author tori
 *
 */
public class TcpipServer implements GameServer {

	
	/**
	 * Server Port
	 */
	int port;
	
	/**
	 * connection limit
	 */
	int limit;
	
	/**
	 * 
	 */
	boolean isWaitForClient;
	
	/**
	 * 
	 */
	BidiMap<Socket, Agent> socketAgentMap;
	
	
	/**
	 * Current game data
	 */
	GameData gameData;
	
	/**
	 * Game Setting
	 */
	GameSettingEntity gameSetting;
	
	/**
	 * 
	 */
	Logger serverLogger;
	

	/**
	 * 
	 * @param port
	 * @param limit
	 */
	public TcpipServer(int port, int limit, GameSettingEntity gameSetting){
		this.gameSetting = gameSetting;
		this.port = port;
		this.limit = limit;
		
		socketAgentMap = new BidiMap<Socket, Agent>();
		String loggerName = this.getClass().getSimpleName();
		serverLogger = Logger.getLogger(loggerName);
//		serverLogger = AiWolfLoggerFactory.getLogger(loggerName);
//		serverLogger.setLevel(Level.FINER);
		//		try {
//			serverLogger = AiWolfLoggerFactory.getLogger(loggerName, new File(loggerName+".log"));
//			serverLogger.setLevel(Level.FINER);
//		} catch (IOException e) {
//			e.printStackTrace();
//			serverLogger = AiWolfLoggerFactory.getLogger(loggerName);
//		}
	}
	
	/**
	 * 
	 * @throws IOException
	 * @throws SocketTimeoutException
	 */
	public void waitForConnection() throws IOException, SocketTimeoutException{
//	    int timeout_msec = 1000*60*10; // time out in 10 miniutes
//		serverLogger.info(String.format("Start Server@port %d\n", port));
		
	    for(Socket sock:socketAgentMap.keySet()){
	    	if(sock != null && sock.isConnected()){
	    		sock.close();
	    	}
	    }
	    socketAgentMap.clear();
	    // サーバソケットの作成

//		serverLogger.info(String.format("Waiting for connection...\n"));
	    System.out.println("Waiting for connection...\n");
	    
	    ServerSocket svsock = new ServerSocket(port);
	    
	    int idx = 0;
	    isWaitForClient = true;
	    while(socketAgentMap.size() < limit && isWaitForClient){
	        Socket socket = svsock.accept();
	        
	        synchronized (socketAgentMap) {
		        Agent agent = Agent.getAgent(idx++);
				socketAgentMap.put(socket, agent);
				
				System.out.printf("Connect %s ( %d/%d )\n", agent, socketAgentMap.size(), limit);
				serverLogger.info(String.format("Connect %s ( %d/%d )", agent, socketAgentMap.size(), limit));
			}
	    }
	    svsock.close();
	}
	
	
	
	@Override
	public List<Agent> getConnectedAgentList() {
		synchronized (socketAgentMap) {
			return new ArrayList<Agent>(socketAgentMap.values());
		}
	}

	/**
	 * send data to client
	 * @param agent
	 * @param sendText
	 */
	protected void send(Agent agent, Request request){
		try{
			String message;
			if(request != Request.Finish){
				Packet packet = new Packet(request, gameData.getGameInfoToSend(agent));
				message = DataConverter.getInstance().convert(packet);
			}
			else{
				Packet packet = new Packet(request, gameData.getFinalGameInfoToSend(agent));
				message = DataConverter.getInstance().convert(packet);
			}
			serverLogger.info("=>"+agent+":"+message);

			Socket sock = socketAgentMap.getKey(agent);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
			bw.append(message);
			bw.append("\n");
    		bw.flush();
		}catch(IOException e){
//			serverLogger.severe(e.getMessage());
			throw new LostClientException(e);
		}
	}

	/**
	 * send data to client
	 * @param agent
	 * @param sendText
	 */
	protected Object request(Agent agent, Request request){
		try{
			Socket sock = socketAgentMap.getKey(agent);
			BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			send(agent, request);
			
	        String line = br.readLine();
			serverLogger.info("<="+agent+":"+line);

	        if(request == Request.Talk || request == Request.Whisper || request == Request.Name || request == Request.Role){
	        	return line;
	        }
	        else if(request == Request.Attack || request == Request.Divine || request == Request.Guard || request == request.Vote){
	        	return DataConverter.getInstance().toAgent(line);
	        }
	        else{
	        	return null;
	        }
	        		
			
		}catch(IOException e){
			throw new LostClientException("Lost connection with "+agent, e);
		}
	}
	
	@Override
	public void init(Agent agent) {
		send(agent, Request.Initialize);
	}


	@Override
	public String requestName(Agent agent) {
		return (String)request(agent, Request.Name);
	}
	

	@Override
	public Role requestRequestRole(Agent agent) {
		String roleString = (String)request(agent, Request.Role);
		try{
			return Role.valueOf(roleString);
		}catch(IllegalArgumentException e){
			return null;
		}
	}
	
	
	@Override
	public String requestTalk(Agent agent) {
		return (String)request(agent, Request.Talk);
	}

	@Override
	public String requestWhisper(Agent agent) {
		return (String)request(agent, Request.Whisper);
	}

	@Override
	public Agent requestVote(Agent agent) {
		return (Agent)request(agent, Request.Vote);
//		return JSON.decode(result);
	}

	@Override
	public Agent requestDivineTarget(Agent agent) {
		return (Agent)request(agent, Request.Divine);
//		return JSON.decode(result);
	}

	@Override
	public Agent requestGuardTarget(Agent agent) {
		return (Agent)request(agent, Request.Guard);
//		return JSON.decode(result);
	}

	@Override
	public Agent requestAttackTarget(Agent agent) {
		return (Agent)request(agent, Request.Attack);
//		return JSON.decode(result);
	}
	

	@Override
	public void finish(Agent agent) {
		send(agent, Request.Finish);
		send(agent, Request.Finish);
	}

	@Override
	public void setGameData(GameData gameData) {
		this.gameData = gameData;
	}
	

	@Override
	public void setGameSetting(GameSettingEntity gameSetting) {
		this.gameSetting = gameSetting;
	}


	@Override
	public void dayStart(Agent agent) {
		send(agent, Request.DailyInitialize);
	}

	/**
	 * @return isWaitForClient
	 */
	public boolean isWaitForClient() {
		return isWaitForClient;
	}

	/**
	 * @param isWaitForClient セットする isWaitForClient
	 */
	public void setWaitForClient(boolean isWaitForClient) {
		this.isWaitForClient = isWaitForClient;
	}

	@Override
	public void close(){
		for(Socket socket:socketAgentMap.keySet()){
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @return serverLogger
	 */
	public Logger getServerLogger() {
		return serverLogger;
	}

	/**
	 * @param serverLogger セットする serverLogger
	 */
	public void setServerLogger(Logger serverLogger) {
		this.serverLogger = serverLogger;
	}

	
}
