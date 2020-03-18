package com.hourglassapps.util;

public final class Option<T> {
    private final static String TAG=Option.class.getName();

    private final boolean mHasVal;
    private final T mVal;

    public Option() {
	mVal=null;
	mHasVal=false;
    }
    
    public Option(T pVal) {
	if(pVal==null) {
	    throw new IllegalArgumentException();
	}
	mVal=pVal;
	mHasVal=true;
    }

    public T val() {
	if(!mHasVal) {
	    throw new IllegalStateException();
	}
	
	return mVal;
    }

    public boolean hasVal() {
	return mHasVal;
    }
    
    @Override
    public boolean equals(Object pOther) {
	if(pOther==null) {
	    return false;
	}

	if(!this.getClass().equals(pOther.getClass())) {
	    return false;
	}

	Option<?> other=(Option<?>)pOther;

	if(mHasVal!=other.mHasVal) {
	    return false;
	}

	if(mHasVal==false) {
	    return true;
	}
	
	return mVal.equals(other.mVal);
    }

    @Override
    public int hashCode() {
	return 4581957 ^ mVal.hashCode() ^ (new Boolean(mHasVal)).hashCode();
    }

    @Override
    public String toString() {
	if(mHasVal) {
	    return TAG+"(val: "+mVal.toString()+")";
	} else {	
	    return TAG+"()";
	}
    }
}
