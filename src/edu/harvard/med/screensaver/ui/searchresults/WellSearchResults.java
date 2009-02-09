// $HeadURL:
// svn+ssh://ant4@orchestra.med.harvard.edu/svn/iccb/screensaver/trunk/src/edu/harvard/med/screensaver/ui/searchresults/WellSearchResults.java
// $
// $Id$

// Copyright 2006 by the President and Fellows of Harvard College.

// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.ui.searchresults;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.harvard.med.screensaver.db.GenericEntityDAO;
import edu.harvard.med.screensaver.db.Query;
import edu.harvard.med.screensaver.db.datafetcher.AllEntitiesOfTypeDataFetcher;
import edu.harvard.med.screensaver.db.datafetcher.DataFetcher;
import edu.harvard.med.screensaver.db.datafetcher.EntityDataFetcher;
import edu.harvard.med.screensaver.db.datafetcher.EntitySetDataFetcher;
import edu.harvard.med.screensaver.db.datafetcher.NoOpDataFetcher;
import edu.harvard.med.screensaver.db.datafetcher.ParentedEntityDataFetcher;
import edu.harvard.med.screensaver.db.hibernate.Conjunction;
import edu.harvard.med.screensaver.db.hibernate.Disjunction;
import edu.harvard.med.screensaver.db.hibernate.HqlBuilder;
import edu.harvard.med.screensaver.db.hibernate.JoinType;
import edu.harvard.med.screensaver.io.DataExporter;
import edu.harvard.med.screensaver.model.PropertyPath;
import edu.harvard.med.screensaver.model.RelationshipPath;
import edu.harvard.med.screensaver.model.libraries.Compound;
import edu.harvard.med.screensaver.model.libraries.Library;
import edu.harvard.med.screensaver.model.libraries.Reagent;
import edu.harvard.med.screensaver.model.libraries.Well;
import edu.harvard.med.screensaver.model.libraries.WellKey;
import edu.harvard.med.screensaver.model.libraries.WellType;
import edu.harvard.med.screensaver.model.screenresults.AnnotationType;
import edu.harvard.med.screensaver.model.screenresults.AnnotationValue;
import edu.harvard.med.screensaver.model.screenresults.AssayWellType;
import edu.harvard.med.screensaver.model.screenresults.ResultValue;
import edu.harvard.med.screensaver.model.screenresults.ResultValueType;
import edu.harvard.med.screensaver.model.screenresults.ScreenResult;
import edu.harvard.med.screensaver.model.screens.ScreenType;
import edu.harvard.med.screensaver.ui.UIControllerMethod;
import edu.harvard.med.screensaver.ui.libraries.CompoundViewer;
import edu.harvard.med.screensaver.ui.libraries.GeneViewer;
import edu.harvard.med.screensaver.ui.libraries.LibraryViewer;
import edu.harvard.med.screensaver.ui.libraries.WellViewer;
import edu.harvard.med.screensaver.ui.screenresults.MetaDataType;
import edu.harvard.med.screensaver.ui.table.Criterion;
import edu.harvard.med.screensaver.ui.table.DataTable;
import edu.harvard.med.screensaver.ui.table.Criterion.Operator;
import edu.harvard.med.screensaver.ui.table.column.TableColumn;
import edu.harvard.med.screensaver.ui.table.column.TableColumnManager;
import edu.harvard.med.screensaver.ui.table.column.entity.BooleanEntityColumn;
import edu.harvard.med.screensaver.ui.table.column.entity.EntityColumn;
import edu.harvard.med.screensaver.ui.table.column.entity.EnumEntityColumn;
import edu.harvard.med.screensaver.ui.table.column.entity.IntegerEntityColumn;
import edu.harvard.med.screensaver.ui.table.column.entity.ListEntityColumn;
import edu.harvard.med.screensaver.ui.table.column.entity.RealEntityColumn;
import edu.harvard.med.screensaver.ui.table.column.entity.TextEntityColumn;
import edu.harvard.med.screensaver.ui.table.model.DataTableModel;
import edu.harvard.med.screensaver.ui.table.model.InMemoryEntityDataModel;
import edu.harvard.med.screensaver.util.Triple;

import org.apache.log4j.Logger;
import org.hibernate.Session;


/**
 * A {@link SearchResults} for {@link Well Wells}.
 *
 * @author <a mailto="john_sullivan@hms.harvard.edu">John Sullivan</a>
 * @author <a mailto="andrew_tolopko@hms.harvard.edu">Andrew Tolopko</a>
 */
public class WellSearchResults extends EntitySearchResults<Well,String>
{

  // private static final fields

  private static final Logger log = Logger.getLogger(WellSearchResults.class);


  private static final String WELL_COLUMNS_GROUP = TableColumn.UNGROUPED;
  private static final String GENE_COLUMNS_GROUP = "Silencing Reagent";
  private static final String COMPOUND_COLUMNS_GROUP = "Compound";
  private static final String OUR_DATA_HEADERS_COLUMNS_GROUP = "Data Headers";
  private static final String OTHER_DATA_HEADERS_COLUMNS_GROUP = "Data Headers (Other Screen Results)";
  private static final String OTHER_ANNOTATION_TYPES_COLUMN_GROUP = "Annotations";
  private static final String OUR_ANNOTATION_TYPES_COLUMN_GROUP = "Annotations (Other Studies)";

  private enum WellSearchResultMode {
    SCREEN_RESULT_WELLS,
    LIBRARY_WELLS,
    ALL_WELLS,
    SET_OF_WELLS };


  // instance fields

  private GenericEntityDAO _dao;
  private LibraryViewer _libraryViewer;
  private WellViewer _wellViewer;
  private CompoundViewer _compoundViewer;
  private GeneViewer _geneViewer;

  private WellSearchResultMode _mode;
  private Library _library;
  private ScreenResult _screenResult;
  private Set<Integer> _plateNumbers;


  // constructors

  /**
   * @motivation for CGLIB2
   */
  protected WellSearchResults()
  {}

  /**
   * Construct a new <code>WellSearchResultsViewer</code> object.
   */
  public WellSearchResults(GenericEntityDAO dao,
                           LibraryViewer libraryViewer,
                           WellViewer wellViewer,
                           CompoundViewer compoundViewer,
                           GeneViewer geneViewer,
                           List<DataExporter<?>> dataExporters)
  {
    super(dataExporters);
    _dao = dao;
    _libraryViewer = libraryViewer;
    _wellViewer = wellViewer;
    _compoundViewer = compoundViewer;
    _geneViewer = geneViewer;
  }

  /**
   * Called from the top level menu page.
   * 
   * @motivation Initializes the DataTable with a NoOpDataFetcher; causing the bean 
   * to return an empty search result on the first viewing.
   * 
   * @see WellSearchResults#searchCommandListener(javax.faces.event.ActionEvent)
   */
  public void searchAllWells()
  {
    _mode = WellSearchResultMode.ALL_WELLS;
    _library = null;
    _screenResult = null;
    _plateNumbers = null;
    // initially, show an empty search result
    initialize(new NoOpDataFetcher<Well,String,PropertyPath<Well>>());
    updateColumnVisibilityForScreenType(true, true);

    // start with search panel open
    setTableFilterMode(true);
  }
  
  public void searchWellsForLibrary(Library library)
  {
    _mode = WellSearchResultMode.LIBRARY_WELLS;
    _library = library;
    _screenResult = null;
    _plateNumbers = null;
    initialize(new ParentedEntityDataFetcher<Well,String>(Well.class,
      new RelationshipPath<Well>(Well.class, "library"),
      library,
      _dao));
    updateColumnVisibilityForScreenType(library.getScreenType() == ScreenType.SMALL_MOLECULE,
                                        library.getScreenType() == ScreenType.RNAI);

    // start with search panel closed
    setTableFilterMode(false);
  }

  public void searchWellsForScreenResult(ScreenResult screenResult)
  {
    _mode = WellSearchResultMode.SCREEN_RESULT_WELLS;
    _library = null;
    _screenResult = screenResult;
    _plateNumbers = null;
    if (screenResult == null) {
      initialize(new NoOpDataFetcher<Well,String,PropertyPath<Well>>());
    }
    else {
      initialize(new ParentedEntityDataFetcher<Well,String>(Well.class,
        new RelationshipPath<Well>(Well.class, "screenResults"),
        screenResult,
        _dao));
      updateColumnVisibilityForScreenType(screenResult.getScreen().getScreenType() == ScreenType.SMALL_MOLECULE,
                                          screenResult.getScreen().getScreenType() == ScreenType.RNAI);
      // show columns for this screenResult's data headers
      for (ResultValueType rvt : screenResult.getResultValueTypes()) {
        TableColumn<Well,?> column = getColumnManager().getColumn(makeColumnName(rvt, screenResult.getScreen().getScreenNumber()));
        if (column != null) {
          column.setVisible(true);
        }
      }

      // start with search panel closed
      setTableFilterMode(false);
    }
  }

  public void searchWells(Set<WellKey> wellKeys)
  {
    _mode = WellSearchResultMode.SET_OF_WELLS;
    _library = null;
    _screenResult = null;
    _plateNumbers = new HashSet<Integer>();
    Set<String> wellKeyStrings = new HashSet<String>(wellKeys.size());
    for (WellKey wellKey : wellKeys) {
      wellKeyStrings.add(wellKey.toString());
      _plateNumbers.add(wellKey.getPlateNumber());
    }
    initialize(new EntitySetDataFetcher<Well,String>(Well.class,
      wellKeyStrings,
      _dao));
    updateColumnVisibilityForScreenType(true, true);

    // start with search panel closed
    setTableFilterMode(false);
  }

  // SearchResults abstract method implementations


  @Override
  protected DataTableModel<Well> doBuildDataModel(EntityDataFetcher<Well,String> dataFetcher,
                                                  List<? extends TableColumn<Well,?>> columns)
  {
    if (_library != null &&
      ((_library.getEndPlate() - _library.getStartPlate()) + 1) * (Well.PLATE_ROWS * Well.PLATE_COLUMNS) <=
      EntitySearchResults.ALL_IN_MEMORY_THRESHOLD) {
      log.debug("using InMemoryDataModel due to domain size");
      return new InMemoryEntityDataModel<Well>(dataFetcher);
    }
//    if (_screenResult != null &&
//      _dao.relationshipSize(_screenResult, "plateNumbers") * (Well.PLATE_ROWS * Well.PLATE_COLUMNS) <= EntitySearchResults.ALL_IN_MEMORY_THRESHOLD) {
//      log.debug("using InMemoryDataModel due to domain size");
//      return new InMemoryEntityDataModel<Well>(dataFetcher);
//    }
    return super.doBuildDataModel(dataFetcher, columns);
  }

  @Override
  protected List<? extends TableColumn<Well,?>> buildColumns()
  {
    List<EntityColumn<Well,?>> columns = new ArrayList<EntityColumn<Well,?>>();
    buildWellPropertyColumns(columns);
    buildReagentPropertyColumns(columns);
    buildCompoundPropertyColumns(columns);
    buildGenePropertyColumns(columns);
    buildResultValueTypeColumns(columns);
    buildAnnotationTypeColumns(columns);
    return columns;
  }

  private void buildGenePropertyColumns(List<EntityColumn<Well,?>> columns)
  {
    columns.add(new TextEntityColumn<Well>(new PropertyPath<Well>(Well.class,
      "gene",
    "geneName"),
    "Gene Name",
    "The gene name for the silencing reagent in the well",
    GENE_COLUMNS_GROUP) {
      @Override
      public String getCellValue(Well well)
      {
        return well.getGene() == null ? null : well.getGene().getGeneName();
      }

      @Override
      public boolean isCommandLink() { return true; }

      @Override
      public Object cellAction(Well well)
      {
        return _geneViewer.viewGene(well.getGene());
      }
    });
    columns.get(columns.size() - 1).setVisible(false);
    columns.add(new IntegerEntityColumn<Well>(new PropertyPath<Well>(Well.class,
      "gene",
    "entrezgeneId"),
    "Entrez Gene ID",
    "The Entrez gene ID for the gene targeted by silencing reagent in the well",
    GENE_COLUMNS_GROUP) {
      @Override
      public Integer getCellValue(Well well)
      {
        return well.getGene() == null ? null : well.getGene().getEntrezgeneId();
      }
    });
    columns.get(columns.size() - 1).setVisible(false);
    columns.add(new TextEntityColumn<Well>(new PropertyPath<Well>(Well.class,
      "gene",
    "entrezgeneSymbol"),
    "Entrez Gene Symbol",
    "The Entrez gene symbol for the gene targeted by silencing reagent in the well",
    GENE_COLUMNS_GROUP) {
      @Override
      public String getCellValue(Well well)
      {
        return well.getGene() == null ? null : well.getGene().getEntrezgeneSymbol();
      }
    });
    columns.get(columns.size() - 1).setVisible(false);
    columns.add(new ListEntityColumn<Well>(new PropertyPath<Well>(Well.class, "gene.genbankAccessionNumbers", ""),
    "Genbank Accession Numbers",
    "The Genbank Accession Numbers for the gene targeted by silencing reagent in the well",
    GENE_COLUMNS_GROUP) {
      @Override
      public List<String> getCellValue(Well well)
      {
        return well.getGene() == null ? null : new ArrayList<String>(well.getGene().getGenbankAccessionNumbers());
      }
    });
    columns.get(columns.size() - 1).setVisible(false);
  }

  private void buildCompoundPropertyColumns(List<EntityColumn<Well,?>> columns)
  {

    columns.add(new ListEntityColumn<Well>(new PropertyPath<Well>(Well.class,
      "compounds",
    "smiles"),
    "Compounds SMILES",
    "The SMILES for the compound(s) in the well",
    COMPOUND_COLUMNS_GROUP) {
      @Override
      public List<String> getCellValue(Well well)
      {
        List<String> smiles = new ArrayList<String>();
        for (Compound compound : well.getOrderedCompounds()) {
          smiles.add(compound.getSmiles());
        }
        return smiles;
      }

      @Override
      public boolean isCommandLink() { return true; }

      @Override
      public Object cellAction(Well well)
      {
        if (well.getCompounds().size() > 0) {
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
    columns.get(columns.size() - 1).setVisible(false);
    columns.add(new ListEntityColumn<Well>(new PropertyPath<Well>(Well.class,
      "compounds",
    "inchi"),
    "Compounds InChi",
    "The InChi for the compound(s) in the well", COMPOUND_COLUMNS_GROUP) {
      @Override
      public List<String> getCellValue(Well well)
      {
        List<String> smiles = new ArrayList<String>();
        for (Compound compound : well.getOrderedCompounds()) {
          smiles.add(compound.getInchi());
        }
        return smiles;
      }

      @Override
      public boolean isCommandLink() { return true; }

      @Override
      public Object cellAction(Well well)
      {
        if (well.getCompounds().size() > 0) {
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
    columns.get(columns.size() - 1).setVisible(false);

    columns.add(new ListEntityColumn<Well>(new PropertyPath<Well>(Well.class, "compounds.compoundNames", ""),
      "Primary Compound Names",
      "The names of the primary compound in the well",
      COMPOUND_COLUMNS_GROUP) {
      @Override
      public List<String> getCellValue(Well well)
      {
        return well.getPrimaryCompound() == null ? null : new ArrayList<String>(well.getPrimaryCompound().getCompoundNames());
      }
    });
    columns.get(columns.size() - 1).setVisible(false);

    columns.add(new ListEntityColumn<Well>(new PropertyPath<Well>(Well.class, "compounds.casNumbers", ""),
      "CAS Numbers",
      "The CAS Numbers of the primary compound in the well",
      COMPOUND_COLUMNS_GROUP) {
      @Override
      public List<String> getCellValue(Well well)
      {
        return well.getPrimaryCompound() == null ? null : new ArrayList<String>(well.getPrimaryCompound().getCasNumbers());
      }
    });
    columns.get(columns.size() - 1).setVisible(false);

    columns.add(new ListEntityColumn<Well>(new PropertyPath<Well>(Well.class, "compounds.nscNumbers", ""),
      "NSC Numbers",
      "The NSC Numbers of the primary compound in the well",
      COMPOUND_COLUMNS_GROUP) {
      @Override
      public List<String> getCellValue(Well well)
      {
        return well.getPrimaryCompound() == null ? null : new ArrayList<String>(well.getPrimaryCompound().getNscNumbers());
      }
    });
    columns.get(columns.size() - 1).setVisible(false);

    columns.add(new ListEntityColumn<Well>(new PropertyPath<Well>(Well.class, "compounds.pubchemCids", ""),
      "PubChem CIDs",
      "The PubChem CIDs of the primary compound in the well",
      COMPOUND_COLUMNS_GROUP) {
      @Override
      public List<String> getCellValue(Well well)
      {
        return well.getPrimaryCompound() == null ? null : new ArrayList<String>(well.getPrimaryCompound().getPubchemCids());
      }
    });
    columns.get(columns.size() - 1).setVisible(false);

    columns.add(new ListEntityColumn<Well>(new PropertyPath<Well>(Well.class, "compounds.chembankIds", ""),
      "ChemBank IDs",
      "The ChemBank IDs of the primary compound in the well",
      COMPOUND_COLUMNS_GROUP) {
      @Override
      public List<String> getCellValue(Well well)
      {
        return well.getPrimaryCompound() == null ? null : new ArrayList<String>(well.getPrimaryCompound().getChembankIds());
      }
    });
    columns.get(columns.size() - 1).setVisible(false);
  }

  private void buildReagentPropertyColumns(List<EntityColumn<Well,?>> columns)
  {
    columns.add(new TextEntityColumn<Well>(
      new PropertyPath<Well>(Well.class, "reagent", "reagentIdString"),
      "Reagent Source ID",
      "The vendor-assigned identifier for the reagent in this well.", WELL_COLUMNS_GROUP) {
      @Override
      public String getCellValue(Well well)
      {
        Reagent reagent = well.getReagent();
        return reagent == null ? null : reagent.getEntityId().getReagentId();
      }
    });
  }

  private void buildWellPropertyColumns(List<EntityColumn<Well,?>> columns)
  {
    columns.add(new TextEntityColumn<Well>(new PropertyPath<Well>(Well.class,
                                                            "library",
                                                            "libraryName"),
                                     "Library",
                                     "The library containing the well",
                                     WELL_COLUMNS_GROUP) {
      @Override
      public String getCellValue(Well well)
      {
        return well.getLibrary().getLibraryName();
      }

      @Override
      public boolean isCommandLink()
      {
        return true;
      }

      @Override
      public Object cellAction(Well well)
      {
        return _libraryViewer.viewLibrary(well.getLibrary());
      }
    });
    columns.add(new IntegerEntityColumn<Well>(new PropertyPath<Well>(Well.class,
                                                               "plateNumber"),
                                        "Plate",
                                        "The number of the plate the well is located on",
                                        WELL_COLUMNS_GROUP) {
      @Override
      public Integer getCellValue(Well well)
      {
        return well.getPlateNumber();
      }

      @Override
      protected Comparator<Well> getAscendingComparator()
      {
        return new Comparator<Well>() {
          @SuppressWarnings("unchecked")
          public int compare(Well w1, Well w2)
          {
            return w1.getWellKey().compareTo(w2.getWellKey());
          }
        };
      }
    });
    columns.add(new TextEntityColumn<Well>(new PropertyPath<Well>(Well.class,
                                                            "wellName"),
                                     "Well",
                                     "The plate coordinates of the well",
                                     WELL_COLUMNS_GROUP) {
      @Override
      public String getCellValue(Well well)
      {
        return well.getWellName();
      }

      @Override
      public boolean isCommandLink()
      {
        return true;
      }

      @Override
      public Object cellAction(Well well)
      {
        return viewSelectedEntity();
      }
    });
    columns.add(new EnumEntityColumn<Well,WellType>(new PropertyPath<Well>(Well.class, "wellType"),
                                              "Well Type",
                                              "The type of well, e.g., 'Experimental', 'Control', 'Empty', etc.",
                                              WELL_COLUMNS_GROUP,
                                              WellType.values()) {
      @Override
      public WellType getCellValue(Well well)
      {
        return well.getWellType();
      }
    });

    columns.add(new TextEntityColumn<Well>(new PropertyPath<Well>(Well.class,
      "iccbNumber"),
      "ICCB Number",
      "The identifier assigned by ICCB-L to the contents of this well",
      WELL_COLUMNS_GROUP) {
      @Override
      public String getCellValue(Well well)
      {
        return well.getIccbNumber();
      }
    });
    columns.get(columns.size() - 1).setVisible(false);

    columns.add(new BooleanEntityColumn<Well>(
      new PropertyPath<Well>(Well.class, "deprecated"),
      "Deprecated", "Whether the well has been deprecated", TableColumn.UNGROUPED) {
      @Override
      public Boolean getCellValue(Well well)
      {
        return well.isDeprecated();
      }
    });
    columns.get(columns.size() - 1).setVisible(false);
    columns.add(new TextEntityColumn<Well>(
      new PropertyPath<Well>(Well.class, "deprecationActivity", "comments"),
      "Deprecation Reason", "Why the well has been deprecated", TableColumn.UNGROUPED) {
      @Override
      public String getCellValue(Well well)
      {
        return well.isDeprecated() ? well.getDeprecationActivity().getComments() : null;
      }
    });
    columns.get(columns.size() - 1).setVisible(false);
  }

  private void buildResultValueTypeColumns(List<EntityColumn<Well,?>> columns)
  {
    List<EntityColumn<Well,?>> otherColumns = new ArrayList<EntityColumn<Well,?>>();
    boolean isAssayWellTypeColumnCreated = false; 
    for (Triple<ResultValueType,Integer,String> rvtAndScreenNumberAndTitle : findValidResultValueTypes()) {
      final ResultValueType rvt = rvtAndScreenNumberAndTitle.getFirst();
      Integer screenNumber = rvtAndScreenNumberAndTitle.getSecond();
      String screenTitle = rvtAndScreenNumberAndTitle.getThird();

      EntityColumn<Well,?> column;
//      if (rvt.isPositiveIndicator() &&
//        rvt.getPositiveIndicatorType() == PositiveIndicatorType.BOOLEAN) {
//          column = new BooleanEntityColumn<Well>(
//            new PropertyPath<Well>(Well.class, "resultValues[resultValueType]", "positive", rvt),
//            makeColumnName(rvt, screenNumber),
//            rvt.getDescription(),
//            columnGroup) {
//            @Override
//            public Boolean getCellValue(Well well)
//            {
//              ResultValue rv = well.getResultValues().get(rvt);
//              return rv == null ? null : rv.isPositive();
//            }
//          };
//        }
//        else if (rvt.getPositiveIndicatorType() == PositiveIndicatorType.NUMERICAL) {
//          column = new FixedDecimalEntityColumn<Well>(
//            new PropertyPath<Well>(Well.class, "resultValues[resultValueType]", "numericValue", rvt),
//            makeColumnName(rvt, screenNumber),
//            rvt.getDescription(),
//            columnGroup) {
//            @Override
//            public BigDecimal getCellValue(Well well)
//            {
//              // TODO: move this code to ResultValue
//              BigDecimal value = null;
//              ResultValue rv = well.getResultValues().get(rvt);
//              if (rv != null && rv.getNumericValue() != null) {
//                value = new BigDecimal(rv.getNumericValue());
//                Integer scale = rv.getNumericDecimalPrecision();
//                if (scale == null) {
//                  scale = ResultValue.DEFAULT_DECIMAL_PRECISION;
//                }
//                value = value.setScale(scale, RoundingMode.HALF_UP);
//              }
//              return value;
//            }
//          };
//        }
//        else if (rvt.getPositiveIndicatorType() == PositiveIndicatorType.PARTITION) {
//          column = new VocabularyEntityColumn<Well,PartitionedValue>(
//            new PropertyPath<Well>(Well.class, "resultValues[resultValueType]", "value", rvt),
//            makeColumnName(rvt, screenNumber) + " Positive",
//            "Positive flag for " + makeColumnName(rvt, screenNumber),
//            columnGroup,
//            null,
//            PartitionedValue.values()) {
//            @Override
//            public PartitionedValue getCellValue(Well well)
//            {
//              ResultValue rv = well.getResultValues().get(rvt);
//              if (rv != null) {
//                return PartitionedValue.lookupByValue(rv.getValue());
//              }
//              return null;
//            }
//          };
//        }
//        else {
//          throw new RuntimeException("system error; unhandled PositiveIndicatorType");
//        }
//      }
//      else
      if (rvt.isNumeric()) {
        column = new RealEntityColumn<Well>(
          new PropertyPath<Well>(Well.class, "resultValues[resultValueType]", "numericValue", rvt),
          makeColumnName(rvt, screenNumber),
          makeColumnDescription(rvt, screenNumber, screenTitle),
          makeColumnGroup(screenNumber, screenTitle)) {
          @Override
          public Double getCellValue(Well well)
          {
            ResultValue rv = well.getResultValues().get(rvt);
            return rv == null ? null : rv.getNumericValue();
          }
        };
      }
      else {
        column = new TextEntityColumn<Well>(
          new PropertyPath<Well>(Well.class, "resultValues[resultValueType]", "value", rvt),
          makeColumnName(rvt, screenNumber),
          makeColumnDescription(rvt, screenNumber, screenTitle),
          makeColumnGroup(screenNumber, screenTitle)) {
          @Override
          public String getCellValue(Well well)
          {
            ResultValue rv = well.getResultValues().get(rvt);
            return rv == null ? null : rv.getValue();
          }
        };
      }

      // request eager fetching of resultValueType, since Hibernate will otherwise fetch these with individual SELECTs
      column.addRelationshipPath(new RelationshipPath<Well>(Well.class, "resultValues.resultValueType"));

      // BII start: (Siew Cheng) Add assay well type column 
      if (!isAssayWellTypeColumnCreated && column.getGroup().equals(OUR_DATA_HEADERS_COLUMNS_GROUP)) {
        columns.add(new EnumEntityColumn<Well,AssayWellType>(new PropertyPath<Well>(Well.class, "resultValues[resultValueType]", "assayWellType", rvt),
          "Assay Well Type",
          "The type of assay well, e.g., 'Experimental', 'Empty', 'Library Control', " +
          "'Assay Positive Control', 'Assay Control', 'Buffer', etc.",
          column.getGroup(),
          AssayWellType.values()) {
          @Override
          public AssayWellType getCellValue(Well well)
          {
            return well.getResultValues().get(rvt) == null ? null : well.getResultValues().get(rvt).getAssayWellType();
          }
        });
        isAssayWellTypeColumnCreated = true;
      }
      // BII end

      if (column.getGroup().equals(OUR_DATA_HEADERS_COLUMNS_GROUP)) {
        columns.add(column);
        column.setVisible(true);
      }
      else {
        otherColumns.add(column);
        column.setVisible(false);
      }
    }
    columns.addAll(otherColumns);
  }

  private String makeColumnGroup(Integer screenNumber, String screenTitle)
  {
    String columnGroup;
    if (_screenResult != null && _screenResult.getScreen().getScreenNumber().equals(screenNumber)) {
      columnGroup = OUR_DATA_HEADERS_COLUMNS_GROUP;
    }
    else {
      columnGroup = OTHER_DATA_HEADERS_COLUMNS_GROUP + TableColumnManager.GROUP_NODE_DELIMITER + screenNumber + " (" + screenTitle + ")";
    }
    return columnGroup;
  }

  /**
   * @return a list of RVTs that have RVs for wells in common with the wells of this search result, ordered by screen number and RVT ordinal
   */
  private List<Triple<ResultValueType,Integer,String>> findValidResultValueTypes()
  {
    return _dao.runQuery(new Query() {
      public List<Triple<ResultValueType,Integer,String>> execute(Session session)
      {
        HqlBuilder hql = new HqlBuilder();
        hql.distinctProjectionValues();
        hql.from(ResultValueType.class, "rvt").from("rvt", "screenResult", "sr", JoinType.INNER).from("sr", "screen", "s", JoinType.INNER);
        hql.select("rvt").select("s", "screenNumber").select("s", "title");
        hql.orderBy("s", "screenNumber").orderBy("rvt", "ordinal");

        if (_mode == WellSearchResultMode.SCREEN_RESULT_WELLS) {
          if (_screenResult == null) {
            return Collections.emptyList();
          }
          hql.from("sr", "plateNumbers", "p1", JoinType.INNER);
          hql.from(ScreenResult.class, "sr0").from("sr0", "plateNumbers", "p0", JoinType.INNER);
          hql.where("p0", Operator.EQUAL, "p1").where("sr0", _screenResult);
        }
        else if (_mode == WellSearchResultMode.LIBRARY_WELLS) {
          if (_library == null) {
            return Collections.emptyList();
          }
          hql.from("sr", "plateNumbers", "p", JoinType.INNER);
          hql.where("p", Operator.GREATER_THAN_EQUAL, _library.getStartPlate());
          hql.where("p", Operator.LESS_THAN_EQUAL, _library.getEndPlate());
        }
        else if (_mode == WellSearchResultMode.SET_OF_WELLS) {
          if (_plateNumbers.size() == 0) {
            return Collections.emptyList();
          }
          hql.from("sr", "plateNumbers", "p", JoinType.INNER);
          hql.whereIn("p", _plateNumbers);
        }
        else {
          // find all result value types, no additional HQL needed
        }
        if (log.isDebugEnabled()) {
          log.debug("findValidResultValueTypes query: " + hql.toHql());
        }
        org.hibernate.Query query = hql.toQuery(session, true);
        query.setResultTransformer(new MetaDataColumnResultTransformer<ResultValueType>());
        List<Triple<ResultValueType,Integer,String>> result = query.list();
        return result;
      }
    });
  }

  private void buildAnnotationTypeColumns(List<EntityColumn<Well,?>> columns)
  {
    List<EntityColumn<Well,?>> otherColumns = new ArrayList<EntityColumn<Well,?>>();
    for (Triple<AnnotationType,Integer,String> atAndStudyNumberAndTitle : findValidAnnotationTypes()) {
      final AnnotationType annotationType = atAndStudyNumberAndTitle.getFirst();
      Integer studyNumber = atAndStudyNumberAndTitle.getSecond();
      String studyTitle = atAndStudyNumberAndTitle.getThird();
      EntityColumn<Well,?> column;

      String columnGroup;
      if (_screenResult != null && _screenResult.getScreen().isStudyOnly() && _screenResult.getScreen().getScreenNumber().equals(studyNumber)) {
        columnGroup = OUR_ANNOTATION_TYPES_COLUMN_GROUP;
      }
      else {
        columnGroup = OTHER_ANNOTATION_TYPES_COLUMN_GROUP + TableColumnManager.GROUP_NODE_DELIMITER + studyNumber + " (" + studyTitle + ")";
      }

      if (annotationType.isNumeric()) {
        column = new RealEntityColumn<Well>(
          new PropertyPath<Well>(Well.class,
            "reagent.annotationValues[annotationType]",
            "numericValue",
            annotationType),
            makeColumnName(annotationType, studyNumber),
            WellSearchResults.makeColumnDescription(annotationType, studyNumber, studyTitle),
            columnGroup) {
          @Override
          public Double getCellValue(Well well)
          {
            Reagent reagent = well.getReagent();
            if (reagent != null) {
              AnnotationValue av = reagent.getAnnotationValues().get(annotationType);
              if (av != null) {
                return av.getNumericValue();
              }
            }
            return null;
          }
        };
      }
      else {
        column = new TextEntityColumn<Well>(
          new PropertyPath<Well>(Well.class,
            "reagent.annotationValues[annotationType]",
            "value",
            annotationType),
            makeColumnName(annotationType, studyNumber),
            WellSearchResults.makeColumnDescription(annotationType, studyNumber, studyTitle),
            columnGroup) {
          @Override
          public String getCellValue(Well well)
          {
            Reagent reagent = well.getReagent();
            if (reagent != null) {
              AnnotationValue av = reagent.getAnnotationValues().get(annotationType);
              if (av != null) {
                return av.getValue();
              }
            }
            return null;
          }
        };
      }

      // request eager fetching of annotationType, since Hibernate will otherwise fetch these with individual SELECTs
      column.addRelationshipPath(new RelationshipPath<Well>(Well.class, "reagent.annotationValues.annotationType.study"));

      if (column.getName().equals(OUR_ANNOTATION_TYPES_COLUMN_GROUP)) {
        columns.add(column);
        column.setVisible(true);
      }
      else {
        otherColumns.add(column);
        column.setVisible(false);
      }
    }
    columns.addAll(otherColumns);
  }

  private List<Triple<AnnotationType,Integer,String>> findValidAnnotationTypes()
  {
    return _dao.runQuery(new Query() {
      public List<Triple<AnnotationType,Integer,String>> execute(Session session)
      {
        HqlBuilder hql = new HqlBuilder();
        hql.distinctProjectionValues();
        hql.from(AnnotationType.class, "at").from("at", "study", "s", JoinType.INNER);
        hql.select("at").select("s", "screenNumber").select("s", "title");
        hql.orderBy("s", "screenNumber").orderBy("at", "ordinal");

        if (_mode == WellSearchResultMode.SCREEN_RESULT_WELLS) {
          if (_screenResult == null) {
            return Collections.emptyList();
          }
          hql.where("s", "screenType", Operator.EQUAL, _screenResult.getScreen().getScreenType());
        }
        else if (_mode == WellSearchResultMode.LIBRARY_WELLS) {
          if (_library == null) {
            return Collections.emptyList();
          }
          hql.where("s", "screenType", Operator.EQUAL, _library.getScreenType());
        }
        else if (_mode == WellSearchResultMode.SET_OF_WELLS) {
          if (_plateNumbers.size() == 0) {
            return Collections.emptyList();
          }
          // select annotation types for studies that have same screenType of the libraries containing the set of wells in this search result
          hql.from(Library.class, "l");
          Disjunction librariesWhere = hql.disjunction();
          for (Integer plateNumber : _plateNumbers) {
            Conjunction libraryWhere = hql.conjunction();
            libraryWhere.add(hql.predicate("l.startPlate", Operator.LESS_THAN_EQUAL, plateNumber));
            libraryWhere.add(hql.predicate("l.endPlate", Operator.GREATER_THAN_EQUAL, plateNumber));
            libraryWhere.add(hql.predicate("l.screenType", "s.screenType", Operator.EQUAL));
            librariesWhere.add(libraryWhere);
          }
          hql.where(librariesWhere);
        }
        else {
          // find all annotation types, no additional HQL needed
        }
        if (log.isDebugEnabled()) {
          log.debug("findValidAnnotationTypes query: " + hql.toHql());
        }
        org.hibernate.Query query = hql.toQuery(session, true);
        query.setResultTransformer(new MetaDataColumnResultTransformer<AnnotationType>());
        List<Triple<AnnotationType,Integer,String>> result = query.list();
        return result;
      }
    });
  }

  @Override
  protected void setEntityToView(Well well)
  {
    _wellViewer.viewWell(well);
  }

  /**
   * Effects the replacement of the NoOpDataFetcher (set as the default search for 
   * users entering the page) so that the current search will return results.
   * 
   * @motivation to be tied to the actionListener for search field buttons.  
   *   
   * Sequence of a user search is:
   * <ol>
   * <li>User enters search patterns and submits (by pressing enter or the search button).</li>
   * <li>The Criteria field notifies the {@link Criterion} of the value change.  This triggers the chain of 
   * events that causes the table to refresh.  If the table has been initialized with a NoOpDataFetcher (the 
   * default), then no data is retrieved.</li>
   * <li>This action listener is invoked; it will call <code>initialize</code> on the parent {@link DataTable} class:</li>
   * <li>replaces the {@link NoOpDataFetcher} with a valid {@link DataFetcher},</li>
   * <li>uses the {@link TableColumn}s from the users search set with the proper {@link Criterion},</li>
   * <li>triggers execution of the search.</li>
   * </ol>
   * 
   * @see WellSearchResults#searchAllWells()
   */
  public void searchCommandListener(javax.faces.event.ActionEvent e)
  {
    // By looking at this flag, we can tell if the search is being done in some context or not.
    if (_mode == WellSearchResultMode.ALL_WELLS) {
      List<TableColumn<Well,?>> columns = new ArrayList<TableColumn<Well,?>>();
      columns.addAll(getColumnManager().getAllColumns());
      initialize(
        new AllEntitiesOfTypeDataFetcher<Well,String>(Well.class, _dao){
          @Override
          protected void addDomainRestrictions(HqlBuilder hql,
                                               Map<RelationshipPath<Well>,String> path2Alias)
          {
            hql.from(getRootAlias(), "library", "l");
            hql.whereIn("l", "libraryType", LibrarySearchResults.LIBRARY_TYPES_TO_DISPLAY);
          }
        }, 
        columns);
    }
  }

  /**
   * Override resetFilter() in order to handle the "reset all" command in
   * "all wells" mode by resetting to an empty search result.
   */
  @UIControllerMethod
  public String resetFilter()
  {
    if (_mode == WellSearchResultMode.ALL_WELLS) {
      log.info("reverting to empty search result");
      initialize(new NoOpDataFetcher<Well,String,PropertyPath<Well>>());
    }
    return super.resetFilter();
  }
  
  @Override
  public String resetColumnFilter()
  {
    TableColumn<Well,?> resetColumn = (TableColumn<Well,?>) getRequestMap().get("column");
    if (_mode == WellSearchResultMode.ALL_WELLS) {
      // if no other columns have criteria defined, reset to empty search result
      boolean willHaveEmptyCriteria = true;
      for (TableColumn<Well,?> column : getColumnManager().getAllColumns()) {
        if (!column.getCriterion().isUndefined() && column != resetColumn) {
          willHaveEmptyCriteria = false;
          break;
        }
      }
      if (willHaveEmptyCriteria) {
        // reset to an empty search result
        log.info("all columns have empty criteria; reverting to empty search result");
        initialize(new NoOpDataFetcher<Well,String,PropertyPath<Well>>());
      }
    }
    return super.resetColumnFilter();
  }

  // private instance methods

  public static String makeColumnName(MetaDataType mdt, Integer parentIdentifier)
  {
    // note: replacing "_" with white space allows column headers to wrap, creating narrower columns
    return String.format("%s [%d]", mdt.getName().replaceAll("_", " "), parentIdentifier);
  }

  public static String makeColumnDescription(MetaDataType mdt, Integer parentIdentifier, String parentTitle)
  {

    return makeColumnDescription(mdt.getName(), mdt.getDescription(), parentIdentifier, parentTitle);
  }  
  
  public static String makeColumnDescription(String name, String description, Integer parentIdentifier, String parentTitle)
  {
    StringBuilder columnDescription = new StringBuilder();
    columnDescription.append("<i>").append(parentIdentifier).append(": ").append(parentTitle).
    append("</i><br/><b>").append(name).append("</b>");
    if (description != null) {
      columnDescription.append(": ").append(description);
    }
    return columnDescription.toString();
  }

  private void updateColumnVisibilityForScreenType(boolean showCompounds, boolean showGenes)
  {
    for (TableColumn<Well,?> s : getColumnManager().getAllColumns()) {
//      if (column.getGroup().equals(COMPOUND_COLUMNS_GROUP)) {
//        column.setVisible(showCompounds);
//      }
//      else if (column.getGroup().equals(GENE_COLUMNS_GROUP)) {
//        column.setVisible(showGenes);
//      }
    }
  }
}
