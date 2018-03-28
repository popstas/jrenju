import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.util.StringTokenizer;

public class JRenju extends Frame implements ActionListener, TextListener {
	final String version = "0.1";
	int[][] field = new int[15][15];
	Button[][] buttons = new Button[15][15];
	TextField chatField, ipField;
	TextArea chatArea;
	TextField timerField[] = {new TextField("20 100"),new TextField("20 100")};
	Timer timer;
	int time[] = {100, 100};
	String r[] = {"X", "O"};
	int player = 0, iAm = 0;
	boolean myMove = true;
	Color color[] = {Color.BLUE, Color.RED};
	int[] NW = {-1, -1}, N = {-1, 0}, NE = {-1, 1};
	int[] W = {0, -1}, E = {0, 1};
	int[] SW = {1, -1}, S = {1, 0}, SE = {1, 1};
	Font font = new Font(null, Font.PLAIN, 16);
	Font boldFont = new Font(null, Font.BOLD, 16);
	String inTCP, outTCP;
	String IP = "127.0.0.1";
	Network net;
	Button hostButton, clientButton, newgameButton;
	boolean isWin = false;
	int size = 15;
	int fieldState = 2;

	public JRenju() {
		super();
		setTitle("JRenju " + version);
		setSize(512, 384);
		setLayout(new BorderLayout());
		add(initField(size), BorderLayout.WEST);
		add(initSidePanel(), BorderLayout.EAST);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		setResizable(false);
		setVisible(true);
	}

	private Panel initField(int side) {
		Panel p = new Panel();
		p.setLayout(new GridLayout(side + 1, side + 1));
		String letters = " ABCDEFGHIJKLMNO";

		for (int i = 0; i < size+1; i++) {
			Button b = new Button(letters.substring(i, i + 1));
			b.setBackground(Color.WHITE);
			p.add(b);
		}
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				if (x == 0) {
					Button b = new Button(Integer.toString(y + 1));
					b.setBackground(Color.WHITE);
					p.add(b);
				}
				buttons[y][x] = new Button();
				p.add(buttons[y][x]);
				String ac = "";
				if (y < 10) {
					ac += "0" + y;
				} else {
					ac += y;
				}
				if (x < 10) {
					ac += "0" + x;
				} else {
					ac += x;
				}
				field[y][x] = -1;
				buttons[y][x].setFont(font);
				buttons[y][x].setBackground(Color.WHITE);
				buttons[y][x].setActionCommand(ac);
				buttons[y][x].addActionListener(this);
			}
		}
		p.setPreferredSize(new Dimension(380, 380));
		return p;
	}

	private void clearField() {
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				field[y][x] = -1;
				buttons[y][x].setLabel("");
				buttons[y][x].setFont(font);
			}
		}
	}
	
	private Panel initSidePanel() {
		Panel p = new Panel(new GridLayout(2, 1));
		Panel controlPanel = new Panel(new GridLayout(5, 1));
		ipField = new TextField("127.0.0.1");
		ipField.setBackground(Color.WHITE);
		ipField.addTextListener(this);
		newgameButton = new Button("New game");
		newgameButton.addActionListener(this);
		Panel butPanel = new Panel(new GridLayout(1, 2));
		hostButton = new Button("Host");
		hostButton.addActionListener(this);
		clientButton = new Button("Client");
		clientButton.addActionListener(this);
		butPanel.add(hostButton);
		butPanel.add(clientButton);

		controlPanel.add(ipField);
		controlPanel.add(newgameButton);
		controlPanel.add(butPanel);
		controlPanel.add(timerField[0]);
		controlPanel.add(timerField[1]);
		p.add(controlPanel);

		Panel chatPanel = new Panel(new BorderLayout());
		chatArea = new TextArea("", 10, 10, TextArea.SCROLLBARS_VERTICAL_ONLY);
		chatField = new TextField();
		chatField.setBackground(Color.WHITE);
		chatField.addActionListener(this);
		chatPanel.add(chatArea, BorderLayout.CENTER);
		chatPanel.add(chatField, BorderLayout.SOUTH);
		p.add(chatPanel);
		p.setPreferredSize(new Dimension(128, 384));
		return p;
	}

	public void textValueChanged(TextEvent e){
		StringTokenizer st = new StringTokenizer(ipField.getText(), ".");
		try{
			if(st.countTokens()!=4){
				throw new IOException("not 4 numbers");
			}
			String s = "";
			while(st.hasMoreTokens()){
				int i = Integer.valueOf(st.nextToken());
				if(i<0|i>255){
					throw new IOException("...");
				}
				if(s.length()==0){
					s+=i;
				} else {
					s+="."+i;
				}
				IP = s;
				ipField.setBackground(Color.YELLOW);
			}
		}catch (Exception ipe){
			ipField.setBackground(Color.RED);
		}
	}

	public void newgame(boolean myMove){
		ipField.setBackground(Color.GREEN);
		time[0] = 100;
		time[1] = 100;
		isWin = false;
		newgameButton.setEnabled(false);
		fieldState = 2;
		clearField();
		timerField[0].setBackground(Color.GREEN);
		timerField[1].setBackground(Color.GREEN);
		this.myMove = myMove;
		player = 0;
		int i = myMove?0:1;
		this.setTitle("JRenju "+version+" - "+r[i]);
		iAm = i;
		timer = new Timer(time[player], this);
		if(myMove){
			net.send("\\*n");
		} else{
			fieldState = 0;
		}
	}

	private void enableButtons(boolean isEnable){
		hostButton.setEnabled(isEnable);
		clientButton.setEnabled(isEnable);
		newgameButton.setEnabled(isEnable);
		if(isEnable){
			ipField.addTextListener(this);
		} else{
			ipField.setBackground(Color.YELLOW);
			ipField.removeTextListener(this);
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		String ac = e.getActionCommand();
		if(ac.equals("New game")){
			newgame(true);
		} else if (ac.equals("Host")) {
			this.setTitle("JRenju " + version + " - Host");
			hostButton.setFont(boldFont);
			enableButtons(false);
			try {
				net = new Network(true, IP, this);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			newgame(true);
		} else if (ac.equals("Client")) {
			this.setTitle("JRenju " + version + " - Client");
			clientButton.setFont(boldFont);
			enableButtons(false);
			try {
				net = new Network(false, IP, this);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} else if (e.getSource().equals(chatField)){
			chatRender(chatField.getText(), false);
			chatField.setText("");
		} else if (ac.length() == 4) {
			if (fieldState == 0) {
				return;
			}
			int y = Integer.valueOf(ac.substring(0, 2));
			int x = Integer.valueOf(ac.substring(2, 4));
			if (fieldState == 1 && field[y][x] != -1) {
				return;
			}
			time[player] = timer.endMove();
			place(y, x);
			fieldState = 0;
			net.send("\\*m*" + y + "*" + x);
			if(!isWin){
				timer = new Timer(time[player], this);
			}
					this.myMove = myMove;
		}
	}

	private void chatRender(String str, boolean isIncoming){
		if(!isIncoming){
			net.send("\\*c*"+str);
			chatArea.append(r[iAm]+": ");
		} else{
			chatArea.append(r[iAm==0?1:0]+": ");
		}
		chatArea.append(str+"\n");
		if(str.substring(0,2).equals("\\*")){
			receive(str);
			/**@todo dodelat' */
		}
	}
	
	public void disconnected(){
		chatArea.append("Connection failed\n");
		ipField.setBackground(Color.RED);
		enableButtons(true);
		//todo
	}
	
	public void place(int y, int x) {
		buttons[y][x].setForeground(color[player]);
		buttons[y][x].setLabel(r[player]);
		buttons[y][x].requestFocus();
		field[y][x] = player;
		int[] win = isWin(y, x);
		if (win != null) {
			win(win);
			return;
		}
		endMove();
	}
	
	private int[] isWin(int y, int x) {
		int[][] dir = {NW, SE, SW, NE, W, E, N, S};
		int c, t[];
		for (int i = 0; i < 8; i += 2) {
			c = 0;
			t = see(y, x, dir[i]);
			int[] begin = {t[0], t[1]};
			c += t[2];

			t = see(y, x, dir[i + 1]);
			int[] end = {t[0], t[1]};
			c += t[2] - 1;
			if (c > 4) {
				return new int[]{begin[0], begin[1], end[0], end[1]};
			}
		}
		return null;
	}

	private int[] see(int y, int x, int[] s) {
		int count = 0;
		while (field[y][x] == player) {
			y += s[0];
			x += s[1];
			count++;
			if (y < 0 | y > size-1 | x < 0 | x > size-1) {
				return new int[]{y - s[0], x - s[1], count};
			}
		}
		return new int[]{y - s[0], x - s[1], count};
	}

	private void win(int coord[]){
		isWin = true;
		fieldState = 0;
		newgameButton.setEnabled(true);
		timer.endMove();
		int by = coord[0];
		int bx = coord[1];
		int ey = coord[2];
		int ex = coord[3];
//		chatArea.append(by+" "+bx+" "+ey+" "+ex+"\n");

		int y = by;
		if (bx == ex) {
			for (int wy = by; wy <= ey; wy++) {
				buttons[wy][bx].setFont(boldFont);
			}
		}
		for (int wx = bx; wx <= ex; wx++) {
			buttons[y][wx].setFont(boldFont);
			y += ey > by ? 1 : ey < by ? -1 : 0;
		}
	}	
	
	private void endMove() {
		myMove = !myMove;
		player = player==0?1:0;
	}

	public void setTime(int pretime, int time){
		int R = time>50?(int)(255*(100-time)/50):255;
		int G = time>50?255:(int)(255*time/50);
		Color c = new Color(R, G, 0);
		timerField[myMove?1:0].setBackground(c);
		timerField[myMove?1:0].setText(pretime+" "+time);
	}
	
	public void timeout(){
	//problems with new game (mix X-O)
		timerField[myMove?1:0].setBackground(Color.BLACK);
		fieldState = 0;
		newgameButton.setEnabled(true);
	}
	
	public void receive(String str){
		StringTokenizer st = new StringTokenizer(str, "*");
		if (!st.nextToken().equals("\\")) {
			chatArea.append("Wrong remote message received: "+str+"\n");
			return;
		}
		String command = st.nextToken();
		if (command.equals("m")) { //move
			time[player] = timer.endMove();
			this.fieldState = 1;
			place(Integer.valueOf(st.nextToken()), Integer.valueOf(st.nextToken()));
			if(!isWin) {
				timer = new Timer(time[player], this);
			}

		} else if(command.equals("n")){ //new game
			newgame(false);

		}else if (command.equals("c")) { //чат
			chatRender(str.substring(4), true);
		}
	}

	public static void main(String[] args) {
		new JRenju();
	}
}

class Timer extends Thread{
	int time;
	int pretime = 20;
	boolean isRun = true;
	JRenju jr;
	
	public Timer(int time, JRenju jr){
		super();
		this.time = time;
		this.jr = jr;
		start();
	}
	
	public int endMove(){
		isRun = false;
		return time;
	}
	
	public void run(){
		jr.setTime(pretime, time);
		while(pretime>0&&isRun){
			try{
				Thread.sleep(1000);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
			pretime--;
			if(isRun){
				jr.setTime(pretime, time);
			}
		} while(time>0&&isRun){
			try{
				Thread.sleep(1000);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
			time--;
			if(isRun){
				jr.setTime(pretime, time);
			}
		}
		if(isRun){
			jr.timeout();
		}
	}
}
	
class Network implements Runnable {
	public static final int PORT = 1234;
	private BufferedReader in;
	private PrintWriter out;
	ServerSocket s;
	Socket socket;
	String IP = "127.0.0.1";
	JRenju jr;
	boolean isRun = true;

	public Network(boolean isHost, String IP, JRenju jr) throws IOException {
		this.jr = jr;
		this.IP = IP;
		if (isHost) {
			host();
		} else {
			client();
		}
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(new BufferedWriter(
			new OutputStreamWriter(socket.getOutputStream())), true);
		Thread listener = new Thread(this, "network-thread");
		listener.start();
	}


	private void host() throws IOException {
		s = new ServerSocket(PORT);
		socket = s.accept();
	}

	private void client() throws IOException {
		InetAddress addr = InetAddress.getByName(IP);
		socket = new Socket(addr, PORT);
		System.out.println("Connect to " + IP);
	}

	public void send(String str) {
		out.println(str);
	}

	public void run() {
		try {
			while (isRun) {
				String str = in.readLine();
				if (str.equals("\\*c*\\*disconnect")) {
					isRun = false;
				} else {
					jr.receive(str);
				}
			}
			jr.disconnected();
			socket.close();
			s.close();
		} catch (SocketException e){
			jr.disconnected();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}