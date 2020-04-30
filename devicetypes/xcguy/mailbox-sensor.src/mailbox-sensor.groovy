/**
 *  Copyright 2015 SmartThings
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
 *  Mailbox Sensor
 *
 *  Author: Bruce Adelsman
 *  Date: 2015-10-30
 */

metadata {
	definition (name: "Mailbox Sensor", namespace: "xcguy", author: "Bruce Adelsman") {
		capability "Contact Sensor"
		capability "Presence Sensor"
		capability "Battery"
		capability "Configuration"

    	attribute "mailbox", "string"
        
        command "setFull"
        command "setEmpty"
        command "reset"

	}
    
    preferences {
    	input "fullDetect", "enum", title: "Full detection method:",
       		options: [
            	"first access",
                "toggle"
                ],
        	defaultValue: "first access", required: true,
            description: "Mailbox is determined to be full on only the first access of the day or on every other access (toggle)."
	}

	// simulator metadata
	simulator {
		// status messages
		status "open":  "command: 2001, payload: FF"
		status "closed": "command: 2001, payload: 00"
        status "full": "full"
        status "empty": "empty"
	}

	// UI tile definitions
	tiles {
		standardTile("mailbox", "device.mailbox", width: 2, height: 2) {
		//	state "full", label: '${name}', icon: "st.quirky.nimbus.quirky-nimbus-mail", backgroundColor: "#ffa81e", action: "setEmpty"
		//	state "empty", label: '${name}', icon: "st.quirky.nimbus.quirky-nimbus-mail", backgroundColor: "#79b821",  action: "setFull"
			state "full", label: '${name}', icon: "st.quirky.nimbus.quirky-nimbus-mail", backgroundColor: "#ffa81e"
			state "empty", label: '${name}', icon: "st.quirky.nimbus.quirky-nimbus-mail", backgroundColor: "#79b821"
}
		standardTile("contact", "device.contact" ) {
			state "open", label: '${name}', icon: "st.contact.contact.open", backgroundColor: "#ffa81e"
			state "closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#79b821"
		}
        standardTile("reset", "device.contact", width: 1, height: 1, canChangeIcon: false, canChangeBackground: false, decoration: "flat") {
			state "default", label: "Reset", icon: "", action: "reset"
        }               
		valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat") {
			state "battery", label:'${currentValue}% battery', unit:""
		}

		main("mailbox")
		details(["mailbox", "contact", "reset", "battery"])
	}
}

def parse(String description) {
	def result = null
	if (description.startsWith("Err 106")) {
		if (state.sec) {
			log.debug description
		} else {
			result = createEvent(
				descriptionText: "This sensor failed to complete the network security key exchange. If you are unable to control it via SmartThings, you must remove it from your network and add it again.",
				eventType: "ALERT",
				name: "secureInclusion",
				value: "failed",
				isStateChange: true,
			)
		}
	} else if (description == "empty" || description == "full") {
   		result = createEvent(name: "mailbox", value: "$description")
    } else if (description != "updated") {
		def cmd = zwave.parse(description, [0x20: 1, 0x25: 1, 0x30: 1, 0x31: 5, 0x80: 1, 0x84: 1, 0x71: 3, 0x9C: 1])
		if (cmd) {
			result = zwaveEvent(cmd)
		}
	}
	log.debug "parsed '$description' to $result"
	return result
}

def setFull() {
	log.debug "Mailbox set to full"
	sendEvent(name: "mailbox", value: "full")
    sendEvent(name: 'presence', value: 'present')
}

def setEmpty() {
	log.debug "Mailbox emptied"
	sendEvent(name: "mailbox", value: "empty")
	sendEvent(name: 'presence', value: 'not present')
}

def reset() {
	setEmpty()
    state.lastDeliveryDay = 0
}

def isEmpty() {
	return device.currentValue("mailbox") == "empty"
}

def isFull() {
	return device.currentValue("mailbox") == "full"
}

def install() {
	state.lastDeliveryDay = 0
}

def updated() {
	state.lastDeliveryDay = state.lastDeliveryDay ?: 0
	def cmds = []
	if (!state.MSR) {
		cmds = [
			zwave.manufacturerSpecificV2.manufacturerSpecificGet().format(),
			"delay 1200",
			zwave.wakeUpV1.wakeUpNoMoreInformation().format()
		]
	} else if (!state.lastbat) {
		cmds = []
	} else {
		cmds = [zwave.wakeUpV1.wakeUpNoMoreInformation().format()]
	}
	response(cmds)
}

def configure() {
	delayBetween([
		zwave.manufacturerSpecificV2.manufacturerSpecificGet().format(),
		batteryGetCommand()
	], 6000)
}

def processDoorClose() {
	log.debug "Processing mailbox door closure (detection: $fullDetect)"
    def currentDay = new Date().format("d", location.timeZone)
    // if first access on the day or full detection is toggle and mailbox is empty
    if (state.lastDeliveryDay != currentDay || (fullDetect == "toggle" && isEmpty())) {
 		setFull()
    } 
    else {
    // else if (isFull()) {
 		setEmpty()
    }
    state.lastDeliveryDay = currentDay
}

def sensorValueEvent(value) {
	if (value) {
		createEvent(name: "contact", value: "open", descriptionText: "$device.displayName is open")
	} else {
    	processDoorClose()
		createEvent(name: "contact", value: "closed", descriptionText: "$device.displayName is closed")
	}
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd)
{
	sensorValueEvent(cmd.value)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd)
{
	sensorValueEvent(cmd.value)
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd)
{
	sensorValueEvent(cmd.value)
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd)
{
	sensorValueEvent(cmd.sensorValue)
}

def zwaveEvent(physicalgraph.zwave.commands.sensoralarmv1.SensorAlarmReport cmd)
{
	sensorValueEvent(cmd.sensorState)
}

def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd)
{
	def result = []
	if (cmd.notificationType == 0x06 && cmd.event == 0x16) {
		result << sensorValueEvent(1)
	} else if (cmd.notificationType == 0x06 && cmd.event == 0x17) {
		result << sensorValueEvent(0)
	} else if (cmd.notificationType == 0x07) {
		if (cmd.v1AlarmType == 0x07) {  // special case for nonstandard messages from Monoprice door/window sensors
			result << sensorValueEvent(cmd.v1AlarmLevel)
		} else if (cmd.event == 0x01 || cmd.event == 0x02) {
			result << sensorValueEvent(1)
		} else if (cmd.event == 0x03) {
			result << createEvent(descriptionText: "$device.displayName covering was removed", isStateChange: true)
			result << response(zwave.wakeUpV1.wakeUpIntervalSet(seconds:4*3600, nodeid:zwaveHubNodeId))
			if(!state.MSR) result << response(zwave.manufacturerSpecificV2.manufacturerSpecificGet())
		} else if (cmd.event == 0x05 || cmd.event == 0x06) {
			result << createEvent(descriptionText: "$device.displayName detected glass breakage", isStateChange: true)
		} else if (cmd.event == 0x07) {
			if(!state.MSR) result << response(zwave.manufacturerSpecificV2.manufacturerSpecificGet())
			result << createEvent(name: "motion", value: "active", descriptionText:"$device.displayName detected motion")
		}
	} else if (cmd.notificationType) {
		def text = "Notification $cmd.notificationType: event ${([cmd.event] + cmd.eventParameter).join(", ")}"
		result << createEvent(name: "notification$cmd.notificationType", value: "$cmd.event", descriptionText: text, displayed: false)
	} else {
		def value = cmd.v1AlarmLevel == 255 ? "active" : cmd.v1AlarmLevel ?: "inactive"
		result << createEvent(name: "alarm $cmd.v1AlarmType", value: value, displayed: false)
	}
	result
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd)
{
	def event = createEvent(descriptionText: "${device.displayName} woke up", isStateChange: false)
	def cmds = []
	if (!state.MSR) {
		cmds << zwave.wakeUpV1.wakeUpIntervalSet(seconds:4*3600, nodeid:zwaveHubNodeId).format()
		cmds << zwave.manufacturerSpecificV2.manufacturerSpecificGet().format()
		cmds << "delay 1200"
	}
	if (!state.lastbat || now() - state.lastbat > 53*60*60*1000) {
		cmds << batteryGetCommand()
	} else {
		cmds << zwave.wakeUpV1.wakeUpNoMoreInformation().format()
	}
	[event, response(cmds)]
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
	def map = [ name: "battery", unit: "%" ]
	if (cmd.batteryLevel == 0xFF) {
		map.value = 1
		map.descriptionText = "${device.displayName} has a low battery"
		map.isStateChange = true
	} else {
		map.value = cmd.batteryLevel
	}
	state.lastbat = now()
	[createEvent(map), response(zwave.wakeUpV1.wakeUpNoMoreInformation())]
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	def result = []

	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	log.debug "msr: $msr"
	updateDataValue("MSR", msr)

	retypeBasedOnMSR()

	result << createEvent(descriptionText: "$device.displayName MSR: $msr", isStateChange: false)

	if (msr == "011A-0601-0901") {  // Enerwave motion doesn't always get the associationSet that the hub sends on join
		result << response(zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId))
	} else if (!device.currentState("battery")) {
		if (msr == "0086-0102-0059") {
			result << response(zwave.securityV1.securityMessageEncapsulation().encapsulate(zwave.batteryV1.batteryGet()).format())
		} else {
			result << response(batteryGetCommand())
		}
	}

	result
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand([0x20: 1, 0x85: 2, 0x70: 1])
	// log.debug "encapsulated: $encapsulatedCommand"
	if (encapsulatedCommand) {
		state.sec = 1
		zwaveEvent(encapsulatedCommand)
	}
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	createEvent(descriptionText: "$device.displayName: $cmd", displayed: false)
}

def batteryGetCommand() {
	def cmd = zwave.batteryV1.batteryGet()
	if (state.sec) {
		cmd = zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd)
	}
	cmd.format()
}

def retypeBasedOnMSR() {
	switch (state.MSR) {
		case "0086-0002-002D":
			log.debug("Changing device type to Z-Wave Water Sensor")
			setDeviceType("Z-Wave Water Sensor")
			break
		case "011F-0001-0001":  // Schlage motion
		case "014A-0001-0001":  // Ecolink motion
		case "0060-0001-0002":  // Everspring SP814
		case "0060-0001-0003":  // Everspring HSP02
		case "011A-0601-0901":  // Enerwave ZWN-BPC
			log.debug("Changing device type to Z-Wave Motion Sensor")
			setDeviceType("Z-Wave Motion Sensor")
			break
		
	}
}
