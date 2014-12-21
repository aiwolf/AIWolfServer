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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.LogManager;
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
import org.aiwolf.common.net.GameSettingEntity;
import org.aiwolf.common.util.AiWolfLoggerFactory;
import org.aiwolf.common.util.Counter;
import org.aiwolf.server.net.GameServer;
import org.aiwolf.server.util.GameLogger;

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
	GameSettingEntity gameSetting;

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
	 * Show console log?
	 */
	boolean isShowConsoleLog = true;


	/**
	 * ログを記録するファイル
	 */
	File logFile;

	/**
	 * ログファイル
	 */
	GameLogger gameLogger;

	/**
	 * Name of Agents
	 */
	Map<Agent, String> agentNameMap;

	/**
	 *
	 */
	public AIWolfGame(GameSettingEntity gameSeting, GameServer gameServer) {
		rand = new Random();
		this.gameSetting = gameSeting;
		this.gameServer = gameServer;
//		gameLogger = AiWolfLoggerFactory.getSimpleLogger(this.getClass().getSimpleName());
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
		gameLogger = new GameLogger(logFile);
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
	protected void init(){
		gameDataMap = new TreeMap<Integer, GameData>();
		gameData = new GameData(gameSetting);
		agentNameMap = new HashMap<Agent, String>();
		gameServer.setGameData(gameData);

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
				if(requestRoleMap.get(requestedRole).size() < gameSetting.getRoleNum(requestedRole)){
					requestRoleMap.get(requestedRole).add(agent);
				}
				else{
					noRequestAgentList.add(agent);
				}
//				System.out.println(agent+" request "+requestedRole);
			}
			else{
				noRequestAgentList.add(agent);
//				System.out.println(agent+" request no role");
			}
		}


		for(Role role:Role.values()){
			List<Agent> requestedAgentList = requestRoleMap.get(role);
			for(int i = 0; i < gameSetting.getRoleNum(role); i++){
				if(requestedAgentList.isEmpty()){
					gameData.addAgent(noRequestAgentList.remove(0), Status.ALIVE, role);
				}
				else{
					gameData.addAgent(requestedAgentList.remove(0), Status.ALIVE, role);
				}
			}
		}

		gameDataMap.put(gameData.getDay(), gameData);

		gameServer.setGameData(gameData);
		gameServer.setGameSetting(gameSetting);
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
			if(gameLogger != null){
				gameLogger.flush();
			}
		}
		log();
		finish();

		if(isShowConsoleLog){
			System.out.println("Winner:"+getWinner());
		}
//		for(Agent agent:gameData.getAgentList()){
//			GameInfo gameInfo = gameData.getGameInfo(agent);
////			System.out.println(JSON.encode(gameInfo));
//			break;
//		}

	}

	public void finish(){
		if(gameLogger != null){
			for(Agent agent:new TreeSet<Agent>(gameData.getAgentList())){
				gameLogger.log(String.format("%d,status,%d,%s,%s,%s", gameData.getDay(), agent.getAgentIdx(),gameData.getRole(agent), gameData.getStatus(agent), agentNameMap.get(agent)));
			}
			gameLogger.log(String.format("%d,result,%d,%d,%s", gameData.getDay(),  getAliveHumanList().size(), getAliveWolfList().size(), getWinner()));
			gameLogger.close();
		}

		for(Agent agent:gameData.getAgentList()){
//			System.out.println("Send finish to "+agent);
			gameServer.finish(agent);
		}
/*		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}*/
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
			if(gameData.getStatus(agent) == Status.DEAD){
				continue;
			}
			if(gameData.getRole(agent).getSpecies() == Species.HUMAN){
				humanSide++;
			}
			else{
				wolfSide++;
			}
		}
		if(wolfSide == 0){
			return Team.VILLAGER;
		}
		else if(humanSide <= wolfSide){
			return Team.WEREWOLF;
		}
		else{
			return null;
		}

	}

	private void log() {

		if(!isShowConsoleLog){
			return;
		}

		GameData yesterday = gameData.getDayBefore();

		System.out.println("=============================================");
		if(yesterday != null){
			System.out.printf("Day %02d\n", yesterday.getDay());
			System.out.println("========talk========");
			for(Talk talk:yesterday.getTalkList()){
				System.out.println(talk);
			}
			System.out.println("========Whisper========");
			for(Talk whisper:yesterday.getWhisperList()){
				System.out.println(whisper);
			}

			System.out.println("========Actions========");
			for(Vote vote:yesterday.getVoteList()){
				System.out.printf("Vote:%s->%s\n", vote.getAgent(), vote.getTarget());
			}

//			System.out.println("Attack Vote Result");
			for(Vote vote:yesterday.getAttackVoteList()){
				System.out.printf("AttackVote:%s->%s\n", vote.getAgent(), vote.getTarget());
			}

			Judge divine = yesterday.getDivine();
			System.out.printf("%s executed\n", yesterday.getExecuted());
			if(divine != null){
				System.out.printf("%s divine %s. Result is %s\n", divine.getAgent(), divine.getTarget(), divine.getResult());
			}
			Guard guard = yesterday.getGuard();
			if(guard != null){
				System.out.printf("%s guarded\n", guard);
			}
			Agent attacked = yesterday.getAttacked();
			if(attacked != null){
				System.out.printf("%s attacked\n", attacked);
			}
		}
		System.out.println("======");
		List<Agent> agentList = gameData.getAgentList();
		Collections.sort(agentList, new Comparator<Agent>() {
			@Override
			public int compare(Agent o1, Agent o2) {
				return o1.getAgentIdx()-o2.getAgentIdx();
			}
		});
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
		System.out.printf("Human:%d\nWerewolf:%d\n", getAliveHumanList().size(), getAliveWolfList().size());

		System.out.println("=============================================");
	}


	protected void day() {
		dayStart();
		talk();

		if(gameData.getDay() != 0){
			vote();
		}
		divine();
		if(gameData.getDay() != 0){
			guard();
			attack();
		}
	}

	/**
	 *
	 */
	protected void night() {

		for(Agent agent:gameData.getAgentList()){
			gameServer.dayFinish(agent);
		}


		//Vote

		List<Vote> voteList = gameData.getVoteList();
		Agent executed = getVotedAgent(voteList);
		if(gameData.getStatus(executed) == Status.ALIVE  && gameData.getDay() != 0){
			gameData.setExecuteTarget(executed);
			if(gameLogger != null){
				gameLogger.log(String.format("%d,execute,%d,%s", gameData.getDay(), executed.getAgentIdx(), gameData.getRole(executed)));
			}
		}

		//Attack
		if(!(getAliveWolfList().size() == 1 && gameData.getRole(gameData.getExecuted()) == Role.WEREWOLF) && gameData.getDay() != 0){
			List<Vote> attackCandidateList = gameData.getAttackVoteList();
			Iterator<Vote> it = attackCandidateList.iterator();
			while(it.hasNext()){
				Vote vote = it.next();
				if(vote.getAgent() == executed){
					it.remove();
				}
			}

			Agent attacked = getAttackVotedAgent(attackCandidateList);
			if(attacked == executed){
				attacked = null;
			}

			if((gameData.getGuard() == null || !gameData.getGuard().getTarget().equals(attacked)) && attacked != null){
				gameData.setAttackedTarget(attacked);
				if(gameLogger != null){
					gameLogger.log(String.format("%d,attack,%d,true", gameData.getDay(), attacked.getAgentIdx()));
				}
			}
			else if(attacked != null){
				if(gameLogger != null){
					gameLogger.log(String.format("%d,attack,%d,false", gameData.getDay(), attacked.getAgentIdx()));
				}
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
			if(gameData.getStatus(vote.getTarget()) == Status.ALIVE){
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
			if(gameData.getStatus(vote.getTarget()) == Status.ALIVE && gameData.getRole(vote.getTarget()) != Role.WEREWOLF){
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
		if(gameLogger != null){
			for(Agent agent:new TreeSet<Agent>(gameData.getAgentList())){
				gameLogger.log(String.format("%d,status,%d,%s,%s,%s", gameData.getDay(), agent.getAgentIdx(),gameData.getRole(agent), gameData.getStatus(agent), agentNameMap.get(agent)));
			}
		}

		for(Agent agent:getGameData().getAgentList()){
			gameServer.dayStart(agent);
		}

	}

	/**
	 * First, all agents have chances to talk.
	 * Next, wolves whispers.
	 * Continue them until all agents finish talking
	 */
	protected void talk() {

		Set<Agent> overSet = new HashSet<Agent>();
		for(int i = 0; i < gameSetting.getMaxTalk(); i++){
			boolean continueTalk = false;

			List<Agent> aliveList = getAliveAgentList();
			Collections.shuffle(aliveList);
			aliveList.removeAll(overSet);
			aliveList.addAll(overSet);
			for(Agent agent:aliveList){
				if(overSet.contains(agent)){
					continue;
				}
				String talkContent = gameServer.requestTalk(agent);
				if(talkContent != null){
					if(!talkContent.isEmpty()){
						Talk sentence = new Talk(gameData.nextTalkIdx(), gameData.getDay(), agent, talkContent);
						gameData.addTalk(agent, sentence);
						if(!talkContent.equals(Talk.OVER)){
							continueTalk = true;
							overSet.clear();
						}
						else{
							overSet.add(agent);
						}
						if(gameLogger != null){
							gameLogger.log(String.format("%d,talk,%d,%d,%s", gameData.getDay(), sentence.getIdx(),agent.getAgentIdx(), sentence.getContent()));
						}
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
		Set<Agent> overSet = new HashSet<Agent>();
		for(int j = 0; j < gameSetting.getMaxTalk(); j++){
			List<Agent> aliveList = getAliveAgentList();
			boolean continueWhisper = false;
			Collections.shuffle(aliveList);
			aliveList.removeAll(overSet);
			aliveList.addAll(overSet);
			for(Agent agent:aliveList){
				if(gameData.getRole(agent) == Role.WEREWOLF){
					if(overSet.contains(agent)){
						continue;
					}
					String whisperContent = gameServer.requestWhisper(agent);
					if(whisperContent != null && !whisperContent.isEmpty()){
						Talk whisper = new Talk(gameData.nextWhisperIdx(), gameData.getDay(), agent, whisperContent);
						gameData.addWisper(agent, whisper);
						if(!whisperContent.equals(Talk.OVER)){
							continueWhisper = true;
							overSet.clear();
						}
						else{
							overSet.add(agent);
						}

						if(gameLogger != null){
							gameLogger.log(String.format("%d,whisper,%d,%d,%s", gameData.getDay(), whisper.getIdx(),agent.getAgentIdx(), whisper.getContent()));
						}
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

		List<Agent> agentList = getAliveAgentList();
		for(Agent agent:getAliveAgentList()){
			Agent target = gameServer.requestVote(agent);
			if(gameData.getStatus(target) == Status.DEAD || target == null){
				target = getRandomAgent(agentList, agent);
			}
			Vote vote = new Vote(gameData.getDay(), agent, target);
			gameData.addVote(vote);

			if(gameLogger != null){
				gameLogger.log(String.format("%d,vote,%d,%d", gameData.getDay(), vote.getAgent().getAgentIdx(), vote.getTarget().getAgentIdx()));
			}
		}
	}

	protected void divine() {
		List<Agent> agentList = getAliveAgentList();
		for(Agent agent:getAliveAgentList()){
			if(gameData.getRole(agent) == Role.SEER){
				Agent target = gameServer.requestDivineTarget(agent);
				if(gameData.getStatus(target) == Status.DEAD || target == null){
					target = getRandomAgent(agentList, agent);
				}

				Judge divine = new Judge(gameData.getDay(), agent, target, gameData.getRole(target).getSpecies());
				gameData.addDivine(divine);

				if(gameLogger != null){
					gameLogger.log(String.format("%d,divine,%d,%d,%s", gameData.getDay(), divine.getAgent().getAgentIdx(), divine.getTarget().getAgentIdx(), divine.getResult()));
				}
			}
		}
	}


	protected void guard() {
		List<Agent> agentList = getAliveAgentList();
		for(Agent agent:getAliveAgentList()){
			if(gameData.getRole(agent) == Role.BODYGUARD){
				Agent target = gameServer.requestGuardTarget(agent);
				if(gameData.getStatus(target) == Status.DEAD || target == null){
					target = getRandomAgent(agentList, agent);
				}
				Guard guard = new Guard(gameData.getDay(), agent, target);
				gameData.addGuard(guard);

				if(gameLogger != null){
					gameLogger.log(String.format("%d,guard,%d,%d,%s", gameData.getDay(), guard.getAgent().getAgentIdx(), guard.getTarget().getAgentIdx(), gameData.getRole(guard.getTarget())));
				}
			}
		}
	}


	protected void attack() {
		List<Agent> agentList = getAliveAgentList();
		Iterator<Agent> it = agentList.iterator();
		while(it.hasNext()){
			Agent agent = it.next();
			if(gameData.getRole(agent) == Role.WEREWOLF){
				it.remove();
			}
		}

		for(Agent agent:getAliveAgentList()){
			if(gameData.getRole(agent) == Role.WEREWOLF){
				Agent target = gameServer.requestAttackTarget(agent);
				if(gameData.getStatus(target) == Status.DEAD || gameData.getRole(target) == Role.WEREWOLF || target == null){
					target = getRandomAgent(agentList, agent);
				}
				Vote attackVote = new Vote(gameData.getDay(), agent, target);
				gameData.addAttack(attackVote);

				if(gameLogger != null){
					gameLogger.log(String.format("%d,attackVote,%d,%d", gameData.getDay(), attackVote.getAgent().getAgentIdx(), attackVote.getTarget().getAgentIdx()));
				}
			}
		}
	}


	/**
	 * ランダムなエージェントを獲得する．ただし，agentを除く．
	 * @param agentList
	 * @param agent
	 * @return
	 */
	protected Agent getRandomAgent(List<Agent> agentList, Agent agent) {
		Agent target;
		boolean removed = agentList.remove(agent);
		target = agentList.get(rand.nextInt(agentList.size()));
		if(removed){
			agentList.add(agent);
		}
		return target;
	}

	/**
	 * get alive agents
	 * @return
	 */
	protected List<Agent> getAliveAgentList(){
		List<Agent> agentList = new ArrayList<Agent>();
		for(Agent agent:gameData.getAgentList()){
			if(gameData.getStatus(agent) == Status.ALIVE){
				agentList.add(agent);
			}
		}
		return agentList;
	}

	protected List<Agent> getAliveHumanList(){
		return gameData.getFilteredAgentList(getAliveAgentList(), Species.HUMAN);
	}

	protected List<Agent> getAliveWolfList(){
		return gameData.getFilteredAgentList(getAliveAgentList(), Species.WEREWOLF);
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


	/**
	 * @return isShowConsoleLog
	 */
	public boolean isShowConsoleLog() {
		return isShowConsoleLog;
	}


	/**
	 * @param isShowConsoleLog isShowConsoleLog
	 */
	public void setShowConsoleLog(boolean isShowConsoleLog) {
		this.isShowConsoleLog = isShowConsoleLog;
	}


}
