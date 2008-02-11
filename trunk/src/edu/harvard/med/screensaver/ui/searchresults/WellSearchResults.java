// $HeadURL$
// $Id$

// Copyright 2006 by the President and Fellows of Harvard College.

// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.ui.searchresults;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import edu.harvard.med.screensaver.io.DataExporter;
import edu.harvard.med.screensaver.model.libraries.Compound;
import edu.harvard.med.screensaver.model.libraries.Well;
import edu.harvard.med.screensaver.model.libraries.WellType;
import edu.harvard.med.screensaver.model.screens.ScreenType;
import edu.harvard.med.screensaver.ui.libraries.CompoundViewer;
import edu.harvard.med.screensaver.ui.libraries.GeneViewer;
import edu.harvard.med.screensaver.ui.libraries.LibraryViewer;
import edu.harvard.med.screensaver.ui.libraries.WellViewer;
import edu.harvard.med.screensaver.ui.table.TableColumn;

import org.apache.log4j.Logger;


/**
 * A {@link SearchResults} for {@link Well Wells}.
 *
 * @author <a mailto="john_sullivan@hms.harvard.edu">John Sullivan</a>
 * @author <a mailto="andrew_tolopko@hms.harvard.edu">Andrew Tolopko</a>
 */
public class WellSearchResults extends EntitySearchResults<Well>
{

  // private static final fields

  private static final Logger log = Logger.getLogger(WellSearchResults.class);


  // instance fields

  private LibraryViewer _libraryViewer;
  private WellViewer _wellViewer;
  private CompoundViewer _compoundViewer;
  private GeneViewer _geneViewer;

  private List<TableColumn<Well,?>> _columns;


  // constructors

  /**
   * @motivation for CGLIB2
   */
  protected WellSearchResults()
  {
  }

  /**
   * Construct a new <code>WellSearchResultsViewer</code> object.
   */
  public WellSearchResults(LibraryViewer libraryViewer,
                           WellViewer wellViewer,
                           CompoundViewer compoundViewer,
                           GeneViewer geneViewer,
                           List<DataExporter<Well>> dataExporters)
  {
    super(dataExporters);
    _libraryViewer = libraryViewer;
    _wellViewer = wellViewer;
    _compoundViewer = compoundViewer;
    _geneViewer = geneViewer;
  }


  // implementations of the SearchResults abstract methods

  @Override
  protected List<TableColumn<Well,?>> getColumns()
  {
    if (_columns == null) {
      _columns = new ArrayList<TableColumn<Well,?>>();
      _columns.add(new TextColumn<Well>("Library", "The library containing the well") {
        @Override
        public String getCellValue(Well well) { return well.getLibrary().getLibraryName(); }

        @Override
        public boolean isCommandLink() { return true; }

        @Override
        public Object cellAction(Well well) { return _libraryViewer.viewLibrary(well.getLibrary()); }
      });
      _columns.add(new IntegerColumn<Well>("Plate", "The number of the plate the well is located on") {
        @Override
        public Integer getCellValue(Well well) { return well.getPlateNumber(); }

        @Override
        protected Comparator<Well> getAscendingComparator()
        {
          return new Comparator<Well>()
          {
            @SuppressWarnings("unchecked")
            public int compare(Well w1, Well w2)
            {
              return w1.getWellKey().compareTo(w2.getWellKey());
            }
          };
        }
      });
      _columns.add(new TextColumn<Well>("Well", "The plate coordinates of the well") {
        @Override
        public String getCellValue(Well well) { return well.getWellName(); }

        @Override
        public boolean isCommandLink() { return true; }

        @Override
        public Object cellAction(Well well) { return viewCurrentEntity(); }
      });
      _columns.add(new EnumColumn<Well,WellType>("Well Type", "The type of well, e.g., 'Experimental', 'Control', 'Empty', etc.", WellType.values()) {
        @Override
        public WellType getCellValue(Well well) { return well.getWellType(); }
      });
      // TODO: consider splitting this column into two, one for compounds, one for genes
      _columns.add(new TextColumn<Well>("Contents", "The gene name for the silencing reagent, or SMILES for the compound, in the well") {
        @Override
        public String getCellValue(Well well) { return getContentsValue(well).toString(); }

        @Override
        protected Comparator<Well> getAscendingComparator()
        {
          return new Comparator<Well>() {
            @SuppressWarnings("unchecked")
            public int compare(Well w1, Well w2) {
              Object o1 = getContentsValue(w1);
              String s1 = (o1 instanceof String) ? (String) o1 : ((List<String>) o1).get(0);
              Object o2 = getContentsValue(w2);
              String s2 = (o2 instanceof String) ? (String) o2 : ((List<String>) o2).get(0);
              return s1.compareTo(s2);
            }
          };
        }

        @Override
        public boolean isCommandLink() { return getContentsCount(getRowData()) == 1; }

        @Override
        public boolean isCommandLinkList() { return getContentsCount(getRowData()) > 1; }

        @Override
        public Object cellAction(Well well)
        {
          if (getGeneCount(well) == 1) {
            return _geneViewer.viewGene(well.getGene());
          }
          if (getCompoundCount(well) > 0) {
            // commandValue is really a smiles, not a compoundId
            String smiles = (String) getRequestParameter("commandValue");
            Compound compound = null;
            for (Compound compound2 : well.getCompounds()) {
              if (compound2.getSmiles().equals(smiles)) {
                compound = compound2;
                break;
              }
            }
            return _compoundViewer.viewCompound(compound);
          }
          return REDISPLAY_PAGE_ACTION_RESULT;
        }
      });
    }
    return _columns;
  }

  @Override
  protected void setEntityToView(Well well)
  {
    _wellViewer.viewWell(well);
  }


  // private instance methods


  private Object getContentsValue(Well well)
  {
    int geneCount = getGeneCount(well);
    if (geneCount == 1) {
      return well.getGene().getGeneName();
    }
    if (geneCount > 1) {
      return "multiple genes";
    }
    int compoundCount = getCompoundCount(well);
    if (compoundCount == 1) {
      return well.getCompounds().iterator().next().getSmiles();
    }
    if (compoundCount > 1) {
      List<String> smiles = new ArrayList<String>();
      for (Compound compound : well.getOrderedCompounds()) {
        smiles.add(compound.getSmiles());
      }
      return smiles;
    }

    // at this point we know the well has no compounds or genes in it. but is it empty?

    if (well.getLibrary().getScreenType().equals(ScreenType.SMALL_MOLECULE) &&
      well.getWellType().equals(WellType.EXPERIMENTAL)) {
      return "compound with unknown structure";
    }
    return "empty well";
  }

  private int getContentsCount(Well well)
  {
    return getGeneCount(well) + getCompoundCount(well);
  }

  private int getCompoundCount(Well well)
  {
    return well.getCompounds().size();
  }

  private int getGeneCount(Well well)
  {
    return well.getGenes().size();
  }
}