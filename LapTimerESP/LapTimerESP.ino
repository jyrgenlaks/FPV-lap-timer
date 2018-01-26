#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>

const char *ssid = "Laptimer";
const char *password = "FPV Tartu";

char dataToSend[1024];
int bufferpos = 0;
char serialBuffer[1024];

ESP8266WebServer server(80);

void handleRoot() {
  server.send(200, "text/plain", "FPV Tartu lap timer. Built by J&uuml;rgen Laks");
  
}

void handleNotFound() {
  String message = "File Not Found\n\n";
  message += "URI: ";
  message += server.uri();
  message += "\nMethod: ";
  message += ( server.method() == HTTP_GET ) ? "GET" : "POST";
  message += "\nArguments: ";
  message += server.args();
  message += "\n";

  for ( uint8_t i = 0; i < server.args(); i++ ) {
    message += " " + server.argName ( i ) + ": " + server.arg ( i ) + "\n";
  }

  server.send ( 404, "text/plain", message );
}

int i = 0;

void setup(void){
  Serial.begin(115200);
  WiFi.mode(WIFI_AP);
  WiFi.softAP(ssid, password);
  Serial.println("");

  IPAddress myIP = WiFi.softAPIP();
  Serial.print("AP IP address: ");
  Serial.println(myIP);

  server.on("/", handleRoot);

  server.on("/get", [](){
    server.send(200, "text/plain", dataToSend);
  });

  server.on("/set", [](){
    for ( uint8_t i = 0; i < server.args(); i++ ) {
      Serial.println(server.arg(i));
    }
  
    server.send(200, "text/plain", server.args()>0?"forwarded!":"no params!");
  });
  
  server.onNotFound ( handleNotFound );
  server.begin();
  Serial.println("HTTP server started");
}



void loop(void){
  server.handleClient();
  
  if(Serial.available()){
    char c = Serial.read();
    if(c != '\n'){
      //Serial.print("Got: ");
      //Serial.println(c);
      if(bufferpos < 1024){
        serialBuffer[bufferpos++] = c;
      }
    }else{
      //Serial.println("----");
      //Serial.println(serialBuffer);

      for(int i = 0; i < bufferpos; i++){
        dataToSend[i] = serialBuffer[i];
      }
      for(int i = bufferpos; i < 1024; i++){
        dataToSend[i] = 0;
      }
      bufferpos = 0;
      //Serial.println(dataToSend);
    }
  }
}
