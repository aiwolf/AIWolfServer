package org.aiwolf.server.net;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.aiwolf.common.AIWolfAgentException;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.server.GameData;

/**
 * Connect player and server directry
 * @author tori
 * @deprecated
 */
public class DirectConnectServer implements GameServer {

	/**
	 * Agents connected to the server
	 */
	protected Map<Agent, Player> agentPlayerMap;
	
	/**
	 * Agents connected to the server
	 */
	protected Map<Player, Agent> playerAgentMap;

	
	protected Map<Agent, Role> requestRoleMap;
	

	/**
	 * GameData
	 */
	protected GameData gameData;
	
	/**
	 * Game Setting
	 */
	protected GameSetting gameSetting;
	
	public DirectConnectServer(List<Player> playerList){
		agentPlayerMap = new LinkedHashMap<Agent, Player>();
		playerAgentMap = new LinkedHashMap<Player, Agent>();
		int idx = 1;
		for(Player player:playerList){
			Agent agent = Agent.getAgent(idx++);
			agentPlayerMap.put(agent, player);
			playerAgentMap.put(player, agent);
		}
		requestRoleMap = new HashMap<Agent, Role>();

	}
	
	public DirectConnectServer(Map<Player, Role> playerMap){
		agentPlayerMap = new LinkedHashMap<Agent, Player>();
		playerAgentMap = new LinkedHashMap<Player, Agent>();
		requestRoleMap = new HashMap<Agent, Role>();

		int idx = 1;
		for(Player player:playerMap.keySet()){
			Agent agent = Agent.getAgent(idx++);
			agentPlayerMap.put(agent, player);
			playerAgentMap.put(player, agent);
			requestRoleMap.put(agent, playerMap.get(player));
		}
		

	}
	
	@Override
	public List<Agent> getConnectedAgentList() {
		return new ArrayList<Agent>(agentPlayerMap.keySet());
	}

	@Override
	public void setGameData(GameData gameData) {
		this.gameData = gameData;
	}
	
	@Override
	public void setGameSetting(GameSetting gameSetting){
		this.gameSetting = gameSetting;
	}


	@Override
	public void init(Agent agent) {
		try{
			agentPlayerMap.get(agent).initialize(gameData.getGameInfo(agent), gameSetting.clone());
		}catch(Throwable e){
			e.printStackTrace();
			throw new AIWolfAgentException(agent, "init", e);
		}
	}
	
	@Override
	public String requestName(Agent agent) {
		try{
			String name = agentPlayerMap.get(agent).getName();
			if(name != null){
				return name;
			}
			else{
				return agentPlayerMap.get(agent).getClass().getSimpleName();
			}
		}catch(Throwable e){
			throw new AIWolfAgentException(agent, "requestName", e);
		}
	}
	
	@Override
	public Role requestRequestRole(Agent agent) {
		try{
			return requestRoleMap.get(agent);
		}catch(Throwable e){
			throw new AIWolfAgentException(agent, "requestRequestRole", e);
		}
	}
	
	@Override
	public void dayStart(Agent agent) {
		try{
			agentPlayerMap.get(agent).update(gameData.getGameInfo(agent));
			agentPlayerMap.get(agent).dayStart();
		}catch(Throwable e){
			throw new AIWolfAgentException(agent, "dayStart", e);
		}
	}
	
	@Override
	public void dayFinish(Agent agent) {
		try{
			agentPlayerMap.get(agent).update(gameData.getGameInfo(agent));
	//		agentPlayerMap.get(agent).dayStart();
		}catch(Throwable e){
			throw new AIWolfAgentException(agent, "dayFinish", e);
		}
	}
	
	@Override
	public String requestTalk(Agent agent) {
		try{
			agentPlayerMap.get(agent).update(gameData.getGameInfo(agent));
			String talk = agentPlayerMap.get(agent).talk();
			return talk;
			//		if(talk == null){
	//			throw new NoReturnObjectException();
	//		}
	//		else{
	//			return talk;
	//		}
		}catch(Throwable e){
			throw new AIWolfAgentException(agent, "requestTalk", e);
		}
	}

	@Override
	public String requestWhisper(Agent agent) {
		try{
			agentPlayerMap.get(agent).update(gameData.getGameInfo(agent));
			String whisper = agentPlayerMap.get(agent).whisper();
			return whisper;
	//		if(whisper == null){
	//			throw new NoReturnObjectException();
	//		}
	//		else{
	//			return whisper;
	//		}
		}catch(Throwable e){
			throw new AIWolfAgentException(agent, "requestWhisper", e);
		}
	}

	@Override
	public Agent requestVote(Agent agent) {
		try{
			agentPlayerMap.get(agent).update(gameData.getGameInfo(agent));
			Agent target = agentPlayerMap.get(agent).vote();
			return target;
	//		if(target == null){
	//			throw new NoReturnObjectException();
	//		}
	//		else{
	//			return target;
	//		}
		}catch(Throwable e){
			throw new AIWolfAgentException(agent, "requestVote", e);
		}
	}

	@Override
	public Agent requestDivineTarget(Agent agent) {
		try{
			agentPlayerMap.get(agent).update(gameData.getGameInfo(agent));
			Agent target = agentPlayerMap.get(agent).divine();
			return target;
			//		if(target == null){
	//			throw new NoReturnObjectException();
	//		}
	//		else{
	//			return target;
	//		}
		}catch(Throwable e){
			throw new AIWolfAgentException(agent, "requestDivineTarget", e);
		}
	}

	@Override
	public Agent requestGuardTarget(Agent agent) {
		try{
			agentPlayerMap.get(agent).update(gameData.getGameInfo(agent));
			Agent target = agentPlayerMap.get(agent).guard();
			return target;
	//		if(target == null){
	//			throw new NoReturnObjectException();
	//		}
	//		else{
	//			return target;
	//		}
		}catch(Throwable e){
			throw new AIWolfAgentException(agent, "requestGuardTarget", e);
		}
	}

	@Override
	public Agent requestAttackTarget(Agent agent) {
		try{
			agentPlayerMap.get(agent).update(gameData.getGameInfo(agent));
			Agent target = agentPlayerMap.get(agent).attack();
			return target;
	//		if(target == null){
	//			throw new NoReturnObjectException();
	//		}
	//		else{
	//			return target;
	//		}
		}catch(Throwable e){
			throw new AIWolfAgentException(agent, "requestAttackTarget", e);
		}		
	}

	@Override
	public void finish(Agent agent){
		try{
			agentPlayerMap.get(agent).update(gameData.getFinalGameInfo(agent));
			agentPlayerMap.get(agent).finish();
		}catch(Throwable e){
			throw new AIWolfAgentException(agent, "finish", e);
		}
	}

	@Override
	public void close() {
	}

	public Agent getAgent(Player player) {
		return playerAgentMap.get(player);
	}

	
	public Player getPlayer(Agent agent){
		return agentPlayerMap.get(agent);
	}
	
	

	

}
