package player;

import player.MyPlayer;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

import org.eclipse.palamedes.gdl.connection.Player;
import org.eclipse.palamedes.gdl.connection.PlayerServer;

public class MyPlayerServer extends PlayerServer {

    private static final Boolean DEBUG = false;
    private static final Boolean TEST_SIMPLEX = false;

    public MyPlayerServer(Player player, Map<String, String> options)
            throws IOException {
        super(player, options);
    }

    /**
     * starts the game player and waits for messages from the game master <br>
     * Command line options: --port=<port> --slave=<true|false>
     */
    public static void main(String[] args) {
        if (!DEBUG) {
            try {
                System.setErr(new PrintStream(new FileOutputStream("/dev/null", true)));
            } catch (FileNotFoundException e) {
            }
        }
        /* create and start player server */
        if (!TEST_SIMPLEX) {
            try {
                Map<String, String> options = getOptions(args);
                new MyPlayerServer(new MyPlayer(), options).waitForExit();
            } catch (IOException ex) {
                ex.printStackTrace();
                System.exit(-1);
            }
        } else {
            SimplexSolver solver = new SimplexSolver();
            solver.test();
        }
    }
}
