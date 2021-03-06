package org.cristalise.kernel.common;

/**
* org/cristalise/kernel/common/PersistencyExceptionHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /home/cristal-ise/workspace/cristal-ise/idl/src/main/idl/CommonExceptions.idl
* Wednesday, 11 December 2019 17:43:31 o'clock CET
*/


/**************************************************************************
    * Error during storing/retrieving objects
    **************************************************************************/
public final class PersistencyExceptionHolder implements org.omg.CORBA.portable.Streamable
{
  public org.cristalise.kernel.common.PersistencyException value = null;

  public PersistencyExceptionHolder ()
  {
  }

  public PersistencyExceptionHolder (org.cristalise.kernel.common.PersistencyException initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = org.cristalise.kernel.common.PersistencyExceptionHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    org.cristalise.kernel.common.PersistencyExceptionHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return org.cristalise.kernel.common.PersistencyExceptionHelper.type ();
  }

}
