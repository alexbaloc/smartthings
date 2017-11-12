/** 
 *  
 *  Panasonic Viera ST60 TV
 *
 *  Copyright 2017 alexbaloc
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
*/

metadata {
	definition (name: "Panasonic VIERA ST60", namespace: "alexbaloc", author: "alexbaloc") {
    capability "Switch"
    capability "Polling"
    capability "Refresh"
    command "mute" 
    command "tv"
    command "hdmi1"
    command "hdmi2"
    command "hdmi3"

    //commands not exposed via UI
    command "apps"
    command "arrowright"
    command "arrowleft"
    command "arrowup"
    command "arrowdown"
    command "ok"
    command "back"
    command "cancel"
    command "volumeup"
    command "volumedown"
    command "chanelup"
    command "chaneldown"
    command "red"
    command "green"
    command "yellow"
    command "blue"

    command "play"
    command "pause"
    command "stop"

    // See list here for more supported Viera Commands
    // https://irule.desk.com/customer/portal/questions/1020311-panasonic-viera-ip-control

    command "checkVolume"

    // Currently selected input source
    attribute "input", "enum", ["tv", "hdmi1", "hdmi2", "hdmi3"]
    // Currently not used. Should serve to display the proper HDMI input names in the UI
    attribute "hdmi1name", "string"
    attribute "hdmi2name", "string"
    attribute "hdmi3name", "string"
    //not used yet
    attribute "volume", "number"
    attribute "muted", "enum", ["true", "false"]
    //display only
    attribute "ip", "string"
	}

  preferences {
    // section() {
    //   input("TvIP", "string", title:"TV IP Address", description: "Please enter your TV's IP Address", required: true, displayDuringSetup: true)
    // }
    section() {
      input("Hdmi1Label", "string", title:"HDMI1 input name", description: "Enter a name for the HDMI input if you want to use it in an action", required: false, displayDuringSetup: true)
      input("Hdmi1CustomIconName", "string", title:"HDMI1 input custom icon name", description: "Smartthings icon name for this input", required: false, displayDuringSetup: true)
    }
    section() {
      input("Hdmi2Label", "string", title:"HDMI2 input name", description: "Enter a name for the HDMI input if you want to use it in an action", required: false, displayDuringSetup: true)
    }
    section() {
      input("Hdmi3Label", "string", title:"HDMI3 input name", description: "Enter a name for the HDMI input if you want to use it in an action", required: false, displayDuringSetup: true)
    }
	}
    
	simulator {
	}

  tiles {
    standardTile("switch", "device.switch", width: 1, height: 1, canChangeIcon: true) {
        state "on", label:'TV', action:"switch.off", icon:"st.Electronics.electronics15", backgroundColor:"#00a0dc"
        state "off", label:'TV', action:"switch.off", icon:"st.Electronics.electronics15", backgroundColor:"#cccccc"
    }
    standardTile("power", "device.switch", width: 2, height: 2, canChangeIcon: false) {
        state "on", label:'TV', action:"switch.off", icon:"st.thermostat.heating-cooling-off", backgroundColor:"#00a0dc"
        state "off", label:'TV', action:"", icon:"st.Electronics.electronics15", backgroundColor:"#A9A9A9"
    }
    standardTile("mute", "mute", decoration: "flat", canChangeIcon: false) {
        state "default", label:'Mute', action:"mute", icon:"st.custom.sonos.muted", backgroundColor:"#ffffff"
    }    
    standardTile("tv", "input", decoration: "flat", canChangeIcon: false) {
        state "tv", label:'Input: TV', action:"tv", icon:"st.Electronics.electronics15", backgroundColor:"#00a0dc"
        state "default", label:'Input: TV', action:"tv", icon:"st.Electronics.electronics15", backgroundColor:"#ffffff"
    }

    standardTile("hdmi1", "input", decoration: "flat", canChangeIcon: false) {
        state "hdmi1", label:'Input: HDMI1', action:"hdmi1", icon:"st.Electronics.electronics8", backgroundColor:"#00a0dc"
        state "default", label:'Input: HDMI1', action:"hdmi1", icon:"st.Electronics.electronics8", backgroundColor:"#ffffff"
    }

    //Test with multi-attribute to display both HDMI label & selected state
    // multiAttributeTile(name:"hdmi1", type: "generic", width:1, height: 1) {
    //     tileAttribute ("input", key: "PRIMARY_CONTROL") {
    //         attributeState "hdmi1", label:'on',  action:"hdmi1", icon:"st.Electronics.electronics5", backgroundColor:"#00a0dc"
    //         attributeState "default", label:'off',  action:"hdmi1", icon:"st.Electronics.electronics5", backgroundColor:"#ffffff"
    //     }
    //     tileAttribute("hdmi1", key: "SECONDARY_CONTROL") {
    // 			attributeState("default", label:'Input: ${currentValue}', icon: "st.Electronics.electronics5")                  
    //     }
    // }
    standardTile("hdmi2", "input", decoration: "flat", canChangeIcon: false) {
        state "hdmi2", label:'Input: HDMI2', action:"hdmi2", icon:"st.Electronics.electronics5", backgroundColor:"#00a0dc"
        state "default", label:'Input: HDMI2', action:"hdmi2", icon:"st.Electronics.electronics5", backgroundColor:"#ffffff"
    }
    standardTile("hdmi3", "input", decoration: "flat", canChangeIcon: false) {
        state "hdmi3", label:'Input: HDMI3', action:"hdmi3", icon:"st.Electronics.electronics18", backgroundColor:"#00a0dc"
        state "default", label:'Input: HDMI3', action:"hdmi3", icon:"st.Electronics.electronics18", backgroundColor:"#ffffff"
    }
    
    valueTile("ipinfo", "ip", width:3, canChangeIcon: false) {
      state "default", label:'${currentValue}', backgroundColor:"#ffffff"
    }

    standardTile("refresh", "", decoration: "flat", canChangeIcon: false) {
        state "default", label:'Refresh', action:"checkVolume", icon:"st.Electronics.electronics13", backgroundColor:"#ffffff"
    }

    main "switch"
    details([ "power", "mute", "tv", "hdmi1", "hdmi2", "hdmi3", "ipinfo", "refresh"])
  }
}

def installed() {
  log.debug "install handler"
  setComputedAttributed()
}

// 
//  Polling/refresh/update still WIP. Not clear how it should work
//
def updated() {
  log.debug "updated with settings: ${settings}"
  return refresh()
}

// polling.poll 
def poll() {
  log.debug "poll()"
  return refresh()
}

// refresh.refresh
def refresh() {
  log.debug "refresh()"

  return checkVolume()
}

// Called by the service manager
def sync(ip, port) {
  log.debug "updating IP/Port to $ip:$port"

	def existingIp = state["ip"];
	def existingPort = state["port"]
	if (ip && ip != existingIp) {
    state["ip"] = ip
    //also update the ui
    sendEvent(name: "ip", value: ip, displayed: false)
	}
	if (port && port != existingPort) {
		state["port"] = port
	}
}

//Constants
def getHdmi1Name() { return Hdmi1Label ? Hdmi1Label : 'HDMI1' }
def getHdmi2Name() { return Hdmi2Label ? Hdmi2Label : 'HDMI2' }
def getHdmi3Name() { return Hdmi3Label ? Hdmi3Label : 'HDMI3' }

def setComputedAttributed() {
  sendEvent(name: "hdmi1name", value: getHdmi1Name(), displayed: false)
  sendEvent(name: "hdmi2name", value: getHdmi2Name(), displayed: false)
  sendEvent(name: "hdmi3name", value: getHdmi3Name(), displayed: false)
}

def parse(String description) {

  log.debug "Parsing '${description}'"
  
  def msg = parseLanMessage(description)
  log.debug "parsed message: $msg"

  def headersAsString = msg.header // => headers as a string
  def headerMap = msg.headers      // => headers as a Map
  def body = msg.body              // => request body as a string
  def status = msg.status          // => http status code of the response
  def json = msg.json              // => any JSON included in response body, as a data structure of lists and maps
  def xml = msg.xml                // => any XML included in response body, as a document tree structure
  def data = msg.data  

//  log.debug "headers: $headerMap"
//  log.debug "status: $status"
//  log.debug "data: $data"
//  log.debug "xml: $xml"

  if (msg.xml) {
    log.debug "todo: check SOAP contents"

  }

  if (msg.requestId != 'POWER') {
    log.debug "Signaling power is on"

    // Since we got a reply, the TV must be on. 
    // The POWER command is the only exception, and we're setting a custom (non-unique) Request ID for that
    def switchStateEv = createEvent(name: "switch", value: "on")
    return [switchStateEv]      
  } else {
    log.debug "Parse called for a POWER OFF request - ignoring"
  }

  return null;
}

def off() {
	log.debug "Turning TV OFF"  
  
  //reset other states
  sendEvent(name:"input", value: "", descriptionText: "Reset input", displayed: false)
  return sendCommand('POWER')
}

def mute() {
	log.debug "Toggle Mute"  
	sendEvent(name:"Command", value: "Mute", displayed: true) 
  return sendCommand('MUTE')
}

def tv() {
	log.debug "Switch input: TV"  
	sendEvent(name:"input", value: "tv", descriptionText: "Switch Input: TV", displayed: true)
  return sendCommand('TV')
}

def hdmi1() {
	log.debug "Switch input: $Hdmi1Label (HDMI1)" 
	sendEvent(name:"input", value: "hdmi1", descriptionText: "Switch Input: ${getHdmi1Name()}", displayed: true)
  return sendCommand('HDMI1')
}

def hdmi2() {
	log.debug "Switch input: $Hdmi2Label (HDMI2)"  
	sendEvent(name:"input", value: "hdmi2", descriptionText: "Switch Input: ${getHdmi2Name()}", displayed: true)
  return sendCommand('HDMI2')
}

def hdmi3() {
	log.debug "Switch input: $Hdmi3Label (HDMI3)"  
	sendEvent(name:"input", value: "hdmi3", descriptionText: "Switch Input: ${getHdm3Name()}", displayed: true)
  return sendCommand('HDMI3')
}

def checkVolume() {
	log.debug "Checking volume - Status update!"  

  //doesn't work. Even if we generate the correct HubAction wrapper, it's not being sent to the device
  //runEvery1Minute(sendQuery)

  return sendQuery()
}

def apps() {
  sendEvent(name:"Command", value: "Apps", displayed: true) 
  return sendCommand('APPS')
}

def arrowright() { return sendCommand('RIGHT') }
def arrowleft() { return sendCommand('LEFT') }
def arrowup() { return sendCommand('UP') }
def arrowdown() { return sendCommand('DOWN') }
def ok() { return sendCommand('ENTER') }
def back() { return sendCommand('RETURN') }
def cancel() { return sendCommand('CANCEL') }

def red() { return sendCommand('RED') }
def green() { return sendCommand('GREEN') }
def yellow() { return sendCommand('YELLOW') }
def blue() { return sendCommand('BLUE') }

def play() { 
  sendEvent(name:"Command", value: "Play", displayed: true) 
  return sendCommand('PLAY')
}
def pause() { 
  sendEvent(name:"Command", value: "Pause", displayed: true) 
  return sendCommand('PAUSE')
}
def stop() { 
  sendEvent(name:"Command", value: "Stop", displayed: true) 
  return sendCommand('STOP')
}

//
//  SOAP call support
//

def sendQuery() {
  //Other queries: GetMute
  return sendRawRequest('render', 'GetVolume', '<InstanceID>0</InstanceID><Channel>Master</Channel>', '')
}

def sendCommand(commandCode) {
  return sendRawRequest('command', 'X_SendKey', "<X_KeyEvent>NRC_$commandCode-ONOFF</X_KeyEvent>", commandCode)
}

def sendRawRequest(type, action, command, commandCode) {
  def host = state["ip"]//TvIP 
  def port = state["port"] //55000

  log.debug "sending $type '$command'"
          
  def url
  def urn
  if (type == "command") {
    url = "/nrc/control_0"
    urn = "panasonic-com:service:p00NetworkControl:1"
  } else if (type == 'render') {
    url = "/dmr/control_0"
    urn = "schemas-upnp-org:service:RenderingControl:1"
  }

  def headers = ["Content-Type": "text/xml; charset=\"utf-8\"",
    "HOST": "$host:$port",
    "SOAPACTION": "\"urn:$urn#$action\""]
  
  //log.debug "The Header is $headers"

  def soapBody = "<?xml version='1.0' encoding='utf-8'?> \
                  <s:Envelope xmlns:s='http://schemas.xmlsoap.org/soap/envelope/' s:encodingStyle='http://schemas.xmlsoap.org/soap/encoding/'> \
                    <s:Body> \
                      <u:$action xmlns:u='urn:$urn'> \
                      $command \
                      </u:$action> \
                    </s:Body> \
                  </s:Envelope>";

  //log.debug soapBody

  try {
    def hubAction = new physicalgraph.device.HubAction(
    	method: "POST",
    	path: "http://$host:$port$url",
     	body: soapBody,
    	headers: headers,
    )

    if (commandCode =='POWER') {
      hubAction.requestId = 'POWER'
    }

    //TODO: not clear why this was set. 
    //When enabled, SOAP message responses are no longer received in parse()
    //hubAction.options = [outputMsgToS3:true]

    //log.debug hubAction

    // Before returning the action, we can assume the TV is turned off. 
    // If we receive any replies, we'll turin it back on. 
    // Kind of hackish, but I see no way around it for now
    sendEvent(name: "switch", value: "off", displayed: false)

    return hubAction
  }
  catch (Exception e) {
    log.debug "Hit Exception $e on $hubAction"
  }
}
