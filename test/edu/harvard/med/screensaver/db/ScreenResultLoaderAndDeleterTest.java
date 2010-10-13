// $HeadURL:
// svn+ssh://ant4@orchestra.med.harvard.edu/svn/iccb/screensaver/trunk/src/test/edu/harvard/med/screensaver/TestHibernate.java
// $
// $Id$
//
// Copyright © 2006, 2010 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.db;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.joda.time.LocalDate;

import edu.harvard.med.screensaver.AbstractSpringPersistenceTest;
import edu.harvard.med.screensaver.io.ParseError;
import edu.harvard.med.screensaver.io.ParseErrorsException;
import edu.harvard.med.screensaver.io.workbook2.Workbook;
import edu.harvard.med.screensaver.model.AdministrativeActivityType;
import edu.harvard.med.screensaver.model.MakeDummyEntities;
import edu.harvard.med.screensaver.model.TestDataFactory;
import edu.harvard.med.screensaver.model.libraries.CopyUsageType;
import edu.harvard.med.screensaver.model.libraries.Library;
import edu.harvard.med.screensaver.model.libraries.LibraryType;
import edu.harvard.med.screensaver.model.libraries.LibraryWellType;
import edu.harvard.med.screensaver.model.libraries.Plate;
import edu.harvard.med.screensaver.model.libraries.PlateSize;
import edu.harvard.med.screensaver.model.libraries.WellKey;
import edu.harvard.med.screensaver.model.screenresults.AssayWell;
import edu.harvard.med.screensaver.model.screenresults.DataColumn;
import edu.harvard.med.screensaver.model.screenresults.ResultValue;
import edu.harvard.med.screensaver.model.screenresults.ScreenResult;
import edu.harvard.med.screensaver.model.screens.LibraryScreening;
import edu.harvard.med.screensaver.model.screens.Screen;
import edu.harvard.med.screensaver.model.screens.ScreenType;
import edu.harvard.med.screensaver.model.users.AdministratorUser;
import edu.harvard.med.screensaver.service.libraries.LibraryCreator;
import edu.harvard.med.screensaver.service.screenresult.ScreenResultDeleter;
import edu.harvard.med.screensaver.service.screenresult.ScreenResultLoader;


public class ScreenResultLoaderAndDeleterTest extends AbstractSpringPersistenceTest
{
  private static final Logger log = Logger.getLogger(ScreenResultLoaderAndDeleterTest.class);

  protected ScreenResultLoader screenResultLoader;
  protected ScreenResultDeleter screenResultDeleter;
  protected LibraryCreator libraryCreator;

  public static final File TEST_INPUT_FILE_DIR = new File("test/edu/harvard/med/screensaver/io/screenresults");
  public static final String SCREEN_RESULT_115_TEST_WORKBOOK_FILE = "ScreenResultTest115.xls";
  public static final int TEST_SCREEN_NUMBER = 115;
  
  @Override
  protected void onSetUp() throws Exception
  {
    super.onSetUp();
    
    final Screen screen = MakeDummyEntities.makeDummyScreen(TEST_SCREEN_NUMBER);
    genericEntityDao.doInTransaction(new DAOTransaction() {

      public void runTransaction()
      {
        genericEntityDao.persistEntity(screen);
        Library library = new Library((AdministratorUser) screen.getCreatedBy(),
                                      "library for screen 115 test",
                                      "lwr",
                                      ScreenType.SMALL_MOLECULE,
                                      LibraryType.COMMERCIAL,
                                      1,
                                      3,
                                      PlateSize.WELLS_384);
        for (int iPlate = 1; iPlate <= 3; ++iPlate) {
          int plateNumber = iPlate;
          for (int col = 'A'; col < 'Q'; col++) {
            for (int row = 1; row <= 24; row++) {
              LibraryWellType libraryWellType = LibraryWellType.EXPERIMENTAL;
              if (plateNumber == 1 && col == 'A') {
                if (row == 1)
                  libraryWellType = LibraryWellType.EMPTY;
                else if (row == 4)
                  libraryWellType = LibraryWellType.EMPTY;
                else if (row == 5)
                  libraryWellType = LibraryWellType.LIBRARY_CONTROL;
                else if (row == 6)
                  libraryWellType = LibraryWellType.BUFFER;
                else if (row == 7)
                  libraryWellType = LibraryWellType.EMPTY;
                else if (row == 8)
                  libraryWellType = LibraryWellType.EMPTY;
              }
              library.createWell(new WellKey(plateNumber, "" + (char) col + row), libraryWellType);
            }
          }
        }
        libraryCreator.createLibrary(library);
        
        library.createCopy((AdministratorUser) library.getCreatedBy(),
                           CopyUsageType.CHERRY_PICK_STOCK_PLATES, "C");
        genericEntityDao.persistEntity(library);
      }
    });
  }
  
  public void testScreenResultLoaderNoIncrementalFlush() throws Exception
  {
    doTest(false);
  }
  
  public void testScreenResultLoaderIncrementalFlush() throws Exception
  {
    doTest(true);
  }
  
  private void doLoad(boolean incrementalFlush) throws FileNotFoundException
  {
    try {
      File workbookFile = new File(TEST_INPUT_FILE_DIR,
                                   SCREEN_RESULT_115_TEST_WORKBOOK_FILE);
      final AdministratorUser admin = new AdministratorUser("Admin", "User", "", "", "", "", "", "");
      genericEntityDao.saveOrUpdateEntity(admin);

      Screen screen = genericEntityDao.findEntityByProperty(Screen.class, "screenNumber", TEST_SCREEN_NUMBER);
      screenResultLoader.parseAndLoad(screen,
                                      new Workbook(workbookFile),
                                      admin,
                                      "test comments",
                                      null,
                                      incrementalFlush);
    }
    catch (ParseErrorsException e) {
      log.error("Parse Errors");
      for (ParseError pe : e.getErrors()) {
        log.error("" + pe);
      }
      fail("due to parse errors");
    }
  }

  private void doTest(boolean incrementalFlush) throws Exception
  {
    Date now = new Date();
    
    // add a LibraryScreening and AssayPlate
    genericEntityDao.doInTransaction(new DAOTransaction() {
      @Override
      public void runTransaction()
      {
        Screen screen = genericEntityDao.findEntityByProperty(Screen.class, "screenNumber", TEST_SCREEN_NUMBER);
        LibraryScreening libraryScreening = screen.createLibraryScreening((AdministratorUser) screen.getCreatedBy(), screen.getLabHead(), new LocalDate());
        libraryScreening.setNumberOfReplicates(1);
        libraryScreening.addAssayPlatesScreened(genericEntityDao.findEntityByProperty(Plate.class, "plateNumber", 1));
      }
    });

    doLoad(incrementalFlush);
    Screen screen = findScreen();
    ScreenResult screenResult = screen.getScreenResult();
    
    assertTrue("Screen parse time is incorrect: " +
               screenResult.getLastDataLoadingActivity() + ", should be after: " + now, 
               screenResult.getLastDataLoadingActivity().getDateCreated().getMillis() > now.getTime());
    
    assertEquals("Loaded data for 3 plates [1..3].  test comments", screenResult.getLastDataLoadingActivity().getComments());

    List<DataColumn> loadedDataColumns = genericEntityDao.findEntitiesByProperty(DataColumn.class,
                                                                               "screenResult",
                                                                               screenResult,
                                                                               true,
                                                                               "resultValues");
    assertNotNull(loadedDataColumns);
    assertEquals("DataColumns count", 7, loadedDataColumns.size());
    for (DataColumn actualCol : loadedDataColumns) {
      assertEquals(960, actualCol.getResultValues().size());
    }
    List<AssayWell> assayWells = genericEntityDao.findEntitiesByProperty(AssayWell.class,
                                                                         "screenResult",
                                                                         screenResult);
    assertNotNull(assayWells);
    assertEquals("Number of AssayWells: ", 960, assayWells.size());
    genericEntityDao.doInTransaction(new DAOTransaction()
    {
      public void runTransaction() {
        Screen screen = findScreen();
        ScreenResult screenResult = screen.getScreenResult();
        assertNotNull(screenResult);
        DataColumn col0 = screenResult.getDataColumnsList().get(0);
        col0.getWellKeyToResultValueMap().keySet().iterator();
        ResultValue rv = col0.getWellKeyToResultValueMap().get(new WellKey(1,"A01"));
        assertEquals(1071894.0, rv.getNumericValue().doubleValue(), 0.01);
        // this tests how Hibernate will make use of WellKey, initializing with a concatenated key string
        rv = col0.getWellKeyToResultValueMap().get(new WellKey("00001:A01"));
        assertEquals(1071894.0, rv.getNumericValue().doubleValue(), 0.01);
      }
    });
    
    assertEquals(3, screen.getLibraryPlatesDataLoadedCount());
    assertEquals(3, screen.getLibraryPlatesDataAnalyzedCount());
    assertTrue(screen.getAssayPlatesDataLoaded().size() > 0);
    assertTrue(screen.getAssayPlatesDataLoaded().size() > screen.getAssayPlatesScreened().size()); // ensure that we're testing with sets of assay plates that have and do not have a library screening
    screenResultDeleter.deleteScreenResult(screenResult, 
                                           genericEntityDao.findEntityByProperty(AdministratorUser.class, "firstName", "Admin"));
    screen = findScreen();
    assertNull(screen.getScreenResult());
    assertEquals(AdministrativeActivityType.SCREEN_RESULT_DATA_DELETION, 
                 screen.getUpdateActivities().last().getType());
    assertEquals(0, screen.getLibraryPlatesDataLoadedCount());
    assertEquals(0, screen.getLibraryPlatesDataAnalyzedCount());
    assertEquals("non-screened assay plates should have been deleted", 0, screen.getAssayPlatesDataLoaded().size());
    assertTrue("screened assay plates should not have been deleted", screen.getAssayPlatesScreened().size() > 0);
  }
  
  public void testAssayPlateLoadingStatus() throws Exception
  {
     // 1. no extant assay plates
    doLoad(false);
    Screen screen = findScreen();
    assertEquals(6, screen.getAssayPlates().size());
    assertEquals("library plates screened", 0, screen.getLibraryPlatesScreenedCount());
    assertEquals("library plates data loaded", 3, screen.getLibraryPlatesDataLoadedCount());
    assertEquals(6, screen.getAssayPlatesDataLoaded().size());

    // 2. insufficient assay plate replicates: a new set of attempt assay plates replicates created
    onSetUp();
    screen = findScreen();
    LibraryScreening libraryScreening = new TestDataFactory().newInstance(LibraryScreening.class, screen);
    libraryScreening.setNumberOfReplicates(1);
    libraryScreening.addAssayPlatesScreened(genericEntityDao.findEntityByProperty(Plate.class, "plateNumber", 1));
    libraryScreening.addAssayPlatesScreened(genericEntityDao.findEntityByProperty(Plate.class, "plateNumber", 2));
    libraryScreening.addAssayPlatesScreened(genericEntityDao.findEntityByProperty(Plate.class, "plateNumber", 3));
    genericEntityDao.saveOrUpdateEntity(screen);
    screen.update();
    screen = findScreen();
    assertEquals(3, screen.getAssayPlates().size());
    assertEquals("library plates screened, pre-load", 3, screen.getLibraryPlatesScreenedCount());
    assertEquals("library plates data loaded, pre-load", 0, screen.getLibraryPlatesDataLoadedCount());
    doLoad(false);
    screen = findScreen();
    assertEquals(3+6, screen.getAssayPlates().size());
    assertEquals("library plates screened", 3, screen.getLibraryPlatesScreenedCount());
    assertEquals("library plates data loaded", 3, screen.getLibraryPlatesDataLoadedCount());
    assertEquals(6, screen.getAssayPlatesDataLoaded().size());
    
     // 3. sufficient assay plate replicates, with multiple attempts: only last attempt assay plates are updated
    onSetUp();
    screen = findScreen();
    libraryScreening = new TestDataFactory().newInstance(LibraryScreening.class, screen);
    libraryScreening.setNumberOfReplicates(2);
    libraryScreening.addAssayPlatesScreened(genericEntityDao.findEntityByProperty(Plate.class, "plateNumber", 1));
    libraryScreening.addAssayPlatesScreened(genericEntityDao.findEntityByProperty(Plate.class, "plateNumber", 2));
    libraryScreening.addAssayPlatesScreened(genericEntityDao.findEntityByProperty(Plate.class, "plateNumber", 3));
    libraryScreening = new TestDataFactory().newInstance(LibraryScreening.class, screen);
    libraryScreening.setNumberOfReplicates(2);
    libraryScreening.addAssayPlatesScreened(genericEntityDao.findEntityByProperty(Plate.class, "plateNumber", 1));
    libraryScreening.addAssayPlatesScreened(genericEntityDao.findEntityByProperty(Plate.class, "plateNumber", 2));
    libraryScreening.addAssayPlatesScreened(genericEntityDao.findEntityByProperty(Plate.class, "plateNumber", 3));
    genericEntityDao.saveOrUpdateEntity(screen);
    screen.update();
    screen = findScreen();
    assertEquals(12, screen.getAssayPlates().size());
    assertEquals("library plates screened, pre-load", 3, screen.getLibraryPlatesScreenedCount());
    assertEquals("library plates data loaded, pre-load", 0, screen.getLibraryPlatesDataLoadedCount());
    doLoad(false);
    screen = findScreen();
    assertEquals(12, screen.getAssayPlates().size());
    assertEquals("library plates screened", 3, screen.getLibraryPlatesScreenedCount());
    assertEquals("library plates data loaded", 3, screen.getLibraryPlatesDataLoadedCount());
    assertEquals(6, screen.getAssayPlatesDataLoaded().size());

    // 4. extra assay plate replicates: only the first N assay plates replicates
    // should be updated, where N is the screen result's replicate count
    onSetUp();
    screen = findScreen();
    libraryScreening = new TestDataFactory().newInstance(LibraryScreening.class, screen);
    libraryScreening.setNumberOfReplicates(3);
    libraryScreening.addAssayPlatesScreened(genericEntityDao.findEntityByProperty(Plate.class, "plateNumber", 1));
    libraryScreening.addAssayPlatesScreened(genericEntityDao.findEntityByProperty(Plate.class, "plateNumber", 2));
    libraryScreening.addAssayPlatesScreened(genericEntityDao.findEntityByProperty(Plate.class, "plateNumber", 3));
    genericEntityDao.saveOrUpdateEntity(screen);
    screen.update();
    screen = findScreen();
    assertEquals(9, screen.getAssayPlates().size());
    assertEquals("library plates screened, pre-load", 3, screen.getLibraryPlatesScreenedCount());
    assertEquals("library plates data loaded, pre-load", 0, screen.getLibraryPlatesDataLoadedCount());
    doLoad(false);
    screen = findScreen();
    assertEquals(9, screen.getAssayPlates().size());
    assertEquals("library plates screened", 3, screen.getLibraryPlatesScreenedCount());
    assertEquals("library plates data loaded", 3, screen.getLibraryPlatesDataLoadedCount());
    assertEquals(6, screen.getAssayPlatesDataLoaded().size());
  }

  private Screen findScreen()
  {
    return (Screen) genericEntityDao.runQuery(new Query()
    {
      public List execute(Session s) {
        Screen screen = genericEntityDao.findEntityByProperty(Screen.class, 
                                                              "screenNumber", 115, 
                                                              true, 
                                                              Screen.assayPlates.getPath());
        genericEntityDao.needReadOnly(screen, Screen.updateActivities.getPath());
        genericEntityDao.needReadOnly(screen, Screen.labActivities.getPath());
        return Lists.newArrayList(screen);
      }
    }).get(0);
  }
}