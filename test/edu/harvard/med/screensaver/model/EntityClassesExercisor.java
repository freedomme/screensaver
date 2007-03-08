// $HeadURL$
// $Id$
//
// Copyright 2006 by the President and Fellows of Harvard College.
// 
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.model;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.harvard.med.screensaver.AbstractSpringTest;
import edu.harvard.med.screensaver.model.libraries.Well;
import edu.harvard.med.screensaver.model.libraries.WellKey;
import edu.harvard.med.screensaver.model.screens.CherryPickRequest;
import edu.harvard.med.screensaver.model.screens.LibraryScreening;
import edu.harvard.med.screensaver.model.screens.RNAiCherryPickRequest;
import edu.harvard.med.screensaver.model.screens.Screening;
import edu.harvard.med.screensaver.model.screens.ScreeningRoomActivity;

import org.apache.commons.lang.time.DateUtils;

/**
 * Exercise the entity classes.
 */
abstract class EntityClassesExercisor extends AbstractSpringTest
{
  
  private static final String [] entityPackages = {
    "edu.harvard.med.screensaver.model",
    "edu.harvard.med.screensaver.model.derivatives",
    "edu.harvard.med.screensaver.model.libraries",
    "edu.harvard.med.screensaver.model.screenresults",
    "edu.harvard.med.screensaver.model.screens",
    "edu.harvard.med.screensaver.model.users"
  };
  
  private static String STRING_TEST_VALUE_PREFIX = "test:";
  private static int STRING_TEST_VALUE_RADIX = 36;
  
  private Integer _integerTestValue = 77;
  private double  _doubleTestValue = 77.1;
  private boolean _booleanTestValue = true;
  private int     _stringTestValueIndex = Integer.parseInt("antz", STRING_TEST_VALUE_RADIX);
  private long    _dateMilliseconds = 0;
  private int     _vocabularyTermCounter = 0;
  private int     _wellNameTestValueIndex = 0;
  private WellKey _wellKeyTestValue = new WellKey("00001:A01");
  
  @SuppressWarnings("unchecked")
  protected Object getTestValueForType(Class type)
  {
    if (type.equals(Integer.class)) {
      _integerTestValue += 1;
      return _integerTestValue;
    }
    if (type.equals(Double.class)) {
      _doubleTestValue *= 1.32;
      return new Double(new Double(_doubleTestValue * 1000).intValue() / 1000);
    }
    if (type.equals(BigDecimal.class)) {
      BigDecimal val = new BigDecimal(((Double) getTestValueForType(Double.class)).doubleValue());
      // 2 is the default scale used in our Hibernate mapping, not sure how to change it via xdoclet
      val = val.setScale(2);
      return val;
    }
    if (type.equals(Boolean.TYPE)) {
      _booleanTestValue = ! _booleanTestValue;
      return _booleanTestValue;
    }
    if (type.equals(String.class)) {
      return STRING_TEST_VALUE_PREFIX + Integer.toString(++_stringTestValueIndex, STRING_TEST_VALUE_RADIX);
    }
    if (type.equals(Date.class)) {
      _dateMilliseconds += 1000 * 60 * 60 * 24 * 1.32;
      return DateUtils.round(new Date(_dateMilliseconds), Calendar.DATE);
    }
    if (AbstractEntity.class.isAssignableFrom(type)) {
      return newInstance((Class<AbstractEntity>) type);
    }
    if (VocabularyTerm.class.isAssignableFrom(type)) {
      try {
        Method valuesMethod = type.getMethod("values");
        Object values = (Object) valuesMethod.invoke(null);
        int numValues = Array.getLength(values);
        int valuesIndex = ++ _vocabularyTermCounter % numValues;
        return Array.get(values, valuesIndex);
      }
      catch (Exception e) {
        e.printStackTrace();
        fail("vocabulary term test value code threw an exception");
      }
    }    
    if (WellKey.class.isAssignableFrom(type)) {
      return nextWellKey(_wellKeyTestValue);
    }
    throw new IllegalArgumentException(
      "can't create test values for type: " + type.getName());
  }
  
  private Object nextWellKey(WellKey wellKey)
  {
    int col = wellKey.getColumn() + 1;
    int row = wellKey.getRow();
    int plateNumber = wellKey.getPlateNumber();
    if (col >= Well.PLATE_COLUMNS) {
      col = 0;
      ++row;
    }
    if (row >= Well.PLATE_ROWS) {
      row = 0;
      ++plateNumber;
    }
    _wellKeyTestValue = new WellKey(plateNumber, row, col);
    return _wellKeyTestValue;
  }

  private Object getTestValueForWellName()
  {
    String wellName = String.format("%c%02d",
                                    'A' + (_wellNameTestValueIndex / 24),
                                    (_wellNameTestValueIndex % 24) + 1);
    ++_wellNameTestValueIndex;
    return wellName;
  }

  protected static interface EntityClassExercizor
  {
    void exercizeEntityClass(Class<AbstractEntity> entityClass);
  }
  
  protected void exercizeEntityClasses(EntityClassExercizor exercizor)
  {
    for (Class<AbstractEntity> entityClass : getEntityClasses()) {
      exercizor.exercizeEntityClass(entityClass);
    }
  }

  @SuppressWarnings("unchecked")
  protected List<Class<AbstractEntity>> getEntityClasses()
  {
    List<Class<AbstractEntity>> entityClasses = new ArrayList<Class<AbstractEntity>>();
    for (String entityPackage : entityPackages) {
      String packagePath = "/" + entityPackage.replace('.', '/');
      URL packageURL = getClass().getResource(packagePath);
      File directory = new File(packageURL.getFile().replace("%20", " "));
      if (! directory.exists()) {
        throw new RuntimeException("directory " + directory + " doesn't exist");
      }
      for (String file : directory.list()) {
        if (! file.endsWith(".class")) {
          continue;
        }
        
        // skip inner classes of AbstractEntityTest
        if (entityPackage.equals("edu.harvard.med.screensaver.model") &&
            file.startsWith("AbstractEntityTest")) {
          continue;
        }
        
        String classname = file.substring(0, file.length() - 6); // remove the .class extension
        Class entityClass;
        try {
          entityClass = Class.forName(entityPackage + "." + classname);
        }
        catch (ClassNotFoundException e) {
          continue;
        }
        
        // skip abstract classes
        if (Modifier.isAbstract(entityClass.getModifiers())) {
          continue;
        }
        //if (AbstractEntity.class.equals(entityClass)) {
        //  continue;
        //}
        
        if (AbstractEntity.class.isAssignableFrom(entityClass)) {
          entityClasses.add((Class<AbstractEntity>) entityClass);
        }
      }
    }
    return entityClasses;
  }

  private static Map<Class<? extends AbstractEntity>,Class<? extends AbstractEntity>> _concreteStandinMap =
      new HashMap<Class<? extends AbstractEntity>,Class<? extends AbstractEntity>>();
  static {
    _concreteStandinMap.put(Screening.class, LibraryScreening.class);
    _concreteStandinMap.put(ScreeningRoomActivity.class, LibraryScreening.class);
    _concreteStandinMap.put(CherryPickRequest.class, RNAiCherryPickRequest.class);
  }
  
  protected AbstractEntity newInstance(Class<? extends AbstractEntity> entityClass) {
    if (Modifier.isAbstract(entityClass.getModifiers())) {
      Class<? extends AbstractEntity> concreteStandin =
        _concreteStandinMap.get(entityClass);
      return newInstance(concreteStandin);
    }
    Constructor constructor = getMaxArgConstructor(entityClass);
    Object[] arguments = getArgumentsForConstructor(constructor);
    try {
      AbstractEntity entity = (AbstractEntity) constructor.newInstance(arguments);
//      // special case logic for entities whose properties/relationships cannot
//      // be fully tested without first calling additional setter methods
//      if (entity instanceof CherryPick) {
//        CherryPick cp = (CherryPick) entity;
//        cp.setAllocated(PlateType.ABGENE,
//                        "testPlateName",
//                        1,
//                        1);
//      }
      return entity;
    }
    catch (Exception e) {
      e.printStackTrace();
      fail("newInstance for " + entityClass + " threw an Exception: " + e);
    }
    return null;
  }
  
  private Object[] getArgumentsForConstructor(Constructor constructor)
  {
    Class[] parameterTypes = constructor.getParameterTypes();
    Object[] arguments = getArgumentsForParameterTypes(parameterTypes);
    return arguments;
  }

  private Object[] getArgumentsForParameterTypes(Class[] parameterTypes) {
    
    Object [] arguments = new Object[parameterTypes.length];
    for (int i = 0; i < arguments.length; i++) {
      arguments[i] = getTestValueForType(parameterTypes[i]);
    }
    
    return arguments;
  }

  private Constructor getMaxArgConstructor(Class<? extends AbstractEntity> entityClass)
  {
    int maxArgs = 0;
    Constructor maxArgConstructor = null;
    for (Constructor constructor : entityClass.getConstructors()) {
      if (Modifier.isPublic(constructor.getModifiers())) {
        int numArgs = constructor.getParameterTypes().length;
        if (numArgs > maxArgs) {
          maxArgs = numArgs;
          maxArgConstructor = constructor;
        }
      }
    }
    return maxArgConstructor;
  }
}
