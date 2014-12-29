package net.sourceforge.carbine;

public class ParseException extends TraceableException
{
    public ParseException()
    {
    }

    public ParseException(String message)
    {
        super(message);
    }

    public ParseException(Exception e)
    {
        super(e);
    }
}
