package com.hourglassapps.cpi_ii;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The fields in this class are populated by the Jackson JSON parser.
 * Each field corresponds to a key-value pair in corresponding JSON object.
 * <p>
 * Setter implementations are required by Jackson and are invoked to set field values.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PoemRecord implements Record<Long, String> {
	private static final String TEXT_INCIPIT_ONLY = "Text incipit only";
	private static final String LANG_LATIN="Latin";
	private String _poem_text_3;
	private long _eprintid;
	private String _date;
	private String _first_line;
	private int _userid;
	private String _author;
	private int _rev_number;
	private String _dir;
	private String _lastmod;
	public enum Topical { No, Yes };
	private Topical _topical;
	private int[] _poem_text_source_multi;
	private int _item_issues_count;
	private String[] _topic_tags;
	private enum MetadataVisiblility { show };
	private MetadataVisiblility _metadata_visibility;
	private enum EprintStatus { archive };
	private EprintStatus _eprint_status;
	private String _status_changed;
	private String _language;
	public static class Stanza {
		private String _rhyme;
		private String _end_accent;
		private String _line_length;
	}
	private Stanza[] _stanzas;
	private String _datestamp;
	private String _uri;
	private enum Type { poem };
	private Type _type;
	private String _title;
	private enum Datable { Yes, No };
	private Datable _datable;
	private String _no_of_stanzas;
	private String _refrain_text_3;
	private final static String BOUNDARY_TEXT="\n0. \n"; //numerals 
	
	@Override
	public Long id() {
		return _eprintid;
	}
	
	@Override
	public String content() {
		assert LANG_LATIN.equals(getLanguage());
		//if(_poem_text_3!=null || _refrain_text_3!=null) {
			StringBuilder text=new StringBuilder();
			boolean added=false;
			if(_title!=null && _title.length()>0) {
				text.append(_title);
				added=true;
			}
			if(added) {
				text.append(BOUNDARY_TEXT);
				added=false;
			}
			if(_poem_text_3!=null && _poem_text_3.length()>0) {
				text.append(_poem_text_3);
				added=true;
			}
			if(added) {
				text.append(BOUNDARY_TEXT);
				added=false;
			}
			if(_refrain_text_3!=null && _refrain_text_3.length()>0) {
				text.append(_refrain_text_3);
			}
			return text.toString().trim();
		//} else {
		//	//assert(TEXT_INCIPIT_ONLY.equals(getNoOfStanzas()));
		//	return getTitle();
		//}
	}
	
	public String[] lines() {
		return content().split("\n");
	}
	
	@Override
	public boolean ignore() {
		return !LANG_LATIN.equals(getLanguage());
	}

	public void setPoem_text_3(String pArg) {
		_poem_text_3=pArg;
	}
	
	public void setRefrain_text_3(String pArg) {
		_refrain_text_3=pArg;
	}
	
	public void setEprintid(long pArg) {
		_eprintid=pArg;
	}
	
	public String getLanguage() {
		return _language;
	}
	
	public void setLanguage(String pArg) {
		_language=pArg;
	}

	public String getTitle() {
		return _title;
	}
	
	public void setTitle(String pArg) {
		_title=pArg;
	}

	public void setDate(String pArg) {
		_date=pArg;
	}
	
	public String getNoOfStanzas() {
		return _no_of_stanzas;
	}
	
	public void setNo_of_stanzas(String pArg) {
		_no_of_stanzas=pArg;
	}
	
}
