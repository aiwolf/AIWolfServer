package org.aiwolf.server;

import java.util.HashMap;
import java.util.Map;

import org.aiwolf.common.data.Role;

/**
 * Settings of game
 * @author tori
 *
 */
public class GameSetting {

	static private int[][] roleNumArray = {
		{0, 0, 1, 0, 1, 4, 2}, //8
		{0, 0, 1, 0, 1, 5, 2}, //9
		{1, 0, 1, 0, 1, 5, 2}, //10
		{1, 0, 1, 1, 1, 5, 2}, //11
		{1, 0, 1, 1, 1, 6, 2}, //12
		{1, 0, 1, 1, 1, 7, 2}, //13
		{1, 0, 1, 1, 1, 7, 3}, //14
		{1, 0, 1, 1, 1, 8, 3}, //15
		{1, 0, 1, 1, 1, 9, 3}, //16
	};
	
	static public GameSetting getDefaultGame(int agentNum){
		if(agentNum < 8){
			throw new IllegalArgumentException("agentNum must be bigger than 8 but "+agentNum);
		}
		if(agentNum > 16){
			throw new IllegalArgumentException("agentNum must be smaller than 16 but "+agentNum);
		}
		
		GameSetting setting = new GameSetting();
		setting.maxTalk = 10;
		setting.isEnableNoAttack = false;
		setting.isVoteVisible = true;
		
		Role[] roles = Role.values();
		for(int i = 0; i < roles.length; i++){
			setting.roleNumMap.put(roles[i], roleNumArray[agentNum-8][i]);
		}
		return setting;
		
	}
	
	
	/**
	 * number of each charactors
	 */
	Map<Role, Integer> roleNumMap;
	
	/**
	 * max number of talk;
	 */
	int maxTalk;
	
	/**
	 * Is the game permit to attack no one 
	 */
	boolean isEnableNoAttack;

	/**
	 * Can agents see who vote to who
	 */
	boolean isVoteVisible;
	
	public GameSetting(){
		roleNumMap = new HashMap<Role, Integer>();
	}
	
	/**
	 * get number of roles
	 * @param role
	 * @return
	 */
	public int getRoleNum(Role role){
		if(roleNumMap.containsKey(role)){
			return roleNumMap.get(role);
		}
		else{
			return 0;
		}
	}

	/**
	 * 
	 * @return
	 */
	public int getMaxTalk() {
		return maxTalk;
	}

	/**
	 * Is enable no attack
	 * @return
	 */
	public boolean isEnableNoAttack() {
		return isEnableNoAttack;
	}
	
	/**
	 * is vote visible
	 * @return
	 */
	public boolean isVoteVisible(){
		return isVoteVisible;
	}
	
	/**
	 * get total numbers of players
	 * @return
	 */
	public int getPlayerNum(){
		int num = 0;
		for(int value:roleNumMap.values()){
			num += value;
		}
		return num;
	}
	
	
}
