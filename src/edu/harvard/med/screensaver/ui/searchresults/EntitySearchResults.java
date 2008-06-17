// $HeadURL:
// svn+ssh://js163@orchestra.med.harvard.edu/svn/iccb/screensaver/trunk/.eclipse.prefs/codetemplates.xml
// $
// $Id$

// Copyright 2006 by the President and Fellows of Harvard College.

// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.ui.searchresults;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.faces.model.DataModelEvent;
import javax.faces.model.DataModelListener;

import edu.harvard.med.screensaver.db.datafetcher.DataFetcher;
import edu.harvard.med.screensaver.db.datafetcher.EntityDataFetcher;
import edu.harvard.med.screensaver.db.datafetcher.EntitySetDataFetcher;
import edu.harvard.med.screensaver.io.DataExporter;
import edu.harvard.med.screensaver.io.TableDataExporter;
import edu.harvard.med.screensaver.model.AbstractEntity;
import edu.harvard.med.screensaver.model.PropertyPath;
import edu.harvard.med.screensaver.ui.UIControllerMethod;
import edu.harvard.med.screensaver.ui.table.RowsPerPageSelector;
import edu.harvard.med.screensaver.ui.table.column.TableColumn;
import edu.harvard.med.screensaver.ui.table.column.entity.EntityColumn;
import edu.harvard.med.screensaver.ui.table.model.DataTableModel;
import edu.harvard.med.screensaver.ui.table.model.InMemoryDataModel;
import edu.harvard.med.screensaver.ui.table.model.InMemoryEntityDataModel;
import edu.harvard.med.screensaver.ui.table.model.VirtualPagingEntityDataModel;
import edu.harvard.med.screensaver.ui.util.JSFUtils;
import edu.harvard.med.screensaver.ui.util.UISelectOneBean;
import edu.harvard.med.screensaver.ui.util.ValueReference;

import org.apache.log4j.Logger;

/**
 * SearchResults subclass that presents a particular type of domain model
 * entity. Subclass adds:
 * <ul>
 * <li>"Summary" and "Entity" viewing modes, corresponding to a multi-entity
 * list view and a single-entity full page view, respectively.</li>
 * <li>Dynamically decides whether to use InMemoryDataModel of
 * VirtualPagingDataModel, based upon data size and column composition.</li>
 * <li>Management of "filter mode"</li>
 * <li>Downloading of search results via one or more {@link DataExporter}s.</li>
 * </ul>
 *
 * @author <a mailto="andrew_tolopko@hms.harvard.edu">Andrew Tolopko</a>
 * @author <a mailto="john_sullivan@hms.harvard.edu">John Sullivan</a>
 */
public abstract class EntitySearchResults<E extends AbstractEntity, K> extends SearchResults<E,K,PropertyPath<E>>
{
  // static members

  /**
   * The maximum number of entities that can be loaded as a single batch search
   * result, using InMemoryDataModel. If more than this number is to be loaded,
   * VirtualPagingDataModel will be used instead.
   */
  public static final int ALL_IN_MEMORY_THRESHOLD = 1024;

  private static Logger log = Logger.getLogger(EntitySearchResults.class);

  private static final String[] CAPABILITIES = { "viewEntity", "exportData", "filter" };

  // instance data members

  private List<DataExporter<?>> _dataExporters = new ArrayList<DataExporter<?>>();
  private UISelectOneBean<DataExporter<?>> _dataExporterSelector;
  /**
   * @motivation to prevent redundant calls to setEntityToView
   */
  private E entityInView = null;



  // abstract methods

  abstract protected void setEntityToView(E entity);

  // protected methods

  @Override
  public void initialize(DataFetcher<E,K,PropertyPath<E>> dataFetcher)
  {
    super.initialize(dataFetcher);

    // reset to default rows-per-page, if in "entity view" mode
    if (isEntityView()) {
      getRowsPerPageSelector().setSelection(getRowsPerPageSelector().getDefaultSelection());
    }
  }

  /**
   * View the entity currently selected in the DataTableModel in entity view
   * mode.
   *
   * @motivation To be called by a TableColumn.cellAction() method to view the
   *             current row's entity in the "entity view" mode, or by any other
   *             code that wants to switch to entity view mode.
   * @motivation To be called by a DataTableModel listener in reponse to
   *             rowSelected() events.
   */
  @UIControllerMethod
  final protected String viewSelectedEntity()
  {
    if (getDataTableModel().getRowCount() > 0 &&
        getDataTableModel().isRowAvailable()) {
      viewEntityAtRow(getDataTableModel().getRowIndex());
    }
    return REDISPLAY_PAGE_ACTION_RESULT;
  }

  final protected RowsPerPageSelector buildRowsPerPageSelector()
  {
    // note: we need a special "single" (1) selection item, for viewing the
    // entity in its full viewer page
    RowsPerPageSelector rowsPerPageSelector = new RowsPerPageSelector(Arrays.asList(1,
                                                                                    10,
                                                                                    20,
                                                                                    50,
                                                                                    100),
                                                                      20) {
      @Override
      public String getLabel(Integer value)
      {
        if (value.equals(1)) {
          return "Single";
        }
        else {
          return super.getLabel(value);
        }
      }
    };

    rowsPerPageSelector.addObserver(new Observer() {
      public void update(Observable obs, Object o)
      {
        if (((Integer) o) == 1) {
          if (getDataTableUIComponent() != null) {
            // this will cause our DataModel listener to set the entity to be viewed
            log.debug("entering 'entity view' mode; setting data table row to first row on page:" +
                      getDataTableUIComponent().getFirst());
            getDataTableModel().setRowIndex(getDataTableUIComponent().getFirst());
          }
        }
      }
    });

    return rowsPerPageSelector;
  }

  // public constructors and methods

  /**
   * @motivation for CGLIB2
   */
  public EntitySearchResults()
  {
    this(Collections.<DataExporter<?>>emptyList());
  }

  /**
   * Constructs a EntitySearchResults object.
   *
   * @param dataExporters a List of DataExporters that must be one of the reified
   *          types DataExporter<DataTableModel<E>> or DataExporter<E>
   */
  public EntitySearchResults(List<DataExporter<?>> dataExporters)
  {
    super(CAPABILITIES);
    _dataExporters.add(new GenericDataExporter<E>("searchResult"));
    _dataExporters.addAll(dataExporters);
  }

  @Override
  public void resort()
  {
    super.resort();
    if (isEntityView()) {
      if (getDataTableUIComponent() != null) {
        getDataTableModel().setRowIndex(getDataTableUIComponent().getFirst());
      }
      viewSelectedEntity();
    }
  }

  public boolean isRowRestricted()
  {
    return getRowData().isRestricted();
  }

  public boolean isSummaryView()
  {
    return !isEntityView();
  }

  public boolean isEntityView()
  {
    return getRowsPerPage() == 1 && getRowCount() > 0;
  }

  @UIControllerMethod
  public String returnToSummaryList()
  {
    getRowsPerPageSelector().setSelection(getRowsPerPageSelector().getDefaultSelection());
    scrollToPageContainingRow(getDataTableUIComponent().getFirst());
    return REDISPLAY_PAGE_ACTION_RESULT;
  }

  /**
   * Switch to entity view mode and show the specified entity, automatically
   * scrolling the data table to the row containing the entity.
   *
   * @param entity
   * @return true if the entity exists in the search result, otherwise false
   */
  public boolean viewEntity(E entity)
  {
//    // first test whether the current row is already the one with the requested entity
//    E currentEntityInSearchResults = null;
//    if (getDataTableUIComponent() != null) {
//      int currentRow = getDataTableUIComponent().getFirst();
//      getDataTableModel().setRowIndex(currentRow);
//      if (getDataTableModel().isRowAvailable()) {
//        currentEntityInSearchResults = (E) getDataTableModel().getRowData();
//        if (entity.equals(currentEntityInSearchResults)) {
//          return true;
//        }
//      }
//    }

    // else, do linear search to find the entity (but only works for InMemoryDataModel)
    int rowIndex = findRowOfEntity(entity);
    if (rowIndex < 0) {
      log.debug("entity " + entity + " not found in entity search results");
      return false;
    }
    log.debug("entity " + entity + " found in entity search results");
    viewEntityAtRow(rowIndex);
    return true;
  }

  private void switchToEntityViewMode()
  {
    getRowsPerPageSelector().setSelection(1);
  }

  private void viewEntityAtRow(int rowIndex)
  {
    if (rowIndex >= 0 && rowIndex < getRowCount()) {
      // first, scroll the data table so that the user-selected row is the first on the page,
      // otherwise rowsPerPageSelector's observer will switch to whatever entity was previously in the first row
      scrollToRow(rowIndex);
      getDataTableModel().setRowIndex(rowIndex);
      E entity = (E) getRowData();
      if (entity != entityInView) { // test instance equality, rather than object equality, in case entity was reloaded and is updated
        log.debug("viewEntityAtRow(): setting entity to view: " + entity);
        // have the entity viewer update its data
        setEntityToView(entity);
        entityInView = entity;
      }
      switchToEntityViewMode();
    }
  }

  /**
   * Override to ensure that "table filter mode" is always disabled when in
   * "entity view" mode, since search fields would entirely hidden from user in
   * this mode.
   */
  @Override
  public boolean isTableFilterMode()
  {
    return super.isTableFilterMode() && isSummaryView();
  }

  /**
   * Override to ensure that "table filter mode" is always disabled when in
   * "entity view" mode, since search fields would entirely hidden from user in
   * this mode.
   */
  @Override
  public void setTableFilterMode(boolean isTableFilterMode)
  {
    if (isEntityView()) {
      return;
    }
    else {
      super.setTableFilterMode(isTableFilterMode);
    }
  }

  /**
   * @motivation type safety of return type
   */
  @Override
  public EntityDataFetcher<E,K> getDataFetcher()
  {
    return (EntityDataFetcher<E,K>) super.getDataFetcher();
  }

  /**
   * @return a List of DataExporters that will be one of the reified types
   *         DataExporter<DataTableModel<E>> or DataExporter<E>
   */
  public List<DataExporter<?>> getDataExporters()
  {
    return _dataExporters;
  }

  public UISelectOneBean<DataExporter<?>> getDataExporterSelector()
  {
    if (_dataExporterSelector == null) {
      _dataExporterSelector = new UISelectOneBean<DataExporter<?>>(getDataExporters()) {
        @Override
        protected String getLabel(DataExporter<?> dataExporter)
        {
          return dataExporter.getFormatName();
        }
      };
    }
    return _dataExporterSelector;
  }

  @SuppressWarnings("unchecked")
  @UIControllerMethod
  /* final (CGLIB2 restriction) */
  public String downloadSearchResults()
  {
    try {
      DataExporter<?> dataExporter = getDataExporterSelector().getSelection();
      InputStream inputStream;
      if (dataExporter instanceof TableDataExporter) {
        ((TableDataExporter<E>) dataExporter).setTableColumns(getColumnManager().getVisibleColumns());
        inputStream = ((TableDataExporter<E>) dataExporter).export(getDataTableModel());
      }
      else {
        inputStream = ((DataExporter<Collection<K>>) dataExporter).export(getDataFetcher().findAllKeys());
      }
      JSFUtils.handleUserDownloadRequest(getFacesContext(),
                                         inputStream,
                                         dataExporter.getFileName(),
                                         dataExporter.getMimeType());
    }
    catch (IOException e) {
      reportApplicationError(e.toString());
    }
    return REDISPLAY_PAGE_ACTION_RESULT;
  }

  // private methods

  @Override
  final protected DataTableModel<E> buildDataTableModel(DataFetcher<E,K,PropertyPath<E>> dataFetcher,
                                                        List<? extends TableColumn<E,?>> columns)
  {
    if (dataFetcher instanceof EntityDataFetcher) {
      DataTableModel<E> dataTableModel = doBuildDataModel((EntityDataFetcher<E,K>) dataFetcher, columns);
      dataTableModel.addDataModelListener(new DataModelListener() {
        public void rowSelected(DataModelEvent event)
        {
          if (isEntityView()) {
            viewSelectedEntity();
          }
        }
      });
      return dataTableModel;
    }
    // for no-op (empty data model)
    return new InMemoryDataModel<E>(dataFetcher);
  }

  /**
   * Factory method to build the data model, allowing subclasses to determine
   * customize decision to use in-memory or virtual paging data model (or some
   * other data access strategy altogether).
   *
   * @param dataFetcher the DataFetcher associated with this SearchResults
   *          object.
   * @return a DataTableModel
   */
  protected DataTableModel<E> doBuildDataModel(EntityDataFetcher<E,K> dataFetcher,
                                               List<? extends TableColumn<E,?>> columns)
  {
    boolean allColumnsHavePropertyPaths = true;
    for (TableColumn<E,?> column : columns) {
      EntityColumn entityColumn = (EntityColumn) column;
      if (entityColumn.getPropertyPath() == null) {
        allColumnsHavePropertyPaths = false;
        break;
      }
    }

    DataTableModel<E> model;
    if (!allColumnsHavePropertyPaths ||
        (dataFetcher instanceof EntitySetDataFetcher &&
          ((EntitySetDataFetcher) dataFetcher).getDomain().size() <= ALL_IN_MEMORY_THRESHOLD)) {
      if (!allColumnsHavePropertyPaths) {
        log.debug("using InMemoryDataModel due to having some columns that do not map directly to database fields");
      }
      else {
        log.debug("using InMemoryDataModel due to domain size");
      }
      model = new InMemoryEntityDataModel<E>(dataFetcher);
    }
    else {
      log.debug("using VirtualPagingDataModel (sweet!)");
      model = new VirtualPagingEntityDataModel<K,E>(dataFetcher,
        new ValueReference<Integer>() { public Integer value() { return getRowsPerPage(); }
      });
    }
    return model;
  }

  private int findRowOfEntity(E entity)
  {
    if (getBaseDataTableModel() instanceof InMemoryDataModel) {
      DataTableModel model = (DataTableModel) getDataTableModel();
      List<E> data = (List<E>) model.getWrappedData();
      for (int i = 0; i < data.size(); i++) {
        if (data.get(i).equals(entity)) {
          return i;
        }
      }
    }
    return -1;
  }
}
