# FPV-lap-timer
Wireless lap timer software for FPV racing quads.

## Structure
* Android - contains the Android client application
  * Speaks out the lap times
  * Logs lap times
  * Configures the STM thresholds
* LapTimerESP - contains the ESP8266 code
  * Transfers the data between the STM and Android
* LapTimerSTM - contains the STM32F103C8T6 code
  * Reads the ADCs
  * Runs a statemachine to monitor each quad's position on the track and counts the laps
  * Uploads data on USB serial and serial3 (to ESP)
