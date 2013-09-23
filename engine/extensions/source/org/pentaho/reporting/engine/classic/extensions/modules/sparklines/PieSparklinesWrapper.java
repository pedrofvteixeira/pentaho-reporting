/*
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
* Foundation.
*
* You should have received a copy of the GNU Lesser General Public License along with this
* program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
* or from the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*
* This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
* without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
* See the GNU Lesser General Public License for more details.
*
* Copyright (c) 2001 - 2013 Object Refinery Ltd, Pentaho Corporation and Contributors..  All rights reserved.
*/

package org.pentaho.reporting.engine.classic.extensions.modules.sparklines;

import java.awt.Color;
import java.awt.geom.Rectangle2D;

import org.pentaho.reporting.engine.classic.core.ResourceBundleFactory;
import org.pentaho.reporting.engine.classic.core.imagemap.ImageMap;
import org.pentaho.reporting.engine.classic.core.style.ElementStyleKeys;
import org.pentaho.reporting.engine.classic.core.style.StyleSheet;
import org.pentaho.reporting.engine.classic.core.util.ReportDrawable;
import org.pentaho.reporting.libraries.base.config.Configuration;
import org.pentaho.reporting.libraries.libsparklines.PieGraphDrawable;
import org.pentaho.reporting.libraries.resourceloader.factory.drawable.DrawableWrapper;

public class PieSparklinesWrapper extends DrawableWrapper implements ReportDrawable
{

  public PieSparklinesWrapper(final PieGraphDrawable sparkline)
  {
    super(sparkline);
  }

  private PieGraphDrawable getPieGraphDrawable()
  {
    return (PieGraphDrawable) getBackend();
  }

  /**
   * Provides the current report configuration of the current report process to the drawable. The report configuration
   * can be used to configure the drawing process through the report.
   *
   * @param config the report configuration.
   */
  public void setConfiguration(final Configuration config)
  {

  }

  /**
   * Provides the computed stylesheet of the report element that contained this drawable. The stylesheet is immutable.
   *
   * @param style the stylesheet.
   */
  public void setStyleSheet(final StyleSheet style)
  {
    if (style != null)
    {
      final Color backgroundColor = (Color) style.getStyleProperty(ElementStyleKeys.BACKGROUND_COLOR);
      if (backgroundColor != null)
      {
        getPieGraphDrawable().setBackground(backgroundColor);
      }
      final Color color = (Color) style.getStyleProperty(ElementStyleKeys.PAINT);
      if (color != null)
      {
        getPieGraphDrawable().setColor(color);
      }
      final Color lowColor = (Color) style.getStyleProperty(SparklineStyleKeys.LOW_COLOR);
      if (lowColor != null)
      {
        getPieGraphDrawable().setLowColor(lowColor);
      }
      final Color mediumColor = (Color) style.getStyleProperty(SparklineStyleKeys.MEDIUM_COLOR);
      if (mediumColor != null)
      {
        getPieGraphDrawable().setMediumColor(mediumColor);
      }
      final Color highColor = (Color) style.getStyleProperty(SparklineStyleKeys.HIGH_COLOR);
      if (highColor != null)
      {
        getPieGraphDrawable().setHighColor(highColor);
      }
    }
  }

  /**
   * Defines the resource-bundle factory that can be used to localize the drawing process.
   *
   * @param bundleFactory the resource-bundle factory.
   */
  public void setResourceBundleFactory(final ResourceBundleFactory bundleFactory)
  {

  }

  /**
   * Returns an optional image-map for the entry.
   *
   * @param bounds the bounds for which the image map is computed.
   * @return the computed image-map or null if there is no image-map available.
   */
  public ImageMap getImageMap(final Rectangle2D bounds)
  {
    return null;
  }
}
