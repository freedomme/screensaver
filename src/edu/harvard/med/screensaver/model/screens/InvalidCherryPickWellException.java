// $HeadURL$
// $Id$
//
// Copyright 2006 by the President and Fellows of Harvard College.
// 
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
 // at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.model.screens;

import edu.harvard.med.screensaver.model.BusinessRuleViolationException;
import edu.harvard.med.screensaver.model.libraries.WellKey;

import org.apache.log4j.Logger;

public class InvalidCherryPickWellException extends BusinessRuleViolationException
{
  // static members

  private static final long serialVersionUID = 1L;
  private static Logger log = Logger.getLogger(InvalidCherryPickWellException.class);
  
  // instance data
  
  private WellKey _wellKey;

  // public constructors and methods

  public InvalidCherryPickWellException(WellKey wellKey, String message)
  {
    super(message);
    _wellKey = wellKey;
  }
  
  public WellKey getWellKey()
  {
    return _wellKey;
  }
}

