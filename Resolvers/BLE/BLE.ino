/**************** OTA HEADERS ****************/
#include <WiFi.h>
#include <ESPmDNS.h>
#include <NetworkUdp.h>
#include <ArduinoOTA.h>

/**************** BLE HEADERS ****************/
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEBeacon.h>
#include <BLEAdvertising.h>

#define LED 2  // NOT USED NOW – but kept if needed later
#define BEACON_UUID "2D7A9F0C-E0E8-4CC9-A71B-A21DB2D034A1"

/************* WiFi Credentials *************/
const char *ssid = "Acer";
const char *password = "Sarthak@017";

/************* BLE Objects *************/
BLEAdvertising *pAdvertising;
BLEBeacon oBeacon;

/************* RGB LED (Common Anode) *************/
#define RED_LED    14
#define GREEN_LED  12
#define BLUE_LED   13   // optional, not used now

// COMMON ANODE LOGIC — LOW = ON, HIGH = OFF
void rgbRed() {
  digitalWrite(RED_LED, LOW);
  digitalWrite(GREEN_LED, HIGH);
  digitalWrite(BLUE_LED, HIGH);
}
void rgbGreen() {
  digitalWrite(RED_LED, HIGH);
  digitalWrite(GREEN_LED, LOW);
  digitalWrite(BLUE_LED, HIGH);
}
void rgbOff() {
  digitalWrite(RED_LED, HIGH);
  digitalWrite(GREEN_LED, HIGH);
  digitalWrite(BLUE_LED, HIGH);
}

/**************** BLE UPDATE FUNCTION ****************/
void updateBeacon(int txPowerLevel, int signalPower) {
  BLEDevice::setPower((esp_power_level_t)txPowerLevel);
  oBeacon.setSignalPower(signalPower);

  BLEAdvertisementData advertisementData;
  advertisementData.setFlags(0x04);

  String strServiceData = "";
  strServiceData += (char)26;
  strServiceData += (char)0xFF;
  strServiceData += oBeacon.getData();
  advertisementData.addData(strServiceData);

  pAdvertising->stop();
  pAdvertising->setAdvertisementData(advertisementData);
  pAdvertising->start();

  Serial.print("Updated Tx Power: ");
  Serial.print(txPowerLevel);
  Serial.print(", RSSI @1m: ");
  Serial.println(signalPower);
}

/**************** SETUP ****************/
void setup() {
  Serial.begin(115200);
  Serial.println("Booting...");

  /************** RGB LED INIT **************/
  pinMode(RED_LED, OUTPUT);
  pinMode(GREEN_LED, OUTPUT);
  pinMode(BLUE_LED, OUTPUT);
  rgbRed();   // Booting = RED

  /************** WIFI + OTA **************/
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  while (WiFi.waitForConnectResult() != WL_CONNECTED) {
    Serial.println("Connection Failed! Rebooting...");
    delay(3000);
    ESP.restart();
  }

  Serial.println("WiFi Connected");
  rgbRed();   // Still RED until BLE starts

  ArduinoOTA
      .onStart([]() {
        String type = (ArduinoOTA.getCommand() == U_FLASH) ? "sketch" : "filesystem";
        Serial.println("Start updating " + type);
      })
      .onEnd([]() { Serial.println("\nEnd"); })
      .onProgress([](unsigned int progress, unsigned int total) {
        Serial.printf("Progress: %u%%\r", (progress / (total / 100)));
      })
      .onError([](ota_error_t error) {
        Serial.printf("Error[%u]: ", error);
        if (error == OTA_AUTH_ERROR) Serial.println("Auth Failed");
        else if (error == OTA_BEGIN_ERROR) Serial.println("Begin Failed");
        else if (error == OTA_CONNECT_ERROR) Serial.println("Connect Failed");
        else if (error == OTA_RECEIVE_ERROR) Serial.println("Receive Failed");
        else if (error == OTA_END_ERROR) Serial.println("End Failed");
      });

  ArduinoOTA.begin();

  Serial.println("OTA Ready!");
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP());

  /************** BLE BEACON SETUP **************/
  Serial.println("\nStarting Dynamic iBeacon...");
  BLEDevice::init("ESP32_Dynamic_Beacon");

  oBeacon.setManufacturerId(0x004C);
  oBeacon.setProximityUUID(BLEUUID(BEACON_UUID));
  oBeacon.setMajor(100);
  oBeacon.setMinor(1);
  oBeacon.setSignalPower(-59);

  pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->setScanResponse(false);
  pAdvertising->setMinPreferred(0x06);
  pAdvertising->setMaxPreferred(0x12);

  updateBeacon(ESP_PWR_LVL_P3, -59);

  rgbGreen();  // BLE started → GREEN
  Serial.println("BLE started successfully → GREEN LED");

  Serial.println("========= Dynamic iBeacon Control =========");
  Serial.println("Enter power level (0–7) in Serial Monitor:");
  Serial.println("Example: 5");
  Serial.println("===========================================\n");
}

/**************** MAIN LOOP ****************/
void loop() {
  ArduinoOTA.handle();

  /************** BLE POWER CONTROL VIA SERIAL **************/
  if (Serial.available()) {
    String input = Serial.readStringUntil('\n');
    input.trim();
    int txLevel = input.toInt();

    if (txLevel >= 0 && txLevel <= 7) {
      int signalPower;
      switch (txLevel) {
        case 0: signalPower = -75; break;
        case 1: signalPower = -72; break;
        case 2: signalPower = -68; break;
        case 3: signalPower = -65; break;
        case 4: signalPower = -62; break;
        case 5: signalPower = -59; break;
        case 6: signalPower = -56; break;
        case 7: signalPower = -52; break;
      }

      updateBeacon(txLevel, signalPower);

      Serial.print("Power Level Set to ");
      Serial.println(txLevel);
    } else {
      Serial.println("Invalid input. Enter 0–7.");
    }
  }
}
