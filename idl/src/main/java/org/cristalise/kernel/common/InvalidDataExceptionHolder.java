package org.cristalise.kernel.common;

/**
* org/cristalise/kernel/common/InvalidDataExceptionHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /home/cristal-ise/workspace/cristal-ise/idl/src/main/idl/CommonExceptions.idl
* Wednesday, 11 December 2019 17:43:31 o'clock CET
*/


/**************************************************************************
    * Either the supplied data, or the relevant description, was invalid.
    **************************************************************************/
public final class InvalidDataExceptionHolder implements org.omg.CORBA.portable.Streamable
{
  public org.cristalise.kernel.common.InvalidDataException value = null;

  public InvalidDataExceptionHolder ()
  {
  }

  public InvalidDataExceptionHolder (org.cristalise.kernel.common.InvalidDataException initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = org.cristalise.kernel.common.InvalidDataExceptionHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    org.cristalise.kernel.common.InvalidDataExceptionHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return org.cristalise.kernel.common.InvalidDataExceptionHelper.type ();
  }

}
