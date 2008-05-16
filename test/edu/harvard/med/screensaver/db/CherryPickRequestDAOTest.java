// $HeadURL:
// svn+ssh://ant4@orchestra.med.harvard.edu/svn/iccb/screensaver/trunk/src/test/edu/harvard/med/screensaver/TestHibernate.java
// $
// $Id: ComplexDAOTest.java 2359 2008-05-09 21:16:57Z ant4 $
//
// Copyright 2006 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.db;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.harvard.med.screensaver.AbstractSpringPersistenceTest;
import edu.harvard.med.screensaver.model.MakeDummyEntities;
import edu.harvard.med.screensaver.model.cherrypicks.CherryPickRequest;
import edu.harvard.med.screensaver.model.cherrypicks.LabCherryPick;
import edu.harvard.med.screensaver.model.cherrypicks.ScreenerCherryPick;
import edu.harvard.med.screensaver.model.libraries.Library;
import edu.harvard.med.screensaver.model.libraries.LibraryType;
import edu.harvard.med.screensaver.model.libraries.Well;
import edu.harvard.med.screensaver.model.libraries.WellKey;
import edu.harvard.med.screensaver.model.libraries.WellName;
import edu.harvard.med.screensaver.model.screens.Screen;
import edu.harvard.med.screensaver.model.screens.ScreenType;
import edu.harvard.med.screensaver.service.cherrypicks.CherryPickRequestAllocatorTest;

import org.apache.log4j.Logger;


/**
 * @author <a mailto="andrew_tolopko@hms.harvard.edu">Andrew Tolopko</a>
 */
public class CherryPickRequestDAOTest extends AbstractSpringPersistenceTest
{

  private static final Logger log = Logger.getLogger(CherryPickRequestDAOTest.class);
  
  protected CherryPickRequestDAO cherryPickRequestDao;
  protected LibrariesDAO librariesDao;


  public void testFindDuplicateCherryPicksForScreen()
  {
    CherryPickRequest cherryPickRequest1 = makeCherryPickRequest(1);
    Screen screen = cherryPickRequest1.getScreen();

    CherryPickRequest cherryPickRequest2 = screen.createCherryPickRequest();
    Iterator<ScreenerCherryPick> scpIter = cherryPickRequest1.getScreenerCherryPicks().iterator();
    ScreenerCherryPick duplicateScreenerCherryPick1 = scpIter.next();
    cherryPickRequest2.createLabCherryPick(
      cherryPickRequest2.createScreenerCherryPick(duplicateScreenerCherryPick1.getScreenedWell()),
      duplicateScreenerCherryPick1.getLabCherryPicks().iterator().next().getSourceWell());
    genericEntityDao.saveOrUpdateEntity(screen);
    Map<WellKey,Number> duplicateCherryPickWells = cherryPickRequestDao.findDuplicateCherryPicksForScreen(screen);
    assertEquals("duplicate cherry picks count", 1, duplicateCherryPickWells.size());
    assertEquals("duplicate cherry pick well keys",
                 new HashSet<WellKey>(Arrays.asList(duplicateScreenerCherryPick1.getScreenedWell().getWellKey())),
                 duplicateCherryPickWells.keySet());

    CherryPickRequest cherryPickRequest3 = screen.createCherryPickRequest();
    ScreenerCherryPick duplicateScreenerCherryPick2 = scpIter.next();
    cherryPickRequest3.createLabCherryPick(
      cherryPickRequest3.createScreenerCherryPick(duplicateScreenerCherryPick2.getScreenedWell()),
      duplicateScreenerCherryPick2.getLabCherryPicks().iterator().next().getSourceWell());
    genericEntityDao.saveOrUpdateEntity(screen);
    duplicateCherryPickWells = cherryPickRequestDao.findDuplicateCherryPicksForScreen(screen);
    assertEquals("duplicate cherry picks count", 2, duplicateCherryPickWells.size());
    assertEquals("duplicate cherry pick well keys",
                 new HashSet<WellKey>(Arrays.asList(duplicateScreenerCherryPick1.getScreenedWell().getWellKey(), duplicateScreenerCherryPick2.getScreenedWell().getWellKey())),
                 duplicateCherryPickWells.keySet());
  }


  public void testDeleteCherryPickRequest()
  {
    final int screenNumber = 1;
    final CherryPickRequest cherryPickRequest = makeCherryPickRequest(screenNumber);
    genericEntityDao.doInTransaction(new DAOTransaction()
    {
      public void runTransaction()
      {
        Screen screen = genericEntityDao.findEntityByProperty(Screen.class, "screenNumber", screenNumber);
        assertEquals("screen has 1 cherry pick request before deleting cherry pick request",
                     1,
                     screen.getCherryPickRequests().size());
      }
    });

    // note: we reload to test under condition of having an entity that has not
    // had any of its lazy relationships initialized (e.g. UI reloads the
    // cherryPickRequest anew when navigating to the CherryPickRequestViewer,
    // and so screen.cherryPickRequests collection is not initialized, but is
    // needed by deleteCherryPickRequest()).
    CherryPickRequest reloadedCherryPickRequest = (CherryPickRequest) genericEntityDao.reloadEntity(cherryPickRequest);
    cherryPickRequestDao.deleteCherryPickRequest(reloadedCherryPickRequest);

    genericEntityDao.doInTransaction(new DAOTransaction()
    {
      public void runTransaction()
      {
        Screen screen = genericEntityDao.findEntityByProperty(Screen.class, "screenNumber", screenNumber);
        assertEquals("screen has no cherry pick requests", 0, screen.getCherryPickRequests().size());
        assertNull("cherry pick request deleted",
                   genericEntityDao.findEntityById(CherryPickRequest.class, cherryPickRequest.getEntityId()));
      }
    });
  }

  // TODO: test case where deletion not allowed
  public void testDeleteScreenerCherryPick()
  {
    final int screenNumber = 1;
    makeCherryPickRequest(screenNumber);
    genericEntityDao.doInTransaction(new DAOTransaction()
    {
      public void runTransaction()
      {
        Screen screen = genericEntityDao.findEntityByProperty(Screen.class, "screenNumber", screenNumber);
        CherryPickRequest cherryPickRequest = screen.getCherryPickRequests().iterator().next();
        assertEquals("screener cherry picks exist before delete",
                     2,
                     cherryPickRequest.getScreenerCherryPicks().size());
        assertEquals("screener cherry picks exist for well1 before delete", 1, cherryPickRequestDao.findScreenerCherryPicksForWell(librariesDao.findWell(new WellKey(1, "A01"))).size());
        assertEquals("screener cherry picks exist for well2 before delete", 1, cherryPickRequestDao.findScreenerCherryPicksForWell(librariesDao.findWell(new WellKey(2, "P24"))).size());
        Set<ScreenerCherryPick> cherryPicksToDelete = new HashSet<ScreenerCherryPick>(cherryPickRequest.getScreenerCherryPicks());
        for (ScreenerCherryPick cherryPick : cherryPicksToDelete) {
          cherryPickRequestDao.deleteScreenerCherryPick(cherryPick);
        }
      }
    });

    genericEntityDao.doInTransaction(new DAOTransaction()
    {
      public void runTransaction()
      {
        Screen screen = genericEntityDao.findEntityByProperty(Screen.class, "screenNumber", screenNumber);
        CherryPickRequest cherryPickRequest = screen.getCherryPickRequests().iterator().next();
        assertEquals("screener cherry picks deleted from cherry pick request", 0, cherryPickRequest.getScreenerCherryPicks().size());
        assertEquals("screener cherry picks deleted from well1", 0, cherryPickRequestDao.findScreenerCherryPicksForWell(librariesDao.findWell(new WellKey(1, "A01"))).size());
        assertEquals("screener cherry picks deleted from well2", 0, cherryPickRequestDao.findScreenerCherryPicksForWell(librariesDao.findWell(new WellKey(2, "P24"))).size());
      }
    });
  }

  // TODO: test case where deletion not allowed
  public void testDeleteLabCherryPick()
  {
    final int screenNumber = 1;
    makeCherryPickRequest(screenNumber);
    genericEntityDao.doInTransaction(new DAOTransaction()
    {
      public void runTransaction()
      {
        Screen screen = genericEntityDao.findEntityByProperty(Screen.class, "screenNumber", screenNumber);
        CherryPickRequest cherryPickRequest = screen.getCherryPickRequests().iterator().next();
        assertEquals("lab cherry picks exist before delete",
                     2,
                     cherryPickRequest.getLabCherryPicks().size());
        assertEquals("lab cherry picks exist in well1 before delete", 1, cherryPickRequestDao.findLabCherryPicksForWell(librariesDao.findWell(new WellKey(3, "A01"))).size());
        assertEquals("lab cherry picks exist in well2 before delete", 1, cherryPickRequestDao.findLabCherryPicksForWell(librariesDao.findWell(new WellKey(4, "P24"))).size());
        Set<LabCherryPick> cherryPicksToDelete = new HashSet<LabCherryPick>(cherryPickRequest.getLabCherryPicks());
        for (LabCherryPick cherryPick : cherryPicksToDelete) {
          cherryPickRequestDao.deleteLabCherryPick(cherryPick);
        }
      }
    });

    genericEntityDao.doInTransaction(new DAOTransaction()
    {
      public void runTransaction()
      {
        Screen screen = genericEntityDao.findEntityByProperty(Screen.class, "screenNumber", screenNumber);
        CherryPickRequest cherryPickRequest = screen.getCherryPickRequests().iterator().next();
        assertEquals("lab cherry picks deleted from cherry pick request", 0, cherryPickRequest.getLabCherryPicks().size());
        assertEquals("lab cherry picks deleted from well1", 0, cherryPickRequestDao.findLabCherryPicksForWell(librariesDao.findWell(new WellKey(3, "A01"))).size());
        assertEquals("lab cherry picks deleted from well2", 0, cherryPickRequestDao.findLabCherryPicksForWell(librariesDao.findWell(new WellKey(4, "P24"))).size());
        assertEquals("number of unfulfilled cherry picks", 0, cherryPickRequest.getNumberUnfulfilledLabCherryPicks());  
      }
    });
  }

  
  // private methods
  
  private CherryPickRequest makeCherryPickRequest(final int screenNumber)
  {
    final CherryPickRequest[] result = new CherryPickRequest[1];
    genericEntityDao.doInTransaction(new DAOTransaction()
    {
      public void runTransaction()
      {
        Library poolLibrary = new Library("Pools library 1",
                                          "poollib1",
                                          ScreenType.RNAI,
                                          LibraryType.COMMERCIAL,
                                          1,
                                          2);
        Well poolWell1 = CherryPickRequestAllocatorTest.makeRNAiWell(poolLibrary, 1, new WellName("A01"));
        Well poolWell2 = CherryPickRequestAllocatorTest.makeRNAiWell(poolLibrary, 2, new WellName("P24"));
        genericEntityDao.saveOrUpdateEntity(poolLibrary);

        Library duplexLibrary = new Library("Duplexes library 1",
                                            "duplib1",
                                            ScreenType.RNAI,
                                            LibraryType.COMMERCIAL,
                                            3,
                                            4);
        Set<Well> pool1DuplexWells = CherryPickRequestAllocatorTest.makeRNAiDuplexWellsForPoolWell(duplexLibrary, poolWell1, 3, new WellName("A01"));
        Set<Well> pool2DuplexWells = CherryPickRequestAllocatorTest.makeRNAiDuplexWellsForPoolWell(duplexLibrary, poolWell2, 4, new WellName("P24"));
        genericEntityDao.saveOrUpdateEntity(duplexLibrary);

        Screen screen = MakeDummyEntities.makeDummyScreen(screenNumber, ScreenType.RNAI);
        CherryPickRequest cherryPickRequest = screen.createCherryPickRequest();
        cherryPickRequest.createLabCherryPick(cherryPickRequest.createScreenerCherryPick(poolWell1), pool1DuplexWells.iterator().next());
        cherryPickRequest.createLabCherryPick(cherryPickRequest.createScreenerCherryPick(poolWell2), pool2DuplexWells.iterator().next());
        genericEntityDao.saveOrUpdateEntity(screen.getLeadScreener());
        genericEntityDao.saveOrUpdateEntity(screen.getLabHead());
        genericEntityDao.saveOrUpdateEntity(screen);
        result[0] = cherryPickRequest;
      }
    });
    return result[0];
  }

  

}
