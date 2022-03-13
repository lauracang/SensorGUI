/*
  ServoListener
 */
#include <Servo.h>
Servo myservo;

const int servoPin = 10;      // the pin that the LED is attached to

void setup() {
  // initialize the serial communication:
  Serial.begin(512000);
  // Attach the servoPin to the servo object
  myservo.attach(servoPin);

}

void loop() {
  byte pos;
  // check if data has been sent from the computer:
  if (Serial.available()) {
    // read the most recent byte:
    pos = Serial.read();
    // set the brightness of the LED:
    myservo.write((int) pos);
  }
}
