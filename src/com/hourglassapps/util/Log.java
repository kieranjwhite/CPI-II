package com.hourglassapps.util;

public class Log {
	public static enum Level {
		VERBOSE(0), INFO(1), ERROR(2);
		
		private final int mImportance;
		Level(int pImportance) {
			this.mImportance=pImportance;
		}
		
		public int importance() {
			return mImportance;
		}
	}
	
	public final static boolean BARE_OUTPUT=true;
    public static Log.Level LEVEL = Log.Level.INFO;

    static private void write(String pTag, String pMsg) {
    	if(BARE_OUTPUT) {
    		System.err.print(pMsg);    		
    	} else {
    		System.err.println(pTag+": "+pMsg);
    	}
    }
    
    static private void writeln(String pTag, String pMsg) {
    	write(pTag, pMsg);
    	System.err.println();
    }
    
    static private void printStack(String tag, Throwable e) {
    	String msg=e.getMessage();
    	if(msg!=null) {
    		writeln(tag, e.getMessage());
    	} else {
    		writeln(tag, "Exception: "+e);
    	}
		for(StackTraceElement frame: e.getStackTrace()) {
			writeln(tag, "\t"+frame.toString()+" ("+frame.getClassName()+":"+frame.getLineNumber()+")");
		}
		Throwable cause=e.getCause();
		if(cause!=null) {
			writeln(tag, "caused by...");
			printStack(tag, cause);
		}
    }
    
    static public void v(String tag, String msgFormat, Object...args)
    {
        if (LEVEL.importance()<=Log.Level.VERBOSE.importance())
        {
            write(tag, String.format(msgFormat, args));
        }
    }

    static public void v(String tag, Throwable t, String msgFormat, Object...args)
    {
        if (LEVEL.importance()<=Log.Level.VERBOSE.importance())
        {
            write(tag, String.format(msgFormat, args));
            printStack(tag, t); 
        }
    }

    static public void i(String tag, String msgFormat, Object...args)
    {
        if (LEVEL.importance()<=Log.Level.INFO.importance())
        {
        	write(tag, String.format(msgFormat, args));
        }
    }

    static public void i(String tag, Throwable t, String msgFormat, Object...args)
    {
        if (LEVEL.importance()<=Log.Level.INFO.importance())
        {
        	write(tag, String.format(msgFormat, args));
            printStack(tag, t); 
        }
    }

    static public void e(String tag, String msgFormat, Object...args)
    {
        if (LEVEL.importance()<=Log.Level.ERROR.importance())
        {
            writeln(tag, String.format(msgFormat, args));
        }
    }

    static public void e(Runnable pIf, Runnable pElse)
    {
        if (LEVEL.importance()<=Log.Level.ERROR.importance()) {
        	pIf.run();
        } else {
        	pElse.run();
        }
    }

    static public void i(Runnable pIf, Runnable pElse)
    {
        if (LEVEL.importance()<=Log.Level.INFO.importance()) {
        	pIf.run();
        } else {
        	pElse.run();
        }
    }

    static public void v(Runnable pIf, Runnable pElse)
    {
        if (LEVEL.importance()<=Log.Level.VERBOSE.importance()) {
        	pIf.run();
        } else {
        	pElse.run();
        }
    }

    static public void e(String tag, Throwable t)
    {
        if (LEVEL.importance()<=Log.Level.ERROR.importance())
        {
            printStack(tag, t); 
        }
    }

    static public void i(String tag, Throwable t)
    {
        if (LEVEL.importance()<=Log.Level.INFO.importance())
        {
            printStack(tag, t); 
        }
    }

    static public void eStack(String tag, String msgFormat, Object...args)
    {
        if (LEVEL.importance()<=Log.Level.ERROR.importance())
        {
            writeln(tag, String.format(msgFormat, args));
            try {
            	throw new Exception();
            } catch(Exception t) {
            	printStack(tag, t);
            }
        }
    }

    static public void e(String tag, Throwable t, String msgFormat, Object...args)
    {
        if (LEVEL.importance()<=Log.Level.ERROR.importance())
        {
            writeln(tag, String.format(msgFormat, args));
            printStack(tag, t); 
        }
    }

}
