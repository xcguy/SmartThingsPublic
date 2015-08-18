/**
 *  Aeon Home Energy Meter
 *
 *  Author: SmartThings
 *
 *  Date: 2013-05-30
 */
metadata {
	// Automatically generated. Make future change here.
	definition (name: "Aeon Home Energy Meter XC", namespace: "smartthings", author: "SmartThings") {
		capability "Energy Meter"
		capability "Power Meter"
		capability "Configuration"
		capability "Sensor"
        
        attribute "powerLow", "number"
        attribute "powerHigh", "number"      
        attribute "energyCost", "number"
        attribute "energyAvg", "number"

		fingerprint deviceId: "0x2101", inClusters: " 0x70,0x31,0x72,0x86,0x32,0x80,0x85,0x60"
	}

	// simulator metadata
	simulator {
		for (int i = 0; i <= 10000; i += 1000) {
			status "power  ${i} W": new physicalgraph.zwave.Zwave().meterV1.meterReport(
				scaledMeterValue: i, precision: 3, meterType: 4, scale: 2, size: 4).incomingMessage()
		}
		for (int i = 0; i <= 100; i += 10) {
			status "energy  ${i} kWh": new physicalgraph.zwave.Zwave().meterV1.meterReport(
				scaledMeterValue: i, precision: 3, meterType: 0, scale: 0, size: 4).incomingMessage()
		}
	}

	// tile definitions
	tiles {
		valueTile("power", "device.power", decoration: "flat") {
			state "default", label:'${currentValue}W'
		}
    	valueTile("powerLow", "device.powerLow", decoration: "flat") {
			state "default", label:'${currentValue}W  Low'
		}
		valueTile("powerHigh", "device.powerHigh", decoration: "flat") {
			state "default", label:'${currentValue}W High'
		}
		valueTile("energy", "device.energy", decoration: "flat") {
			state "default", label:'${currentValue} kWh'
		}
		valueTile("energyCost", "device.energyCost", decoration: "flat") {
			state "default", label:'\$${currentValue} Cost'
		}  
        valueTile("energyAvg", "device.energyAvg", decoration: "flat") {
			state "default", label:'${currentValue} kWh Avg'
		}
        standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		standardTile("configure", "device.power", inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}

		main (["power", "energy"])
		details(["power", "powerLow", "powerHigh", "energy", "energyCost", "energyAvg", "refresh", "configure"])
	}
    preferences {
        input "kWhCost", "string", title: "\$/kWh (0.12)", defaultValue: "0.12" as String
    }
}

def installed() {
	log.debug "Install"
    resetPower()
}

def updated() {
	log.debug "Updated"
    resetPower()
}

def parse(String description) {
	def result = null
	def cmd = zwave.parse(description, [0x31: 1, 0x32: 1, 0x60: 3])
	if (cmd) {
		result = createEvent(zwaveEvent(cmd))
	}
	log.debug "Parse returned ${result?.descriptionText}"
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.meterv1.MeterReport cmd) {
	log.debug "zwaveEvent ${cmd}"
	if (cmd.scale == 0) {
    	def valAvg = cmd.scaledMeterValue / getEnergyDays() as Float
        valAvg = valAvg.round(2)
        sendEvent(name: "energyAvg", value: valAvg, unit: "kWh")
    	def valCost = cmd.scaledMeterValue * (kWhCost as Float) as Float
        valCost = String.format("%.2f", valCost.round(2))
        sendEvent(name: "energyCost", value: valCost, unit: "\$")        
		[name: "energy", value: cmd.scaledMeterValue, unit: "kWh"]
	} else if (cmd.scale == 1) {
		[name: "energy", value: cmd.scaledMeterValue, unit: "kVAh"]
	}
	else {
    	checkForReset()
    	def value =  Math.round(cmd.scaledMeterValue) as Integer
    	if (value < (device.currentValue("powerLow") as Integer)) {
            sendEvent(name: "powerLow", value: value, unit: "W")
        }
        if (value > (device.currentValue("powerHigh") as Integer)) {
        	sendEvent(name: "powerHigh", value: value, unit: "W")
        }
		[name: "power", value: value, unit: "W"]
	}
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
	[:]
}

def refresh() {
	delayBetween([
		zwave.meterV2.meterGet(scale: 0).format(),
		zwave.meterV2.meterGet(scale: 2).format()
	])
}

def resetEnergy() {
	// No V1 available
    log.info "Energy used last month - Total "+device.currentValue("energy")+" kWH, Daily average "+device.currentValue("energyAvg")+" kWH"
	return [
		zwave.meterV2.meterReset().format(),
		zwave.meterV2.meterGet(scale: 0).format()
	]
}

private resetPower() {
    log.info "Power used for the day - low: "+device.currentValue("powerLow")+", high: "+device.currentValue("powerHigh")
	sendEvent(name: "powerLow", value: 99999, unit: "W")
    sendEvent(name: "powerHigh", value: 0, unit: "W")
}


private checkForReset() {
	TimeZone.setDefault(TimeZone.getTimeZone("CST"))
    def powerState = device.currentState("powerHigh")
	def curtime = new Date()
    if (powerState.rawDateCreated[Calendar.DATE] != curtime[Calendar.DATE]) {
    	if (powerState.rawDateCreated[Calendar.MONTH] != curtime[Calendar.MONTH]) {  // month changed too
	    	log.debug "Resetting Energy"
			resetEnergy()
        }
    	log.debug "Resetting Power"
        resetPower()
    }
}

private getEnergyDays() {
	def energyState = device.currentState("energy")
	def energyStart = Date.parse("yyyy-MM-dd", ""+energyState.rawDateCreated[Calendar.YEAR]+"-"+(energyState.rawDateCreated[Calendar.MONTH]+1)+"-01")
    Float daysDiff = (energyState.rawDateCreated.getTime() - energyStart.getTime())/86400000.0
    return daysDiff
}

def configure() {
	def cmd = delayBetween([
		zwave.configurationV1.configurationSet(parameterNumber: 101, size: 4, scaledConfigurationValue: 4).format(),   // combined power in watts
		zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: 300).format(), // every 5 min
		zwave.configurationV1.configurationSet(parameterNumber: 102, size: 4, scaledConfigurationValue: 8).format(),   // combined energy in kWh
		zwave.configurationV1.configurationSet(parameterNumber: 112, size: 4, scaledConfigurationValue: 300).format(), // every 5 min
		zwave.configurationV1.configurationSet(parameterNumber: 103, size: 4, scaledConfigurationValue: 0).format(),    // no third report
		zwave.configurationV1.configurationSet(parameterNumber: 113, size: 4, scaledConfigurationValue: 300).format() // every 5 min
	])
	log.debug cmd
	cmd
}
