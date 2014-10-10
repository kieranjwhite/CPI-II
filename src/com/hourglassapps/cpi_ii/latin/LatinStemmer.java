// This file was generated automatically by the Snowball to Java compiler

package com.hourglassapps.cpi_ii.latin;

import com.hourglassapps.cpi_ii.snowball.tartarus.Among;

/**
 * This class was automatically generated by a Snowball to Java compiler 
 * It implements the stemming algorithm defined by a snowball script.
 */

public class LatinStemmer extends com.hourglassapps.cpi_ii.snowball.tartarus.SnowballProgram {

	private static final long serialVersionUID = 1L;

	//when ASSUMED_POS is UNKNOWN the shortest result from verb stemming and noun stemming is returned
	public enum PartOfSpeech {
		NOUN, VERB, UNKNOWN
	}
	
	private final static PartOfSpeech ASSUMED_POS=PartOfSpeech.UNKNOWN;
	
	private final static LatinStemmer methodObject = new LatinStemmer ();

	private final static Among a_0[] = {
		new Among ( "ita", -1, -1, "", methodObject ),
		new Among ( "qua", -1, -1, "", methodObject ),
		new Among ( "adae", -1, -1, "", methodObject ),
		new Among ( "perae", -1, -1, "", methodObject ),
		new Among ( "quae", -1, -1, "", methodObject ),
		new Among ( "de", -1, -1, "", methodObject ),
		new Among ( "ne", -1, -1, "", methodObject ),
		new Among ( "utribi", -1, -1, "", methodObject ),
		new Among ( "ubi", -1, -1, "", methodObject ),
		new Among ( "undi", -1, -1, "", methodObject ),
		new Among ( "obli", -1, -1, "", methodObject ),
		new Among ( "deni", -1, -1, "", methodObject ),
		new Among ( "uti", -1, -1, "", methodObject ),
		new Among ( "cui", -1, -1, "", methodObject ),
		new Among ( "qui", -1, -1, "", methodObject ),
		new Among ( "quam", -1, -1, "", methodObject ),
		new Among ( "quem", -1, -1, "", methodObject ),
		new Among ( "quarum", -1, -1, "", methodObject ),
		new Among ( "quorum", -1, -1, "", methodObject ),
		new Among ( "co", -1, -1, "", methodObject ),
		new Among ( "deco", 19, -1, "", methodObject ),
		new Among ( "reco", 19, -1, "", methodObject ),
		new Among ( "inco", 19, -1, "", methodObject ),
		new Among ( "conco", 19, -1, "", methodObject ),
		new Among ( "exco", 19, -1, "", methodObject ),
		new Among ( "quando", -1, -1, "", methodObject ),
		new Among ( "utro", -1, -1, "", methodObject ),
		new Among ( "quo", -1, -1, "", methodObject ),
		new Among ( "uter", -1, -1, "", methodObject ),
		new Among ( "tor", -1, -1, "", methodObject ),
		new Among ( "obtor", 29, -1, "", methodObject ),
		new Among ( "praetor", 29, -1, "", methodObject ),
		new Among ( "detor", 29, -1, "", methodObject ),
		new Among ( "retor", 29, -1, "", methodObject ),
		new Among ( "intor", 29, -1, "", methodObject ),
		new Among ( "contor", 29, -1, "", methodObject ),
		new Among ( "optor", 29, -1, "", methodObject ),
		new Among ( "attor", 29, -1, "", methodObject ),
		new Among ( "extor", 29, -1, "", methodObject ),
		new Among ( "quas", -1, -1, "", methodObject ),
		new Among ( "abs", -1, -1, "", methodObject ),
		new Among ( "plenis", -1, -1, "", methodObject ),
		new Among ( "quis", -1, -1, "", methodObject ),
		new Among ( "quotusquis", 42, -1, "", methodObject ),
		new Among ( "quos", -1, -1, "", methodObject ),
		new Among ( "aps", -1, -1, "", methodObject ),
		new Among ( "us", -1, -1, "", methodObject ),
		new Among ( "abus", 46, -1, "", methodObject ),
		new Among ( "quibus", 46, -1, "", methodObject ),
		new Among ( "adus", 46, -1, "", methodObject ),
		new Among ( "cuius", 46, -1, "", methodObject ),
		new Among ( "quous", 46, -1, "", methodObject ),
		new Among ( "sus", 46, -1, "", methodObject ),
		new Among ( "at", -1, -1, "", methodObject )
	};

	private final static Among a_1[] = {
		new Among ( "a", -1, 1, "", methodObject ),
		new Among ( "ia", 0, 1, "", methodObject ),
		new Among ( "ud", -1, 1, "", methodObject ),
		new Among ( "e", -1, 1, "", methodObject ),
		new Among ( "ae", 3, 1, "", methodObject ),
		new Among ( "i", -1, 1, "", methodObject ),
		new Among ( "am", -1, 1, "", methodObject ),
		new Among ( "em", -1, 1, "", methodObject ),
		new Among ( "um", -1, 1, "", methodObject ),
		new Among ( "o", -1, 1, "", methodObject ),
		new Among ( "as", -1, 1, "", methodObject ),
		new Among ( "es", -1, 1, "", methodObject ),
		new Among ( "is", -1, 1, "", methodObject ),
		new Among ( "os", -1, 1, "", methodObject ),
		new Among ( "us", -1, 1, "", methodObject ),
		new Among ( "ibus", 14, 1, "", methodObject ),
		new Among ( "ius", 14, 1, "", methodObject ),
		new Among ( "nt", -1, 1, "", methodObject ),
		new Among ( "u", -1, 1, "", methodObject )
	};

	private final static Among a_2[] = {
		new Among ( "mini", -1, 4, "", methodObject ),
		new Among ( "ri", -1, 4, "", methodObject ),
		new Among ( "sti", -1, 4, "", methodObject ),
		new Among ( "m", -1, 4, "", methodObject ),
		new Among ( "bo", -1, 2, "", methodObject ),
		new Among ( "ero", -1, 3, "", methodObject ),
		new Among ( "r", -1, 4, "", methodObject ),
		new Among ( "bor", 6, 2, "", methodObject ),
		new Among ( "mur", 6, 4, "", methodObject ),
		new Among ( "tur", 6, 4, "", methodObject ),
		new Among ( "ntur", 9, 4, "", methodObject ),
		new Among ( "untur", 10, 1, "", methodObject ),
		new Among ( "iuntur", 11, 1, "", methodObject ),
		new Among ( "s", -1, 4, "", methodObject ),
		new Among ( "ris", 13, 4, "", methodObject ),
		new Among ( "beris", 14, 2, "", methodObject ),
		new Among ( "tis", 13, 4, "", methodObject ),
		new Among ( "stis", 16, 4, "", methodObject ),
		new Among ( "ns", 13, 4, "", methodObject ),
		new Among ( "mus", 13, 4, "", methodObject ),
		new Among ( "t", -1, 4, "", methodObject ),
		new Among ( "nt", 20, 4, "", methodObject ),
		new Among ( "unt", 21, 1, "", methodObject ),
		new Among ( "iunt", 22, 1, "", methodObject ),
		new Among ( "erunt", 22, 1, "", methodObject )
	};

	private java.lang.StringBuilder S_verb_form = new java.lang.StringBuilder();
	private java.lang.StringBuilder S_noun_form = new java.lang.StringBuilder();

	private void copy_from(LatinStemmer other) {
		S_verb_form = other.S_verb_form;
		S_noun_form = other.S_noun_form;
		super.copy_from(other);
	}

	private boolean r_map_letters() {
		int v_1;
		int v_2;
		int v_3;
		int v_4;
		int v_5;
		int v_6;
		// (, line 11
		// do, line 13
		v_1 = cursor;
		lab0: do {
			// repeat, line 13
			replab1: while(true)
			{
				v_2 = cursor;
				lab2: do {
					// (, line 13
					// goto, line 13
					golab3: while(true)
					{
						v_3 = cursor;
						lab4: do {
							// (, line 13
							// [, line 13
							bra = cursor;
							// literal, line 13
							if (!(eq_s(1, "j")))
							{
								break lab4;
							}
							// ], line 13
							ket = cursor;
							cursor = v_3;
							break golab3;
						} while (false);
						cursor = v_3;
						if (cursor >= limit)
						{
							break lab2;
						}
						cursor++;
					}
				// <-, line 13
				slice_from("i");
				continue replab1;
				} while (false);
				cursor = v_2;
				break replab1;
			}
		} while (false);
		cursor = v_1;
		// do, line 14
		v_4 = cursor;
		lab5: do {
			// repeat, line 14
			replab6: while(true)
			{
				v_5 = cursor;
				lab7: do {
					// (, line 14
					// goto, line 14
					golab8: while(true)
					{
						v_6 = cursor;
						lab9: do {
							// (, line 14
							// [, line 14
							bra = cursor;
							// literal, line 14
							if (!(eq_s(1, "v")))
							{
								break lab9;
							}
							// ], line 14
							ket = cursor;
							cursor = v_6;
							break golab8;
						} while (false);
						cursor = v_6;
						if (cursor >= limit)
						{
							break lab7;
						}
						cursor++;
					}
				// <-, line 14
				slice_from("u");
				continue replab6;
				} while (false);
				cursor = v_5;
				break replab6;
			}
		} while (false);
		cursor = v_4;
		return true;
	}

	private boolean r_que_word() {
		int v_1;
		// (, line 19
		// [, line 21
		ket = cursor;
		// literal, line 21
		if (!(eq_s_b(3, "que")))
		{
			return false;
		}
		// ], line 21
		bra = cursor;
		// or, line 34
		lab0: do {
			v_1 = limit - cursor;
			lab1: do {
				// (, line 21
				// among, line 22
				if (find_among_b(a_0, 54) == 0)
				{
					break lab1;
				}
				// atlimit, line 31
				if (cursor > limit_backward)
				{
					break lab1;
				}
				// ], line 31
				bra = cursor;
				// => noun_form, line 32
				S_noun_form = assign_to(S_noun_form);
				// => verb_form, line 33
				S_verb_form = assign_to(S_verb_form);
				break lab0;
			} while (false);
			cursor = limit - v_1;
			// fail, line 34
			// (, line 34
			// delete, line 34
			slice_del();
			return false;
		} while (false);
		return true;
	}

	public boolean stem() {
		return stem(ASSUMED_POS);
	}
	
	public boolean stem(PartOfSpeech pPos) {
		StringBuffer currentAttempt=current;
		int among_var;
		int v_1;
		int v_3;
		int v_5;
		// (, line 38
		// call map_letters, line 40
		if (!r_map_letters())
		{
			return false;
		}
		// backwards, line 42
		limit_backward = cursor; cursor = limit;
		// (, line 42
		// or, line 43
		lab0: do {
			v_1 = limit - cursor;
			lab1: do {
				// call que_word, line 43
				if (!r_que_word())
				{
					break lab1;
				}
				break lab0;
			} while (false);
			cursor = limit - v_1;
			// (, line 43
			// => noun_form, line 44
			S_noun_form = assign_to(S_noun_form);
			// => verb_form, line 45
			S_verb_form = assign_to(S_verb_form);
			// $ noun_form, line 47
			if(pPos!=PartOfSpeech.VERB){
				LatinStemmer v_2 = this;
				current = new StringBuffer(S_noun_form.toString());
				cursor = 0;
				limit = (current.length());
				// backwards, line 47
				limit_backward = cursor; cursor = limit;
				// try, line 47
				v_3 = limit - cursor;
				lab2: do {
					// (, line 47
					// [, line 48
					ket = cursor;
					// substring, line 48
					among_var = find_among_b(a_1, 19);
					if (among_var == 0)
					{
						cursor = limit - v_3;
						break lab2;
					}
					// ], line 48
					bra = cursor;
					// hop, line 48
					{
						int c = cursor - 2;
						if (limit_backward > c || c > limit)
						{
							cursor = limit - v_3;
							break lab2;
						}
						cursor = c;
					}
					switch(among_var) {
					case 0:
						cursor = limit - v_3;
						break lab2;
					case 1:
						// (, line 52
						// delete, line 52
						slice_del();
						break;
					}
				} while (false);
				cursor = limit_backward;                            
				copy_from(v_2);
				currentAttempt=current;
			} 
			if(pPos!=PartOfSpeech.NOUN)
			// $ verb_form, line 56
			{
				
				LatinStemmer v_4 = this;
				current = new StringBuffer(S_verb_form.toString());
				cursor = 0;
				limit = (current.length());
				// backwards, line 56
				limit_backward = cursor; cursor = limit;
				// try, line 56
				v_5 = limit - cursor;
				lab3: do {
					// (, line 56
					// [, line 57
					ket = cursor;
					// substring, line 57
					among_var = find_among_b(a_2, 25);
					if (among_var == 0)
					{
						cursor = limit - v_5;
						break lab3;
					}
					// ], line 57
					bra = cursor;
					// hop, line 57
					{
						int c = cursor - 2;
						if (limit_backward > c || c > limit)
						{
							cursor = limit - v_5;
							break lab3;
						}
						cursor = c;
					}
					switch(among_var) {
					case 0:
						cursor = limit - v_5;
						break lab3;
					case 1:
						// (, line 60
						// <-, line 60
						slice_from("i");
						break;
					case 2:
						// (, line 62
						// <-, line 62
						slice_from("bi");
						break;
					case 3:
						// (, line 64
						// <-, line 64
						slice_from("eri");
						break;
					case 4:
						// (, line 67
						// delete, line 67
						slice_del();
						break;
					}
				} while (false);
				cursor = limit_backward;                            
				copy_from(v_4);
				
				if(currentAttempt.length()<current.length()) {
					current=currentAttempt;
				}
			}
		} while (false);
		cursor = limit_backward;                    
		return true;
	}

	public boolean equals( Object o ) {
		return o instanceof LatinStemmer;
	}

	public int hashCode() {
		return LatinStemmer.class.getName().hashCode();
	}



}

