/*global $,jQuery,glb,jx,when
*/
(function() {
    var g=glb();
    var document_root="../";
    var hashTag=null;
    var blacklist=[];
    var num_blacklisted=0;
    var result_num=null;
    var me=null;
    
    g.results={
	root:"results/completed/",
	query:null,
	
	setupPage:function() {
	    me=window.location.pathname;
	    
	    var pQuery=window.location.hash.substring(1);
	    hashTag=decodeHashId(pQuery);
	    g.results.query=g.results.root+hashTag.f;
	    var title;

	    if(hashTag.hasOwnProperty('t')) {
		title=hashTag.t;
	    } else {
		title=hashTag.f;
	    }

	    if(hashTag.hasOwnProperty('n')) {
		result_num=hashTag.n;
	    }
	    
	    $("div[data-role='header']>h2").html(title);
	},

	setBlacklist:function(pBlacklist) {
	    blacklist=pBlacklist;
	    num_blacklisted=blacklist.length;
	}
    };

    var blacklisted=function(url) {
	for(var i=0; i<num_blacklisted; i++) {
	    if(blacklist[i].test(url)) {
		return true;
	    }
	}
	return false;
    };
    
    var src=function(result_idx,path) {
	var source={
	    i:result_idx,
	    file:null,
	    line:null
	};

	var parent=g.rtu.dirname(path);
	var parentName=g.rtu.basename(parent);
	source.file=parent+'/'+'_'+parentName;
	var filename=g.rtu.basename(path);
	var extensionIdx=filename.lastIndexOf(".");
	if(extensionIdx===-1) {
	    source.line=filename>>0;
	} else {
	    source.line=filename.substring(0, extensionIdx)>>0;
	}
	return source;
    };

    var encodeHashId=function(result_idx, title) {
	var new_hash_tag={
	    t: title,
	    f: hashTag.f,
	    n: result_idx
	};

	var stringified=JSON.stringify(new_hash_tag);
	var utf8_string=unescape(encodeURIComponent(stringified));
	var encoded=window.btoa(utf8_string);
	var fstEq=encoded.indexOf('=');
	if(fstEq==-1) {
	    fstEq=encoded.length;
	}
	var numEqs=encoded.length-fstEq;
	return 'J'+numEqs+g.rtu.trList(encoded.substring(0,fstEq), ["\\+","\\/"], "-_");
    };
    
    var decodeHashId=function(id) {
	var padding_cnt=id.substring(1,2)>>0;
	var stripped=id.substring(2);
	var base64_encoded=g.rtu.tr(stripped+('='.repeat(padding_cnt)), "-_", "+/");
	var arg_str_utf8=window.atob(base64_encoded);
	var arg_str=decodeURIComponent(escape(arg_str_utf8));
	return jQuery.parseJSON(arg_str);
    };
    
    var pathToTextCopy=function(path) {
	var middle_arr=path.split('/');
	var journal_name=middle_arr[1];
	var journal_num=journal_name.split('_')[0];
	var ngram=middle_arr[3];
	var num_extn=middle_arr[4];
	var num=num_extn.split('.')[0];
	return "results/completed/"+journal_num+'+'+(g.rtu.escapeHTML(ngram)).replace(/\+/g,'%20')+'+'+num+"/links.js";
    };
    
    var listItem=function(result_idx, data, original_url, hidden) {
	var style;
	if(hidden) {
	    style="style=\"display:none;\"";
	} else {
	    style="";
	}
	var title;
	if(data.t==="") {
	    title=data.p;
	} else {
	    title=data.t;
	}
	var encoded_title=g.rtu.escapeHtml(title);
	
	return "<li "+style+">"+
	    "<a href=\""+original_url+"\" data-rel=\"external\" target=\"_blank\"><h3>"+encoded_title+"</h3><p>"+g.rtu.escapeHtml(original_url)+"</p></a>"+
	    "<a href=\""+me+'#'+encodeHashId(result_idx, encoded_title)+"\" data-rel=\"external\" target=\"_blank\">Local copy</a>"+
	    "</li>\n";
    };

    var Doc=function(result) {
	var textPath=pathToTextCopy(result.p);
	var text=null;
	var spans=result.s; //byte offsets

	var loaded=jx.load(textPath, 'text/plain');

	var offsetToIdx=function(offset) {
	    return offset<<1;
	};
	
	var identityWrapper=function(input) {
	    return input;
	};

	var highlightWrapper=function(input) {
	    return "<div class=\"highlight\">"+input+"</div>";
	};
	
	var addSection=function(jquery_obj, start_offset, end_offset, wrapper) {
	    if(typeof wrapper==='undefined') {
		wrapper=identityWrapper;
	    }
	    
	    var content=wrapper(g.rtu.escapeHtml(text.substring(offsetToIdx(start_offset), offsetToIdx(end_offset))).replace(/\n{2,}/g, '<br><br>').replace(/\n/, '<br>'));
	    jquery_obj.append(content);
	};

	var displayed=when.defer();
	this.display=function(jquery_obj) {
	    loaded.then(function(response) {
		var from_offset=0;
		text=response;
		for(var i=0; i<spans.length; i++) {
		    addSection(jquery_obj, from_offset, spans[i].s);
		    addSection(jquery_obj, spans[i].s, spans[i].e, highlightWrapper);
		    from_offset=spans[i].e;
		}
		addSection(jquery_obj, from_offset, text.length);
		displayed.resolve();
	    });
	};

	this.jump=function(span_num) {
	    displayed.then(function(){
		
	    });
	};
    };

    g.list=function(results) {
	if(result_num===null) {
	    var page=$("#pageone").first();
	    var l="<ul id=\"list\" data-role=\"listview\" data-split-icon=\"arrow-d\" data-split-theme=\"d\" data-inset=\"true\"></ul>";
	    page.append(l);
	    $("#list").first().listview();
	    listResults(results);
	} else {
	    //show document
	    var doc=new Doc(results[result_num]);
	    doc.display($("#pageone").first());
	    doc.jump(0);
	}
    };
    
    var listResults=function(results) {
	var todo=0;
	var idxToPendingURL={};
	var urlToIdx={};
	var list=$("#list").first();

	var lookupSrc=function(promise, i) {
	    return promise.then(function() {
		var source=src(i,results[i].p);
		var deferred=$.Deferred();
		jx.load(document_root+source.file, 'text/plain').then(function(response) {
		    var sources=response.result;
		    if(sources==="") {
			g.rtu.report("Unable to load original source URLs. Your browser may be imposing cross-domain restrictions on iframe loading. Not all results will be listed");
		    }
		    var original_url=sources.split('\n')[source.line-1];
		    if(urlToIdx.hasOwnProperty(original_url)) {
			if(source.i<urlToIdx[original_url]) {
			    urlToIdx[original_url]=source.i;
			}
		    } else if(blacklisted(original_url)) {
			urlToIdx[original_url]=-1;
		    } else {
			urlToIdx[original_url]=source.i;
		    }
		    var changes="";
		    idxToPendingURL[source.i]=original_url;
		    if(source.i===todo) {
			var any_visible=false;
			while(idxToPendingURL.hasOwnProperty(todo)) {
			    var visible=(urlToIdx[idxToPendingURL[todo]]===todo);
			    any_visible=any_visible || visible;
			    changes+=listItem(i,results[todo], original_url, !visible);
			    delete(idxToPendingURL[todo]);
			    todo++;
			}
		    }
		    if(changes.length>0) {
			list.append(changes);
		    }
		    if(any_visible) {
			list.listview("refresh");
		    }
		    deferred.resolve();
		}), function(pErr) {
		    g.rtu.report("Failed to load source: "+pErr.msg+" ("+pErr.status+")");
		};
		return deferred.promise();
	    });
	};
	
	var deferred=$.Deferred();
	deferred.resolve();
	var promise=deferred.promise();
	for(var i=0; i<results.length; i++) {
	    if(results[i]===null) {
		break;
	    }
	    promise=lookupSrc(promise, i);
	}
    };
}());
