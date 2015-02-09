package org.aiwolf.server.net;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.aiwolf.common.NoReturnObjectException;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.server.GameData;

/**
 * Connect player and server directry
 * @author tori
 *
 */
public class DirectConnectServer implements GameServer {

	/**
	 * Agents connected to the server
	 */
	Map<Agent, Player> agentPlayerMap;
	
	/**
	 * Agents connected to the server
	 */
	Map<Player, Agent> playerAgentMap;

	
	Map<Agent, Role> requestRoleMap;
	
	/**
	 * GameData
	 */
	GameData gameData;
	
	/**
	 * Game Setting
	 */
	GameSetting gameSetting;
	
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
		agentPlayerMap.get(agent).initialize(gameData.getGameInfo(agent), gameSetting);
	}
	
	@Override
	public String requestName(Agent agent) {
		String name = agentPlayerMap.get(agent).getName();
		if(name != null){
			return name;
		}
		else{
			return agentPlayerMap.get(agent).getClass().getSimpleName();
		}
	}
	
	@Override
	public Role requestRequestRole(Agent agent) {
		return requestRoleMap.get(agent);
	}
	
	@Override
	public void dayStart(Agent agent) {
		agentPlayerMap.get(agent).update(gameData.getGameInfo(agent));
		agentPlayerMap.get(agent).dayStart();
	}
	
	@Override
	public void dayFinish(Agent agent) {
		agentPlayerMap.get(agent).update(gameData.getGameInfo(agent));
//		agentPlayerMap.get(agent).dayStart();
	}
	
	@Override
	public String requestTalk(Agent agent) {
		agentPlayerMap.get(agent).update(gameData.getGameInfo(agent));
		String talk = agentPlayerMap.get(agent).talk();
		return talk;
		//		if(talk == null){
//			throw new NoReturnObjectException();
//		}
//		else{
//			return talk;
//		}
	}

	@Override
	public String requestWhisper(Agent agent) {
		agentPlayerMap.get(agent).update(gameData.getGameInfo(agent));
		String whisper = agentPlayerMap.get(agent).whisper();
		return whisper;
//		if(whisper == null){
//			throw new NoReturnObjectException();
//		}
//		else{
//			return whisper;
//		}
	}

	@Override
	public Agent requestVote(Agent agent) {
		agentPlayerMap.get(agent).update(gameData.getGameInfo(agent));
		Agent target = agentPlayerMap.get(agent).vote();
		return target;
//		if(target == null){
//			throw new NoReturnObjectException();
//		}
//		else{
//			return target;
//		}
	}

	@Override
	public Agent requestDivineTarget(Agent agent) {
		agentPlayerMap.get(agent).update(gameData.getGameInfo(agent));
		Agent target = agentPlayerMap.get(agent).divine();
		return target;
		//		if(target == null){
//			throw new NoReturnObjectException();
//		}
//		else{
//			return target;
//		}
	}

	@Override
	public Agent requestGuardTarget(Agent agent) {
		agentPlayerMap.get(agent).update(gameData.getGameInfo(agent));
		Agent target = agentPlayerMap.get(agent).guard();
		return target;
//		if(target == null){
//			throw new NoReturnObjectException();
//		}
//		else{
//			return target;
//		}
	}

	@Override
	public Agent requestAttackTarget(Agent agent) {
		agentPlayerMap.get(agent).update(gameData.getGameInfo(agent));
		Agent target = agentPlayerMap.get(agent).attack();
		return target;
//		if(target == null){
//			throw new NoReturnObjectException();
//		}
//		else{
//			return target;
//		}
		
	}

	@Override
	public void finish(Agent agent){
		agentPlayerMap.get(agent).update(gameData.getFinalGameInfo(agent));
		agentPlayerMap.get(agent).finish();
	}

	@Override
	public void close() {
	}

	public Agent getAgent(Player player) {
		return playerAgentMap.get(player);
	}


	

}
