package com.hourglassapps.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Ii<F,S> implements Cloneable, Serializable {
	private static final long serialVersionUID = 3011084707804958069L;
	protected F mFst;
	protected S mSnd;
	public Ii(final F fst, final S snd) {
		this.mFst=fst;
		this.mSnd=snd;
	}
	
	public Ii(Ii<F, S> orig) {
		this(orig.fst(), orig.snd());
	}

	public F fst() {
		return this.mFst;
	}
	
	public S snd() {
		return this.mSnd;
	}
	
	public int hashCode() {
		if(mFst==null && mSnd==null) {
			return 0;
		} else if(mFst==null && mSnd!=null) {
			return mSnd.hashCode();
		} else if(mFst!=null && mSnd==null) {
			return mFst.hashCode()*1023;
		} else {
			return this.mFst.hashCode()*1023+this.mSnd.hashCode();
		}
	}
	
	public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
		}
        if (!(obj instanceof Ii<?,?>)) {
        	return false;
        }
        //handle null values of fst and snd
        if(this.mFst==null && this.mSnd==null) {
        	return ((Ii<?, ?>) obj).fst()==null && ((Ii<?, ?>) obj).snd()==null;
        }
        if(this.mFst==null) {
        	return ((Ii<?, ?>) obj).fst()==null && this.mSnd.equals(((Ii<?, ?>) obj).snd());
        }
        if(this.mSnd==null) {
        	return ((Ii<?, ?>) obj).snd()==null && this.mFst.equals(((Ii<?, ?>) obj).fst());
        }
        
        return this.mFst.equals(((Ii<?, ?>) obj).fst()) && this.mSnd.equals(((Ii<?, ?>) obj).snd());
	}
	
	public String toString() {
		return new StringBuffer().append("<").append(this.mFst).append(", ").append(this.mSnd).append(">").toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected Ii<F,S> clone() throws CloneNotSupportedException {
		return new Ii<F,S>(fst(), snd());
	}

	public static <F,S> List<Ii<F, S>> zip(List<F> pFsts, List<S> pSnds) {
		if(pFsts.size()!=pSnds.size()) {
			throw new IllegalArgumentException("zip. args lengths don't match");
		}
		List<Ii<F,S>> out=new ArrayList<Ii<F,S>>();
		for(int i=0; i<pFsts.size(); i++) {
			out.add(new Ii<F,S>(pFsts.get(i), pSnds.get(i)));
		}
		return out;
	}
	
	public static <F, S> Ii<List<F>, List<S>> unzip(Collection<Ii<F, S>> pZip) {
		List<F> outFst=new ArrayList<F>();
		List<S> outSnd=new ArrayList<S>();
		for(Ii<F,S> el: pZip) {
			outFst.add(el.fst());
			outSnd.add(el.snd());
		}
		return new Ii<List<F>,List<S>>(outFst, outSnd);
	}
	
	public interface TypeChecker<T> {
		public boolean socket(T male);
		public T plug();
	}
	
	public static <T, F extends TypeChecker<T>, S extends TypeChecker<T>> Ii<F, S> couple(F fst, S snd) {
		if(fst.socket(snd.plug())) {
			return new Ii<F,S>(fst, snd);
		}
		throw new IllegalArgumentException("couple. incompatible couple. fst: "+fst+" snd: "+snd);
	}
}
