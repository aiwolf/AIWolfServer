import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.StreamHandler;

import org.aiwolf.common.util.ServerLogFormat;
import org.aiwolf.server.AIWolfGame;
import org.aiwolf.server.GameSetting;
import org.aiwolf.server.net.TcpipServer;

/**
 * Start TCP/IP Server
 * @author tori
 * @deprecated
 */
public class ServerStartMain {

	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws SocketTimeoutException 
	 */
	public static void main(String[] args) throws SocketTimeoutException, IOException {
		int port = 10000;
		int playerNum = 12;
		
		TcpipServer gameServer = new TcpipServer(port, playerNum);
		Handler handler = new StreamHandler(System.out, new ServerLogFormat(true));
		gameServer.getServerLogger().addHandler(handler);
		gameServer.waitForConnection();
		
		GameSetting gameSetting = GameSetting.getDefaultGame(playerNum);
		AIWolfGame game = new AIWolfGame(gameSetting, gameServer);
		game.setRand(new Random());
		game.start();
		
		
		
	}

}
