package org.cristalise.kernel.common;

/**
* org/cristalise/kernel/common/GTimeStampHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /home/cristal-ise/workspace/cristal-ise/idl/src/main/idl/CommonTypes.idl
* Wednesday, 11 December 2019 17:43:31 o'clock CET
*/

public final class GTimeStampHolder implements org.omg.CORBA.portable.Streamable
{
  public org.cristalise.kernel.common.GTimeStamp value = null;

  public GTimeStampHolder ()
  {
  }

  public GTimeStampHolder (org.cristalise.kernel.common.GTimeStamp initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = org.cristalise.kernel.common.GTimeStampHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    org.cristalise.kernel.common.GTimeStampHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return org.cristalise.kernel.common.GTimeStampHelper.type ();
  }

}
