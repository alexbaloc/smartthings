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
          state "default", label:'TV', action:"switch.off", icon:"st.Electronics.electronics15", backgroundColor:"#ffffff"
      }
      standardTile("power", "device.switch", width: 2, height: 2, canChangeIcon: false) {
          state "default", label:'', action:"switch.off", decoration: "flat", icon:"st.thermostat.heating-cooling-off", backgroundColor:"#f45c42"
      }
      standardTile("mute", "device.switch", decoration: "flat", canChangeIcon: false) {
          state "default", label:'Mute', action:"mute", icon:"st.custom.sonos.muted", backgroundColor:"#ffffff"
      }    
      standardTile("tv", "device.switch", decoration: "flat", canChangeIcon: false) {
          state "default", label:'Input: TV', action:"tv", icon:"st.Electronics.electronics15", backgroundColor:"#ffffff"
      }

      standardTile("hdmi1", "device.switch", decoration: "flat", canChangeIcon: false) {
          state "default", label:'Input: HDMI1', action:"hdmi1", icon:"st.Electronics.electronics5", backgroundColor:"#ffffff"
      }

      main "switch"
      details([ "power", "mute", "tv", "hdmi1"])
  }
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

    if (msg.xml) {
      log.debug "todo: check SOAP contents"
      //def rootNode = new XmlSlurper().parseText(msg.xml)
      //log.debug rootNode

      def dummyOn = createEvent(name: "switch", value: "on")
      return [dummyOn]      
    }

    //def dummyOn = createEvent(name: "switch", value: "on")
    //return [dummyOn]

    return null;
}

def off() {
	log.debug "Turning TV OFF"  
  
  sendEvent(name:"Command", value: "Power Off", displayed: true)
  return sendCommand('POWER')
}

def mute() {
	log.debug "Toggle Mute"  
	sendEvent(name:"Command", value: "Mute", displayed: true) 
  return sendCommand('MUTE')
}

def tv() {
	log.debug "Switch input: TV"  
	sendEvent(name:"Command", value: "Switch input: TV", displayed: true) 
  return sendCommand('TV')
}

def hdmi1() {
	log.debug "Switch input: $Hdmi1Label (HDMI1)"  
	sendEvent(name:"Command", value: "Switch input: HDMI1", displayed: true) 
  return sendCommand('HDMI1')
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