/*global $,jQuery, glb, Module
 */

if(typeof window==='undefined') {
    window={};
}

var hourglassapps_rtu_domless=
function() {
    "use strict";
    if(typeof(window.hourglassapps_globals)==='undefined') {
	window.hourglassapps_globals={};
    }

    glb=function() {
	return window.hourglassapps_globals;
    };

    if(typeof glb().rtu==='undefined') {
	glb().rtu={};
    }

    var r=glb().rtu;

    r.LOG_INFO=0;
    r.LOG_ASSERT=1;
    r.LOG_ERROR=2;
    r.logLevel=r.LOG_INFO;

    // Object.keys implementation for older browsers
    // From https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/keys
    if (!Object.keys) {
	Object.keys = (function () {
	    'use strict';
	    var hasOwnProperty = Object.prototype.hasOwnProperty,
		hasDontEnumBug = !({toString: null}).propertyIsEnumerable('toString'),
		dontEnums = [
		    'toString',
		    'toLocaleString',
		    'valueOf',
		    'hasOwnProperty',
		    'isPrototypeOf',
		    'propertyIsEnumerable',
		    'constructor'
		],
		dontEnumsLength = dontEnums.length;

	    return function (obj) {
		if (typeof obj !== 'object' && (typeof obj !== 'function' || obj === null)) {
		    throw new TypeError('Object.keys called on non-object');
		}

		var result = [], prop, i;

		for (prop in obj) {
		    if (hasOwnProperty.call(obj, prop)) {
			result.push(prop);
		    }
		}

		if (hasDontEnumBug) {
		    for (i = 0; i < dontEnumsLength; i++) {
			if (hasOwnProperty.call(obj, dontEnums[i])) {
			    result.push(dontEnums[i]);
			}
		    }
		}
		return result;
	    };
	}());
    }

    r.toInt=function(n){ 
	return Math.round(Number(n)); 
    };

    r.str2TypedArray=function(pStr, pDest) {
	//pDest must be a Uint16Array
	var len=pStr.length;
	for(var i=0; i<len; i++) {
	    pDest[i]=pStr.charCodeAt(i);
	}
    };

    r.stack=function() {
	var e = new Error('dummy');
	return e.stack;
    };

    r.range=function(start, end) {
	var nums=[];
	for(var i=start; i<end; i++) {
	    nums.push(i);
	}
	return nums;
    };

    r.defaultFor=function(arg, val) { 
	return typeof arg !== 'undefined' ? arg : val; 
    };

    r.UserException=function(pMsg) {
	this.message=pMsg;
	this.name="UserException";
    };

    r.AssertException=function (pMsg) {
	this.message=pMsg;
	this.name="AssertException";
    };

    r.littleEndian=function() {
	var bytes=new ArrayBuffer(4);
	var arr=new Uint32Array(bytes);
	arr[0]=0x01020304;
	return !(bytes[0]===1 && bytes[1]===2 && bytes[2]===3 && bytes[4]===4);
    };

    r.sgn=function(num) {
	return num?num<0?-1:1:0;
    };

    r.singleton=function(constructor, proto, additions) {
	additions=typeof additions!=='undefined'?additions:{};
	constructor.prototype=proto;

	for(var methodName in additions) {
	    if(additions.hasOwnProperty(methodName)) {
		constructor.prototype[methodName]=additions[methodName];
	    }
	};
	
	return new constructor();
    };

    r.expBackOff=function(interval, retries) {
	if(retries>=16) {
	    return -1;
	}
	if(retries>=10) {
	    retries=10;
	}
	var min=0;
	var max=Math.pow(2, retries);
	var backOff=Math.floor(Math.random()*max); //range of 0 to (max-1)
	return backOff*interval;
    };

    String.prototype.repeat = function( num ) {
	return new Array( num + 1 ).join( this );
    };

    r.repeat=function(pTimes, pGenerator) {
	var arr=[];
	for(var i=0; i<pTimes; i++) {
	    arr.push(pGenerator());
	}
	return arr;
    };

    r.uuid=function() {
	// 23/10/2013 from http://stackoverflow.com/questions/105034/how-to-create-a-guid-uuid-in-javascript
	// TODO 23/10/2013 but see http://stackoverflow.com/questions/6906916/collisions-when-generating-uuids-in-javascript
	// We can probably simplify this function a bit more now that the '-'s are gone
	return 'xxxxxxxxxxxx4xxxyxxxxxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
	    var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
	    return v.toString(16);
	});
    };

    r.basename=function(i) {
	var names=i.match(/^(.*\/)?([^\/]+).?$/);
	if(names!==null) {
	    return names[2];
	}
	return "/";
    };

    r.dirname=function(i) {
	var names=i.match(/^(?:(.*)\/)?[^\/]+.?$/);
	if(names!==null) {
	    return names[1];
	}
	return ".";
    };

    
    var entityMap = {
	"&": "&amp;",
	"<": "&lt;",
	">": "&gt;",
	'"': '&quot;',
	"'": '&#39;',
	"/": '&#x2F;'
    };

    r.escapeHtml=function(string) {
	return String(string).replace(/[&<>"'\/]/g, function (s) {
	    return entityMap[s];
	});
    };
    
    return glb;
};

if(typeof glb==='undefined') {
    var glb=hourglassapps_rtu_domless();
} else {
    hourglassapps_rtu_domless();
}

delete(hourglassapps_rtu_domless);
