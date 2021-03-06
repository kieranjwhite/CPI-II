/*global $,jQuery,glb,jx,when
*/
(function() {
    var g=glb();
    var document_root="../";
    var hashTag=null;
    var blacklist=[];
    var num_blacklisted=0;
    var result_num=null;
    var doc_hash_id=null;
    var me=null;
    
    g.results={
	root:"results/completed/",
	doc:null,
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
	    document.title=unescape(title);
	    
	    $("div[data-role='header']>h2").html(title);
	    if(hashTag.hasOwnProperty('n')) {
		result_num=hashTag.n;
		$("div[data-role='header']").addClass('ui-header-fixed');
	    }
	    
	    if(hashTag.hasOwnProperty('h')) {
		doc_hash_id=hashTag.h;
	    }
	    
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
	return "results/text/completed/"+journal_num+'+'+(g.rtu.escapeHTML(ngram)).replace(/\+/g,'%252B')+'+'+num;
    };

    var provideSnippets=function(path, spans, size, total_size) {
	var textPath=pathToTextCopy(path);
	return jx.load(textPath, 'text/plain').then(function(response){
	    var text=response.result;
	    var snippets=new SnippetsBuilder(text, spans, size, false).buildList();
	    if(snippets.length===0) {
		g.rtu.localError("zero snippets found in "+path);
	    }
	    var accum_size=0;
	    var s=0;
	    for(; s<snippets.length; s++) {
		if(accum_size>=total_size) {
		    break;
		}
		accum_size+=snippets[s].length;
	    }
	    return snippets.slice(0,s).join("...");
	});
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
	    "<a href=\""+original_url+"\" data-rel=\"external\" target=\"_blank\"><h3>"+encoded_title+"</h3><p>"+g.rtu.escapeHtml(original_url)+"</p>"+
	    "<p id=\"r"+result_idx+"\" class=\"snippet\" style=\"white-space: normal;\"></p></a>"+
	    "<a href=\""+me+'#'+encodeHashId(result_idx, encoded_title)+"\" data-rel=\"external\" target=\"_blank\">All snippets</a>"+
	    "</li>\n";
    };

    var SnippetsBuilder=function(text, spans, size, navigable) {
	if(typeof navigable==="undefined") {
	    navigable=true;
	}
	var highlighted=[];
	var hl_num=0;
	var num_highlighted=0;
	
	var identityWrapper=function(input) {
	    return input;
	};

	var highlightWrapper=function(input) {
	    if(navigable) {
		return "<span id=\"h"+(num_highlighted++)+"\" class=\"highlight\">"+input+"</span>";
	    } else {
		return "<span class=\"highlight\">"+input+"</span>";
	    }
	};
	
	var section=function(start_offset, end_offset, wrapper) {
	    if(typeof wrapper==='undefined') {
		wrapper=identityWrapper;
	    }
	    
	    var content=g.rtu.escapeHtml(text.substring(start_offset, end_offset));
	    if(navigable) {
		content=content.replace(/\n/g, "<br>");
	    }
	    return wrapper(content);
	};

	this.buildList=function() {
	    var cur_snip=[];
	    var from_offset=0;
	    var num_highlights=spans.length;
	    var snippet_end;
	    for(var i=0; i<num_highlights; i++) {
		snippet_end=from_offset+size;
		var snippet_start=spans[i].s-size;
		if(snippet_end>snippet_start) {
		    cur_snip.push(section(from_offset, spans[i].s));
		} else {
		    cur_snip.push(section(from_offset, snippet_end));
		    highlighted[hl_num++]=cur_snip.join();
		    cur_snip=[];
		    cur_snip.push(section(snippet_start, spans[i].s));
		}
		cur_snip.push(section(spans[i].s, spans[i].e, highlightWrapper));
		from_offset=spans[i].e;
	    }
	    snippet_end=Math.min(from_offset+size, text.length);
	    cur_snip.push(section(from_offset, snippet_end));
	    if(snippet_end<text.length) {
		highlighted[hl_num++]=cur_snip.join();
		cur_snip=[""];
	    }
	    highlighted[hl_num++]=cur_snip.join();
	    return highlighted;
	};

    };
    
    var Doc=function(result) {
	var textPath=pathToTextCopy(result.p);
	var text=null;
	var spans=result.s; //byte offsets
	var cur_highlight=null;
	var space_at_top=20;
	
	var loaded=jx.load(textPath, 'text/plain').then(function(response){
	    text=response.result;
	});

	var displayed=when.defer();
	var context_size=5*1024;
	var snip_str="<br><p style=\"text-align: center\"><b>...non-matching content omitted...</b></p><br>";

	this.display=function(jquery_obj) {
	    this.snippets(context_size).then(function(snippets) {
		//the <br> tags on the next line are unfortunately needed because jquery mobile allows the header to overlap the content. Adjusting top-margin to compensate had no effect -- probably need to figure how to force layout change.
		jquery_obj.html("<div id=\"top\" class=\"plain\"><br><br><br>"+snippets.join(snip_str)+"</div>");
		displayed.resolve();
	    });
	};
	
	this.snippets=function(size) {
	    return loaded.then(function(){
		return new SnippetsBuilder(text, spans, size).buildList();
	    });
	};
	
	this.jump=function(span_num) {
	    if(span_num<0 || span_num>=spans.length) {
		var msg;
		if(span_num<0) {
		    msg="No prior matches";
		} else {
		    msg="No more matches";
		}
		g.rtu.showMessage(msg, 1500);
		return;
	    }
	    displayed.promise.then(function(){
		$("body,html").scrollTop(Math.max(0,(($("#h"+span_num).position().top-$("div[data-role='header']").height())-space_at_top)));
	    });
	    cur_highlight=span_num;
	};

	this.ret=function() {
	    var doc=this;
	    displayed.promise.then(function(){
		var body=$("body");
		if(cur_highlight===null) {
		    body.scrollTop((body.position().top-$("div[data-role='header']").height()));
		} else {
		    doc.jump(cur_highlight);
		}
	    });
	};

	this.prev=function() {
	    var doc=this;
	    displayed.promise.then(function(){
		if(cur_highlight===null) {
		    cur_highlight=spans.length;
		}
		doc.jump(cur_highlight-1);
	    });
	};

	this.next=function() {
	    var doc=this;
	    displayed.promise.then(function(){
		if(cur_highlight===null) {
		    cur_highlight=-1;
		}
		doc.jump(cur_highlight+1);
	    });
	};
    };

    g.list=function(results) {
	if(result_num===null) {
	    var page=$("#content").first();
	    var l="<ul id=\"list\" data-role=\"listview\" data-split-icon=\"arrow-d\" data-split-theme=\"d\" data-inset=\"true\"></ul>";
	    page.html(l);
	    $("#list").first().listview();
	    listResults(results);
	} else {
	    //show document
	    $("div[data-role='header']").append("<div data-iconpos=\"left\" data-role=\"navbar\">\
<ul><li><a href=\"#\" data-icon=\"arrow-u\" data-rel=\"external\" onclick=\"glb().results.doc.prev();return false\">Previous</a></li>\
<li><a href=\"#\" data-icon=\"forward\" data-rel=\"external\" onclick=\"glb().results.doc.ret();return false\">Reposition</a></li>\
<li><a href=\"#\" data-icon=\"arrow-d\" data-rel=\"external\" onclick=\"glb().results.doc.next();return false\">Next</a></li></ul></div>");
	    glb().results.doc=new Doc(results[result_num]);
	    glb().results.doc.display($("#content").first());
	    /*
	    if(doc_hash_id!==null) {
		doc.jump(doc_hash_id);
	    }
	     */
	    $("[data-role=\"navbar\"]").navbar();
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
		    var new_results=[];
		    idxToPendingURL[source.i]=original_url;
		    if(source.i===todo) {
			var any_visible=false;
			while(idxToPendingURL.hasOwnProperty(todo)) {
			    var visible=(urlToIdx[idxToPendingURL[todo]]===todo);
			    any_visible=any_visible || visible;
			    var r=results[todo];
			    changes+=listItem(todo,r, original_url, !visible);
			    new_results.push({i:todo, res:r}); //can't do anything with these until 'changes' has been added to the list

			    delete(idxToPendingURL[todo]);
			    todo++;
			}
		    }
		    if(changes.length>0) {
			list.append(changes);
			for(var new_idx=0; new_idx<new_results.length; new_idx++) {
			    var idx_result=new_results[new_idx];
			    var added_idx=idx_result.i;
			    var res=idx_result.res;
			    var prom=provideSnippets(res.p, res.s, 30, 512);
			    prom.then(
				function(snippetHTML) {
				    $("#r"+added_idx).html(snippetHTML.trim());
				    list.listview("refresh");
				}
			    );
			}
			new_results=[];
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
