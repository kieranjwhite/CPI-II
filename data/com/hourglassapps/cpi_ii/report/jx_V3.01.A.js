/*global ActiveXObject, when
*/

//V3.01.A - http://www.openjs.com/scripts/jx/
var jx = {
    //Create a xmlHttpRequest object - this is the constructor. 
    getHTTPObject : function() {
	var http = false;
	//Use IE's ActiveX items to load the file.
	if(typeof ActiveXObject != 'undefined') {
	    try {http = new ActiveXObject("Msxml2.XMLHTTP");}
	    catch (e) {
		try {http = new ActiveXObject("Microsoft.XMLHTTP");}
		catch (E) {http = false;}
	    }
	    //If ActiveX is not available, use the XMLHttpRequest of Firefox/Mozilla etc. to load the document.
	} else if (XMLHttpRequest) {
	    try {http = new XMLHttpRequest();}
	    catch (e) {http = false;}
	}
	return http;
    },
    // This function is called from the user's script. 
    //Arguments - 
    //	url	- The url of the serverside script that is to be called. Append all the arguments to 
    //			this url - eg. 'get_data.php?id=5&car=benz'
    //	callback - Function that must be called once the data is ready.
    //	format - The return type for this function. Could be 'xml','json', 'bin' or 'text'. If it is json, 
    //			the string will be 'eval'ed before returning it. Default:'text'
    load : function (url, format, headers) {
	var def=when.defer();
	var http = this.init(); //The XMLHttpRequest object is recreated at every call - to defeat Cache problem in IE
	if(!http||!url) {
	    def.reject({status: -1, msg: 'Failed to send in jx.load'});
	    return def.promise;
	}

	 //Causes error in firefox in web workers
	if (typeof(format)!=='undefined' && typeof http.overrideMimeType!='undefined') {
	    http.overrideMimeType(format);
	}
	    

	//Kill the Cache problem in IE.
	var now = "uid=" + new Date().getTime();
	url += (url.indexOf("?")+1) ? "&" : "?";
	url += now;

	http.open("GET", url, true);

	if(typeof headers!=='undefined') {
	    for(var idx in headers) {
		if(headers.hasOwnProperty(idx)) {
		    http.setRequestHeader(headers[idx].name, headers[idx].val);
		}
	    }
	}

	http.onreadystatechange = function () {//Call a function when the state changes.
	    if (http.readyState == 4) {//Ready State will be 4 when the document is loaded.
		if(http.status===200 || http.status===206 || http.status===0) {
		    var result = "";
		    if(http.responseText) {
			result = http.responseText;
		    }
		    
		    //If the return is in JSON format, eval the result before returning it.
		    if(typeof format!=='undefined' && format==="application/json") {
			//\n's in JSON string, when evaluated will create errors in IE
			result = result.replace(/[\n\r]/g,"");
			result = eval('('+result+')'); 
		    }
		    
		    def.resolve({http: http, result: result});
		} else { //An error occured
		    def.reject({ status: http.status, msg: http.responseText});
		}
	    }
	};
	http.send(null);
	return def.promise;
    },
    init : function() {return this.getHTTPObject();}
};
