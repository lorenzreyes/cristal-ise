package org.cristalise.kernel.common;

/**
* org/cristalise/kernel/common/ObjectNotFoundExceptionHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /home/cristal-ise/workspace/cristal-ise/idl/src/main/idl/CommonExceptions.idl
* Wednesday, 11 December 2019 17:43:31 o'clock CET
*/


/**************************************************************************
    * The requested object was not found
    **************************************************************************/
public final class ObjectNotFoundExceptionHolder implements org.omg.CORBA.portable.Streamable
{
  public org.cristalise.kernel.common.ObjectNotFoundException value = null;

  public ObjectNotFoundExceptionHolder ()
  {
  }

  public ObjectNotFoundExceptionHolder (org.cristalise.kernel.common.ObjectNotFoundException initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = org.cristalise.kernel.common.ObjectNotFoundExceptionHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    org.cristalise.kernel.common.ObjectNotFoundExceptionHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return org.cristalise.kernel.common.ObjectNotFoundExceptionHelper.type ();
  }

}