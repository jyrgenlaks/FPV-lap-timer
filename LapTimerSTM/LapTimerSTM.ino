
#include <EEPROM.h>

const int nrOfChannels = 4;
int minimum_lap_time = 4000;
int threshold_hysterisis = 500;

int rssis[nrOfChannels];  // analog RSSI reading buffer
int states[nrOfChannels]; // bit1: 0 - no laps gone through, 1 - has entered a gate at least once
                          // bit0: 0 - waiting for threshold reach; 
                          // bit0: 1 - recording max values/waiting for exiting the threshold  
int thresholds[nrOfChannels];       //the threshold region for each channel
int last_lap_times[nrOfChannels];   //last lap time in milliseconds
int lap_numbers[nrOfChannels];      //the lap number to that corresponds to the last_lap_time value
int max_values[nrOfChannels];       //maximum RSSI readings
unsigned long lap_start_times[nrOfChannels];  //millis() of the last gate entering
unsigned long max_times[nrOfChannels];        //millis() taken at the highest RSSI reading


void setup() {

  Serial.begin(115200);
  Serial3.begin(115200);
  
  initEEPROM();
  
  resetAll(1);
  
}

void loop() {

  for(int i = 0; i < 100; i++){

    //read the RSSI values
    for(int i = 0; i < nrOfChannels; i++){
      rssis[i] = analogRead(i);
    }
    
    doStatemachine();
    delay(1);
  }
  
  printOutput();

  handleInput();
  
}

void resetAll(int all){
  for(int i = 0; i < nrOfChannels; i++){
    rssis[i] = 0;
    if(all) {
      pinMode(i, INPUT_ANALOG);
      uint16_t th = getThreshold(i);
      if(th == 65535) th = 2000;
      thresholds[i] = th;
    }
    max_values[i] = 0;
    max_times[i] = 9000;
    states[i] = 0;
    lap_start_times[i] = 0;
    last_lap_times[i] = -1;
    lap_numbers[i] = 0;
  }
}

void doStatemachine(){
  //do the fancy statemachine stuff
  for(int i = 0; i < nrOfChannels; i++){
    
    if(  (states[i]&(1 << 0)) == 0  &&  rssis[i] > thresholds[i]  &&
         millis() - lap_start_times[i] > minimum_lap_time  ){
      //waiting for threshold reach  &&  reached the threshold
      states[i] |= (1 << 0);
      
    }

    if(   (states[i]&(1 << 0)) == 1  &&  (rssis[i] < thresholds[i] - threshold_hysterisis)  ){
      //waiting for threshold exit  &&  exited the threshold
      
      //Exited the threshold
      states[i] &= ~(1 << 0); //change the state
      
      if(states[i] & (1 << 1)){
        last_lap_times[i] = max_times[i] - lap_start_times[i];  //calculate the lap time
        lap_numbers[i]++;
      }else{
        //this was the first gate entry, cannot calculate the lap times just yet
        states[i] |= (1 << 1);
      }

      //reset the variables
      lap_start_times[i] = max_times[i];  //set the current lap entering time as the next lap start time
      max_times[i] = 0;  //reset the maximum RSSI reading time
      max_values[i] = 0; //reset the maximum RSSI reading value
      
    }

    if(  (states[i]&(1 << 0)) == 1  ){
      //in threshold range
      if(rssis[i] > max_values[i]){
        max_values[i] = rssis[i];
        max_times[i] = millis();
      }
    }
    
  }
}


void printOutput(){
  //output the result
  for(int i = 0; i < nrOfChannels; i++){
    if(i != 0) Serial.print("&");
    Serial.print(last_lap_times[i]);
    Serial.print("|");
    Serial.print(lap_numbers[i]);
    Serial.print("|");
    Serial.print(rssis[i]);
    Serial.print("|");
    Serial.print(thresholds[i]);
    
    
    /*Serial.print("|states:");
    Serial.print(states[i]);
    Serial.print("|max:");
    Serial.print(max_values[i]);
    Serial.print("|max_tim:");
    Serial.print(max_times[i]);
    Serial.print("|start:");
    Serial.print(lap_start_times[i]);*/
    
  }
  Serial.println();

  //output the result to UART3
  for(int i = 0; i < nrOfChannels; i++){
    if(i != 0) Serial3.print("&");
    Serial3.print(last_lap_times[i]);
    Serial3.print("|");
    Serial3.print(lap_numbers[i]);
    Serial3.print("|");
    Serial3.print(rssis[i]);
    Serial3.print("|");
    Serial3.print(thresholds[i]);
  }
  Serial3.println();
}


void handleInput(){
  //USB connection
  if(Serial.available()){
    delay(5); //wait for the buffer to be filled
    int pos = 0;
    char buf[128];
    
    int channel = -1;
    int newThreshold = 0;
    
    while(Serial.available()){
      char c = Serial.read();
      buf[pos] = c;
      if(buf[0] == 't'  &&  buf[1] == 'h'){
        if(pos == 2){
          channel = c - '0';
        }else if(pos > 2  &&  c != '\n'){
          //Serial.print("Adding ");
          //Serial.println((c - '0'));
          newThreshold *= 10;
          newThreshold += (c - '0');
        }
      }else if(buf[0] == 'r'  &&  buf[1] == 's'  &&  buf[2] == 't'){
        resetAll(0);
      }
      pos += 1;
    }
    for(int i = pos; i < 128; i++){
      buf[i] = 0;
    }
    /*
    Serial.print("Got:");
    Serial.print(buf);
    Serial.println("END");
    */
    if( channel >= 0  && channel < nrOfChannels ){
      /*Serial.print("Changing channel ");
      Serial.print(channel);
      Serial.print(" th to ");
      Serial.print(newThreshold);
      Serial.print("!\n");*/
      thresholds[channel] = newThreshold;
      saveThreshold(channel, newThreshold);
      
    }
  }





  //ESP connection
  if(Serial3.available()){
    delay(5); //wait for the buffer to be filled
    int pos = 0;
    char buf[128];
    
    int channel = -1;
    int newThreshold = 0;
    
    while(Serial3.available()){
      char c = Serial3.read();
      buf[pos] = c;
      if(buf[0] == 't'  &&  buf[1] == 'h'){
        if(pos == 2){
          channel = c - '0';
        }else if(pos > 2  &&  c >= '0'  &&  c <= '9'){
          //Serial.print("Adding ");
          //Serial.println((c - '0'));
          newThreshold *= 10;
          newThreshold += (c - '0');
        }
      }else if(buf[0] == 'r'  &&  buf[1] == 's'  &&  buf[2] == 't'){
        resetAll(0);
      }
      pos += 1;
    }
    for(int i = pos; i < 128; i++){
      buf[i] = 0;
    }
    /*
    Serial.print("Got:");
    Serial.print(buf);
    Serial.println("END");
    */
    if( channel >= 0  && channel < nrOfChannels ){
      /*Serial.print("Changing channel ");
      Serial.print(channel);
      Serial.print(" th to ");
      Serial.print(newThreshold);
      Serial.print("!\n");*/
      thresholds[channel] = newThreshold;
      saveThreshold(channel, newThreshold);
    }
  }
}

//EEPROM STUFF
void initEEPROM(){
  EEPROM.init();
  EEPROM.PageBase0 = 0x801F000;
  EEPROM.PageBase1 = 0x801F800;
  EEPROM.PageSize  = 0x400;
}

void saveThreshold(uint8_t craft, uint16_t threshold){
  EEPROM.write(0x10+craft, threshold);
}

uint16_t getThreshold(int craft){
  uint16_t Data;
  EEPROM.read(0x10+craft, &Data);
  return Data;
}












