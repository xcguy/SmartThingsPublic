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
        
        // state.startEnergy - the total energy at the start of the reporting (usually start of month)
        // start.startEnergyDate - date of the start of the reporting (usually start of the month)
        // start.currentPowerDate - date for tracking current high/low power usage (daily)

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
        input "kWhCost", "string", title: "\$/kWh (0.12)", description: "Cost of electricy", defaultValue: "0.12" as String
    }
}

import java.text.SimpleDateFormat

def installed() {
	log.debug "Install"

}

def updated() {
	log.debug "Updated"

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
	log.debug "state - energy ${state.startEnergy} ${state.startEnergyDate}, power ${state.currentPowerDate}"
	if (cmd.scale == 0) {
    	def valTotalEnergy = cmd.scaledMeterValue as Float
        checkForEnergyReset(valTotalEnergy)
    	def valEnergy = getEnergyUsed(valTotalEnergy)
    	def valAvg = valEnergy / getEnergyDays() as Float
        valAvg = valAvg.round(2)
        sendEvent(name: "energyAvg", value: valAvg, unit: "kWh")
    	def valCost = valEnergy * (kWhCost as Float) as Float
        valCost = String.format("%.2f", valCost.round(2))
        sendEvent(name: "energyCost", value: valCost, unit: "\$")    
        valEnergy = valEnergy.round(3)
		[name: "energy", value: valEnergy, unit: "kWh"]
	} else if (cmd.scale == 1) {
		[name: "energy", value: cmd.scaledMeterValue, unit: "kVAh"]
	}
	else {
    	checkForPowerReset()
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

def resetEnergy(Float val) {
	log.debug "Resetting Energy"
	if (state.startEnergy != null) {
		def energyUsed = device.currentValue("energy") as double
    	def energyDailyAvg = energyUsed / getEnergyDays() as float
   		log.info "Energy used last month - Total "+String.format("%.1f", energyUsed)+" kWH, Daily average "+String.format("%.2f", energyDailyAvg)+" kWH"
    }
    // start new reporting period and save total energy
    state.startEnergyDate = getISOString(new Date())
    state.startEnergy = val
	// Not V1 available
	return [
		zwave.meterV2.meterReset().format(),
		zwave.meterV2.meterGet(scale: 0).format()
	]
}

private resetPower() {
    log.debug "Resetting Power"
	if (device.currentValue("powerLow") != null && (device.currentValue("powerLow") as float) > 1) {
    	log.info "Power levels for yesterday - Low: "+device.currentValue("powerLow")+", High: "+device.currentValue("powerHigh")
    }
	state.currentPowerDate = getISOString(new Date())
	sendEvent(name: "powerLow", value: 99999, unit: "W")
    sendEvent(name: "powerHigh", value: 0, unit: "W")
}


private checkForPowerReset() {
    def currentPowerDate = getISODate(state.currentPowerDate)
	def curtime = new Date()
    if (currentPowerDate == null || currentPowerDate.format("d", location.timeZone) != curtime.format("d", location.timeZone)) {
        // date changed
        resetPower()
    }
}

private checkForEnergyReset(Float val) {
    def startEnergyDate = getISODate(state.startEnergyDate)
    def curtime = new Date()
    if (state.startEnergy == null || startEnergyDate == null || startEnergyDate.format("M", location.timeZone) != curtime.format("M", location.timeZone)) {
        // month changed
		resetEnergy(val)
    }
}

private getEnergyUsed(Float totalEnergy) {
    // log.debug "getEnergyUsed - total: $totalEnergy,  start: ${state.startEnergy}"
    Float energyUsed = totalEnergy - (state.startEnergy as float)
    return energyUsed
}

private getEnergyDays() {
    def startEnergyDate = getISODate(state.startEnergyDate)
    // log.debug "getEnergyDays - start: ${startEnergyDate}"
    Float daysDiff = (new Date().getTime() - startEnergyDate.getTime())/86400000.0
    return daysDiff
}

private getISODate(String t) {
    def sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    return(t == null ? null : sdf.parse(t))
}

private getISOString(Date t) {
    def sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    return(t == null ? null : sdf.format(t))
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
