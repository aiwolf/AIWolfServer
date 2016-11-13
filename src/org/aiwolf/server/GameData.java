package org.aiwolf.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Guard;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Status;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.data.Team;
import org.aiwolf.common.data.Vote;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameInfoToSend;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.common.net.JudgeToSend;
import org.aiwolf.common.net.TalkToSend;
import org.aiwolf.common.net.VoteToSend;

/**
 * Record game information of a day
 * @author tori
 *
 */
public class GameData {
	static final int firstDay = 1;

	/**
	 * The day of the data
	 */
	protected int day;
	
	/**
	 * status of each agents
	 */
	protected Map<Agent, Status> agentStatusMap;

	/**
	 * roles of each agents
	 */
	protected Map<Agent, Role> agentRoleMap;
	
	/**
	 * 
	 */
	protected List<Talk> talkList;
	
	/**
	 * 
	 */
	protected List<Talk> wisperList;

	/**
	 * 
	 */
	protected List<Vote> voteList;

	/**
	 * 
	 */
	protected List<Vote> attackCandidateList;
	
	
	/**
	 * Result divine
	 */
	protected Judge divine;
	
	/**
	 * Result divine
	 */
	protected Guard guard;

	/**
	 * agents who killed by villegers
	 */
	protected Agent executed;
	
	/**
	 * <div lang="ja">昨夜人狼が襲ったエージェント（成否は問わない）</div>
	 * 
	 * <div lang="en">the agent werewolves attacked last night (no matter
	 * whether or not the attack succeeded)</div>
	 */
	protected Agent attacked;

	/**
	 * <div lang="ja">昨夜死亡したエージェントのリスト</div>
	 * 
	 * <div lang="en">the list of agents died last night</div>
	 */
	// 妖狐導入されるまでは，実質的に襲撃死エージェントが入る
	protected List<Agent> lastDeadAgentList;

	/**
	 * agents who sudden death
	 */
	protected List<Agent> suddendeathList;

	/**
	 * game data of one day before
	 */
	protected GameData dayBefore;

	protected int talkIdx;

	protected int wisperIdx;

	
	/**
	 * ゲームの設定
	 */
	protected GameSetting gameSetting;
	
	protected GameData(GameSetting gameSetting){
		agentStatusMap = new LinkedHashMap<Agent, Status>();
		agentRoleMap = new HashMap<Agent, Role>();
		talkList = new ArrayList<Talk>();
		wisperList = new ArrayList<Talk>();
		voteList = new ArrayList<Vote>();
		attackCandidateList = new ArrayList<Vote>();
		lastDeadAgentList = new ArrayList<Agent>();
		suddendeathList = new ArrayList<Agent>();
		
		this.gameSetting = gameSetting;
	}

	/**
	 * get specific game information
	 * @param agent
	 * @return
	 */
	public GameInfo getGameInfo(Agent agent){
		return getGameInfoToSend(agent).toGameInfo();
	}

	/**
	 * get final game information
	 * @param agent
	 * @return
	 */
	public GameInfo getFinalGameInfo(Agent agent){
		return getFinalGameInfoToSend(agent).toGameInfo();
	}

	/**
	 * get game info with all information
	 * @return
	 */
	public GameInfo getGameInfo(){
		return getFinalGameInfo(null);
	}

	
	/**
	 * 
	 * @param agent if null, get all information
	 * @return
	 */
	public GameInfoToSend getGameInfoToSend(Agent agent){
		GameData today = this;
		GameInfoToSend gi = new GameInfoToSend();
		
		int day = today.getDay();
		if(agent != null){
			gi.setAgent(agent.getAgentIdx());
		}
		
		GameData yesterday = today.getDayBefore();

		if (yesterday != null) {
			Agent executed = yesterday.getExecuted();
			if(executed != null){
				gi.setExecutedAgent(executed.getAgentIdx());
			}

			Agent attacked = yesterday.getAttacked();
			if(attacked != null){
				gi.setAttackedAgent(attacked.getAgentIdx());
			}
			
			if(gameSetting.isVoteVisible()){
				List<VoteToSend> voteList = new ArrayList<VoteToSend>();
				for(Vote vote:yesterday.getVoteList()){
					voteList.add(new VoteToSend(vote));
				}
				gi.setVoteList(voteList);
			}
			
			if (agent != null && (today.getRole(agent).equals(Role.MEDIUM) && executed != null)) {
				Species result = yesterday.getRole(executed).getSpecies();
				gi.setMediumResult(new JudgeToSend(new Judge(day, agent, executed, result)));
			}

			if (agent == null || today.getRole(agent).equals(Role.SEER)) {
				Judge divine = yesterday.getDivine();
				if (divine != null && divine.getTarget() != null) {
					Species result = yesterday.getRole(divine.getTarget()).getSpecies();
					gi.setDivineResult(new JudgeToSend(new Judge(day, divine.getAgent(), divine.getTarget(), result)));
				}
			}

			if (agent == null || today.getRole(agent).equals(Role.WEREWOLF)) {
				List<VoteToSend> attackVoteList = new ArrayList<VoteToSend>();
				for(Vote vote:yesterday.getAttackVoteList()){
					attackVoteList.add(new VoteToSend(vote));
				}
				gi.setAttackVoteList(attackVoteList);
			}
			if(agent == null || today.getRole(agent).equals(Role.BODYGUARD)){
				Guard guard = yesterday.getGuard();
				if(guard != null){
					gi.setGuardedAgent(guard.getTarget().getAgentIdx());
				}
				
			}
		}
		List<TalkToSend> talkList = new ArrayList<TalkToSend>();
		for(Talk talk:today.getTalkList()){
			talkList.add(new TalkToSend(talk));
		}
		gi.setTalkList(talkList);

		LinkedHashMap<Integer, String> statusMap = new LinkedHashMap<Integer, String>();
		for(Agent a:agentStatusMap.keySet()){
			statusMap.put(a.getAgentIdx(), agentStatusMap.get(a).toString());
		}
		gi.setStatusMap(statusMap);
		
		LinkedHashMap<Integer, String> roleMap = new LinkedHashMap<Integer, String>();
		Role role = agentRoleMap.get(agent);

		if(Role.WEREWOLF.equals(role) || agent == null){
			List<TalkToSend> whisperList = new ArrayList<TalkToSend>();
			for(Talk talk:today.getWhisperList()){
				whisperList.add(new TalkToSend(talk));
			}
			gi.setWhisperList(whisperList);
		}
		
		if(role != null){
			roleMap.put(agent.getAgentIdx(), role.toString());
			if (today.getRole(agent).equals(Role.WEREWOLF)) {
//				List<TalkToSend> whisperList = new ArrayList<TalkToSend>();
//				for(Talk talk:today.getWhisperList()){
//					whisperList.add(new TalkToSend(talk));
//				}
//				gi.setWhisperList(whisperList);
				
				for (Agent target : today.getAgentList()) {
					if (today.getRole(target) == Role.WEREWOLF) {
						// wolfList.add(target);
						roleMap.put(target.getAgentIdx(), Role.WEREWOLF.toString());
					}
				}
			}
			if (today.getRole(agent).equals(Role.FREEMASON)) {
				for (Agent target : today.getAgentList()) {
					if (today.getRole(target) == Role.FREEMASON) {
						roleMap.put(target.getAgentIdx(), Role.FREEMASON.toString());
					}
				}
			}
		}
		gi.setRoleMap(roleMap);
		gi.setDay(day);

		return gi;
	}
	
	public GameInfoToSend getFinalGameInfoToSend(Agent agent) {
		GameInfoToSend gi = getGameInfoToSend(agent);

		LinkedHashMap<Integer, String> roleMap = new LinkedHashMap<Integer, String>();
		for(Agent a:agentRoleMap.keySet()){
			roleMap.put(a.getAgentIdx(), agentRoleMap.get(a).toString());
		}
		gi.setRoleMap(roleMap);
		
		return gi;
	}

	
	/**
	 * Add new agent with thier role
	 * 
	 * @param agent
	 * @param status
	 * @param role
	 */
	public void addAgent(Agent agent, Status status, Role role){
		agentRoleMap.put(agent, role);
		agentStatusMap.put(agent, status);
	}

	/**
	 * get agents
	 * @return
	 */
	public List<Agent> getAgentList() {
		return new ArrayList<Agent>(agentRoleMap.keySet());
	}

	/**
	 * get status of agent
	 * @param agent
	 */
	public Status getStatus(Agent agent) {
		return agentStatusMap.get(agent);
	}

	/**
	 * 
	 * @param agent
	 * @return
	 */
	public Role getRole(Agent agent) {
		return agentRoleMap.get(agent);
	}


	
	/**
	 * 
	 * @param agent
	 * @param talk
	 */
	public void addTalk(Agent agent, Talk talk) {
		talkList.add(talk);
	}
	
	public void addWisper(Agent agent, Talk wisper) {
		wisperList.add(wisper);
	}

	/**
	 * Add vote data
	 * 
	 * @param vote
	 */
	public void addVote(Vote vote) {
		voteList.add(vote);
	}

	/**
	 * Add divine
	 * 
	 * @param divine
	 */
	public void addDivine(Judge divine) {
		this.divine = divine;
	}

	public void addGuard(Guard guard) {
		this.guard = guard;
	}

	public void addAttack(Vote attack) {
		attackCandidateList.add(attack);
	}

	public List<Vote> getVoteList() {
		return voteList;
	}

	/**
	 * set executed 
	 * @param target
	 */
	public void setExecuteTarget(Agent target) {
		this.executed = target;
	}

	/**
	 * 
	 * @param attacked
	 */
	public void setAttackedTarget(Agent attacked) {
		this.attacked = attacked;
	}

	/**
	 * 
	 * @return
	 */
	public List<Vote> getAttackVoteList() {
		return attackCandidateList;
	}

	/**
	 * 
	 * @return
	 */
	public Guard getGuard() {
		return guard;
	}

	/**
	 * @return day
	 */
	public int getDay() {
		return day;
	}

	/**
	 * @return talkList
	 */
	public List<Talk> getTalkList() {
		return talkList;
	}

	/**
	 * @return wisperList
	 */
	public List<Talk> getWhisperList() {
		return wisperList;
	}

	/**
	 * @return divine
	 */
	public Judge getDivine() {
		return divine;
	}

	/**
	 * @return executed
	 */
	public Agent getExecuted() {
		return executed;
	}

	/**
	 * <div lang="ja">昨夜人狼が襲ったエージェント（成否は問わない）を返す</div>
	 * 
	 * <div lang="en">Returns the agent werewolves attacked last night (no
	 * matter whether or not the attack succeeded).</div>
	 * 
	 * @return attackedAgent - <div lang="ja">昨夜人狼が襲ったエージェント</div>
	 * 
	 *         <div lang="en">the agent werewolves attacked last night</div>
	 */
	public Agent getAttacked() {
		return attacked;
	}

	/**
	 * <div lang="ja">昨夜死亡したエージェントを追加する
	 * 
	 * </div> <div lang="en">Adds the agent died last night.</div>
	 * 
	 * @param agent
	 *            - <div lang="ja">追加するエージェント</div><div lang="en">the agent to
	 *            be added</div>
	 */
	public void addLastDeadAgent(Agent agent) {
		if (!lastDeadAgentList.contains(agent)) {
			lastDeadAgentList.add(agent);
		}
	}

	/**
	 * @return <div lang="ja">昨夜死亡したエージェントのリスト</div> <div lang="en">the list of agents died last night</div>
	 */
	public List<Agent> getLastDeadAgentList() {
		return lastDeadAgentList;
	}

	/**
	 * @return suddendeathList
	 */
	public List<Agent> getSuddendeathList() {
		return suddendeathList;
	}

	/**
	 * Create GameData of next day
	 * @return
	 */
	protected GameData nextDay(){
		GameData gameData = new GameData(gameSetting);
		
		gameData.day = this.day+1;
		gameData.agentStatusMap = new HashMap<Agent, Status>(agentStatusMap);
		if(executed != null){
			gameData.agentStatusMap.put(executed, Status.DEAD);
		}

		for (Agent a : lastDeadAgentList) {
			gameData.agentStatusMap.put(a, Status.DEAD);
		}
		gameData.agentRoleMap = new HashMap<Agent, Role>(agentRoleMap);
		
		gameData.dayBefore = this;
		
		return gameData;
	}
	
	/**
	 * get game data of one day before
	 * @return
	 */
	public GameData getDayBefore() {
		return dayBefore;
	}
	
//	/**
//	 * get wolf agents
//	 * @return
//	 */
//	public List<Agent> getWolfList(){
//		List<Agent> wolfList = new ArrayList<>();
//		for(Agent agent:getAgentList()){
//			if(getRole(agent).getSpecies() == Species.Werewolf){
//				wolfList.add(agent);
//			}
//		}
//		return wolfList;
//	}
//	
//	/**
//	 * get human agents
//	 * @return
//	 */
//	public List<Agent> getHumanList(){
//		List<Agent> humanList = new ArrayList<>(getAgentList());
//		humanList.removeAll(getWolfList());
//		return humanList;
//	}
	
	protected List<Agent> getFilteredAgentList(List<Agent> agentList, Species species){
		List<Agent> resultList = new ArrayList<Agent>();
		for(Agent agent:agentList){
			if(getRole(agent).getSpecies() == species){
				resultList.add(agent);
			}
		}
		return resultList;
	}
	
	protected List<Agent> getFilteredAgentList(List<Agent> agentList, Status status){
		List<Agent> resultList = new ArrayList<Agent>();
		for(Agent agent:agentList){
			if(getStatus(agent) == status){
				resultList.add(agent);
			}
		}
		return resultList;
	}

	protected List<Agent> getFilteredAgentList(List<Agent> agentList, Role role){
		List<Agent> resultList = new ArrayList<Agent>();
		for(Agent agent:agentList){
			if(getRole(agent) == role){
				resultList.add(agent);
			}
		}
		return resultList;
	}

	protected List<Agent> getFilteredAgentList(List<Agent> agentList, Team team){
		List<Agent> resultList = new ArrayList<Agent>();
		for(Agent agent:agentList){
			if(getRole(agent).getTeam() == team){
				resultList.add(agent);
			}
		}
		return resultList;
	}

	
	

	public int nextTalkIdx() {
		return talkIdx++;
	}

	public int nextWhisperIdx() {
		return wisperIdx++;
	}


}
