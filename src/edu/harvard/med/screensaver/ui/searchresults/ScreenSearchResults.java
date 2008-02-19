// $HeadURL$
// $Id$

// Copyright 2006 by the President and Fellows of Harvard College.

// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.ui.searchresults;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import edu.harvard.med.screensaver.db.GenericEntityDAO;
import edu.harvard.med.screensaver.db.datafetcher.AllEntitiesOfTypeDataFetcher;
import edu.harvard.med.screensaver.db.datafetcher.EntitySetDataFetcher;
import edu.harvard.med.screensaver.db.hibernate.HqlBuilder;
import edu.harvard.med.screensaver.model.PropertyPath;
import edu.harvard.med.screensaver.model.RelationshipPath;
import edu.harvard.med.screensaver.model.screenresults.ScreenResult;
import edu.harvard.med.screensaver.model.screens.Screen;
import edu.harvard.med.screensaver.model.screens.ScreenType;
import edu.harvard.med.screensaver.model.screens.StatusItem;
import edu.harvard.med.screensaver.model.screens.StatusValue;
import edu.harvard.med.screensaver.model.screens.Study;
import edu.harvard.med.screensaver.model.users.ScreeningRoomUser;
import edu.harvard.med.screensaver.model.users.ScreensaverUser;
import edu.harvard.med.screensaver.model.users.ScreensaverUserRole;
import edu.harvard.med.screensaver.ui.screens.ScreenViewer;
import edu.harvard.med.screensaver.ui.table.Criterion.Operator;
import edu.harvard.med.screensaver.ui.table.column.TableColumn;
import edu.harvard.med.screensaver.ui.table.column.entity.DateEntityColumn;
import edu.harvard.med.screensaver.ui.table.column.entity.EntityColumn;
import edu.harvard.med.screensaver.ui.table.column.entity.EnumEntityColumn;
import edu.harvard.med.screensaver.ui.table.column.entity.IntegerEntityColumn;
import edu.harvard.med.screensaver.ui.table.column.entity.TextEntityColumn;
import edu.harvard.med.screensaver.ui.table.column.entity.UserNameColumn;
import edu.harvard.med.screensaver.util.CollectionUtils;
import edu.harvard.med.screensaver.util.NullSafeComparator;


/**
 * A {@link SearchResults} for {@link Screen Screens}.
 *
 * @author <a mailto="john_sullivan@hms.harvard.edu">John Sullivan</a>
 * @author <a mailto="andrew_tolopko@hms.harvard.edu">Andrew Tolopko</a>
 */
public class ScreenSearchResults extends EntitySearchResults<Screen,Integer>
{

  // instance fields

  private ScreenViewer _screenViewer;
  protected GenericEntityDAO _dao;


  // public constructor

  /**
   * @motivation for CGLIB2
   */
  protected ScreenSearchResults()
  {
  }

  public ScreenSearchResults(ScreenViewer screenViewer,
                             GenericEntityDAO dao)
  {
    _screenViewer = screenViewer;
    _dao = dao;
  }

  public void searchScreensForUser(ScreensaverUser screensaverUser)
  {
    Set<Screen> screens = new HashSet<Screen>();
    if (getScreensaverUser() instanceof ScreeningRoomUser) {
      ScreeningRoomUser screener = (ScreeningRoomUser) getScreensaverUser();
      screens.addAll(screener.getScreensHeaded());
      screens.addAll(screener.getScreensLed());
      screens.addAll(screener.getScreensCollaborated());
      if (screens.size() == 0) {
        showMessage("screens.noScreensForUser");
      }
      else {
        initialize(new EntitySetDataFetcher<Screen,Integer>(Screen.class, CollectionUtils.<Integer>entityIds(screens), _dao));
        // default to descending sort order on screen number
        getColumnManager().setSortAscending(false);
      }

    }
  }

  public void searchAllScreens()
  {
    initialize(new AllEntitiesOfTypeDataFetcher<Screen,Integer>(Screen.class, _dao) {
      @Override
      protected void addDomainRestrictions(HqlBuilder hql,
                                           Map<RelationshipPath<Screen>,String> path2Alias)
      {
        super.addDomainRestrictions(hql, path2Alias);
        hql.where(getRootAlias(), "screenNumber", Operator.LESS_THAN, Study.MIN_STUDY_NUMBER);
      }
    });
    // default to descending sort order on screen number
    getColumnManager().setSortAscending(false);
  }

  public void searchScreens(Set<Screen> screens)
  {
    Set<Integer> screenIds = new HashSet<Integer>();
    for (Screen screen : screens) {
      screenIds.add(screen.getEntityId());
    }
    initialize(new EntitySetDataFetcher<Screen,Integer>(Screen.class, screenIds, _dao));
    // default to descending sort order on screen number
    getColumnManager().setSortAscending(false);
  }


  // implementations of the SearchResults abstract methods

  @SuppressWarnings("unchecked")
  protected List<? extends TableColumn<Screen,?>> buildColumns()
  {
    ArrayList<EntityColumn<Screen,?>> columns = new ArrayList<EntityColumn<Screen,?>>();
    columns.add(new IntegerEntityColumn<Screen>(
      new PropertyPath(Screen.class, "screenNumber"),
      "Screen Number", "The screen number", TableColumn.UNGROUPED) {
      @Override
      public Integer getCellValue(Screen screen) { return screen.getScreenNumber(); }

      @Override
      public Object cellAction(Screen screen) { return viewCurrentEntity(); }

      @Override
      public boolean isCommandLink() { return true; }
    });
    columns.add(new TextEntityColumn<Screen>(
      new PropertyPath(Screen.class, "title"),
      "Title", "The title of the screen", TableColumn.UNGROUPED) {
      @Override
      public String getCellValue(Screen screen) { return screen.getTitle(); }
    });
    columns.add(new UserNameColumn<Screen>(
      new RelationshipPath(Screen.class, "labHead"),
      "Lab Head", "The head of the lab performing the screen", TableColumn.UNGROUPED) {
      @Override
      public ScreensaverUser getUser(Screen screen) { return screen.getLabHead(); }
    });
    columns.add(new UserNameColumn<Screen>(
      new RelationshipPath(Screen.class, "leadScreener"),
      "Lead Screener", "The scientist primarily responsible for running the screen", TableColumn.UNGROUPED) {
      @Override
      public ScreensaverUser getUser(Screen screen) { return screen.getLeadScreener(); }
    });
    columns.add(new EnumEntityColumn<Screen,ScreenResultAvailability>(
      new RelationshipPath(Screen.class, "screenResult"),
      "Screen Result",
      "'available' if the screen result is loaded into Screensaver and viewable by the current user;" +
      " 'not shared' if loaded but not viewable by the current user; otherwise 'none'",
      TableColumn.UNGROUPED, ScreenResultAvailability.values()) {
      @Override
      public ScreenResultAvailability getCellValue(Screen screen)
      {
        if (screen.getScreenResult() == null) {
          return ScreenResultAvailability.NONE;
        }
        else if (screen.getScreenResult().isRestricted()) {
          return ScreenResultAvailability.NOT_SHARED;
        }
        else {
          return ScreenResultAvailability.AVAILABLE;
        }
      }

      @Override
      protected Comparator<Screen> getAscendingComparator()
      {
        return new Comparator<Screen>() {
          private NullSafeComparator<ScreenResult> _srComparator =
            new NullSafeComparator<ScreenResult>(true)
            {
            @Override
            protected int doCompare(ScreenResult sr1, ScreenResult sr2)
            {
              if (!sr1.isRestricted() && sr2.isRestricted()) {
                return -1;
              }
              if (sr1.isRestricted() && !sr2.isRestricted()) {
                return 1;
              }
              return sr1.getScreen().getScreenNumber().compareTo(sr2.getScreen().getScreenNumber());
            }
            };

            public int compare(Screen s1, Screen s2) {
              return _srComparator.compare(s1.getScreenResult(),
                                           s2.getScreenResult());
            }
        };
      }
    });
    columns.add(new EnumEntityColumn<Screen, ScreenType>(
      new PropertyPath(Screen.class, "screenType"),
      "Screen Type", "'RNAi' or 'Small Molecule'", TableColumn.UNGROUPED, ScreenType.values()) {
      @Override
      public ScreenType getCellValue(Screen screen) { return screen.getScreenType(); }
    });
    columns.add(new EnumEntityColumn<Screen,StatusValue>(
      new PropertyPath(Screen.class, "statusItems", "statusValue"),
      "Status", "The current status of the screen, e.g., 'Completed', 'Ongoing', 'Pending', etc.", TableColumn.UNGROUPED, StatusValue.values()) {
      @Override
      public StatusValue getCellValue(Screen screen)
      {
        SortedSet<StatusItem> statusItems = screen.getSortedStatusItems();
        if (statusItems.size() == 0) {
          return null;
        }
        StatusItem statusItem = statusItems.last();
        return statusItem.getStatusValue();
      }
    });
    columns.get(columns.size() - 1).setVisible(showStatusFields());
    columns.add(new DateEntityColumn<Screen>(
      new PropertyPath(Screen.class, "statusItems", "statusDate"),
      "Status Date", "The date of the most recent change of status for the screen", TableColumn.UNGROUPED) {
      @Override
      protected Date getDate(Screen screen) {
        SortedSet<StatusItem> statusItems = screen.getSortedStatusItems();
        return statusItems.size() == 0 ? null : statusItems.last().getStatusDate();
      }
    });
    columns.get(columns.size() - 1).setVisible(showStatusFields());

//    TableColumnManager<Screen> columnManager = getColumnManager();
//    columnManager.addCompoundSortColumns(columnManager.getColumn("Lab Head"),
//                                         columnManager.getColumn("Lead Screener"),
//                                         columnManager.getColumn("Screen Number"));
//    columnManager.addCompoundSortColumns(columnManager.getColumn("Lead Screener"),
//                                         columnManager.getColumn("Lab Head"),
//                                         columnManager.getColumn("Screen Number"));

    return columns;
  }

  @Override
  protected void setEntityToView(Screen screen)
  {
    _screenViewer.viewScreen(screen);
  }

  private boolean showStatusFields()
  {
    return isUserInRole(ScreensaverUserRole.SCREENS_ADMIN) ||
      isUserInRole(ScreensaverUserRole.READ_EVERYTHING_ADMIN);
  }
}
