package player;

import player.MyPlayer;
import java.io.IOException;
import java.util.Map;

import org.eclipse.palamedes.gdl.connection.Player;
import org.eclipse.palamedes.gdl.connection.PlayerServer;

 
public class MyPlayerServer extends PlayerServer {

    public MyPlayerServer(Player player, Map<String,String> options) 
    throws IOException {
        super(player, options);
    }

    /**
     * starts the game player and waits for messages from the game master <br>
     * Command line options: --port=<port> --slave=<true|false>
     */
    public static void main(String[] args){

    	/* create and start player server */
    	try {
    		Map<String, String> options = getOptions(args);
            new MyPlayerServer(new MyPlayer(), options).waitForExit();
          // test simplex solver:
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
 /*           SimplexSolver solver = new SimplexSolver();
            solver.test();
 */   }

}
