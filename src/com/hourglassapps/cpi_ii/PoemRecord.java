package com.hourglassapps.cpi_ii;

public class PoemRecord implements Record<Long, String> {
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
	private enum Language { Latin };
	private Language _language;
	private static class Stanza {
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
	private int _no_of_stanzas;
	
	public Long id() {
		return _eprintid;
	}
	
	public String content() {
		return _poem_text_3;
	}
}
