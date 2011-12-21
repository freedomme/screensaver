// $HeadURL$
// $Id$
//
// Copyright © 2006, 2010 by the President and Fellows of Harvard College.
// 
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.io.libraries.smallmolecule;

import edu.harvard.med.screensaver.io.image.ImageLocator;
import edu.harvard.med.screensaver.model.libraries.SmallMoleculeReagent;

/**
 * Type-safe tag interface for services that can provide images of small molecule structures.
 */
public interface StructureImageLocator extends ImageLocator<SmallMoleculeReagent>
{
}