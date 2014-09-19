package org.aiwolf.server.net;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.common.net.GameSettingEntity;
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
	
	
	Map<Agent, Role> requestRoleMap;
	
	/**
	 * GameData
	 */
	GameData gameData;
	
	/**
	 * Game Setting
	 */
	GameSettingEntity gameSetting;
	
	public DirectConnectServer(List<Player> playerList){
		agentPlayerMap = new LinkedHashMap<Agent, Player>();
		int idx = 1;
		for(Player player:playerList){
			agentPlayerMap.put(Agent.getAgent(idx++), player);
		}
		requestRoleMap = new HashMap<Agent, Role>();
	}
	
	public DirectConnectServer(Map<Player, Role> playerMap){
		agentPlayerMap = new LinkedHashMap<Agent, Player>();
		requestRoleMap = new HashMap<Agent, Role>();

		int idx = 1;
		for(Player player:playerMap.keySet()){
			Agent agent = Agent.getAgent(idx++);
			agentPlayerMap.put(agent, player);
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
	public void setGameSetting(GameSettingEntity gameSetting){
		this.gameSetting = gameSetting;
	}


	@Override
	public void init(Agent agent) {
		agentPlayerMap.get(agent).initialize(gameData.getGameInfo(agent), gameSetting);
	}
	
	@Override
	public String requestName(Agent agent) {
		return agentPlayerMap.get(agent).getName();
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
		return agentPlayerMap.get(agent).talk();
	}

	@Override
	public String requestWhisper(Agent agent) {
		agentPlayerMap.get(agent).update(gameData.getGameInfo(agent));
		return agentPlayerMap.get(agent).whisper();
	}

	@Override
	public Agent requestVote(Agent agent) {
		agentPlayerMap.get(agent).update(gameData.getGameInfo(agent));
		return agentPlayerMap.get(agent).vote();
	}

	@Override
	public Agent requestDivineTarget(Agent agent) {
		agentPlayerMap.get(agent).update(gameData.getGameInfo(agent));
		return agentPlayerMap.get(agent).divine();
	}

	@Override
	public Agent requestGuardTarget(Agent agent) {
		agentPlayerMap.get(agent).update(gameData.getGameInfo(agent));
		return agentPlayerMap.get(agent).guard();

	}

	@Override
	public Agent requestAttackTarget(Agent agent) {
		agentPlayerMap.get(agent).update(gameData.getGameInfo(agent));
		return agentPlayerMap.get(agent).attack();
	}

	@Override
	public void finish(Agent agent){
		agentPlayerMap.get(agent).update(gameData.getFinalGameInfo(agent));
		agentPlayerMap.get(agent).finish();
	}

	@Override
	public void close() {
	}


	

}
