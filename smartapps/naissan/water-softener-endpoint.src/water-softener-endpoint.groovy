/**
 *  Water Softener Endpoint
 *
 *  Copyright 2015 Bruce Adelsman
 *
 */
definition(
    name: "Water Softener Endpoint",
    namespace: "naissan",
    author: "Bruce Adelsman",
    description: "Receives updates on water softener salt level.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/water_moisture.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/water_moisture@2x.png",
    oauth: true)


preferences {
	section("Allow Endpoint to Monitor these Water Softener Sensors...") {
		input "sensors", "device.waterSoftenerSensor", title: "Which Water Softeners?", multiple: true
	}
    section("Send Warnings") {
       	input "sendNotification", "bool", title: "Push notification", required: false, defaultValue: "false"
       	input "phoneNumber", "phone", title: "Send a text message to:", required: false
 	}   
//	section ("GroveStreams Feed PUT API key") {
//		input "channelKey", "text", title: "API key", required: false
//	}

}

mappings {

	path("/watersoftener") {
		action: [
			GET: "listSensors"
		]
	}
	path("/watersoftener/:id") {
		action: [
			GET: "showSensors"
		]
	}
	path("/watersoftener/:id/:command/:val") {
		action: [
			GET: "updateSensor"
		]
	}
   
}

def installed() {
	subscribe(sensors, "lowSaltFlag", saltLevelWarning)
}

def updated() {
	unsubscribe()
    subscribe(sensors, "lowSaltFlag", saltLevelWarning)
}


def listSensors() {
	sensors.collect{device(it,"sensor")}
}

def showSensors() {
	show(sensors, "sensor")
}
void updateSensor() {
	update(sensors)
}


def deviceHandler(evt) {}

private void update(devices) {
	// log.debug "update, request: params: ${params}, devices: $devices.id"   
    
    def command = params.command
    def val = params.val
 	if (command) 
    {
		def device = devices.find { it.id == params.id }
		if (!device) {
			httpError(404, "Device not found")
		} else {
            // log.debug "Previous saltlevel: "+device.currentState("saltlevel")
            device.updateSaltLevel(val)
            // if (channelKey) 
            //	logValue(device, "percentage", val)
		}
	}
}

private show(devices, type) {
	def device = devices.find { it.id == params.id }
	if (!device) {
		httpError(404, "Device not found")
	}
	else {
		def attributeName = type == "motionSensor" ? "motion" : type
		def s = device.currentState(attributeName)
		[id: device.id, label: device.displayName, value: s?.value, unitTime: s?.date?.time, type: type]
	}
}

private device(it, type) {
	it ? [id: it.id, label: it.label, type: type] : null
}

def saltLevelWarning(evt) {
 
 	log.debug "saltLevelWarning event evt: $evt.name"
 	// if the salt level warning became active
 	if (evt.value == "On") {
    	log.debug "saltLevelWarning flag is on"
    	def sensor = sensors.find { it.id == evt.deviceId }
		sendWarningMessage(sensor)
        // schedule a daily reminder
        runOnce(new Date() + 1, saltLevelWarningReminder)
	}
}

def saltLevelWarningReminder() {
	// find all the low salt level sensors
    log.debug "saltLevelWarningReminder activated"
	def warnSensors = sensors.findAll{ it.currentValue("lowSaltFlag") == "On" }
    if (warnSensors) {
    	// send a message for each
    	for (sensor in warnSensors) {
        	sendWarningMessage(sensor)
        }
        // schedule next reminder
        runOnce(new Date() + 1, saltLevelWarningReminder)
	} else {
		log.debug "saltLevelWarningReminder found no sensors with low salt"
    }
}

def sendWarningMessage(sensor)
{
	def msg = "${sensor.displayName} is running low on salt (${sensor.currentValue("saltLevel")}%)"

	log.info msg
    if (sendNotification) {
        sendPush(msg)
    } 
    if (phoneNumber) {
	    sendSms(phoneNumber, msg)
    }
}


//private logValue(device, valtype, val) {
//	def compId = URLEncoder.encode(device.displayName.trim())
//	def streamId = URLEncoder.encode(valtype)
//	def value = val
//	
//	log.debug "Logging to GroveStreams ${compId}, ${streamId} = ${value}"
//	
//	def url = "https://grovestreams.com/api/feed?api_key=${channelKey}&compId=${compId}&${streamId}=${value}"
//	def putParams = [
//		uri: url,
//		body: []]
//
//	httpPut(putParams) { response ->
//		if (response.status != 200 ) {
//			log.debug "GroveStreams logging failed, status = ${response.status}"
//		}
//	}
//}

