package org.aiwolf.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.aiwolf.common.AIWolfRuntimeException;
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
import org.aiwolf.common.util.AiWolfLoggerFactory;
import org.aiwolf.common.util.Counter;
import org.aiwolf.server.net.GameServer;

/**
 * Game Class of AI Wolf Contest
 * @author tori
 *
 */
public class AIWolfGame {


	Random rand;

	/**
	 * Settings of the game
	 */
	GameSetting gameSetting;

	/**
	 * server to connect clients
	 */
	GameServer gameServer;

	/**
	 *
	 */
	Map<Integer, GameData> gameDataMap;

	/**
	 *
	 */
	GameData gameData;

	/**
	 * ログを記録するファイル
	 */
	File logFile;
	
	/**
	 * ログファイル
	 */
	Logger gameLogger;
	
	Map<Agent, String> agentNameMap;
	
	/**
	 *
	 */
	public AIWolfGame(GameSetting gameSeting, GameServer gameServer) {
		rand = new Random();
		this.gameSetting = gameSeting;
		this.gameServer = gameServer;
	}


	/**
	 * @return logFile
	 */
	public File getLogFile() {
		return logFile;
	}


	/**
	 * @param logFile セットする logFile
	 * @throws IOException 
	 */
	public void setLogFile(File logFile) throws IOException {
		this.logFile = logFile;
		gameLogger = AiWolfLoggerFactory.getGameLogger(logFile);
	}


	/**
	 * Set Random Class
	 * @param rand
	 */
	public void setRand(Random rand) {
		this.rand = rand;
	}

	/**
	 * Initialize Game
	 */
	public void init(){
		gameDataMap = new TreeMap<Integer, GameData>();
		gameData = new GameData(gameSetting);
		agentNameMap = new HashMap<Agent, String>();

		List<Agent> agentList = gameServer.getConnectedAgentList();

		if(agentList.size() != gameSetting.getPlayerNum()){
			throw new IllegalPlayerNumException("Player num is "+gameSetting.getPlayerNum()+" but connected agent is "+agentList.size());
		}

		Collections.shuffle(agentList, rand);

		Map<Role, List<Agent>> requestRoleMap = new HashMap<Role, List<Agent>>();
		for(Role role:Role.values()){
			requestRoleMap.put(role, new ArrayList<Agent>());
		}
		List<Agent> noRequestAgentList = new ArrayList<Agent>();
		for(Agent agent:agentList){
			Role requestedRole = gameServer.requestRequestRole(agent);
			if(requestedRole != null){
				requestRoleMap.get(requestedRole).add(agent);
				System.out.println(agent+" request "+requestedRole);
			}
			else{
				noRequestAgentList.add(agent);
				System.out.println(agent+" request no role");
			}
		}
		
		
		for(Role role:Role.values()){
			List<Agent> requestedAgentList = requestRoleMap.get(role);
			for(int i = 0; i < gameSetting.getRoleNum(role); i++){
				if(requestedAgentList.isEmpty()){
					gameData.addAgent(noRequestAgentList.remove(0), Status.alive, role);
				}
				else{
					gameData.addAgent(requestedAgentList.remove(0), Status.alive, role);
				}
			}
		}

		gameDataMap.put(gameData.getDay(), gameData);

		gameServer.setGameData(gameData);
		for(Agent agent:agentList){
			gameServer.init(agent);
			agentNameMap.put(agent, gameServer.requestName(agent));
		}
	}


	/**
	 * Start game
	 */
	public void start(){
		init();

//		System.out.printf("%d-%d\n", getAliveHumanList().size(), getAliveWolfList().size());
		while(!isGameFinished()){
			log();

			day();
			night();

		}
		log();
		finish();

		System.out.println("Winner:"+getWinner());

		for(Agent agent:gameData.getAgentList()){
			GameInfo gameInfo = gameData.getGameInfo(agent);
//			System.out.println(JSON.encode(gameInfo));
			break;
		}
		
	}

	public void finish(){
		for(Agent agent:new TreeSet<Agent>(gameData.getAgentList())){
			gameLogger.info(String.format("%d,status,%d,%s,%s,%s", gameData.getDay(), agent.getAgentIdx(),gameData.getRole(agent), gameData.getStatus(agent), agentNameMap.get(agent)));
		}
		gameLogger.info(String.format("%d,result,%d,%d,%s", gameData.getDay(),  getAliveHumanList().size(), getAliveWolfList().size(), getWinner()));
		
		for(Agent agent:gameData.getAgentList()){
			System.out.println("Send finish to "+agent);
			gameServer.finish(agent);
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get won team.
	 * if game not finished, return null
	 * @return
	 */
	public Team getWinner(){
		int humanSide = 0;
		int wolfSide = 0;
		for(Agent agent:gameData.getAgentList()){
			if(gameData.getStatus(agent) == Status.dead){
				continue;
			}
			if(gameData.getRole(agent).getSpecies() == Species.Human){
				humanSide++;
			}
			else{
				wolfSide++;
			}
		}
		if(wolfSide == 0){
			return Team.villager;
		}
		else if(humanSide <= wolfSide){
			return Team.werewolf;
		}
		else{
			return null;
		}

	}

	private void log() {
		GameData yesterday = gameData.getDayBefore();
		
		System.out.println("===========");
		System.out.printf("Day %02d\n", gameData.getDay());
		if(yesterday != null){
			Judge divine = yesterday.getDivine();
			System.out.printf("%s executed\n", yesterday.getExecuted());
			if(divine != null){
				System.out.printf("%s divine %s. Result is %s\n", divine.getAgent(), divine.getTarget(), divine.getResult());
			}
			Guard guard = yesterday.getGuard();
			if(guard != null){
				System.out.printf("%s guarded\n", guard);
			}
			System.out.printf("%s attacked\n", yesterday.getAttacked());		
		}		
		List<Agent> agentList = gameData.getAgentList();
		Collections.sort(agentList, new Comparator<Agent>() {
			@Override
			public int compare(Agent o1, Agent o2) {
				return o1.getAgentIdx()-o2.getAgentIdx();
			}
		});
		System.out.println("======");
		for(Agent agent:agentList){
			System.out.printf("%s\t%s\t%s\t%s", agent, agentNameMap.get(agent), gameData.getStatus(agent), gameData.getRole(agent));
			if(yesterday != null){
				if(yesterday.getExecuted() == agent){
					System.out.print("\texecuted");
				}
				if(yesterday.getAttacked() == agent){
					System.out.print("\tattacked");
				}
				Judge divine = yesterday.getDivine();
				if(divine != null && divine.getTarget() == agent){
					System.out.print("\tdivined");
				}
				Guard guard = yesterday.getGuard();
				if(guard != null && guard.getTarget() == agent){
					System.out.print("\tguarded");
				}
			}
			System.out.println();
		}
		System.out.printf("%d-%d\n", getAliveHumanList().size(), getAliveWolfList().size());
		if(yesterday != null){
			for(Talk talk:yesterday.getTalkList()){
				System.out.println(talk);
			}
		}
		if(yesterday != null){
			for(Talk whisper:yesterday.getWhisperList()){
				System.out.println(whisper);
			}
		}

		System.out.println("===========");
	}


	protected void day() {
		dayStart();
		talk();
		vote();
		divine();
		guard();
		attack();
	}

	/**
	 *
	 */
	protected void night() {

		//Vote
		
		List<Vote> voteList = gameData.getVoteList();
		Agent target = getVotedAgent(voteList);
		if(gameData.getStatus(target) == Status.alive){
			gameData.setExecuteTarget(target);
			gameLogger.info(String.format("%d,execute,%d,%s", gameData.getDay(), target.getAgentIdx(), gameData.getRole(target)));
		}
		
		
		//Attack
		if(!(getAliveWolfList().size() == 1 && gameData.getRole(gameData.getExecuted()) == Role.werewolf)){
			List<Vote> attackCandidateList = gameData.getAttackVoteList();
			Agent attacked = getAttackVotedAgent(attackCandidateList);
			
			if((gameData.getGuard() == null || !gameData.getGuard().getTarget().equals(attacked)) && attacked != null){
				gameData.setAttackedTarget(attacked);
				gameLogger.info(String.format("%d,attack,%d,true", gameData.getDay(), attacked.getAgentIdx()));
			}
			else if(attacked != null){
				gameLogger.info(String.format("%d,attack,%d,false", gameData.getDay(), attacked.getAgentIdx()));
			}
		}
		
		
		gameData = gameData.nextDay();
		gameDataMap.put(gameData.getDay(), gameData);
		gameServer.setGameData(gameData);
		
	}


	/**
	 *
	 * @param voteList
	 * @return
	 */
	protected Agent getVotedAgent(List<Vote> voteList) {
		Counter<Agent> counter = new Counter<Agent>();
		for(Vote vote:voteList){
			if(gameData.getStatus(vote.getTarget()) == Status.alive){
				counter.add(vote.getTarget());
			}
		}

		int max = counter.get(counter.getLargest());
		List<Agent> candidateList = new ArrayList<Agent>();
		for(Agent agent:counter){
			if(counter.get(agent) == max){
				candidateList.add(agent);
			}
		}

		if(candidateList.isEmpty()){
			return null;
		}
		else{
			Collections.shuffle(candidateList, rand);

			return candidateList.get(0);
		}
	}


	/**
	 *
	 * @param voteList
	 * @return
	 */
	protected Agent getAttackVotedAgent(List<Vote> voteList) {
		Counter<Agent> counter = new Counter<Agent>();
		for(Vote vote:voteList){
			if(gameData.getStatus(vote.getTarget()) == Status.alive && gameData.getRole(vote.getTarget()) != Role.werewolf){
				counter.add(vote.getTarget());
			}
		}
		if(!gameSetting.isEnableNoAttack()){
			for(Agent agent:getAliveHumanList()){
				counter.add(agent);
			}
		}
		
		int max = counter.get(counter.getLargest());
		List<Agent> candidateList = new ArrayList<Agent>();
		for(Agent agent:counter){
			if(counter.get(agent) == max){
				candidateList.add(agent);
			}
		}

		if(candidateList.isEmpty()){
			return null;
		}
		else{
			Collections.shuffle(candidateList, rand);

			return candidateList.get(0);
		}
	}
	/**
	 *
	 */
	protected void dayStart(){
		for(Agent agent:new TreeSet<Agent>(gameData.getAgentList())){
			gameLogger.info(String.format("%d,status,%d,%s,%s,%s", gameData.getDay(), agent.getAgentIdx(),gameData.getRole(agent), gameData.getStatus(agent), agentNameMap.get(agent)));
		}
		
		for(Agent agent:getAliveAgentList()){
			gameServer.dayStart(agent);
		}

	}

	/**
	 * First, all agents have chances to talk.
	 * Next, wolves whispers.
	 * Continue them until all agents finish talking
	 */
	protected void talk() {

		for(int i = 0; i < gameSetting.getMaxTalk(); i++){
			boolean continueTalk = false;

			List<Agent> alivelist = getAliveAgentList();
			Collections.shuffle(alivelist);
			for(Agent agent:alivelist){
				String talkContent = gameServer.requestTalk(agent);
				if(talkContent != null){
					if(!talkContent.isEmpty()){
						Talk sentence = new Talk(gameData.nextTalkIdx(), gameData.getDay(), agent, talkContent);
						gameData.addTalk(agent, sentence);
						if(!talkContent.equals(Talk.OVER)){
							continueTalk = true;
						}
						gameLogger.info(String.format("%d,talk,%d,%d,%s", gameData.getDay(), sentence.getIdx(),agent.getAgentIdx(), sentence.getContent()));
					}
				}
			}
			whisper();

			if(!continueTalk){
				break;
			}
		}
	}

	protected void whisper() {
		//Whisper by werewolf
		for(int j = 0; j < gameSetting.getMaxTalk(); j++){
			List<Agent> alivelist = getAliveAgentList();
			boolean continueWhisper = false;
			Collections.shuffle(alivelist);
			for(Agent agent:alivelist){
				if(gameData.getRole(agent) == Role.werewolf){
					String whisperContent = gameServer.requestWhisper(agent);
					if(!whisperContent.isEmpty()){
						Talk whisper = new Talk(gameData.nextWhisperIdx(), gameData.getDay(), agent, whisperContent);
						gameData.addWisper(agent, whisper);
						if(!whisperContent.equals(Talk.OVER)){
							continueWhisper = true;
						}
						gameLogger.info(String.format("%d,whisper,%d,%d,%s", gameData.getDay(), whisper.getIdx(),agent.getAgentIdx(), whisper.getContent()));
					}
				}
			}
			if(!continueWhisper){
				break;
			}
		}
	}

	/**
	 *
	 */
	protected void vote() {

		for(Agent agent:getAliveAgentList()){
			Agent target = gameServer.requestVote(agent);
			Vote vote = new Vote(gameData.getDay(), agent, target);
			gameData.addVote(vote);
			
			gameLogger.info(String.format("%d,vote,%d,%d", gameData.getDay(), vote.getAgent().getAgentIdx(), vote.getTarget().getAgentIdx()));

		}
	}


	protected void divine() {
		for(Agent agent:getAliveAgentList()){
			if(gameData.getRole(agent) == Role.seer){
				Agent target = gameServer.requestDivineTarget(agent);
				Judge divine = new Judge(gameData.getDay(), agent, target, gameData.getRole(target).getSpecies());
				gameData.addDivine(divine);

				gameLogger.info(String.format("%d,divine,%d,%d,%s", gameData.getDay(), divine.getAgent().getAgentIdx(), divine.getTarget().getAgentIdx(), divine.getResult()));
			}
		}
	}


	protected void guard() {
		for(Agent agent:getAliveAgentList()){
			if(gameData.getRole(agent) == Role.bodyguard){
				Agent target = gameServer.requestGuardTarget(agent);
				Guard guard = new Guard(gameData.getDay(), agent, target);
				gameData.addGuard(guard);
				
				gameLogger.info(String.format("%d,guard,%d,%d,%s", gameData.getDay(), guard.getAgent().getAgentIdx(), guard.getTarget().getAgentIdx(), gameData.getRole(guard.getTarget())));

			}
		}
	}


	protected void attack() {
		for(Agent agent:getAliveAgentList()){
			if(gameData.getRole(agent) == Role.werewolf){
				Agent target = gameServer.requestAttackTarget(agent);
				Vote attackVote = new Vote(gameData.getDay(), agent, target);
				gameData.addAttack(attackVote);

				gameLogger.info(String.format("%d,attackVote,%d,%d", gameData.getDay(), attackVote.getAgent().getAgentIdx(), attackVote.getTarget().getAgentIdx()));

			}
		}
	}

	/**
	 * get alive agents
	 * @return
	 */
	protected List<Agent> getAliveAgentList(){
		List<Agent> agentList = new ArrayList<Agent>();
		for(Agent agent:gameData.getAgentList()){
			if(gameData.getStatus(agent) == Status.alive){
				agentList.add(agent);
			}
		}
		return agentList;
	}

	protected List<Agent> getAliveHumanList(){
		return gameData.getFilteredAgentList(getAliveAgentList(), Species.Human);
	}

	protected List<Agent> getAliveWolfList(){
		return gameData.getFilteredAgentList(getAliveAgentList(), Species.Werewolf);
	}



	/**
	 * return is game finished
	 * @return
	 */
	public boolean isGameFinished() {
		Team winner = getWinner();
		return winner != null;
	}


	public GameData getGameData() {
		return gameData;
	}





}
