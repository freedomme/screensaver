// $HeadURL$
// $Id$
//
// Copyright © 2006, 2010, 2011, 2012 by the President and Fellows of Harvard College.
// 
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.ui.arch.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

import edu.harvard.med.screensaver.db.GenericEntityDAO;
import edu.harvard.med.screensaver.model.users.AdministratorUser;
import edu.harvard.med.screensaver.model.users.ChecklistItem;
import edu.harvard.med.screensaver.model.users.ChecklistItemEvent;
import edu.harvard.med.screensaver.model.users.ChecklistItemGroup;
import edu.harvard.med.screensaver.model.users.ChecklistItemsEntity;
import edu.harvard.med.screensaver.model.users.ScreensaverUserRole;
import edu.harvard.med.screensaver.ui.arch.view.EditResult;
import edu.harvard.med.screensaver.ui.arch.view.EditableEntityViewer;
import edu.harvard.med.screensaver.ui.arch.view.EditableEntityViewerBackingBean;
import edu.harvard.med.screensaver.ui.arch.view.aspects.UICommand;
import edu.harvard.med.screensaver.ui.users.UserViewer;

import org.apache.log4j.Logger;
import org.joda.time.LocalDate;

import com.google.common.collect.Lists;

/**
 * Checklist Items backing bean.
 *
 * @author <a mailto="andrew_tolopko@hms.harvard.edu">Andrew Tolopko</a>
 */
public class ChecklistItems<CIE extends ChecklistItemsEntity<Integer>> extends EditableEntityViewerBackingBean<CIE>
{
  private static Logger log = Logger.getLogger(ChecklistItems.class);
  
  private UserViewer _userViewer;

  private Map<ChecklistItemGroup,DataModel> _checklistItemDataModelMap;
  private Map<ChecklistItem,LocalDate> _newChecklistItemDatePerformed;
  private List<ChecklistItemGroup> _checklistItemGroups;

  
  protected ChecklistItems() {}
  
  public ChecklistItems(ChecklistItems thisProxy,
                        Class<CIE> entityClass,
                        UserViewer userViewer,
                        GenericEntityDAO dao)
  {
    super(thisProxy, entityClass, VIEW_USER, dao);
    _userViewer = userViewer;
    getIsPanelCollapsedMap().put("checklistItems", true);
  }

  /**
   * We override {@link EditableEntityViewer#isReadOnly()} since that method
   * will determines read-only status using ScreensaverUser entity, which is not
   * correct
   */
  @Override
  public boolean isReadOnly()
  {
    return !!!getScreensaverUser().isUserInRole(ScreensaverUserRole.USER_CHECKLIST_ITEMS_ADMIN);
  }
  
  @Override
  protected void initializeEntity(ChecklistItemsEntity entity)
  {
  }
  
  @Override
  protected void initializeViewer(ChecklistItemsEntity entity)
  {
    _checklistItemDataModelMap = null;
    _newChecklistItemDatePerformed = null;
  }
  
  @Override
  protected String postEditAction(EditResult editResult)
  {
    return _userViewer.reload();
  }

  public void setChecklistItemGroups(List<ChecklistItemGroup> checklistItemGroups)
  {
    _checklistItemGroups = checklistItemGroups;
  }
  
  public List<ChecklistItemGroup> getChecklistItemGroups()
  {
    return _checklistItemGroups;
  }

  public Map<ChecklistItemGroup,DataModel> getChecklistItemsDataModelMap()
  {
    if (_checklistItemDataModelMap == null) {
      _checklistItemDataModelMap = new HashMap<ChecklistItemGroup,DataModel>();
      for (ChecklistItemGroup group : getChecklistItemGroups()) {
        Map<ChecklistItem,ChecklistItemEvent> checklistItemsMap = new LinkedHashMap<ChecklistItem,ChecklistItemEvent>();
        List<ChecklistItem> checklistItems = getDao().findEntitiesByProperty(ChecklistItem.class, "checklistItemGroup", group);
        Collections.sort(checklistItems);
        for (ChecklistItem type : checklistItems) {
          checklistItemsMap.put(type, null);
        }
        ChecklistItemsEntity<?> entity = getEntity();
        SortedSet<ChecklistItemEvent> checklistItemEvents = entity.getChecklistItemEvents();
        for (ChecklistItemEvent checklistItemEvent : checklistItemEvents) {
          if (checklistItemEvent.getChecklistItem().getChecklistItemGroup() == group) {
            checklistItemsMap.put(checklistItemEvent.getChecklistItem(), checklistItemEvent);
          }
        }
        DataModel checklistItemDataModel = new ListDataModel(Lists.newArrayList(checklistItemsMap.entrySet()));
        _checklistItemDataModelMap.put(group, checklistItemDataModel);
      }
    }
    return _checklistItemDataModelMap;
  }
  
  public Map<ChecklistItem,LocalDate> getNewChecklistItemDatePerformed()
  {
    if (_newChecklistItemDatePerformed == null) {
      LocalDate today = new LocalDate();
      _newChecklistItemDatePerformed = new HashMap<ChecklistItem,LocalDate>();
      for (DataModel groupDataModel : getChecklistItemsDataModelMap().values()) {
        for (Map.Entry<ChecklistItem,ChecklistItemEvent> entry : (List<Map.Entry<ChecklistItem,ChecklistItemEvent>>) groupDataModel.getWrappedData()) {
          _newChecklistItemDatePerformed.put(entry.getKey(), today);
        }
      }
    }
    return _newChecklistItemDatePerformed;
  }

  @UICommand
  public String checklistItemActivated()
  {
    Map.Entry<ChecklistItem,ChecklistItemEvent> entry = (Map.Entry<ChecklistItem,ChecklistItemEvent>) getRequestMap().get("element");
    assert entry.getKey().isExpirable() && (entry.getValue() == null || entry.getValue().isExpiration());
    ChecklistItemEvent cie = getEntity().createChecklistItemActivationEvent(entry.getKey(),
                                               getNewChecklistItemDatePerformed().get(entry.getKey()),
                                               (AdministratorUser) getScreensaverUser());
    
    cie.getScreeningRoomUser().createUpdateActivity((AdministratorUser) getScreensaverUser(), "activated: '" + cie.getChecklistItem().getItemName() + "' checklist item with date performed: " + cie.getDatePerformed());

    _checklistItemDataModelMap = null;
    return REDISPLAY_PAGE_ACTION_RESULT;
  }

  @UICommand
  public String checklistItemDeactivated()
  {
    Map.Entry<ChecklistItem,ChecklistItemEvent> entry = (Map.Entry<ChecklistItem,ChecklistItemEvent>) getRequestMap().get("element");
    assert entry.getKey().isExpirable() && entry.getValue() != null && !entry.getValue().isExpiration();
    ChecklistItemEvent cie = entry.getValue().createChecklistItemExpirationEvent(getNewChecklistItemDatePerformed().get(entry.getKey()),
                                                        (AdministratorUser) getScreensaverUser());
    cie.getScreeningRoomUser().createUpdateActivity((AdministratorUser) getScreensaverUser(), "deactivated: '" + cie.getChecklistItem().getItemName() + "' checklist item with date performed: " + cie.getDatePerformed());

    _checklistItemDataModelMap = null;
    return REDISPLAY_PAGE_ACTION_RESULT;
  }

  @UICommand
  public String checklistItemCompleted()
  {
    Map.Entry<ChecklistItem,ChecklistItemEvent> entry = (Map.Entry<ChecklistItem,ChecklistItemEvent>) getRequestMap().get("element");
    assert !entry.getKey().isExpirable() && entry.getValue() == null;
    ChecklistItemEvent cie = getEntity().createChecklistItemActivationEvent(entry.getKey(),
                                                   getNewChecklistItemDatePerformed().get(entry.getKey()),
                                                   (AdministratorUser) getScreensaverUser());

    cie.getScreeningRoomUser().createUpdateActivity((AdministratorUser) getScreensaverUser(), "marked completed: '" + cie.getChecklistItem().getItemName() + "' checklist item with date performed: " + cie.getDatePerformed());
    _checklistItemDataModelMap = null;
    return REDISPLAY_PAGE_ACTION_RESULT;
  }

  @UICommand
  @SuppressWarnings("unchecked")
  public String checklistItemNotApplicable()
  {
    Map.Entry<ChecklistItem,ChecklistItemEvent> entry = 
      (Map.Entry<ChecklistItem,ChecklistItemEvent>) getRequestMap().get("element");

    assert entry.getValue() == null || entry.getKey().isExpirable() || entry.getValue().isExpiration();
    ChecklistItemEvent cie = getEntity().createChecklistItemNotApplicableEvent(entry.getKey(),
                                                  getNewChecklistItemDatePerformed().get(entry.getKey()),
                                                  (AdministratorUser) getScreensaverUser());
    cie.getScreeningRoomUser().createUpdateActivity((AdministratorUser) getScreensaverUser(), "marked n/a: '" + cie.getChecklistItem().getItemName() + "' checklist item with date performed: " + cie.getDatePerformed());
    
    _checklistItemDataModelMap = null;
    return REDISPLAY_PAGE_ACTION_RESULT;
  }
}
