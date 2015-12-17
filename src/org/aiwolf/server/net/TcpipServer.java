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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//import net.arnx.jsonic.JSON;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Request;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.net.DataConverter;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.common.net.Packet;
import org.aiwolf.common.net.TalkToSend;
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
	GameSetting gameSetting;
	
	/**
	 * 
	 */
	Logger serverLogger;
	
	Map<Agent, Integer> lastTalkIdxMap;
	Map<Agent, Integer> lastWhisperIdxMap;
	

	/**
	 * 
	 * @param port
	 * @param limit
	 */
	public TcpipServer(int port, int limit, GameSetting gameSetting){
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
		
		lastTalkIdxMap = new HashMap<Agent, Integer>();
		lastWhisperIdxMap = new HashMap<Agent, Integer>();
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
	    
	    int idx = 1;
	    isWaitForClient = true;
	    while(socketAgentMap.size() < limit && isWaitForClient){
	        Socket socket = svsock.accept();
	        
	        synchronized (socketAgentMap) {
		        Agent agent = Agent.getAgent(idx++);
				socketAgentMap.put(socket, agent);
				
//				System.out.printf("Connect %s ( %d/%d )\n", agent, socketAgentMap.size(), limit);
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
			if(request == Request.DAILY_INITIALIZE || request == Request.INITIALIZE){
				lastTalkIdxMap.clear();
				lastWhisperIdxMap.clear();
				Packet packet = new Packet(request, gameData.getGameInfoToSend(agent), gameSetting);
				message = DataConverter.getInstance().convert(packet);
			}
			else if(request == Request.NAME || request == Request.ROLE){
				Packet packet = new Packet(request);
				message = DataConverter.getInstance().convert(packet);
			}
			else if(request != Request.FINISH){
//				Packet packet = new Packet(request, gameData.getGameInfoToSend(agent), gameSetting);
//				message = DataConverter.getInstance().convert(packet);
				List<TalkToSend> talkList = gameData.getGameInfoToSend(agent).getTalkList();
				List<TalkToSend> whisperList = gameData.getGameInfoToSend(agent).getWhisperList();
				
				talkList = minimize(agent, talkList, lastTalkIdxMap);
				whisperList = minimize(agent, whisperList, lastWhisperIdxMap);
				
				Packet packet = new Packet(request, talkList, whisperList);
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
	 * delete talks already sent
	 * @param agent
	 * @param list
	 * @param lastIdxMap
	 * @return
	 */
	private List<TalkToSend> minimize(Agent agent, List<TalkToSend> list, Map<Agent, Integer> lastIdxMap) {
		int lastIdx = list.size();
		if(lastIdxMap.containsKey(agent) && list.size() >= lastIdxMap.get(agent)){
			list = list.subList(lastIdxMap.get(agent), list.size());
		}
		lastIdxMap.put(agent, lastIdx);
		return list;
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

			if(line.isEmpty()){
				line = null;
			}
	        if(request == Request.TALK || request == Request.WHISPER || request == Request.NAME || request == Request.ROLE){
	        	return line;
	        }
	        else if(request == Request.ATTACK || request == Request.DIVINE || request == Request.GUARD || request == request.VOTE){
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
		send(agent, Request.INITIALIZE);
	}

	@Override
	public void dayStart(Agent agent) {
		send(agent, Request.DAILY_INITIALIZE);
	}

	@Override
	public void dayFinish(Agent agent){
		send(agent, Request.DAILY_FINISH);
	}

	@Override
	public String requestName(Agent agent) {
		return (String)request(agent, Request.NAME);
	}
	

	@Override
	public Role requestRequestRole(Agent agent) {
		String roleString = (String)request(agent, Request.ROLE);
		try{
			return Role.valueOf(roleString);
		}catch(IllegalArgumentException e){
			return null;
		}
	}
	
	
	@Override
	public String requestTalk(Agent agent) {
		return (String)request(agent, Request.TALK);
	}

	@Override
	public String requestWhisper(Agent agent) {
		return (String)request(agent, Request.WHISPER);
	}

	@Override
	public Agent requestVote(Agent agent) {
		return (Agent)request(agent, Request.VOTE);
//		return JSON.decode(result);
	}

	@Override
	public Agent requestDivineTarget(Agent agent) {
		return (Agent)request(agent, Request.DIVINE);
//		return JSON.decode(result);
	}

	@Override
	public Agent requestGuardTarget(Agent agent) {
		return (Agent)request(agent, Request.GUARD);
//		return JSON.decode(result);
	}

	@Override
	public Agent requestAttackTarget(Agent agent) {
		return (Agent)request(agent, Request.ATTACK);
//		return JSON.decode(result);
	}
	

	@Override
	public void finish(Agent agent) {
		send(agent, Request.FINISH);
		send(agent, Request.FINISH);
	}

	@Override
	public void setGameData(GameData gameData) {
		this.gameData = gameData;
	}
	

	@Override
	public void setGameSetting(GameSetting gameSetting) {
		this.gameSetting = gameSetting;
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
