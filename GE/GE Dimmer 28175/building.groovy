/**
 *  GE Smart Dual Channel Dimmer (28175)
 *  Author: Thomas Slaymaker (sudoursa)
 *  Date: 2018-12-06
 *
 *  Copyright 2018 Thomas Slaymaker
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 */

metadata {
    definition(name: "GE 28175 (ZW3106) Plug-In Dual Smart Dimmer", namespace: "sudoursa", author: "Thomas Slaymaker") {
        capability "Switch Level"
        capability "Actuator"
        capability "Indicator"
        capability "Switch"
        capability "Polling"
        capability "Refresh"
        capability "Sensor"
        capability "Health Check"

        fingerprint mfr: "0063", prod: "4457", deviceJoinName: "Z-Wave Wall Dimmer"
        fingerprint mfr: "0063", prod: "4944", deviceJoinName: "GE Z-Wave Wall Dimmer"
        fingerprint mfr: "0063", prod: "5044", deviceJoinName: "Z-Wave Plug-In Dimmer"
        fingerprint mfr: "0063", prod: "5044", model: "3132", deviceJoinName: "GE Plug-In Dual Dimmer"
    }

    tiles(scale: 2) {
        standardTile("switch", "device.switch", width: 6, height: 4, canChangeIcon: true) {
            state "off", label:'${currentValue}', action:"switch.on",
                    icon:"st.switches.switch.off", backgroundColor: "#ffffff"
            state "on", label:'${currentValue}', action:"switch.on",
                    icon:"st.switches.switch.off", backgroundColor: "#00a0dc"
        }

        valueTile("level", "device.level", decoration: "flat", width: 2, height: 2){
            state "level", label:'${currentValue} Level'
        }

        main("switch")

        details(["switch", "level"])
    }
}