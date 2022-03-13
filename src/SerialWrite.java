import java.io.BufferedReader;                    //BufferedReader makes reading operation efficient
import java.io.InputStreamReader;         //InputStreamReader decodes a stream of bytes into a character set
import java.io.OutputStream;          //writes stream of bytes into serial port

import gnu.io.CommPortIdentifier;           
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;            //deals with possible events in serial port (eg: data received)
import gnu.io.SerialPortEventListener; //listens to the a possible event on serial port and notifies when it does

import java.util.Enumeration;

import gnu.io.PortInUseException;           //all the exceptions.Never mind them for now

import java.io.IOException;

import gnu.io.UnsupportedCommOperationException;

import java.util.TooManyListenersException;
import java.util.Scanner;                                   //to get user input of name

public class SerialWrite implements SerialPortEventListener {        
   
    private SerialPort serialPort ;         //defining serial port object
    private CommPortIdentifier portId  = null;       //my COM port
    private static final int TIME_OUT = 2000;    //time in milliseconds
    private static final int BAUD_RATE = 512000; //baud rate to 9600bps
    private BufferedReader input;               //declaring my input buffer
    private OutputStream output;                //declaring output stream
    private String name;        //user input name string
    Scanner inputName;          //user input name

    //method initialize
    public void initialize(){
        CommPortIdentifier ports = null;      //to browse through each port identified
        Enumeration portEnum = CommPortIdentifier.getPortIdentifiers(); //store all available ports
        
        String PORT_NAME[] = {  
        		"/dev/tty.usbserial-A700e1oo", // Mac OS X
        		"/dev/ttyACM0", // Raspberry Pi
        		"/dev/ttyUSB0", // Linux
        		"COM28", // Windows
        
        };
        
        while(portEnum.hasMoreElements()){  //browse through available ports
                ports = (CommPortIdentifier)portEnum.nextElement();
                //following line checks whether there is the port i am looking for and whether it is serial
                
                for(String s : PORT_NAME) {
                	if (ports.getPortType() == CommPortIdentifier.PORT_SERIAL 
                			&& ports.getName().equals(s)) { 
                        System.out.println("COM port found: " + s);
                        portId = ports; // initialize my port
                        break;                                                                                     }	
                }          
            }
       //if serial port am looking for is not found
        if(portId==null){
            System.out.println("Bit COM port not found");
            System.exit(1);
        }
        
                            }
    
    //end of initialize method
    
    //connect method
   
    public void portConnect(){
        //connect to port
        try{
         serialPort = (SerialPort)portId.open(this.getClass().getName(),TIME_OUT);   //down cast the comm port to serial port
                                                                                     //give the name of the application
                                                                                     //time to wait
        System.out.println("Port open succesful: COM20"); 
        
        //set serial port parameters
serialPort.setSerialPortParams(BAUD_RATE,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
        
        

        }
        catch(PortInUseException e){
            System.out.println("Port already in use");
            System.exit(1);
        }
        catch(NullPointerException e2){
            System.out.println("COM port maybe disconnected");
        }
        catch(UnsupportedCommOperationException e3){
            System.out.println(e3.toString());
        }
       
        //input and output channels
        try{
      //defining reader and output stream
  //     input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
        output =  serialPort.getOutputStream();
        //adding listeners to input and output streams
        serialPort.addEventListener(this);
        serialPort.notifyOnDataAvailable(true);
        serialPort.notifyOnOutputEmpty(true);
        }
        catch(Exception e){
            System.out.println(e.toString());
                            }
        
    }
    //end of portConncet method
    
    //readWrite method
     public void write(short b) {
    	 try {
			output.write((short)b/2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
     }
    public void serialEvent(SerialPortEvent evt) { 
   
       if (evt.getEventType() == SerialPortEvent.DATA_AVAILABLE) { //if data available on serial port
   try {
    String inputLine=input.readLine();
    System.out.println(inputLine);
                                inputName = new Scanner(System.in); //get user name
                                name = inputName.nextLine();
                                name = name + '\n';
                                System.out.printf("%s",name);
                                output.write(name.getBytes());     //sends the user name
   } catch (Exception e) {
    System.err.println(e.toString());
   }
  }
       
    }
    //end of serialEvent method
    
    //closePort method
    public void close(){
        if(serialPort!=null){
            serialPort.close(); //close serial port
        }
        input = null;        //close input and output streams
        output = null;
    }
    //main method
//    public static void main(String[] args) {
//        SerialWrite myTest = new SerialWrite();  //creates an object of the class
//        myTest.initialize();
//        myTest.portConnect();
//        short b = 0;
//        for(int i = 0; i<100000; i++){
//        	if (i % 10 == 0) {
//        		b = (short)((b+1) % 180);//	
//        	}
//        	myTest.serialWrite(b);
//        	System.out.println(b);
//        }
//      System.out.println("Started");
//       //while(1>0);       //wait till any activity
//       myTest.close(); 
//  
        
       
                                            }
//end of main method
// end of  SerialTest class
//}