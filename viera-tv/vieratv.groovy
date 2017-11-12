/** 
 *  
 *  Generic Camera Device v1.1.01302017
 *
 *  Copyright 2017 patrick@patrickstuart.com
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
	definition (name: "Panasonic VIERA St60", namespace: "alexbaloc", author: "alexbaloc") {
    capability "Switch"
			command "mute" 
			command "tv"
      command "hdmi1"
      command "hdmi2"
      command "hdmi3"

      attribute "input", "string"
      attribute "hdmi1", "string"
	}

  preferences {
    input("TvIP", "string", title:"TV IP Address", description: "Please enter your TV's IP Address", required: true, displayDuringSetup: true)
    input("Hdmi1Label", "string", title:"HDMI1 input name", description: "Enter a name for the HDMI input if you want to use it in an action", required: false, displayDuringSetup: true)
    input("Hdmi1CustomIconName", "string", title:"HDMI1 input custom icon name", description: "Smartthings icon name for this input", required: false, displayDuringSetup: true)
    input("Hdmi2Label", "string", title:"HDMI2 input name", description: "Enter a name for the HDMI input if you want to use it in an action", required: false, displayDuringSetup: true)
    input("Hdmi3Label", "string", title:"HDMI3 input name", description: "Enter a name for the HDMI input if you want to use it in an action", required: false, displayDuringSetup: true)
	}
    
	simulator {
    
	}

  tiles {
      standardTile("switch", "device.switch", width: 1, height: 1, canChangeIcon: true) {
          state "on", label:'TV', action:"switch.off", icon:"st.Electronics.electronics15", backgroundColor:"#00a0dc"
          state "off", label:'TV', action:"", icon:"st.Electronics.electronics15", backgroundColor:"#cccccc"
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
      main "switch"
      details([ "power", "mute", "tv", "hdmi1", "hdmi2", "hdmi3"])
  }
}

def installed() {
  log.debug "install handler. $Hdmi1Label"
  setStates()
  runEvery1Minute(checkVolume)
}

//Constants
def getHdmi1Name() { return Hdmi1Label ? Hdmi1Label : 'HDMI1' }
def getHdmi2Name() { return Hdmi2Label ? Hdmi2Label : 'HDMI2' }
def getHdmi3Name() { return Hdmi3Label ? Hdmi3Label : 'HDMI3' }

def setStates() {
  sendEvent(name: "hdmi1", value: getHdmi1Name(), displayed: false)
  sendEvent(name: "hdmi2", value: getHdmi2Name(), displayed: false)
  sendEvent(name: "hdmi3", value: getHdmi3Name, displayed: false)

  return checkVolume()
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

    log.debug "headers: $headerMap"
    log.debug "status: $status"
    log.debug "data: $data"
    log.debug "xml: $xml"
    log.debug "json: $json"

    def switchValue = 'off'
    if (msg.xml) {
      log.debug "todo: check SOAP contents"
      //def rootNode = new XmlSlurper().parseText(msg.xml)
      //log.debug rootNode

      switchValue = 'on'
    }

    def switchStateEv = createEvent(name: "switch", value: switchValue)
    return [switchStateEv]      
}

def off() {
	log.debug "Turning TV OFF"  
  
  sendEvent(name:"switch", value: "off", descriptionText: "Power off",  displayed: true)
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
	log.debug "Checking volume"  
  return sendQuery()
}

def sendQuery() {
  return sendRawRequest('render', 'GetVolume', '<InstanceID>0</InstanceID><Channel>Master</Channel>')
}

def sendCommand(commandCode) {
  return sendRawRequest('command', 'X_SendKey', "<X_KeyEvent>NRC_$commandCode-ONOFF</X_KeyEvent>")
}

def sendRawRequest(type, action, command) {
    def host = TvIP 
    def port = 55000
    def hosthex = convertIPtoHex(host).toUpperCase() //thanks to @foxxyben for catching this
    def porthex = convertPortToHex(port).toUpperCase()
    device.deviceNetworkId = "$hosthex:$porthex" 
    
    log.debug "The device id configured is: $device.deviceNetworkId"
            
    def url
    def urn
    if (type == "command") {
        url = "/nrc/control_0"
        urn = "panasonic-com:service:p00NetworkControl:1"
    } else if (type == 'render') {
        url = "/dmr/control_0"
        urn = "schemas-upnp-org:service:RenderingControl:1"
    }

    log.debug "ULR: $url, URN: $urn"
  
    def headers = ["Content-Type": "text/xml; charset=\"utf-8\"",
          "HOST": "$host:$port",
          "SOAPACTION": "\"urn:$urn#$action\""]
    
    log.debug "The Header is $headers"

    def soapBody = "<?xml version='1.0' encoding='utf-8'?> \
                    <s:Envelope xmlns:s='http://schemas.xmlsoap.org/soap/envelope/' s:encodingStyle='http://schemas.xmlsoap.org/soap/encoding/'> \
                      <s:Body> \
                        <u:$action xmlns:u='urn:$urn'> \
                        $command \
                        </u:$action> \
                      </s:Body> \
                    </s:Envelope>";

    log.debug soapBody

    try {
    def hubAction = new physicalgraph.device.HubAction(
    	method: "POST",
    	path: "http://$host:$port$url",
     	body: soapBody,
    	headers: headers
    )
        	
    //hubAction.options = [outputMsgToS3:true]
    log.debug hubAction

    return hubAction
    }
    catch (Exception e) {
    	log.debug "Hit Exception $e on $hubAction"
    }
    
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex

}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    log.debug hexport
    return hexport
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}


private String convertHexToIP(hex) {
	log.debug("Convert hex to ip: $hex") 
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private getHostAddress() {
	def parts = device.deviceNetworkId.split(":")
    log.debug device.deviceNetworkId
	def ip = convertHexToIP(parts[0])
	def port = convertHexToInt(parts[1])
	return ip + ":" + port
}