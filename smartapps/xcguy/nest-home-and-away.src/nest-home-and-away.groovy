/**
 *  Nest Home and Away
 *
 *  Sets thermostat based on hub mode.
 */

definition(
    name:        "Nest Home and Away",
    namespace:   "xcguy",
    author:      "Bruce Adelsman",
    description: "Sets the thermostat to away or present based on hub mode.",
    category:    "Green Living",
    iconUrl:     "https://s3.amazonaws.com/smartapp-icons/Partner/nest.png",
    iconX2Url:   "https://s3.amazonaws.com/smartapp-icons/Partner/nest@2x.png"
)

preferences {

  section("Change these thermostats modes...") {
    input "thermostats", "capability.thermostat", multiple: true
  }
}

def installed() {
  state.lastMode = null
  subscribe(location, changeMode)
}

def updated() {
  state.lastMode = null
  unsubscribe()
  subscribe(location, changeMode)
}

def changeMode(evt) {

  if(evt.value == "Away") {
    log.info("Setting thermostats to away")
    thermostats?.away()
  }
  else if (state.lastMode != null && state.lastMode == "Away") {
    log.info("Setting thermostats to present")
    thermostats?.present()
  }
  state.lastMode = evt.value
}
