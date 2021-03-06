// $HeadURL$
// $Id$
//
// Copyright © 2006, 2010, 2011, 2012 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.io.screenresults;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.hibernate.ScrollableResults;
import org.joda.time.DateTime;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import edu.harvard.med.screensaver.db.CellsDAO;
import edu.harvard.med.screensaver.db.DAOTransaction;
import edu.harvard.med.screensaver.db.GenericEntityDAO;
import edu.harvard.med.screensaver.db.HqlBuilderCallback;
import edu.harvard.med.screensaver.db.LibrariesDAO;
import edu.harvard.med.screensaver.db.Query;
import edu.harvard.med.screensaver.db.ScreenResultsDAO;
import edu.harvard.med.screensaver.db.SortDirection;
import edu.harvard.med.screensaver.io.libraries.LibraryCopyPlateListParserResult;
import edu.harvard.med.screensaver.model.DuplicateEntityException;
import edu.harvard.med.screensaver.model.Entity;
import edu.harvard.med.screensaver.model.Volume;
import edu.harvard.med.screensaver.model.cells.Cell;
import edu.harvard.med.screensaver.model.cells.ExperimentalCellInformation;
import edu.harvard.med.screensaver.model.cherrypicks.CherryPickRequest;
import edu.harvard.med.screensaver.model.libraries.Copy;
import edu.harvard.med.screensaver.model.libraries.CopyUsageType;
import edu.harvard.med.screensaver.model.libraries.Library;
import edu.harvard.med.screensaver.model.libraries.LibraryContentsVersion;
import edu.harvard.med.screensaver.model.libraries.LibraryType;
import edu.harvard.med.screensaver.model.libraries.LibraryWellType;
import edu.harvard.med.screensaver.model.libraries.Plate;
import edu.harvard.med.screensaver.model.libraries.PlateSize;
import edu.harvard.med.screensaver.model.libraries.Reagent;
import edu.harvard.med.screensaver.model.libraries.ReagentVendorIdentifier;
import edu.harvard.med.screensaver.model.libraries.SmallMoleculeReagent;
import edu.harvard.med.screensaver.model.libraries.Well;
import edu.harvard.med.screensaver.model.libraries.WellCopy;
import edu.harvard.med.screensaver.model.libraries.WellKey;
import edu.harvard.med.screensaver.model.meta.RelationshipPath;
import edu.harvard.med.screensaver.model.screenresults.AssayWell;
import edu.harvard.med.screensaver.model.screenresults.DataColumn;
import edu.harvard.med.screensaver.model.screenresults.ResultValue;
import edu.harvard.med.screensaver.model.screenresults.ScreenResult;
import edu.harvard.med.screensaver.model.screens.ProjectPhase;
import edu.harvard.med.screensaver.model.screens.Screen;
import edu.harvard.med.screensaver.model.screens.ScreenType;
import edu.harvard.med.screensaver.model.screens.StudyType;
import edu.harvard.med.screensaver.model.users.LabHead;

/**
 * A DAO implementation that can be used in a database-free environment.
 *
 * @motivation for command-line ScreenResultImporter, when used in parse-only
 *             (non-import) mode.
 * @motivation testing ScreenResultImporter
 *
 * @author <a mailto="andrew_tolopko@hms.harvard.edu">Andrew Tolopko</a>
 * @author <a mailto="john_sullivan@hms.harvard.edu">John Sullivan</a>
 */
public class MockDaoForScreenResultImporter implements GenericEntityDAO, ScreenResultsDAO, LibrariesDAO, CellsDAO
{
  private static final Logger log = Logger.getLogger(MockDaoForScreenResultImporter.class);

  private static final String NAME_OF_PSEUDO_LIBRARY_FOR_IMPORT = "PseudoLibraryForScreenResultImport";

  private static final Set<String> EMPTY_WELL_NAMES = Sets.newHashSet("A01", "A04", "A07");

  private Library _library;

  public MockDaoForScreenResultImporter()
  {
    _library =
      new Library(null,
                  NAME_OF_PSEUDO_LIBRARY_FOR_IMPORT,
                  NAME_OF_PSEUDO_LIBRARY_FOR_IMPORT,
                  ScreenType.SMALL_MOLECULE,
                  LibraryType.OTHER,
                  1,
                  Integer.MAX_VALUE,
                  PlateSize.WELLS_384);
  }

  public Well findWell(WellKey wellKey)
  {
    try {
      return _library.createWell(wellKey, EMPTY_WELL_NAMES.contains(wellKey.getWellName()) ? LibraryWellType.EMPTY : LibraryWellType.EXPERIMENTAL);
    }
    catch (DuplicateEntityException e) {
      for (Well well : _library.getWells()) {
        if (well.getWellKey().equals(wellKey)) {
          return well;
        }
      }
      return null;
    }
  }

  public Collection<String> findAllVendorNames()
  {
    return new ArrayList<String>();
  }

  public Library findLibraryWithPlate(Integer plateNumber)
  {
    return _library;
  }

  public Set<Reagent> findReagents(ReagentVendorIdentifier rvi, boolean latestReleasedVersionsOnly)
  {
    return Sets.newHashSet();
  }

  public void deleteLibraryContentsVersion(LibraryContentsVersion libraryContentsVersion)
  {
  }

  public Set<Well> findWellsForPlate(int plate)
  {
    return null;
  }

  public void loadOrCreateWellsForLibrary(Library library)
  {
  }

  public void deleteEntity(Entity entity)
  {
  }

  public void doInTransaction(DAOTransaction daoTransaction)
  {
    daoTransaction.runTransaction();
  }

  public <E extends Entity> List<E> findAllEntitiesOfType(Class<E> entityClass)
  {
    return null;
  }

  public <E extends Entity> List<E> findAllEntitiesOfType(Class<E> entityClass,
                                                          boolean readOnly,
                                                          RelationshipPath<E> relationship)
  {
    return null;
  }

  public <E extends Entity> List<E> findAllEntitiesOfType(Class<E> entityClass,
                                                          boolean readOnly)
  {
    return null;
  }

  public <E extends Entity> List<E> findEntitiesByHql(Class<E> entityClass, String hql, Object... hqlParameters)
  {
    return null;
  }

  public <E extends Entity> List<E> findEntitiesByProperty(Class<E> entityClass, String propertyName, Object propertyValue)
  {
    return null;
  }

  public <E extends Entity> List<E> findEntitiesByProperty(Class<E> entityClass,
                                                           String propertyName,
                                                           Object propertyValue,
                                                           boolean readOnly,
                                                           RelationshipPath<E> relationship)
  {
    return null;
  }

  public <E extends Entity> List<E> findEntitiesByProperty(Class<E> entityClass,
                                                           String propertyName,
                                                           Object propertyValue,
                                                           boolean readOnly)
  {
    return null;
  }

  public <E extends Entity<K>,K extends Serializable> E findEntityById(Class<E> entityClass, K id)
  {
    return null;
  }

  public <E extends Entity<K>,K extends Serializable> E findEntityById(Class<E> entityClass,
                                                                       K id,
                                                                       boolean readOnly,
                                                                       RelationshipPath<E> relationship)
  {
    return null;
  }

  public <E extends Entity<K>,K extends Serializable> E findEntityById(Class<E> entityClass,
                                                                       K id,
                                                                       boolean readOnly)
  {
    return null;
  }

  @SuppressWarnings("unchecked")
  public <E extends Entity> E findEntityByProperty(Class<E> entityClass, String propertyName, Object propertyValue)
  {
    if (entityClass.equals(Screen.class) && propertyName.equals(Screen.facilityId.getPropertyName())) {
      LabHead user = new LabHead("First", "Last", null);
      Screen screen = new Screen(null,
                                 propertyValue.toString(),
                                 user,
                                 user,
                                 ScreenType.SMALL_MOLECULE,
                                 StudyType.IN_VITRO,
                                 ProjectPhase.PRIMARY_SCREEN,
                                 "title");
      return (E) screen;
    }
    return null;
  }

  public <E extends Entity> E findEntityByProperty(Class<E> entityClass,
                                                   String propertyName,
                                                   Object propertyValue,
                                                   boolean readOnly,
                                                   RelationshipPath<E> relationship)
  {
    return null;
  }

  public <E extends Entity> E findEntityByProperty(Class<E> entityClass,
                                                   String propertyName,
                                                   Object propertyValue,
                                                   boolean readOnly)
  {
    return null;
  }

  public void flush()
  {
  }

  public <E extends Entity> void need(E entity, RelationshipPath<E> relationship)
  {
  }

  public <E extends Entity> void needReadOnly(E entity, RelationshipPath<E> relationship)
  {
  }

  public void saveOrUpdateEntity(Entity entity)
  {
  }

  public void persistEntity(Entity entity)
  {
  }

  public <E extends Entity> E reattachEntity(E entity)
  {
    return null;
  }

  public <E extends Entity> E reloadEntity(E entity)
  {
    return null;
  }

  public <E extends Entity> E reloadEntity(E entity, boolean readOnly, RelationshipPath<E> relationship)
  {
    return null;
  }

  public <E extends Entity> E reloadEntity(E entity, boolean readOnly)
  {
    return null;
  }

  public void deleteScreenResult(ScreenResult screenResult)
  {
  }

  public Map<WellKey,ResultValue> findResultValuesByPlate(Integer plateNumber, DataColumn col)
  {
    return null;
  }

  public Map<WellKey,List<ResultValue>> findResultValuesByPlate(Integer plateNumber, List<DataColumn> col)
  {
    return null;
  }

  public Map<WellKey,List<ResultValue>> findSortedResultValueTableByRange(List<DataColumn> selectedCols,
                                                                          int sortBy,
                                                                          SortDirection sortDirection,
                                                                          int fromIndex,
                                                                          Integer rowsToFetch,
                                                                          DataColumn positivesOnlyCol,
                                                                          Integer plateNumber)
  {
    return null;
  }

  public Map<Copy,Volume> findRemainingVolumesInWellCopies(Well well, CopyUsageType copyUsageType)
  {
    return Maps.newHashMap();
  }

  public <E extends Entity> List<E> findEntitiesByProperties(Class<E> entityClass, Map<String,Object> name2Value)
  {
    return null;
  }

  public <E extends Entity> List<E> findEntitiesByProperties(Class<E> entityClass,
                                                             Map<String,Object> name2Value,
                                                             boolean readOnly,
                                                             RelationshipPath<E> relationshipIn)
  {
    return null;
  }

  public <E extends Entity> List<E> findEntitiesByProperties(Class<E> entityClass,
                                                             Map<String,Object> name2Value,
                                                             boolean readOnly)
  {
    return null;
  }

  public <E extends Entity> E findEntityByProperties(Class<E> entityClass, Map<String,Object> name2Value)
  {
    return null;
  }

  public <E extends Entity> E findEntityByProperties(Class<E> entityClass,
                                                     Map<String,Object> name2Value,
                                                     boolean readOnly,
                                                     RelationshipPath<E> relationship)
  {
    return null;
  }

  public <E extends Entity> E findEntityByProperties(Class<E> entityClass,
                                                     Map<String,Object> name2Value,
                                                     boolean readOnly)
  {
    return null;
  }

  public List<WellCopy> findWellCopyVolumes(Library library)
  {
    return null;
  }

  public List<WellCopy> findWellCopyVolumes(Copy copy)
  {
    return null;
  }

  public List<WellCopy> findWellCopyVolumes(Integer plateNumber)
  {
    return null;
  }

  public Collection<WellCopy> findWellCopyVolumes(Copy copy, Integer plateNumber)
  {
    return null;
  }

  public Collection<WellCopy> findWellCopyVolumes(WellKey wellKey)
  {
    return null;
  }

  public Collection<WellCopy> findWellCopyVolumes(CherryPickRequest cherryPickRequest,
                                                  boolean forUnfufilledLabCherryPicksOnly)
  {
    return null;
  }

  public List<Library> findLibrariesOfType(LibraryType[] libraryTypes,
                                           ScreenType[] screenTypes)
  {
    return null;
  }

  public boolean isPlateRangeAvailable(Integer startPlate, Integer endPlate)
  {
    return false;
  }

  public void clear() {}

  public <T> List<T> runQuery(Query<T> query)
  {
    return null;
  }

  public ScrollableResults runScrollQuery(final edu.harvard.med.screensaver.db.ScrollQuery query)
  {
    return null;
  }

  public AssayWell findAssayWell(ScreenResult screenResult, WellKey wellKey)
  {
    return null;
  }

  @SuppressWarnings("unchecked")
  public Entity mergeEntity(Entity entity)
  {
    return null;
  }

  @Override
  public int countExperimentalWells(int startPlate, int endPlate)
  {
    return 0;
  }

  @Override
  public Plate findPlate(int plateNumber, String copyName)
  {
    return null;
  }
   
  @Override
  public List<DataColumn> findMutualPositiveColumns(ScreenResult sr)
  {
    return null;
  }   
  
  @Override
  public ScreenResult getLatestScreenResult()
  {
    return null;
  }

  @Override
  public Set<ScreenType> findScreenTypesForReagents(Set<String> reagentIds)
  {
    return null;
  }

  @Override
  public Set<ScreenType> findScreenTypesForWells(Set<WellKey> wellKeys)
  {
    return null;
  }

  @Override
  public List<DataColumn> findMutualPositiveColumns(HqlBuilderCallback hqlBuilderCallback)
  {
    return null;
  }

  @Override
  public <E extends Entity,T> Set<T> findDistinctPropertyValues(Class<E> entityClass, String propertyName)
  {
    return Sets.newHashSet();
  }

  public Set<Integer> queryForPlateIds(LibraryCopyPlateListParserResult parserResult)
  {
    return Sets.newHashSet();
  }

  @Override
  public void calculateCopyScreeningStatistics(Collection<Copy> copies)
  {
  }

  @Override
  public void calculateCopyVolumeStatistics(Collection<Copy> copies)
  {
  }

  @Override
  public void calculatePlateScreeningStatistics(Collection<Plate> plates)
  {
  }

  @Override
  public void calculatePlateVolumeStatistics(Collection<Plate> plates)
  {
  }
  
  public Set<SmallMoleculeReagent> findReagents(String facilityId,
                                                Integer saltId,
                                                Integer batchId,
                                                boolean latestReleasedVersionsOnly)
  {
    return Sets.newHashSet();
  }

	@Override
	public Set<WellKey> findWellKeysForReagentVendorID(String facilityVendorId, int limitSize) {
		// TODO Auto-generated method stub
		return null;
	}

  @Override
  public Set<WellKey> findWellKeysForCompoundName(final String compoundSearchName, int limitSize)
  {
    return Sets.newHashSet();
  }

  @Override
  public Well findCanonicalReagentWell(String facilityId,
                                       Integer saltId,
                                       Integer facilityBatchId)
  {
    return null;
  }

  @Override
  public Set<String> findCanonicalReagentWellIds(Set<String> wellIds)
  {
    return null;
  }

  @Override
  public Set<Well> findAllCanonicalReagentWells()
  {
    return null;
  }

	@Override
	public SortedSet<ExperimentalCellInformation> findCellExperimentsFromCLOIds(String[] split) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Set<Cell> findCellsByCloIds(String[] cloIds) {
		return null;
	}
	
	public 	Set<Cell> findCellsByHMSID(String[] split){
		return null;
	}
	
	public Set<String> findCanonicalCompoundsScreenedByWellId(Cell cell) 
	{
		return null;
	}

	@Override
	public DateTime getLatestDataLoadingDate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Well> findWells(String facilityId, Integer saltId, Integer facilityBatchId) {
		// TODO Auto-generated method stub
		return null;
	}

}
