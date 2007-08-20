// $HeadURL$
// $Id$
//
// Copyright 2006 by the President and Fellows of Harvard College.
// 
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.ui.libraries;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;

import edu.harvard.med.screensaver.io.libraries.rnai.RNAiLibraryContentsParser;
import edu.harvard.med.screensaver.model.libraries.Library;
import edu.harvard.med.screensaver.model.libraries.SilencingReagentType;
import edu.harvard.med.screensaver.ui.AbstractBackingBean;
import edu.harvard.med.screensaver.ui.control.LibrariesController;
import edu.harvard.med.screensaver.ui.util.JSFUtils;

import org.apache.log4j.Logger;
import org.apache.myfaces.custom.fileupload.UploadedFile;

/**
 * The JSF backing bean for the rnaiLibraryContentsImporter subview.
 * 
 * @author s
 */
public class RNAiLibraryContentsImporter extends AbstractBackingBean
{
  
  private static Logger log = Logger.getLogger(RNAiLibraryContentsImporter.class);

  // instance data

  private LibrariesController _librariesController;
  private RNAiLibraryContentsParser _rnaiLibraryContentsParser;
  private UploadedFile _uploadedFile;
  private Library _library;
  private SilencingReagentType _silencingReagentType =
    RNAiLibraryContentsParser.DEFAULT_SILENCING_REAGENT_TYPE;


  // backing bean property getter and setter methods

  public LibrariesController getLibrariesController()
  {
    return _librariesController;
  }
  
  public void setLibrariesController(LibrariesController librariesController)
  {
    _librariesController = librariesController;
  }

  public RNAiLibraryContentsParser getRnaiLibraryContentsParser()
  {
    return _rnaiLibraryContentsParser;
  }

  public void setRnaiLibraryContentsParser(RNAiLibraryContentsParser rnaiLibraryContentsParser)
  {
    _rnaiLibraryContentsParser = rnaiLibraryContentsParser;
  }

  public void setUploadedFile(UploadedFile uploadedFile)
  {
    _uploadedFile = uploadedFile;
  }

  public UploadedFile getUploadedFile()
  {
    return _uploadedFile;
  }

  public Library getLibrary()
  {
    return _library;
  }

  public void setLibrary(Library library)
  {
    _library = library;
  }

  /**
   * Get the silencingReagentType.
   * @return the silencingReagentType
   */
  public SilencingReagentType getSilencingReagentType()
  {
    return _silencingReagentType;
  }

  /**
   * Set the silencingReagentType.
   * @param silencingReagentType the silencingReagentType
   */
  public void setSilencingReagentType(SilencingReagentType silencingReagentType)
  {
    _silencingReagentType = silencingReagentType;
  }

  public List<SelectItem> getSilencingReagentTypeSelections()
  {
    List<SilencingReagentType> selections = new ArrayList<SilencingReagentType>();
    for (SilencingReagentType silencingReagentType : SilencingReagentType.values()) {
      selections.add(silencingReagentType);
    }
    return JSFUtils.createUISelectItems(selections);
  }

  public boolean getHasErrors()
  {
    return _rnaiLibraryContentsParser.getHasErrors();
  }
  
  public DataModel getImportErrors()
  {
    return new ListDataModel(_rnaiLibraryContentsParser.getErrors());
  }
  
  public String viewLibrary()
  {
    return _librariesController.viewLibrary(_library, null);
  }
  
  /**
   * Parse the RNAi library contents from the file, and go to the appropriate next page
   * depending on the result.
   * @return the control code for the appropriate next page
   */
  public String importRNAiLibraryContents()
  {
    return _librariesController.importRNAiLibraryContents(
      _library,
      _uploadedFile,
      _silencingReagentType);
  }
}
