// $HeadURL: svn+ssh://js163@orchestra.med.harvard.edu/svn/iccb/screensaver/trunk/.settings/org.eclipse.jdt.ui.prefs $
// $Id: org.eclipse.jdt.ui.prefs 169 2006-06-14 21:57:49Z js163 $
//
// Copyright 2006 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.io.libraries.compound;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import org.apache.log4j.Logger;

import edu.harvard.med.screensaver.AbstractSpringTest;
import edu.harvard.med.screensaver.db.GenericEntityDAO;
import edu.harvard.med.screensaver.db.SchemaUtil;
import edu.harvard.med.screensaver.io.ParseError;
import edu.harvard.med.screensaver.model.libraries.Library;
import edu.harvard.med.screensaver.model.libraries.LibraryType;
import edu.harvard.med.screensaver.model.screens.ScreenType;


public class SDFileCompoundLibraryContentsParserTest extends AbstractSpringTest
{

  // static fields

  private static final Logger log = Logger.getLogger(SDFileCompoundLibraryContentsParserTest.class);
  private static final File TEST_INPUT_FILE_DIR =
    new File("test/edu/harvard/med/screensaver/io/libraries/compound");


  // instance fields

  protected SDFileCompoundLibraryContentsParser compoundLibraryContentsParser;
  protected GenericEntityDAO genericEntityDao;
  protected SchemaUtil schemaUtil;


  // constructor and test methods

  protected void onSetUp() throws Exception
  {
    schemaUtil.truncateTablesOrCreateSchema();
    //schemaUtil.initializeDatabase();
  }

  // TODO: convert this into an actual test
  // (right now it is kinda testing that the SDFCLCParser doesnt blow up on a
  // reasonable SDFile.)
  public void testFoo()
  {
    Library library = new Library("COMP", "COMP", ScreenType.OTHER, LibraryType.OTHER, 1534, 1534);
    String filename = "biomol-timtec-111.sdf";
    File file = new File(TEST_INPUT_FILE_DIR, filename);
    InputStream stream = null;
    try {
      stream = new FileInputStream(file);
    }
    catch (FileNotFoundException e) {
      fail("file not found: " + filename);
    }
    library = compoundLibraryContentsParser.parseLibraryContents(library, file, stream);

    List<? extends ParseError> errors = compoundLibraryContentsParser.getErrors();
    if (errors.size() > 0) {
      log.debug(errors);
    }
    assertEquals("workbook has no errors", 0, errors.size());
    //ParseError error;
    genericEntityDao.saveOrUpdateEntity(library);
  }
}