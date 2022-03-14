import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//import java.lang.Object;
import java.util.AbstractSequentialList;
import java.util.LinkedList;



public class SensorReader implements SerialPortEventListener {
	private long fCount;
	SerialPort serialPort;
	/** The port we're normally going to use. */
	private static final String PORT_NAMES[] = { "/dev/tty.usbserial-A9007UX1", // Mac
																				// OS
																				// X
			"/dev/ttyACM0", // Raspberry Pi
			"/dev/ttyUSB0", // Linux
			"COM4", // Windows
	};
	/**
	 * A BufferedReader which will be fed by a InputStreamReader converting the
	 * bytes into characters making the displayed results codepage independent
	 */
	private BufferedReader inputA;
	/** The output stream to the port */
	// private BufferedReader inputB; // ready to double buffer once I figure
	// out the single buffer system
	private OutputStream output;
	/** Milliseconds to block while waiting for port open */
	private static final int TIME_OUT = 2000;
	/** Default bits per second for COM port. */
	private static final int DATA_RATE = 512000; // baud-rate
	private static final int TOTAL_FRAMES = 108; // user-defined for number of
													// frames (probably going to
													// gauge 2 second intervals)
	private static final int POWER = 10;
	private static final int GROUND = 10;
	private static final int BUF = (POWER * GROUND * 2) + 9; // size of each
																// buffer
	private static final int faker = 10;
	private long[][] frameBufferA = new long[POWER][GROUND]; // full read
	// private long[][] frameBufferB = new long [POWER][GROUND]; // when ready
	// for second frameBuffer
	// private long[] timestamp = new long[TOTAL_FRAMES]; // for the check sum
	private int frameCounter = 0; // ensuring we don't exceed total # of frames
	// private LinkedList<int> frameVal = new LinkedList<int>();
	// LinkedList<Integer> frameSum = new LinkedList<Integer>();

	private boolean loopDone = false;
	Object bufferLock = new Object();
	public static boolean startedCollection = false;
	
	private static final Integer[][] weights = {
        {985, 967, 996, 991, 983, 955, 945, 955, 1022, 1022},
        {1013, 1009, 1015, 1011, 1012, 1007, 1007, 1008, 1022, 1022},
        {935, 845, 961, 944, 910, 680, 475, 551, 1021, 1021},
        {999, 987, 1007, 1004, 998, 978, 968, 976, 1022, 1022},
        {1017, 1015, 1019, 1019, 1017, 1014, 1012, 1014, 1022, 1022},
        {1018, 1017, 1020, 1019, 1018, 1016, 1016, 1017, 1022, 1022},
        {1012, 1007, 1016, 1014, 1012, 1004, 1001, 1003, 1022, 1022},
        {921, 770, 953, 936, 898, 530, 613, 629, 1021, 1021},
        {922, 769, 953, 936, 899, 529, 611, 628, 1021, 1021}
    };
	
	private static final Integer[][] weights1 = { 
		{1019, 1016, 1016, 1018, 1018, 1018, 1018, 1018, 1017, 1023},
		{1009, 1005, 1000, 1005,  999, 1003, 1002, 1002, 1003,  992},
		{1023, 1022, 1023, 1023, 1022, 1022, 1023, 1023, 1021, 1023},
		{1021, 1023, 1021, 1023, 1023, 1023, 1023, 1022, 1022, 1023},
		{1019, 1020, 1021, 1018, 1018, 1017, 1020, 1019, 1023, 1013},
		{1016, 1019, 1019, 1016, 1019, 1018, 1018, 1019, 1016, 1014},
		{1019, 1021, 1016, 1018, 1019, 1020, 1020, 1020, 1013, 1023},
		{1022, 1020, 1021, 1021, 1020, 1019, 1021, 1020, 1015, 1005},
		{1020, 1022, 1020, 1022, 1020, 1021, 1022, 1020, 1010, 1023},
		{1023, 1019, 1020, 1020, 1017, 1019, 1021, 1020, 1017, 1013}
	};
		
	public void start() {
		
		CommPortIdentifier portId = null;
		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

		// First, Find an instance of serial port as set in PORT_NAMES.
		while (portEnum.hasMoreElements()) {
			CommPortIdentifier currPortId = (CommPortIdentifier) portEnum
					.nextElement();
			//System.out.println(currPortId.getName());
			for (String portName : PORT_NAMES) {
				if (currPortId.getName().equals(portName)) {
					portId = currPortId;
					break;
				}
			}
		}

		if (portId == null) {
			System.out.println("Could not find COM 3 port.");
			System.out.println(portId);
			return;
		}

		try {
			// open serial port, and use class name for the appName.
			serialPort = (SerialPort) portId.open(this.getClass().getName(),
					TIME_OUT);

			// set port parameters
			serialPort.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			// serialPort.flush();

			// open the streams
			inputA = new BufferedReader(new InputStreamReader(
					serialPort.getInputStream()));
			// inputB = new BufferedReader(new
			// InputStreamReader(serialPort.getInputStream()));
			output = serialPort.getOutputStream();

			// add event listeners
			serialPort.addEventListener(this);
			
			serialPort.notifyOnDataAvailable(true);
			startedCollection = true;

			System.out.println("port opened: " + serialPort.getName());
		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}

	/**
	 * This should be called when you stop using the port. This will prevent
	 * port locking on platforms like Linux.
	 */
	public synchronized void stop() {
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
		}
	}

	// attempt1 to get unsigned bytes
	public long unsignedVal(int signedValue) {
		long unsignedValue = signedValue & 0xffffffffl;
		return unsignedValue;
	}

	// attempt2 to get unsigned bytes
	public static int unsigned(int b) {
		return b & 0xFF;
	}

	/**
	 * Handle an event on the serial port. Read the data and print it.
	 */
	/*
	 * public synchronized long[][] copyBuffer(long[][] buffer) { long[][]
	 * frameBufferB = buffer; return frameBufferB; }
	 */

	public synchronized void serialEvent(SerialPortEvent oEvent) {
		/*
		 * if (frameCounter >= TOTAL_FRAMES) { // to force a close try {
		 * inputA.close(); } catch (Exception e) {
		 * System.err.println(e.toString()); } System.exit(0); }
		 */
		// Object frameLock = new Object();
		if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			//for (int i = 0; i < TOTAL_FRAMES; i++) {
				try {
					int tmp = 0;
					int count255 = 0;
					while (count255 < 4) { // check for 4 255's in a row -
											// indicates ready
						tmp = inputA.read(); // so as not to consume another
												// value while checking
						if (tmp == 255) {
							count255++;
						} else {
							count255 = 0;
						}
					}
					frameCounter++;
					long timeTemp = inputA.read() + inputA.read() * 256
							+ inputA.read() * (256 ^ 2) + inputA.read()
							* (256 ^ 3); // used for checksum
					// time stamp consumes 4 bytes
					long framesum = 0;
					long pixVal = -1;
					synchronized (bufferLock) { // locks the loop for reading to
												// frameBufferA
						for (int j = 0; j < 10; j++) {
							for (int k = 0; k < 10; k++) {
								//if(j==)
								int byteA = inputA.read();
								int byteB = inputA.read();

								pixVal = (byteA + byteB * 256);
								if (pixVal <= 1023 && pixVal >= 0) {
								//	long pixValn = (long) (1023 - pixVal - Math.floor(0.1*(1023 - weights[k][j])));
								//	long pixValn = (long) (0.9 * (1023 - pixVal));
								//	long pixValn = (long) (1 - (weights[k][j])/1023) * (1023 - pixVal);
									long pixValn = weights1[j][k] - pixVal;
								//	long pixValn = 1023 - pixVal;
									framesum = framesum + pixValn ;
									System.out.println(framesum);// add to
																	// frame
																	// sum
									if (pixValn < faker+1) {
										frameBufferA[k][j] = 0;
									} else {
										frameBufferA[k][j] = pixValn - faker;
									}// add to
																	// frame
																	// location
									// if (pixValn > 500) {
									// System.out.println("byteA is " + byteA
									// + "\n" + "byteB is " + byteB
									// + "\n");

									// }
								} else {
									frameBufferA[k][j] = 0;
								}
								// store polled pixel pressure Value

								// actual data consumes 2 bytes per pixel
								// converting to Big Endian

							}
						}
					} // end of synchronized bufferLock
						// TODO
						// if (frameSum.size() < 108 && frameSum.size() > 0) {
						// frameSum.addLast((int) framesum);
						// }

					// long check_sum = inputA.read();
					// check_sum consumes 1 byte
					// long calc = (framesum + timeTemp) % 256;
					// if (check_sum != calc) {
					// System.out.println("bad data");
					// }

					//System.out.println(Arrays.deepToString(frameBufferA));

				} catch (Exception e) {
					//System.err.println(e.toString());
				}
			//}

		}

		// return frameBufferA;
		// Ignore all the other eventTypes, but you should consider the other
		// ones.
	}

	/*
	 * public class Lock {
	 * 
	 * private boolean isLocked = false;
	 * 
	 * public synchronized void lock() throws InterruptedException { while
	 * (isLocked) { wait(); } isLocked = true; }
	 * 
	 * public synchronized void unlock() { isLocked = false; notify(); } }
	 */
	public SensorFrame getFrame() throws InterruptedException {
		SensorFrame frame = new SensorFrame();
		// private Object lock2 = new Object();
		frame.data = new long[frameBufferA.length][];
		// lock frameBufferA

		synchronized (bufferLock) {
			// long[][] frameBufferCopy = new long[frameBufferA.length][];
			for (int i = 0; i < frameBufferA.length; i++) {
				long[] aMatrix = frameBufferA[i];
				int aLength = aMatrix.length;
				frame.data[i] = new long[aLength];
				System.arraycopy(aMatrix, 0, frame.data[i], 0, aLength);
			}
			
			frame.count = frameCounter;
		} // end of bufferLock
		return frame;

	}
	
	/*
	 * public static void main(String[] args) throws Exception { SerialTest main
	 * = new SerialTest(); main.initialize(); Thread t=new Thread() { public
	 * void run() { //the following line will keep this app alive for 1000
	 * seconds, //waiting for events to occur and responding to them (printing
	 * incoming messages to console). try {Thread.sleep(10000);} catch
	 * (InterruptedException ie) {} } }; t.start();
	 * System.out.println("Started"); }
	 */

	public class SensorFrame
	{
		public int count;
		public long[][] data;
	}
}
