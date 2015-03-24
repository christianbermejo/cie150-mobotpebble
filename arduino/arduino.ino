/*
Authors:
John Garcia
Christian Bermejo
*/
// include the library code:
#include <string.h>
#include <ctype.h>
#include <SoftwareSerial.h>
#include <stdio.h>  

//define RX and TX
#define RxD 14
#define TxD 15

//Variable Declaration

//Pebble Variables
// 12 is the max length of a decimal representation of a 32-bit integer
// including space for a leading minus sign and terminating null byte
char intBuffer[12];

String intData = "";
int delimiter = (int) '\n'; //int value is 10
int count = 1;

SoftwareSerial bt(RxD,TxD);

int xpass, ypass, zpass;

//MOBOT Variables
int i;
int run1 = 9; // 0 to 255, 255 = max, 0 = stop
int run2 = 10;
int dir1 = 8;
int dir2 = 11;

void setup() {        
  Serial.begin(9600);
  bt.setTimeout(100);
  bt.begin(9600);
  Serial.println("Pebble-Android-Arduino Test");
  
  /*
  for(i=2; i <=7;i=i+1) 
    pinMode(i, INPUT); //sensors
  for(i=8; i <=11;i=i+1) {
      pinMode(i, OUTPUT);  //motor control
  }  
  delay(10);
  //stop motors 
  for(i=8; i <=11;i=i+1)
    digitalWrite(i,LOW);
  */
}

void loop() { 

  while (bt.available()) {
    int ch = bt.read();
    if (ch == -1) {
      Serial.println("-1 received");
      break;
    } else if (ch == delimiter) {
      //if (ch == 10)
      print_value();
      count++;
      break;
    } else if (ch == 0) {
      break;
    } else {
      intData += (char) ch;
    }
  }
  
  delay(5);
  /*
  Serial.println();
  Serial.println(xpass);
  Serial.println(ypass);
  Serial.println(zpass);
  */
   // cmd1(200,0,200,0);   //reverse
   // cmd1(200,1,150,0);   //right
   // cmd1(150,0,150,1);   //left
   // cmd1(150,1,150,1);   //forward
   // cmd1(0,0,0,0);       //stop
   
   if ((xpass > 800) && (xpass < 1200) && (zpass < 0))
  {
      cmd1(200,1,180,1);   //forward
  }
  else if ((xpass > 800) && (xpass < 1200) && (zpass > 0))
  {
      cmd1(200,0,180,0);   //reverse
  }
  else if ((ypass < 0) && (xpass < 300))
  {
      cmd1(200,1,150,0);   //right
  }
  else if ((ypass > 0) && (xpass < 300))
  {
      cmd1(150,0,200,1);   //left
  }
  else 
  {
      cmd1(0,0,0,0);   //stop
  }
  
 delay(5);
} 

void cmd1(int speed1, int mydir1, int speed2, int mydir2 )  {
  // motor1
  digitalWrite(dir1,mydir1); //direction control of motor1, 1 = forward
  analogWrite(run1,speed1);  //speed control of motor1, 0 =stop, 255 =fastest
  // motor2
  digitalWrite(dir2,mydir2); //direction control of motor2, 1 = forward
  analogWrite(run2,speed2);  //speed control of motor2, 0 =stop, 255 =fastest
}  

void print_value() {
      // Copy read data into char array for use by atoi
      // Include room for null terminator
      int intLength = intData.length() + 1;
      intData.toCharArray(intBuffer, intLength);
      // Reinitialize intData for next loop
      intData = "";
      // Convert ASCII-encoded integer to int
      int p = atoi(intBuffer);
      //
      int a = p;
      
      // STUPID CHECK FOR count
      // Please optimize me if you can
      if (count==1) {
        Serial.print("x: ");
        xpass = a;        
      }
      
      if (count==2) {
        Serial.print("y: ");
        ypass = a;
      }
      
      if (count==3) {
        Serial.print("z: ");
        zpass = a;
        //reset count for next x,y,z
        count=0;
      }
      
      Serial.println(a);
      //Serial.println(i);
 
}
