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
    definition(name: "Z-Wave GE Dual Dimmer", namespace: "sudoursa", author: "Thomas Slaymaker") {
        capability "Switch Level"
        capability "Actuator"
        capability "Indicator"
        capability "Switch"
        capability "Polling"
        capability "Refresh"
        capability "Sensor"
        capability "Health Check"

        fingerprint mfr: "0063", prod: "5044", model: "3132", deviceJoinName: "Z-Wave GE Dual Outlet Dimmer"
    }

    tiles(scale: 2) {
        multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
            tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label: '${name}', action: "switch.off",
                        icon: "st.switches.switch.on", backgroundColor: "#00a0dc", nextState: "turningOff"
                attributeState "off", label: '${name}', action: "switch.on",
                        icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
                attributeState "turningOn", label: '${name}', action: "switch.off",
                        icon: "st.switches.switch.on", backgroundColor: "#00a0dc", nextState: "turningOff"
                attributeState "turningOff", label: '${name}', action: "switch.on",
                        icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
            }
            tileAttribute("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action: "switch level.setLevel"
            }
        }

        childDeviceTile("outlet1", "outlet1", height: 2, width: 2, childTileName: "switch")
        childDeviceTile("outlet2", "outlet2", height: 2, width: 2, childTileName: "switch") 
        childDeviceTile("levelSliderControl1", "outlet1", height: 2, width: 2, childTileName: "level")
        childDeviceTile("levelSliderControl2", "outlet2", height: 2, width: 2, childTileName: "level")
        standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", label: '', action: "refresh.refresh", icon: "st.secondary.refresh"
        }
                      
        main("switch")
        details(["switch", "outlet1", "levelSliderControl1", "refresh", "outlet2", "levelSliderControl2"])
    }
}

void installed() {
	initialize()
}

private initialize() {
	log.debug "initialize()"
	// outlet one
    addChildDevice(
       	"Z-Wave GE Dual Dimmer Outlet", 
        "${device.deviceNetworkId}-ep1", 
        null, 
        [completedSetup:true, label:"${device.displayName} (CH1)", isComponent:true, componentName:"outlet1", componentLabel:"Outlet 1"
    ])
    // outlet two
    addChildDevice(
       	"Z-Wave GE Dual Dimmer Outlet", 
        "${device.deviceNetworkId}-ep2", 
        null, 
        [completedSetup:true, label:"${device.displayName} (CH2)", isComponent:true, componentName:"outlet2", componentLabel:"Outlet 2"
    ])
}

def updated() {
    log.debug "Updated with settings: ${settings}"
	if (device.label != state.oldLabel) {
        childDevices.each {
            if (it.label == "${state.oldLabel} (CH${channelNumber(it.deviceNetworkId)})") {
                def newLabel = "${device.displayName} (CH${channelNumber(it.deviceNetworkId)})"
                it.setLabel(newLabel)
                it.save()
            }
        }
        state.oldLabel = device.label
    }
}

def parse(String description) {
    def result = []
    def cmd = zwave.parse(description)
    if (cmd) {
        result += zwaveEvent(cmd)
        log.debug "Parsed ${cmd} to ${result.inspect()}"
    } else {
        log.debug "Non-parsed event: ${description}"
    }
    return result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd, value=null) {
    log.debug "BasicSet ${cmd} with a child of ${value}"
    def result = createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "digital")
    def cmds = []
    cmds << encap(zwave.switchMultilevelV2.switchMultilevelGet(), 1)
    cmds << encap(zwave.switchMultilevelV2.switchMultilevelGet(), 2)
    return [result, response(commands(cmds))] // returns the result of response()
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd, ep = null) {
    log.debug "SwitchMultilevelReport ${cmd} - ep${ep}"
    if (ep) {
        def event
        def childDevice = childDevices.find {
            it.deviceNetworkId == "$device.deviceNetworkId-ep$ep"
        }
        if (childDevice) {
            childDevice.sendEvent(name: "switch", value: cmd.value ? "on" : "off")
            if (cmd.value && cmd.value <= 100) {
            	childDevice.sendEvent(name: "level", value: cmd.value == 99 ? 100 : cmd.value)
            }
        }
        if (cmd.value) {
            event = [createEvent([name: "switch", value: "on"])]
        } else {
            def allOff = true
            childDevices.each {
                n ->
                    if (n.currentState("switch").value != "off") allOff = false
            }
            if (allOff) {
                event = [createEvent([name: "switch", value: "off"])]
            } else {
                event = [createEvent([name: "switch", value: "on"])]
            }
        }
        return event
    } else {
        def result = createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "digital")
        def cmds = []
        cmds << encap(zwave.switchMultilevelV2.switchMultilevelGet(), 1)
        cmds << encap(zwave.switchMultilevelV2.switchMultilevelGet(), 2)
        return [result, response(commands(cmds))] // returns the result of response()
    }
}


def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
    log.debug "MultiChannelCmdEncap ${cmd}"
    def encapsulatedCommand = cmd.encapsulatedCommand([0x32: 3, 0x25: 1, 0x20: 1])
    if (encapsulatedCommand) {
        zwaveEvent(encapsulatedCommand, cmd.sourceEndPoint as Integer)
    }
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
    log.debug "BasicReport ${cmd} - ep${ep}"
    if (ep) {
        def event
        childDevices.each {
            childDevice ->
                if (childDevice.deviceNetworkId == "$device.deviceNetworkId-ep$ep") {
                    childDevice.sendEvent(name: "switch", value: cmd.value ? "on" : "off")
                }
        }
        if (cmd.value) {
            event = [createEvent([name: "switch", value: "on"])]
        } else {
            def allOff = true
            childDevices.each {
                n ->
                    if (n.deviceNetworkId != "$device.deviceNetworkId-ep$ep" && n.currentState("switch").value != "off") allOff = false
            }
            if (allOff) {
                event = [createEvent([name: "switch", value: "off"])]
            } else {
                event = [createEvent([name: "switch", value: "on"])]
            }
        }
        return event
    }
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    // This will capture any commands not handled by other instances of zwaveEvent
    // and is recommended for development so you can see every command the device sends
    log.error "Unhandled Event: ${cmd}"
    [:]
}

def on() {
    log.debug "on()"
    commands([
            zwave.switchAllV1.switchAllOn(),
            encap(zwave.switchMultilevelV2.switchMultilevelGet(), 1),
            encap(zwave.switchMultilevelV2.switchMultilevelGet(), 2)
    ])
}

def off() {
    log.debug "off()"
    commands([
            zwave.switchAllV1.switchAllOff(),
            encap(zwave.switchMultilevelV2.switchMultilevelGet(), 1),
            encap(zwave.switchMultilevelV2.switchMultilevelGet(), 2)
    ], 3000)
}

def setLevel(value) {
    log.debug "setLevel >> value: $value"
    def valueaux = value as Integer
    def level = Math.max(Math.min(valueaux, 99), 0)
    log.debug "both channel setLevel >> value: $value"
    if (level > 0) {
        sendEvent(name: "switch", value: "on")
    } else {
        sendEvent(name: "switch", value: "off")
    }
    sendEvent(name: "level", value: valueaux, unit: "%")
    delayBetween([zwave.switchMultilevelV1.switchMultilevelSet(value: level).format(), zwave.switchMultilevelV1.switchMultilevelGet().format()], 5000)
}

def refresh() {
    log.debug "refresh()"
    commands([
            encap(zwave.switchMultilevelV2.switchMultilevelGet() , 1),
            encap(zwave.switchMultilevelV2.switchMultilevelGet() , 2),
    ])
}

def poll() {
    log.debug "poll()"
    commands([
            encap(zwave.switchMultilevelV2.switchMultilevelGet(), 1),
            encap(zwave.switchMultilevelV2.switchMultilevelGet(), 2),
    ])
}

def ping() {
    log.debug "ping()"
    refresh()
}


//child device methods
void childSetLevel(value, String dni) {
	log.debug "childSetLevel($dni,$value)"   
    def childDevice = childDevices.find {
    	it.deviceNetworkId == "$dni"
    } 
    def valueaux = value as Integer
    def level = Math.max(Math.min(valueaux, 99), 0)
    if (level > 0) {
        childDevice.sendEvent(name: "switch", value: "on")
    } else {
        childDevice.sendEvent(name: "switch", value: "off")
    }
    def cmds = []
    cmds << new physicalgraph.device.HubAction(command(encap(zwave.switchMultilevelV1.switchMultilevelSet(value: level), channelNumber(dni))))
    cmds << new physicalgraph.device.HubAction(command(encap(zwave.switchMultilevelV1.switchMultilevelGet(), channelNumber(dni))))
    sendHubCommand(cmds, 1000)
}

void childOn(String dni) {
    log.debug "childOn($dni)"
    def cmds = []
    cmds << new physicalgraph.device.HubAction(command(encap(zwave.basicV1.basicSet(value: 0xFF), channelNumber(dni))))
    cmds << new physicalgraph.device.HubAction(command(encap(zwave.switchMultilevelV2.switchMultilevelGet(), channelNumber(dni))))
    sendHubCommand(cmds, 1000)
}

void childOff(String dni) {
    log.debug "childOff($dni)"
    def cmds = []
    cmds << new physicalgraph.device.HubAction(command(encap(zwave.basicV1.basicSet(value: 0x00), channelNumber(dni))))
    cmds << new physicalgraph.device.HubAction(command(encap(zwave.switchMultilevelV2.switchMultilevelGet(), channelNumber(dni))))
    sendHubCommand(cmds, 1000)

}

void childRefresh(String dni) {
    log.debug "childRefresh($dni)"
    def cmds = []
    cmds << new physicalgraph.device.HubAction(command(encap(zwave.switchMultilevelV2.switchMultilevelGet(), channelNumber(dni))))
    sendHubCommand(cmds, 1000)
}


// Utility Methods
private encap(cmd, endpoint) {
    if (endpoint) {
        zwave.multiChannelV3.multiChannelCmdEncap(destinationEndPoint: endpoint).encapsulate(cmd)
    } else {
        cmd
    }
}

private command(physicalgraph.zwave.Command cmd) {
    if (state.sec) {
        zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
    } else {
        cmd.format()
    }
}

private commands(commands, delay = 1000) {
    delayBetween(commands.collect {
        command(it)
    }, delay)
}

private channelNumber(String dni) {
    dni.split("-ep")[-1] as Integer
}