import java.util.concurrent.TimeUnit;


public class BitHandler {
	SerialWrite serial;
	BufferQueue behaviours;
	BitThread bitThread;
	double t = 0;
	double hz = 0.01;
	double amp = 90;
	double offset = 90;
	
	final int NOTOUCH = 0;
	final int CONSTANT = 1;
	final int RUB = 2;
	final int PAT = 3;
	final int SCRATCH = 4;
	final int STROKE = 5;
	final int TICKLE = 6;	

	public BitHandler() {
		behaviours = new BufferQueue(108);
		bitThread = new BitThread();

	}
	
	public void update(String s) {
		behaviours.push(s);
		String mcb = mostCommonBehaviour();
		if (mcb == "notouch") {
			amp = 90;
			hz = 0.01;
			offset = 90;
		}
		else if (mcb == "tickle") {
			amp = 30;
			hz = 0.1;
			offset = 110;
		}
		else if (mcb == "stroke") {
			amp = 90;
			hz = 0.02;
			offset = 90;
		}
		else if (mcb == "rub") {
			amp = 90;
			hz = 0.02;
			offset = 90;
		}
		else if (mcb == "pat") {
			amp = 15;
			offset = 50;
			hz = 0.05;
		}
		else if (mcb == "scratch") {
			amp = 50;
			offset = 100;
			hz = 0.05;
		}
		else if (mcb == "constant") {
			amp = 80;
			hz = 0.06;
			offset = 100;
		}
	}
	
	public String mostCommonBehaviour() {
		int notouch = 0;
		int rub = 0;
		int stroke = 0;
		int constant = 0;
		int pat = 0;
		int tickle = 0;
		int scratch = 0;
		
		for (String b : behaviours.queue) {
			
			if (b=="notouch") {
				notouch += 1;
			}
			else if (b=="constant") {
				constant += 1;
			}
			else if (b=="rub") {
				rub += 1;
			}
			else if (b=="pat") {
				pat += 1;
			}
			else if (b=="stroke") {
				stroke += 1;
			}
			else if (b=="tickle") {
				tickle += 1;
			}
			else if (b=="scratch") {
				scratch += 1;
			}
		}
		int max = 0;
		String max_b = "notouch";

		if (notouch > max) {
			max = notouch;
			max_b = "notouch";
		}
		if (constant > max) {
			max = constant;
			max_b = "constant";
		}
		if (rub > max) {
			max = rub;
			max_b = "rub";
		}
		if (pat > max) {
			max = pat;
			max_b = "pat";
		}
		if (stroke > max) {
			max = stroke;
			max_b = "stroke";
		}
		if (tickle > max) {
			max = tickle;
			max_b = "tickle";
		}
		if (scratch > max) {
			max_b = "scratch";
		}

		System.out.println("Max behaviour count is for: " + max_b);

		return max_b;
	}
	
	public void runit() {
		bitThread.runit();
		
	}
	public void step() {
//		System.out.println("Stepping");
		bitThread.step();
	}
	public int sine() {
		double s = (amp * Math.sin(t*Math.PI)) + offset;
		int i = (int) s;
		return i;
	}

	private class BitThread {
		int count = 0;
		
		public void runit() {
			serial = new SerialWrite();  //creates an object of the class
			serial.initialize();
			serial.portConnect();
//			serial.close();
			System.out.println("Don't you worry your little heart Paul...we've connected.");
			
			System.out.println("done sending fer now");
		}
		public void loop(int i) {
			int j = 0;
			if (i < 0) {
				while(true) {
					send(sine());
					
//					 2 seconds per period
					try {
						TimeUnit.MILLISECONDS.sleep(20);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					t = t + hz;
					if (t > 10000000) {
						t = 0;
					}
				}
			}
			while (j < i) {
				j++;
				send(180);
			} 
		}
		
		public void step() {
			send(sine());
		//	System.out.println("Bot is going to " + sine() + " degrees.");
			t = t + hz;
			if (t > 10000000) {
				t = 0;
			}
		}
		public void send(short s) {
			serial.write(s);
		}
		public void send(int i) {
			short s = (short) (i); 
			serial.write(s);
		}
		
	}
//	// for testing
//	public static void main(String args[]) {
//        BitHandler b = new BitHandler();
//    }
}