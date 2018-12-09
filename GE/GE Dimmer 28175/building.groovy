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

        fingerprint mfr: "0063", prod: "5044", model: "3132", deviceJoinName: "GE Plug-In Dual Dimmer"
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

        standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", label: '', action: "refresh.refresh", icon: "st.secondary.refresh"
        }

        main("switch")

        details(["switch", "refresh"])
    }
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    if (!childDevices) {
        createChildDevices()
    } else if (device.label != state.oldLabel) {
        childDevices.each {
            if (it.label == "${state.oldLabel} (CH${channelNumber(it.deviceNetworkId)})") {
                def newLabel = "${device.displayName} (CH${channelNumber(it.deviceNetworkId)})"
                it.setLabel(newLabel)
            }
        }
        state.oldLabel = device.label
    }
    def commands = []
    commands << new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [ledIndicator == "on" ? 1 : ledIndicator == "never" ? 2 : 0], parameterNumber: 3, size: 1).format())
    commands << new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [invertSwitch == true ? 1 : 0], parameterNumber: 4, size: 1).format())
    commands << new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [zwaveSteps], parameterNumber: 7, size: 1).format())
    commands << new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [zwaveDelay], parameterNumber: 8, size: 1).format())
    commands << new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [manualSteps], parameterNumber: 9, size: 1).format())
    commands << new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [manualDelay], parameterNumber: 10, size: 1).format())
    commands << new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [allonSteps], parameterNumber: 11, size: 1).format())
    commands << new physicalgraph.device.HubAction(zwave.configurationV1.configurationSet(configurationValue: [allonDelay], parameterNumber: 12, size: 1).format())
    commands << new physicalgraph.device.HubAction(zwave.configurationV1.configurationGet(parameterNumber: 3).format())
    commands << new physicalgraph.device.HubAction(zwave.configurationV1.configurationGet(parameterNumber: 4).format())
    commands << new physicalgraph.device.HubAction(zwave.configurationV1.configurationGet(parameterNumber: 7).format())
    commands << new physicalgraph.device.HubAction(zwave.configurationV1.configurationGet(parameterNumber: 8).format())
    commands << new physicalgraph.device.HubAction(zwave.configurationV1.configurationGet(parameterNumber: 9).format())
    commands << new physicalgraph.device.HubAction(zwave.configurationV1.configurationGet(parameterNumber: 10).format())
    commands << new physicalgraph.device.HubAction(zwave.configurationV1.configurationGet(parameterNumber: 11).format())
    commands << new physicalgraph.device.HubAction(zwave.configurationV1.configurationGet(parameterNumber: 12).format())
    sendHubCommand(commands, 1500)
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


def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd, ep = null) {
    log.debug "SwitchMultilevelReport ${cmd} - ep ${ep}"
    if (ep) {
        def event
        def childDevice = childDevices.find {
            it.deviceNetworkId == "$device.deviceNetworkId-ep$ep"
            log.info "Child device ID: ${it.deviceNetworkId}"
        }
        if (childDevice) {
            childDevice.sendEvent(name: "switch", value: cmd.value ? "on" : "off")
            log.info "Child device event created for ${childDevice}"
        }
        if (cmd.value) {
            event = [createEvent([name: "switch", value: "on"])]
            log.info "Command Value is ${cmd.value}"
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
        log.info "hit else block in MultilevelReport block"
        def result = createEvent(name: "switch", value: cmd.value ? "on" : "off", type: "digital")
        def cmds = []
        cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), 1)
        cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), 2)
        return [result, response(commands(cmds))] // returns the result of reponse()
    }
}


def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
    log.debug "MultiChannelCmdEncap ${cmd}"
    def encapsulatedCommand = cmd.encapsulatedCommand([0x32: 3, 0x25: 1, 0x20: 1])
    log.info "Encapsulated Command is ${encapsulatedCommand}"
    if (encapsulatedCommand) {
        zwaveEvent(encapsulatedCommand, cmd.sourceEndPoint as Integer)
    }
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
    // this is to catch basic reports, not sure what to do with it yet... seems to be the dimmer level from the last endpoint that was changed
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    // This will capture any commands not handled by other instances of zwaveEvent
    // and is recommended for development so you can see every command the device sends
    log.error "Unhandled Event from catchall: ${cmd}"
}

private channelNumber(String dni) {
    dni.split("-ep")[-1] as Integer
}

private void createChildDevices() {
    state.oldLabel = device.label
    for (i in 1..2) {
        addChildDevice("Child Channel", "${device.deviceNetworkId}-ep${i}", null, [completedSetup: true, label: "${device.displayName} (CH${i})",
                                                                                         isComponent   : false, componentName: "ep$i", componentLabel: "Channel $i"
        ])
    }
}