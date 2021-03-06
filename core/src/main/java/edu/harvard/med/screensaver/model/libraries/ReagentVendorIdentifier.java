// $HeadURL$
// $Id$
//
// Copyright © 2006, 2010, 2011, 2012 by the President and Fellows of Harvard College.
//
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.model.libraries;

import java.io.Serializable;

import javax.persistence.Embeddable;

import org.hibernate.annotations.Type;
import com.google.common.base.Function;

import edu.harvard.med.screensaver.model.RequiredPropertyException;
import edu.harvard.med.screensaver.util.StringUtils;


/**
 * @author <a mailto="andrew_tolopko@hms.harvard.edu">Andrew Tolopko</a>
 * @author <a mailto="john_sullivan@hms.harvard.edu">John Sullivan</a>
 */
@Embeddable
public class ReagentVendorIdentifier implements Serializable, Comparable<ReagentVendorIdentifier>
{
  private static final long serialVersionUID = 1L;

  static final ReagentVendorIdentifier NULL_VENDOR_ID = new ReagentVendorIdentifier();

  public static Function<ReagentVendorIdentifier,String> ToVendorName = new Function<ReagentVendorIdentifier,String>() {
    public String apply(ReagentVendorIdentifier rvi)
    {
      return rvi.getVendorName();
    }
  };

  public static Function<ReagentVendorIdentifier,String> ToVendorIdentifier = new Function<ReagentVendorIdentifier,String>() {
    public String apply(ReagentVendorIdentifier rvi)
    {
      return rvi.getVendorIdentifier();
    }
  };

  static { NULL_VENDOR_ID._asString = ""; }

  private String _vendorName;
  private String _vendorIdentifier;
  private transient String _asString;

  public ReagentVendorIdentifier(String vendorName, String reagentIdentifier)
  {
    if (StringUtils.isEmpty(vendorName)) {
      throw new RequiredPropertyException(Reagent.class, "reagent vendor name");
    }
    if (StringUtils.isEmpty(reagentIdentifier)) {
      throw new RequiredPropertyException(Reagent.class, "reagent identifier");
    }
    setVendorName(vendorName);
    setVendorIdentifier(reagentIdentifier);
  }

  /**
   * @return vendor the vendor that produced and provided the reagent
   */
  @Type(type="text")
  public String getVendorName()
  {
    return _vendorName;
  }

  /**
   * @return the vendor-assigned identifier for the reagent
   */
  @Type(type="text")
  public String getVendorIdentifier()
  {
    return _vendorIdentifier;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == null) { 
      return false;
    }
    ReagentVendorIdentifier other = (ReagentVendorIdentifier) o;
    if (_vendorName == null) {
      return other.getVendorName() == null;
    }
    assert _vendorIdentifier != null;
    return _vendorName.equals(other.getVendorName()) && _vendorIdentifier.equals(other.getVendorIdentifier());
  }

  @Override
  public int hashCode()
  {
    return toString().hashCode();
  }

  @Override
  public String toString()
  {
    if (_asString == null) {
      _asString = _vendorName + " " + _vendorIdentifier;
    }
    return _asString;
  }

  public int compareTo(ReagentVendorIdentifier other)
  {
    int result = _vendorName.compareTo(other._vendorName);
    if (result == 0) {
      result = _vendorIdentifier.compareTo(other._vendorIdentifier);
    }
    return result;
  }

  private void setVendorName(String vendorName)
  {
    _vendorName = vendorName;
  }

  private void setVendorIdentifier(String vendorIdentifier)
  {
    _vendorIdentifier = vendorIdentifier;
  }

  /**
   * @motivation for Hibernate and NULL_VENDOR_ID
   */
  private ReagentVendorIdentifier()
  {
  }
}
