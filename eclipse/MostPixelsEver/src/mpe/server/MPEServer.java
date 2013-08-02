/**
 * The "Most Pixels Ever" Wallserver.
 * This server can accept two values from the command line:
 * -port<port number> Defines the port.  Defaults to 9002
 * -ini<Init file path>  File path to mpeServer.ini.  Defaults to directory of server.
 * @author Shiffman and Kairalla
 *
 */

package mpe.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;
import java.util.Timer;
import java.util.TimerTask;

public class MPEServer {
	
	public static String version = "1.0.8";
	
    private Vector<Connection> connections = new Vector<Connection>(); // change to vector to see if fixes issues (As its thread safe)
    private int port;
    private boolean running = false;
    public boolean[] connected;  // When the clients connect, they switch their slot to true
    public boolean[] ready;      // When the clients are ready for next frame, they switch their slot to true
    public boolean allConnected = false;  // When true, we are off and running
    int frameCount = 0;
    private long before;
    
    // Server will add a message to the frameEvent
    public boolean newMessage = false;
    public String message = "";
    
    // Server can send a byte array!
    public boolean newBytes = false;
    public byte[] bytes = null;
    
    // Server can send an int array!
    public boolean newInts = false;
    public int[] ints = null;
    
    public boolean dataload = false;

    // Back door connection stuff
    ListenerConnection backDoorConnection = null;
    private static boolean listener = false;
    private int listenPort;
    private boolean backDoorConnected = false;
    BackDoor backdoor;
    
    public ShutdownTimerTask shutdown_timer_task;
    public static long shutdown_time; 
    public static boolean has_shutdown = false; 
   // private static Date now_date;
    
    public MPEServer(int _screens, int _framerate, int _port, int _listenPort) {
        MPEPrefs.setScreens(_screens);
        MPEPrefs.setFramerate(_framerate);
        port = _port;
        listenPort = _listenPort;
        out("framerate = " + MPEPrefs.FRAMERATE + ",  screens = " + MPEPrefs.SCREENS + ", debug = " + MPEPrefs.DEBUG);
        
        connected = new boolean[MPEPrefs.SCREENS];  // default to all false
        ready = new boolean[MPEPrefs.SCREENS];      // default to all false
    }
    
    public void run() {
        running = true;
        if (listener) startListener();
        before = System.currentTimeMillis(); // Getting the current time
        ServerSocket frontDoor;
        
        if(MPEServer.has_shutdown) {
    		shutdown_timer_task = new ShutdownTimerTask(new Timer());
        	System.out.println("shuting down in " + shutdown_time + " milliseconds");
        	shutdown_timer_task.timer.schedule(shutdown_timer_task, shutdown_time);
        }
        
        try {
            frontDoor = new ServerSocket(port);

            System.out.println("Starting server: " + InetAddress.getLocalHost() + "  " + frontDoor.getLocalPort());

            // Wait for connections (could thread this)
            while (running) {
            	
            	//if(connections.size() < MPEPrefs.SCREENS){ //adamh: don't allow connections above our limit
	                Socket socket = frontDoor.accept();  // BLOCKING!                       
	                
	                //TODO: adamh - check if a socket of same address is already in connections? - but how as can have multiple clients form same ip.. 
	                // Make  and start connection object
	                Connection conn = new Connection(socket,this);
	                conn.start();
	                // Add to list of connections
	                connections.add(conn); 
	                System.out.println("Connected:" + connections.size() + "/" + MPEPrefs.SCREENS + " : " + socket.getRemoteSocketAddress() + " connected. "+ socket.getInetAddress().getCanonicalHostName() ); //  +", " + socket.getInetAddress().getHostAddress());
	                //System.out.println(connections.size() + " clients now connected. " + MPEPrefs.SCREENS + " expected.");
	                //printState();
            	/*}else{
               		//Socket socket = frontDoor.accept();
            		System.out.println("Ignoring connection request as full. " + MPEPrefs.SCREENS + " clients already connected. " + System.currentTimeMillis());//
            		//+ connections.size() + ". Failed connection by:" + socket.getRemoteSocketAddress() + " - "+ socket.getInetAddress().getCanonicalHostName()););
            	}*/
                
            }
        } catch (IOException e) {
            System.out.println("Zoinks!" + e);
        }
        
        
    }
    
    // Synchronize?!!!
    public synchronized void triggerFrame() {
        /*if (frameCount % 10 == 0) {
            System.out.println("Framecount: " + frameCount);
        }*/
        
        // We can't go on if the server is still loading data from a client
        /*while (dataload) {
            System.out.println("Data loading!");
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/

        int desired = (int) ((1.0f / (float) MPEPrefs.FRAMERATE) * 1000.0f);
        long now = System.currentTimeMillis();
        int diff = (int) (now - before);
        if (diff < desired) {
            // Where do we max out a framerate?  Here?
            try {
                long sleepTime = desired-diff;
                if (sleepTime >= 0){
                    Thread.sleep(sleepTime);
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            try {
                long sleepTime = 2;
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        // Reset everything to false
        for (int i = 0; i < ready.length; i++) {
            ready[i] = false;
        }        

        frameCount++;
        
        String send = "G,"+(frameCount-1);
        
        // Adding a data message to the frameEvent
        //substring removes the ":" at the end.
        if (newMessage) {
        	if(message.length() > 0) send += ":" + message.substring(0, message.length()-1);  //adamh added as had errors sometimes of empty messages throwing the substring().
        }
        newMessage = false;
        message = "";
        
        if (newBytes) {
          send = "B" + send;
          sendAll(send);
          sendAllBytes();
          newBytes = false;
        } else if (newInts) {
          send = "I" + send;
          sendAll(send);
          sendAllInts();
          newInts = false;
        } else {
          sendAll(send);
        }
        before = System.currentTimeMillis();
        
    }

    private void print(String string) {
        System.out.println("MPEServer: "+string);

    }
    
    public void printState() {
    	System.out.println("  connected: " + arrayToString(connected));
    	System.out.println("  ready:     " + arrayToString(ready));
    }
    
    private String arrayToString(boolean[] arr) {
    	String out = "[";
    	for(int i = 0; i<arr.length; i++){
    		out += ((i==0) ? "" : ",") + (arr[i] ? "1" : "0"); 
    	}
    	return out + "]";
    }
    
    public synchronized void reset() { // adamh added
    	print("reset called");
    	frameCount = 0; // resetFrameCount();
		sendAll("R");    	
    }
    
    public synchronized void sendAll(String msg){
        //System.out.println("Sending " + msg + " to clients: " + connections.size());
        for (int i = 0; i < connections.size(); i++){
            Connection conn = connections.get(i);
            conn.send(msg);
        }
    }
    
    public synchronized void sendAllBytes(){
        //System.out.println("Sending " + msg + " to clients: " + connections.size());
        for (int i = 0; i < connections.size(); i++){
            Connection conn = connections.get(i);
            conn.sendBytes(bytes);
        }
    }
    
    public synchronized void sendAllInts(){
        //System.out.println("Sending " + msg + " to clients: " + connections.size());
        for (int i = 0; i < connections.size(); i++){
            Connection conn = connections.get(i);
            conn.sendInts(ints);
        }
    }

    public void killConnection(Connection conn){
        connections.remove(conn);
        System.out.println(connections.size() + " clients connected. " + MPEPrefs.SCREENS + " expected.");
    }

    boolean allDisconected(){
        if (connections.size() < 1){
            return true;
        } else return false;
    }
    
    void resetFrameCount(){
        frameCount = 0;
        newMessage = false;
        message = "";
        print ("resetting frame count.");
    }
    public void killServer() {
        running = false;
    }
    
    @SuppressWarnings("deprecation")
	public static void main(String[] args) {
        // set default values
        int screens = 2;
        int framerate = 30;
        int port = 9001;
        int listenPort = 9002;
        
        boolean help = false;
        
        // see if info is given on the command line
        for (int i = 0; i < args.length; i++) {
        	if (args[i].contains("-screens")) {
                args[i] = args[i].substring(8);
                try{
                    screens = Integer.parseInt(args[i]);
                } catch (Exception e) {
                    out("ERROR: I can't parse the # of screens " + args[i] + "\n" + e);
                    help = true;
                }
                
            } else if (args[i].contains("-framerate")) {
                args[i] = args[i].substring(10);
                try{
                    framerate = Integer.parseInt(args[i]);
                } catch (Exception e) {
                    out("ERROR: I can't parse the frame rate " + args[i] + "\n" + e);
                    help = true;
                }
                
            } else if (args[i].contains("-port")) {
                args[i] = args[i].substring(5);
                try {
                    port = Integer.parseInt(args[i]);
                } catch (Exception e) {
                    out("ERROR: I can't parse the port number " + args[i] + "\n" + e);
                    help = true;
                }
                
            } else if (args[i].contains("-listener")){
                listener = true;
                
            } else if (args[i].contains("-listenPort")) {
            	
                args[i] = args[i].substring(11);
                try{
                    listenPort = Integer.parseInt(args[i]);
                } catch (Exception e) {
                    out("ERROR: I can't parse the listening port number " + args[i] + "\n" + e);
                    help = true;
                }
                
            } else if (args[i].contains("-debug")) {
                MPEPrefs.DEBUG = true;
            
            } else if (args[i].contains("-shutdown")) {
                
            	args[i] = args[i].substring(9);
            	
            	SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
            	
            	try {
            		Date d = sdf.parse(args[i]);
            		Date shutdown_date = new Date();
            		shutdown_date.setHours(d.getHours());
            		shutdown_date.setMinutes(d.getMinutes());
            		shutdown_date.setSeconds(d.getSeconds());
            		Date now_date = new Date();
            		if(now_date.getTime() > shutdown_date.getTime()) { 
    					// Already elapsed so make it tomorrow, same time
    					System.out.println("Shutdown: Time has already elapsed today, so making it tomorrow");
    					shutdown_date.setTime(shutdown_date.getTime() + 86400000); // 86400000 == 1 day of milliseconds
    				}
            		
            		shutdown_time = shutdown_date.getTime()-now_date.getTime(); 
            		
            		System.out.println("Shutdown: Set for " + shutdown_date.toString());
            		has_shutdown = true;
            	
            	} catch (Exception e) {
                     out("ERROR: I can't parse the shutdown time" + args[i] + "\n" + e);
                     help = true;
                 }
                
            	
            } else {
                help = true;
            }
        }
        
        if (help) {
            // if anything unrecognized is sent to the command line, help is
            // displayed and the server quits
            System.out.println(" * The \"Most Pixels Ever\" Wallserver.\n" +
                    " * This server can accept the following parameters from the command line:\n" +
                    " * -screens<number of screens> Total # of expected clients.  Defaults to 2\n" +
                    " * -framerate<framerate> Desired frame rate.  Defaults to 30\n" +
                    " * -port<port number> Defines the port.  Defaults to 9002\n" +
                    " * -listener Turns on an optional port listener so that other apps can send data to the screens.\n" +
                    " * -listenPort<port number>  Defines listening port.  Defaults to 9003.\n" +
                    " * -debug Turns debugging messages on.\n" +
                    " * -shutdown Time to automatically kill the server. Format is HH:MM \n" +
                    " * -ini<INI file path.>  Path to initialization file.  Defaults to \"mpeServer.ini\".\n" +
                    "    Please note the use of an INI file with the server is now deprecated.");
            System.exit(1);
        }
        else {
        	        	
            MPEServer ws = new MPEServer(screens, framerate, port, listenPort);
            ws.run();
        }
    }
    private static void out(String s){
        System.out.println("MPEServer: v" + version +", " + s);
    }

    public void drop(int i) {
        connected[i] = false;
        ready[i] = false;
    }

    // synchronize??
    public synchronized void setReady(int clientID) { 
        ready[clientID] = true;
        if (isReady()) triggerFrame();
    }

    // synchronize?
    public synchronized boolean isReady() {
        /*boolean allReady = true;
        for (int i = 0; i < ready.length; i++){  //if any are false then wait
            if (ready[i] == false) allReady = false;
        }
        return allReady;*/
    	
    	for (int i = 0; i < ready.length; i++){  //if any are false then wait
            if (ready[i] == false) return false;
        }
    	return true;
    }


  //********************** Shutdown timer  **********************************   
    
    class ShutdownTimerTask extends TimerTask{
    	
    	public Timer timer;
    	
    	public ShutdownTimerTask(Timer timer) {
    		this.timer = timer;
    	}
    	
    	@Override
    	public void run() {
    	   	System.out.println("Shutdown Timer Elapsed @" + (new Date()));
    	   	this.timer.cancel(); // kill it
    	   	System.exit(1);
    	}
    
    }
    
//********************** BACKDOOR LISTENER METHODS **********************************
    private void startListener(){
       backdoor = new BackDoor(this);
       Thread t = new Thread(backdoor);
       t.start();
    }
    public void killListenerConnection(){
    	backDoorConnected = false;
    }
    
    class BackDoor implements Runnable{
    	MPEServer parent;
    	BackDoor(MPEServer _parent){
    		parent = _parent;
    	}
    	
		public void run() {
			 ServerSocket backDoor;
			 ListenerConnection backDoorConnection;
		    	try {
		            backDoor= new ServerSocket(listenPort);
		            System.out.println("Starting backdoor Listener: " + InetAddress.getLocalHost() + "  " + backDoor.getLocalPort());
		            while (running) {
		                if (!backDoorConnected){
		                	System.out.println("Waiting for backdoor connection");
		                Socket socket = backDoor.accept();  // BLOCKING!
		                System.out.println("backdoor port "+socket.getRemoteSocketAddress() + " connected.");
		                // Make  and start connection object
		                backDoorConnection = new ListenerConnection(socket,parent);
		                backDoorConnection.start();
		                backDoorConnected= true;
		                } else {
							Thread.sleep(500);
		                }
		            }
		        } catch (IOException e) {
		            System.out.println("Zoinks, Backdoor Style!" + e);
		        } catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
		}
    	
    }
}