package org.aiwolf.server.bin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.aiwolf.common.bin.ClientStarter;
import org.aiwolf.common.data.Player;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.common.util.CalendarTools;
import org.aiwolf.common.util.Counter;
import org.aiwolf.server.AIWolfGame;
import org.aiwolf.server.net.DirectConnectServer;
import org.aiwolf.server.net.GameServer;

/**
 * クライアントを指定して直接シミュレーションを実行する
 * @author tori
 *
 */
public class DirectStarter {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
		Counter<String> clsCountMap = new Counter<String>();
		String logDir = "./log/";
		for(int i = 0; i < args.length; i++){
			if(args[i].startsWith("-")){
				if(args[i].equals("-c")){
					i++;
					String clsName = args[i];
					i++;
					int num = Integer.parseInt(args[i]);

					clsCountMap.put(clsName, num);
				}
				else if(args[i].equals("-l")){
					i++;
					logDir = args[i];
				}
			}
		}
		if(clsCountMap.isEmpty()){
			System.err.println("Usage:"+ClientStarter.class+" -c clientClass num [-c clientClass num ...] [-l logDir]");
			return;
		}

		
		start(clsCountMap, logDir);
	}

	/**
	 * 
	 * @param clsCountMap
	 * @param logDir
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static void start(Counter<String> clsCountMap, String logDir) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
		int playerNum = clsCountMap.getTotalCount();
		List<Player> playerList = new ArrayList<Player>();
//		for(int i = 0; i < playerNum; i++){
//			playerList.add((Player) Class.forName(clsName).newInstance());
//		}
		for(String clsName:clsCountMap){
			int num = clsCountMap.get(clsName);
			for(int i = 0; i < num; i++){
				playerList.add((Player) Class.forName(clsName).newInstance());
			}
		}
		
		String timeString = CalendarTools.toDateTime(System.currentTimeMillis()).replaceAll("[\\s-/:]", "");
		File logFile = new File(String.format("%s/aiwolfGame%s.log", logDir, timeString));
		
		GameServer gameServer = new DirectConnectServer(playerList);
		GameSetting gameSetting = GameSetting.getDefaultGame(playerNum);
		AIWolfGame game = new AIWolfGame(gameSetting, gameServer);
//		game.setLogFile(logFile);
		game.setRand(new Random(gameSetting.getRandomSeed()));
//		game.init();
		game.start();
//		game.finish();
	}

}
