// $HeadURL: svn+ssh://js163@orchestra.med.harvard.edu/svn/iccb/screensaver/trunk/.eclipse.prefs/codetemplates.xml $
// $Id: codetemplates.xml 169 2006-06-14 21:57:49Z js163 $
//
// Copyright 2006 by the President and Fellows of Harvard College.
// 
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.service;

import edu.harvard.med.screensaver.model.AbstractEntity;

public class EntityNotFoundException extends Exception
{
  private static final long serialVersionUID = 1L;

  private Class<? extends AbstractEntity> entityClass;
  
  public EntityNotFoundException(Class<? extends AbstractEntity> entityClass, Object key)
  {
    super("Entity: " + entityClass.getName() + ", key: " + key);
    this.entityClass = entityClass;
  }

  public EntityNotFoundException(Class<? extends AbstractEntity> entityClass, Object key, Throwable cause)
  {
    super("Entity: " + entityClass.getName() + ", key: " + key, cause);
    this.entityClass = entityClass;
  }

}