// $HeadURL$
// $Id$
//
// Copyright © 2006, 2010, 2011, 2012 by the President and Fellows of Harvard College.
// 
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.ui.arch.datatable.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.faces.model.DataModelListener;
import javax.faces.model.ListDataModel;

import org.apache.log4j.Logger;

import edu.harvard.med.screensaver.db.Criterion;
import edu.harvard.med.screensaver.db.CriterionMatchException;
import edu.harvard.med.screensaver.db.SortDirection;
import edu.harvard.med.screensaver.db.datafetcher.DataFetcher;
import edu.harvard.med.screensaver.ui.arch.datatable.DataTableModelType;
import edu.harvard.med.screensaver.ui.arch.datatable.column.CompoundColumnComparator;
import edu.harvard.med.screensaver.ui.arch.datatable.column.TableColumn;


public class InMemoryDataModel<R> extends DataTableModel<R>
{
  private static Logger log = Logger.getLogger(InMemoryEntityDataModel.class);
  private static final Comparator<Object> PHYSICAL_ORDER_COMPARATOR = new Comparator<Object>() {
    public int compare(Object o1, Object o2){ return 0; }
  };
    
  protected DataFetcher<R,?,?> _dataFetcher;
  protected List<R> _unfilteredData;
  private ListDataModel _baseModel = new ListDataModel();

  public InMemoryDataModel(DataFetcher<R,?,?> dataFetcher)
  {
    _dataFetcher = dataFetcher;
  }

  @Override
  public DataTableModelType getModelType()
  {
    return DataTableModelType.IN_MEMORY;
  }

  @Override
  public void fetch(List<? extends TableColumn<R,?>> columns)
  {
    _unfilteredData = _dataFetcher.fetchAllData();
    setWrappedData(_unfilteredData);
  }

  public void filter(List<? extends TableColumn<R,?>> columns)
  {
    if (_unfilteredData == null) {
      throw new IllegalStateException("fetch() must be called first");
    }
    // TODO: consider operator types to optimize this (can short-circuit <, <=, >, >=, can do binary search with equals)
    ArrayList<R> filteredData = new ArrayList<R>(_unfilteredData);
    for (TableColumn<R,?> column : columns) {
      try {
        if (column.hasCriteria()) {
          Iterator<? extends R> rowIter = filteredData.iterator();
          while (rowIter.hasNext()) {
            List<? extends Criterion<?>> criteria = column.getCriteria();
            if (!matches(column.getCellValue(rowIter.next()), criteria)) {
              rowIter.remove();
            }
          }
        }
      }
      catch (CriterionMatchException e) {
        e.getCriterion().reset();
        // TODO: reportApplicationError(e);
      }
    }
    setWrappedData(filteredData);
  }

  public void sort(List<? extends TableColumn<R,?>> sortColumns, SortDirection sortDirection)
  {
    if (_unfilteredData == null) {
      throw new IllegalStateException("fetch() must be called first");
    }
    Collections.sort(getData(), getComparator(sortColumns, sortDirection));
  }

  public int getFilteredRowCount()
  {
    return getRowCount();
  }

  /**
   * Get a comparator that can be used to sort rows. If there exists a compound
   * sorting order for the primary sort column, the comparator will take this
   * into account.
   *
   * @return the sort columns
   */
  @SuppressWarnings("unchecked")
  public Comparator<R> getComparator(List<? extends TableColumn<R,?>> sortColumns, SortDirection sortDirection)
  {
    if (sortColumns == null || sortColumns.size()== 0) {
      return (Comparator<R>) new Comparator<R>() {
        public int compare(R e1, R e2){ return 0; }
      };
    }
    if (sortColumns.size() == 1) {
      return sortColumns.get(0).getComparator(sortDirection);
    }
    return new CompoundColumnComparator<R>(sortColumns, sortDirection);
  }

  @SuppressWarnings("unchecked")
  public List<R> getData()
  {
    return (List<R>) getWrappedData();
  }

  /**
   * Returns whether the specified entity matches the criterion for the column.
   * @param list
   * @param e the entity to be matched
   * @return boolean
   */
  private boolean matches(Object datum, List<? extends Criterion<?>> criteria)
  {
    for (Criterion<?> criterion : criteria) {
      if (!criterion.matches(datum)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int getRowCount()
  {
    return _baseModel.getRowCount();
  }

  @Override
  public Object getRowData()
  {
    return _baseModel.getRowData();
  }

  @Override
  public int getRowIndex()
  {
    return _baseModel.getRowIndex();
  }

  @Override
  public Object getWrappedData()
  {
    return _baseModel.getWrappedData();
  }

  @Override
  public boolean isRowAvailable()
  {
    return _baseModel.isRowAvailable();
  }

  @Override
  public void setRowIndex(int rowIndex)
  {
    _baseModel.setRowIndex(rowIndex);
  }

  @Override
  public void setWrappedData(Object data)
  {
    _baseModel.setWrappedData(data);
  }
  
  @Override
  public void addDataModelListener(DataModelListener listener)
  {
    _baseModel.addDataModelListener(listener);
  }
  
  @Override
  public DataModelListener[] getDataModelListeners()
  {
    return _baseModel.getDataModelListeners();
  }

  @Override
  public Iterator<R> iterator()
  {
    return new Iterator<R>() {
      private int row = 0;

      @Override
      public boolean hasNext()
      {
        return row < getRowCount();
      }

      @Override
      public R next()
      {
        setRowIndex(row++);
        return (R) getRowData();
      }

      @Override
      public void remove()
      {
        throw new UnsupportedOperationException();
      }
    };
  }

}