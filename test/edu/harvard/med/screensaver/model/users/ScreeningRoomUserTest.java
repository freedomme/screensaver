// $HeadURL: svn+ssh://js163@orchestra.med.harvard.edu/svn/iccb/screensaver/trunk/.eclipse.prefs/codetemplates.xml $
// $Id: codetemplates.xml 169 2006-06-14 21:57:49Z js163 $
//
// Copyright 2006 by the President and Fellows of Harvard College.
// 
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.model.users;

import java.beans.IntrospectionException;
import java.util.Date;

import edu.harvard.med.screensaver.db.DAOTransaction;
import edu.harvard.med.screensaver.model.AbstractEntityInstanceTest;
import edu.harvard.med.screensaver.model.DataModelViolationException;

import org.apache.log4j.Logger;

public class ScreeningRoomUserTest extends AbstractEntityInstanceTest
{
  // static members

  private static Logger log = Logger.getLogger(ScreeningRoomUserTest.class);


  // instance data members

  
  // public constructors and methods

  public ScreeningRoomUserTest() throws IntrospectionException
  {
    super(ScreeningRoomUser.class);
  }
  
  public void testDerivedLabName()
  {
    schemaUtil.truncateTablesOrCreateSchema();
    genericEntityDao.doInTransaction(new DAOTransaction() 
    {
      public void runTransaction() 
      {
        ScreeningRoomUser labMember = new ScreeningRoomUser(new Date(), "Lab", "Member", "lab_member@hms.harvard.edu", "", "","", "", "", ScreeningRoomUserClassification.ICCBL_NSRB_STAFF, false);
        genericEntityDao.persistEntity(labMember);
        assertNull("lab member without lab head", labMember.getLabName());

        ScreeningRoomUser labHead = new ScreeningRoomUser(new Date(), "Lab", "Head", "lab_head@hms.harvard.edu", "", "","", "", "", ScreeningRoomUserClassification.ICCBL_NSRB_STAFF, false);
        labHead.setLabAffiliation(new LabAffiliation("LabAffiliation", AffiliationCategory.HMS));
        labMember.setLabHead(labHead);
        genericEntityDao.persistEntity(labHead);
      }
    });
    
    ScreeningRoomUser labMember = genericEntityDao.findEntityByProperty(ScreeningRoomUser.class, "lastName", "Member");
    ScreeningRoomUser labHead = labMember.getLabHead();
    assertEquals("lab member with lab head", "Head, Lab - LabAffiliation", labMember.getLabName());
    assertEquals("lab head", "Head, Lab - LabAffiliation", labHead.getLabName());
  }
  
  public void testAdministrativeRoleNotAllowed() {
    final ScreeningRoomUser user = new ScreeningRoomUser(new Date(),
                                                   "first",
                                                   "last",
                                                   "first_last@hms.harvard.edu",
                                                   "",
                                                   "",
                                                   "",
                                                   "ec1",
                                                   "",
                                                   ScreeningRoomUserClassification.ICCBL_NSRB_STAFF,
                                                   false);
    user.addScreensaverUserRole(ScreensaverUserRole.RNAI_SCREENING_ROOM_USER);
    genericEntityDao.persistEntity(user);
    
    ScreeningRoomUser user2 = genericEntityDao.findEntityById(ScreeningRoomUser.class, user.getEntityId(), false, "screensaverUserRoles");
    assertEquals(1, user2.getScreensaverUserRoles().size());
    assertEquals(ScreensaverUserRole.RNAI_SCREENING_ROOM_USER, user2.getScreensaverUserRoles().iterator().next());

    try {
      genericEntityDao.doInTransaction(new DAOTransaction() {
        public void runTransaction() 
        {
          ScreeningRoomUser user2 = genericEntityDao.findEntityById(ScreeningRoomUser.class, user.getEntityId(), false, "screensaverUserRoles");
          user2.getScreensaverUserRoles().add(ScreensaverUserRole.READ_EVERYTHING_ADMIN);
        };
      });
      fail("expected DataModelViolationException after adding administrative role to screening room user");
    }
    catch (Exception e) {}
    
    try {
      genericEntityDao.doInTransaction(new DAOTransaction() {
        public void runTransaction() 
        {
          ScreeningRoomUser user2 = genericEntityDao.findEntityById(ScreeningRoomUser.class, user.getEntityId(), false, "screensaverUserRoles");
          user2.addScreensaverUserRole(ScreensaverUserRole.READ_EVERYTHING_ADMIN);
        };
      });
      fail("expected DataModelViolationException after adding administrative role to screening room user");
    }
    catch (Exception e) {}
  }
}

