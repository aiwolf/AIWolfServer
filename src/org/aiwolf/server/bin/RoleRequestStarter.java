package org.aiwolf.server.bin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.net.GameSettingEntity;
import org.aiwolf.common.util.CalendarTools;
import org.aiwolf.common.util.Pair;
import org.aiwolf.server.AIWolfGame;
import org.aiwolf.server.net.DirectConnectServer;

/**
 * 役割を指定してスタートするStarter<br>
 * DirectStarter
 * @author tori
 *
 */
public class RoleRequestStarter {
	
	static final private String description = 
			"このクラスを利用して，自作Playerごとに役職を決めてゲームをスタートできます．\n" +
					"Usage:"+RoleRequestStarter.class+" -n agentNum -c playerClass role [-c playerClass role] [-d defaultPlayer]\n"+
					"-n\tゲームに参加するエージェント数を決定します．\n"+
					"-c 'プレイヤークラス'　'設定したい役職'　を設定します．\n"+
					"-c 'プレイヤークラス'　で，役職を指定せずに利用するPlayerを指定します．\n"+
					"-d デフォルトのプレイヤークラスを指定します．指定しなければSamplePlayerが使われます．\n"+
					"-l ログを保存するディレクトリの指定．デフォルトは./log/\n"+
					"例えば，自作のorg.aiwolf.MyPlayerをbodyguardとして12体のエージェントで人狼を実行したければ\n"+
					RoleRequestStarter.class+" -n 12 -c org.aiwolf.MyPlayer bodyguard\n"+
					"としてください．\n";

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {


		List<Pair<String, Role>> playerRoleList = new ArrayList<Pair<String,Role>>();
		String defaultClsName = Class.forName("org.aiwolf.client.base.smpl.SampleRoleAssignPlayer").getName();
		int playerNum = -1;
		String logDir = "./log/";
		for(int i = 0; i < args.length; i++){
			if(args[i].startsWith("-")){
				if(args[i].equals("-c")){
					i++;
					String clsName = args[i];
					i++;
					try{
						if(i > args.length-1  || args[i].startsWith("-")){
							i--;
							playerRoleList.add(new Pair<String, Role>(clsName, null));
							continue;
						}
						Role role = Role.valueOf(args[i]);
						playerRoleList.add(new Pair<String, Role>(clsName, role));
					}catch(IllegalArgumentException e){
						System.err.println("No such role as "+args[i]);
						return;
					}
				}
				else if(args[i].equals("-n")){
					i++;
					playerNum = Integer.parseInt(args[i]);
				}
				else if(args[i].equals("-d")){
					i++;
					defaultClsName = args[i];
				}
				else if(args[i].equals("-l")){
					i++;
					logDir = args[i];
				}
				else if(args[i].equals("-h")){
					System.out.println(description);
					System.out.println("利用可能な役職");
					for(Role role:Role.values()){
						System.out.println(role);
					}
				}
			}
		}
		if(playerNum < 0){
			System.err.println("Usage:"+RoleRequestStarter.class+" -n agentNum -c playerClass role [-c playerClass role...] [-d defaultPlayer] [-l logDir]");
			return;
		}

		
		Map<Player, Role> playerMap = new HashMap<Player, Role>();
		for(Pair<String, Role> pair:playerRoleList){
			playerMap.put((Player) Class.forName(pair.getKey()).newInstance(), pair.getValue());
		}
		while(playerMap.size() < playerNum){
			playerMap.put((Player) Class.forName(defaultClsName).newInstance(), null);
		}
		
		start(playerNum, playerMap, logDir);
	}

	/**
	 * 一人のRoleを指定してDirectに実行
	 * @param player
	 * @param role
	 * @param playerNum
	 * @param defaultClsName
	 * @param logDir
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static void start(Player player, Role role, int playerNum, String defaultClsName, String logDir) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException{
		Map<Player, Role> playerMap = new LinkedHashMap<Player, Role>();

		playerMap.put(player, role);
		while(playerMap.size() < playerNum){
			playerMap.put((Player) Class.forName(defaultClsName).newInstance(), null);
		}
		start(playerNum, playerMap, logDir);
	}
	
	/**
	 * すべてのプレイヤーインスタンスとそのRoleを設定して開始
	 * @param playerNum
	 * @param playerMap
	 * @param logDir
	 * @throws IOException
	 */
	public static void start(int playerNum, Map<Player, Role> playerMap, String logDir) throws IOException {
		String timeString = CalendarTools.toDateTime(System.currentTimeMillis()).replaceAll("[\\s-/:]", "");
	
		DirectConnectServer gameServer = new DirectConnectServer(playerMap);
		GameSettingEntity gameSetting = GameSettingEntity.getDefaultGame(playerNum);
		AIWolfGame game = new AIWolfGame(gameSetting, gameServer);
		if(logDir != null){
			File logFile = new File(String.format("%s/aiwolfGame%s.log", logDir, timeString));
			game.setLogFile(logFile);
		}
		game.setRand(new Random());
		game.start();
	}

}
