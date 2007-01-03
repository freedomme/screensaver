// $HeadURL: svn+ssh://js163@orchestra.med.harvard.edu/svn/iccb/screensaver/trunk/.eclipse.prefs/codetemplates.xml $
// $Id: codetemplates.xml 169 2006-06-14 21:57:49Z js163 $
//
// Copyright 2006 by the President and Fellows of Harvard College.
// 
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.ui.util;

import java.util.Collection;

import edu.harvard.med.screensaver.db.DAO;
import edu.harvard.med.screensaver.model.AbstractEntity;

import org.apache.log4j.Logger;

public class UISelectOneEntityBean<E extends AbstractEntity> extends UISelectOneBean<E>
{

  // static members

  private static Logger log = Logger.getLogger(UISelectOneEntityBean.class);


  // instance data members

  private DAO _dao;

  
  // public constructors and methods

  public UISelectOneEntityBean(Collection<E> entities, DAO dao)
  {
    this(entities, null, dao);
  }
  
  public UISelectOneEntityBean(Collection<E> entities, E defaultSelection, DAO dao)
  {
    super(entities, defaultSelection);
    _dao = dao;
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public E getSelection()
  {
    AbstractEntity entity = super.getSelection();
    // can't do this, cuz entity may have already been loaded into the current session, causing NonUniqueObjectException
    //_dao.persistEntity(entity); 
    return (E) _dao.findEntityById(entity.getClass(), entity.getEntityId());
  }

}
