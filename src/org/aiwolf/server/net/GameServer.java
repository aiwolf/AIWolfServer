package org.aiwolf.server.net;

import java.util.List;


import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.server.GameData;

public interface GameServer {
	
	/**
	 * 
	 * @return
	 */
	List<Agent> getConnectedAgentList();


	/**
	 * 
	 * @param agent
	 */
	void init(Agent agent);
	
	/**
	 * Request agent's name
	 * @param agent
	 * @return
	 */
	String requestName(Agent agent);
	
	/**
	 * Request roles that agent request
	 * @param agent
	 * @return
	 */
	Role requestRequestRole(Agent agent);
	
	/**
	 * 
	 * @param agent
	 * @return
	 */
	String requestTalk(Agent agent);

	/**
	 * 
	 * @param agent
	 * @return
	 */
	String requestWhisper(Agent agent);
	

	/**
	 * 
	 * @param agent
	 * @return
	 */
	Agent requestVote(Agent agent);
	
	
	/**
	 * 
	 * @param agent
	 * @return
	 */
	Agent requestDivineTarget(Agent agent);
	
	/**
	 * 
	 * @param agent
	 * @return
	 */
	Agent requestGuardTarget(Agent agent);
	
	
	/**
	 * 
	 * @param agent
	 * @return
	 */
	Agent requestAttackTarget(Agent agent);

	/**
	 * 
	 * @param gameInfo
	 */
	void setGameData(GameData gameData);

	/**
	 * called when day started
	 * @param agent
	 */
	void dayStart(Agent agent);

	/**
	 * send finished message
	 */
	void finish(Agent agent);

	/**
	 * close connections
	 */
	void close();
	
}
