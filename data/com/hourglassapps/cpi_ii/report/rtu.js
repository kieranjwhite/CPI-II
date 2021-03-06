/*global $,jQuery, glb, Module, setTimeout
 */

var hourglassapps_rtu=function() {
    "use strict";

    if(typeof window.hourglassapps_globals==='undefined') {
	window.hourglassapps_globals={};
    }

    glb=function() {
	return window.hourglassapps_globals;
    };

    if(typeof glb().rtu==='undefined') {
	glb().rtu={};
    }

    var r=glb().rtu;

    if(!window.console) { 
	window.console = {log: function(){} }; 
    }

    r.localError=function(details, level) {
	if(typeof level!='undefined' && level<r.logLevel) {
	    return;
	}

	console.log(details);
    };

    r.log=function(details, level) {
	if(typeof level!='undefined' && level<r.logLevel) {
	    return;
	}
	var dets=JSON.stringify(details);
	r.localError(dets);
    };

    r.report=function(pMsg) {
	r.log(pMsg);
	r.error(pMsg);
    };

    r.logError=r.log;
    window.onerror = function(message, file, line) {
	r.logError(file + ':' + line + '\n\n' + message);
    };

    r.error=function(message) {
	alert(message);
    };

    r.defer=function() {
	var deferred=$.Deferred();
	deferred.resolve();
	return deferred.promise();
    };

    r.rejectedDefer=function(msg) {
	var def=$.Deferred();
	def.reject(msg);
	return def.promise();
	//var deferred=$.Deferred();
	//var prom=deferred.promise();
	//deferred.reject(msg);
	//return prom;
    };

    r.check=function(msg, val) {
	if(val!==true) {
	    alert(msg+'\n'+r.stack());
	}
	return val;
    };

    r.validate=function(val) {
	r.check("Validation failed.", val);
	return val;
    };

    r.assert=function(val) {
	//if(window.hourglassapps_globals.asserts.checking>0) {
	r.check("Assertion failed.", val);
	//}
	return val;
    };

    r.caretIdx=function(ctrl) {
	// 23/10/2013 from http://blog.vishalon.net/index.php/javascript-getting-and-setting-caret-position-in-textarea
	var CaretPos = 0;	// IE Support
	if (document.selection && navigator.appVersion.indexOf("MSIE 10") == -1) {
	    ctrl.focus ();
	    var Sel = document.selection.createRange ();
	    Sel.moveStart ('character', -ctrl.value.length);
	    CaretPos = Sel.text.length;
	}
	// Firefox support
	else if (ctrl.selectionStart || ctrl.selectionStart == '0')
	    CaretPos = ctrl.selectionStart;
	return (CaretPos);
    };


    r.escapeHTML=function(str) {
	var div = document.createElement('div');
	var text = document.createTextNode(str);
	div.appendChild(text);
	return div.innerHTML;
    };
    
    r.loadJSFile=function(filename) {
	var fileref=document.createElement('script');
	fileref.setAttribute("type","text/javascript");
	fileref.setAttribute("src", filename);
	document.getElementsByTagName("head")[0].appendChild(fileref);
    };

    r.resize=function(ref) {
        r.resizeEl($(ref));
    };

    r.resizeEl=function(el) {
	var top=el.offset().top;
	var bottom=$(window).height();
	var new_height=Math.max((bottom-top)-10,400);
	if(new_height>=0) {
	    el.height(new_height);
	}
    };
    
    r.showMessage=function(message, delay) {
	$.mobile.loading('show',
			 { theme: "b", text: (message || 'ERROR'),
			   textonly: true, textVisible: true });
	setTimeout(function() {
            $.mobile.loading('hide');
	}, ((delay && delay > 0) ? delay : 1000));
    };

    return glb;
};

if(typeof glb==='undefined') {
    var glb=hourglassapps_rtu();
} else {
    hourglassapps_rtu();
}

delete(hourglassapps_rtu);
