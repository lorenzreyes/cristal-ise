package org.cristalise.kernel.common;


/**
* org/cristalise/kernel/common/InvalidCollectionModification.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from /home/cristal-ise/workspace/cristal-ise/idl/src/main/idl/CommonExceptions.idl
* Wednesday, 11 December 2019 17:43:31 o'clock CET
*/


/**************************************************************************
    * The modification to the collection could not be performed, due to rules
    * within the collection itself.
    **************************************************************************/
public final class InvalidCollectionModification extends org.omg.CORBA.UserException
{
  public String details = null;

  public InvalidCollectionModification ()
  {
    super(InvalidCollectionModificationHelper.id());
  } // ctor

  public InvalidCollectionModification (String _details)
  {
    super(InvalidCollectionModificationHelper.id());
    details = _details;
  } // ctor


  public InvalidCollectionModification (String $reason, String _details)
  {
    super(InvalidCollectionModificationHelper.id() + "  " + $reason);
    details = _details;
  } // ctor

} // class InvalidCollectionModification
