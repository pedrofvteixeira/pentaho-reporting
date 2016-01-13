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
 * Copyright (c) 2001 - 2009 Object Refinery Ltd, Pentaho Corporation and Contributors..  All rights reserved.
 */

package org.pentaho.reporting.engine.classic.core.modules.output.table.base;

import org.pentaho.reporting.engine.classic.core.layout.model.BlockRenderBox;
import org.pentaho.reporting.engine.classic.core.layout.model.CanvasRenderBox;
import org.pentaho.reporting.engine.classic.core.layout.model.InlineRenderBox;
import org.pentaho.reporting.engine.classic.core.layout.model.LayoutNodeTypes;
import org.pentaho.reporting.engine.classic.core.layout.model.ParagraphRenderBox;
import org.pentaho.reporting.engine.classic.core.layout.model.RenderBox;
import org.pentaho.reporting.engine.classic.core.layout.model.RenderNode;
import org.pentaho.reporting.engine.classic.core.layout.model.RenderableReplacedContent;
import org.pentaho.reporting.engine.classic.core.layout.model.RenderableReplacedContentBox;
import org.pentaho.reporting.engine.classic.core.layout.model.RenderableText;
import org.pentaho.reporting.engine.classic.core.layout.model.SpacerRenderNode;
import org.pentaho.reporting.engine.classic.core.layout.model.table.TableCellRenderBox;
import org.pentaho.reporting.engine.classic.core.layout.model.table.TableColumnGroupNode;
import org.pentaho.reporting.engine.classic.core.layout.model.table.TableRenderBox;
import org.pentaho.reporting.engine.classic.core.layout.model.table.TableRowRenderBox;
import org.pentaho.reporting.engine.classic.core.layout.model.table.TableSectionRenderBox;
import org.pentaho.reporting.engine.classic.core.layout.output.OutputProcessorMetaData;
import org.pentaho.reporting.engine.classic.core.layout.process.IterateStructuralProcessStep;
import org.pentaho.reporting.engine.classic.core.layout.process.RevalidateTextEllipseProcessStep;
import org.pentaho.reporting.engine.classic.core.layout.text.GlyphList;
import org.pentaho.reporting.engine.classic.core.util.RotationUtils;
import org.pentaho.reporting.engine.classic.core.util.geom.StrictBounds;
import org.pentaho.reporting.libraries.fonts.encoding.CodePointBuffer;

/**
 * Creation-Date: 02.11.2007, 14:14:23
 *
 * @author Thomas Morgner
 */
public class DefaultTextExtractor extends IterateStructuralProcessStep
{
  private StringBuffer text;
  private Object rawResult;
  private RenderNode rawSource;
  private StrictBounds paragraphBounds;
  private boolean overflowX;
  private boolean overflowY;
  private boolean textLineOverflow;
  private RevalidateTextEllipseProcessStep revalidateTextEllipseProcessStep;
  private CodePointBuffer codePointBuffer;
  private boolean manualBreak;
  //  private long contentAreaX1;
  private long contentAreaX2;
  private boolean ellipseDrawn;
  private boolean clipOnWordBoundary;

  public DefaultTextExtractor(final OutputProcessorMetaData metaData)
  {
    if (metaData == null)
    {
      throw new NullPointerException();
    }

    codePointBuffer = new CodePointBuffer(400);
    text = new StringBuffer(400);
    paragraphBounds = new StrictBounds();
    revalidateTextEllipseProcessStep = new RevalidateTextEllipseProcessStep(metaData);
    this.clipOnWordBoundary = "true".equals
        (metaData.getConfiguration().getConfigProperty(
            "org.pentaho.reporting.engine.classic.core.LastLineBreaksOnWordBoundary"));
  }

  protected CodePointBuffer getCodePointBuffer()
  {
    return codePointBuffer;
  }

  public Object compute(final RenderBox box)
  {
    rawResult = null;
    rawSource = null;
    // initialize it once. It may be overriden later, if there is a real paragraph
    paragraphBounds.setRect(box.getX(), box.getY(), box.getWidth(), box.getHeight());
    overflowX = box.isBoxOverflowX();
    overflowY = box.isBoxOverflowY();
    clearText();
    startProcessing(box);

    // A simple result. So there's no need to create a rich-text string.
    if (rawResult != null)
    {
      return rawResult;
    }
    return text.toString();
  }

  public String getFormattedtext()
  {
    return text.toString();
  }

  private long extractEllipseSize(final RenderNode node)
  {
    if (node == null)
    {
      return 0;
    }
    final RenderBox parent = node.getParent();
    if (parent == null)
    {
      return 0;
    }
    final RenderBox textEllipseBox = parent.getTextEllipseBox();
    if (textEllipseBox == null)
    {
      return 0;
    }
    return textEllipseBox.getWidth();
  }

  protected void processOtherNode(final RenderNode node)
  {
    final int nodeType = node.getNodeType();
    if (isTextLineOverflow())
    {
      if (node.isNodeVisible(paragraphBounds, overflowX, overflowY) == false)
      {
        return;
      }

      if (node.isVirtualNode())
      {

        if (ellipseDrawn)
        {
          return;
        }
        ellipseDrawn = true;

        if (clipOnWordBoundary == false &&
            nodeType == LayoutNodeTypes.TYPE_NODE_TEXT)
        {
          final RenderableText text = (RenderableText) node;
          final long ellipseSize = extractEllipseSize(node);
          final long x1 = text.getX();
          final long effectiveAreaX2 = (contentAreaX2 - ellipseSize);

          if (x1 >= contentAreaX2)
          {
            // Skip, the node will not be visible.
          }
          else
          {
            // The text node that is printed will overlap with the ellipse we need to print.
            drawText(text, effectiveAreaX2);
          }
        }

        final RenderBox parent = node.getParent();
        if (parent != null)
        {
          final RenderBox textEllipseBox = parent.getTextEllipseBox();
          if (textEllipseBox != null)
          {
            processBoxChilds(textEllipseBox);
          }
        }
        return;
      }
    }

    if (nodeType == LayoutNodeTypes.TYPE_NODE_TEXT)
    {
      final RenderableText textNode = (RenderableText) node;
      if (isTextLineOverflow())
      {
        if (textNode.isNodeVisible(paragraphBounds, overflowX, overflowY) == false)
        {
          return;
        }

        final long ellipseSize = extractEllipseSize(node);
        
        final long minCoord, maxCoord, effectiveArea, contentArea;
        
        /* Rotation minCoord and maxCoord instead of x1 x2 */
        if( RotationUtils.isVerticalOrientation( node.getParent().getParent() ) ){
          // applied rotation aligns the text with the Y axis [-270,-90,90,270]
          minCoord = node.getY();
          /* TODO assuming rotation 90 == -90, Top vs bottom border+padding */
          /* simulate contentAreaY2 */
          contentArea = node.getParent().getParent().getY()
              + node.getParent().getParent().getOverflowAreaHeight()
              - node.getParent().getParent().getBoxDefinition().getPaddingBottom()
              - node.getParent().getParent().getStaticBoxLayoutProperties().getBorderBottom();
        }else{
          // TODO diagonal rotations (ex: 45 degrees)
          minCoord = node.getX();
          contentArea = contentAreaX2;
        }
        effectiveArea = contentArea - ellipseSize;
        maxCoord = minCoord + node.getWidth();
        
        if ( maxCoord <= effectiveArea )
        {
          // the text will be fully visible.
          drawText(textNode, maxCoord);
        }
        else if (minCoord >= contentArea)
        {
          // Skip, the node will not be visible.
        }
        else
        {
          // The text node that is printed will overlap with the ellipse we need to print.
          drawText(textNode, effectiveArea);
          final RenderBox parent = node.getParent();
          if (parent != null)
          {
            final RenderBox textEllipseBox = parent.getTextEllipseBox();
            if (textEllipseBox != null)
            {
              processBoxChilds(textEllipseBox);
            }
          }
        }

      }
      else
      {
        drawText(textNode, textNode.getX() + textNode.getWidth());
      }
      if (textNode.isForceLinebreak())
      {
        manualBreak = true;
      }
    }
    else if (nodeType == LayoutNodeTypes.TYPE_NODE_SPACER)
    {
      final SpacerRenderNode spacer = (SpacerRenderNode) node;
      final int count = Math.max(1, spacer.getSpaceCount());
      for (int i = 0; i < count; i++)
      {
        this.text.append(' ');
      }
    }
  }

  /**
   * Renders the glyphs stored in the text node.
   *
   * @param renderableText the text node that should be rendered.
   * @param contentX2
   */
  protected void drawText(final RenderableText renderableText, final long contentX2)
  {
    if (renderableText.getLength() == 0)
    {
      // This text is empty.
      return;
    }

    final GlyphList gs = renderableText.getGlyphs();
    final int maxLength = renderableText.computeMaximumTextSize(contentX2);
    this.text.append(gs.getText(renderableText.getOffset(), maxLength, codePointBuffer));
  }

  protected boolean startOtherBox(final RenderBox box)
  {
    return false;
  }

  protected boolean isContentField(final RenderBox box)
  {
    return (box.getNodeType() == LayoutNodeTypes.TYPE_BOX_CONTENT);
  }

  public boolean startCanvasBox(final CanvasRenderBox box)
  {
    return true;
  }

  protected void processRenderableContent(final RenderableReplacedContentBox box)
  {
    final RenderableReplacedContent rpc = box.getContent();
    this.rawResult = rpc.getRawObject();
    this.rawSource = box;
  }

  protected boolean startBlockBox(final BlockRenderBox box)
  {
    if (box.getStaticBoxLayoutProperties().isVisible() == false)
    {
      return false;
    }
    return true;
  }

  protected boolean startRowBox(final RenderBox box)
  {
    if (box.getStaticBoxLayoutProperties().isVisible() == false)
    {
      return false;
    }
    return true;
  }

  public RenderNode getRawSource()
  {
    return rawSource;
  }

  protected boolean startInlineBox(final InlineRenderBox box)
  {
    if (box.getStaticBoxLayoutProperties().isVisible() == false)
    {
      return false;
    }
    
    return true;
  }

  protected boolean startTableCellBox(final TableCellRenderBox box)
  {
    if (box.getStaticBoxLayoutProperties().isVisible() == false)
    {
      return false;
    }

    return true;
  }

  protected boolean startTableRowBox(final TableRowRenderBox box)
  {
    if (box.getStaticBoxLayoutProperties().isVisible() == false)
    {
      return false;
    }

    return true;
  }

  protected boolean startTableSectionBox(final TableSectionRenderBox box)
  {
    if (box.getStaticBoxLayoutProperties().isVisible() == false)
    {
      return false;
    }

    return true;
  }

  protected boolean startTableColumnGroupBox(final TableColumnGroupNode box)
  {
    if (box.getStaticBoxLayoutProperties().isVisible() == false)
    {
      return false;
    }

    return true;
  }

  protected boolean startTableBox(final TableRenderBox box)
  {
    if (box.getStaticBoxLayoutProperties().isVisible() == false)
    {
      return false;
    }

    return true;
  }

  protected boolean startAutoBox(final RenderBox box)
  {
    if (box.getStaticBoxLayoutProperties().isVisible() == false)
    {
      return false;
    }

    return true;
  }

  protected void processParagraphChilds(final ParagraphRenderBox box)
  {
    rawResult = box.getRawValue();
    paragraphBounds.setRect(box.getX(), box.getY(), box.getWidth(), box.getHeight());
    overflowX = box.isBoxOverflowX();
    overflowY = box.isBoxOverflowY();
    
    final long contentAreaX1 = box.getContentAreaX1();
    contentAreaX2 = box.getContentAreaX2();

    RenderBox lineBox = (RenderBox) box.getFirstChild();
    while (lineBox != null)
    {
      manualBreak = false;
      processTextLine(lineBox, contentAreaX1, contentAreaX2 );
      if (manualBreak)
      {
        addLinebreak();
      }
      else if (lineBox.getNext() != null)
      {
        if (lineBox.getStaticBoxLayoutProperties().isPreserveSpace() == false)
        {
          addSoftBreak();
        }
        else
        {
          addEmptyBreak();
        }
      }
      lineBox = (RenderBox) lineBox.getNext();
    }
  }

  protected void addEmptyBreak()
  {
    text.append(' ');
  }

  protected void addSoftBreak()
  {
    text.append(' ');
  }

  protected void addLinebreak()
  {
    text.append('\n');
  }


  protected void processTextLine(final RenderBox lineBox,
                                 final long contentAreaX1,
                                 final long contentAreaX2)
  {
    if (lineBox.isNodeVisible(paragraphBounds, overflowX, overflowY) == false)
    {
      return;
    }
    ellipseDrawn = false;

    /* Account for rotations in text overflow calculation */
    
    // check if a rotation is applied, and is not one that still keeps the text aligned with the X axis [0, -180,180]
    if( RotationUtils.isVerticalOrientation( lineBox.getParent() ) ){
      // applied rotation aligns the text with the Y axis [-270,-90,90,270]
      final long contentAreaY2 = lineBox.getY() + lineBox.getParent().getOverflowAreaHeight()
          - lineBox.getParent().getBoxDefinition().getPaddingBottom()
          - lineBox.getParent().getStaticBoxLayoutProperties().getBorderBottom();
      // assuming lineBox is horizontal (not yet rotated)
      // assuming lineBox.getY() [Y +border +padding ]
      // assuming lineBox.getParent().getOverflowAreaHeight() [height +borders +paddings ]
      textLineOverflow =
          (lineBox.getY() + lineBox.getWidth() > contentAreaY2) &&
          !lineBox.getParent().getStaticBoxLayoutProperties().isOverflowY();

    }else{ // TODO diagonal rotations (ex: 45 degrees)
      textLineOverflow =
          ((lineBox.getX() + lineBox.getWidth()) > contentAreaX2) &&
          !lineBox.getParent().getStaticBoxLayoutProperties().isOverflowX();
    }

    if (textLineOverflow)
    {
      revalidateTextEllipseProcessStep.compute(lineBox, contentAreaX1, contentAreaX2);
    }

    startProcessing(lineBox);
  }


  public Object getRawResult()
  {
    return rawResult;
  }

  protected void setRawResult(final Object rawResult)
  {
    this.rawResult = rawResult;
  }

  public String getText()
  {
    return text.toString();
  }

  public int getTextLength()
  {
    return text.length();
  }

  protected void clearText()
  {
    text.delete(0, text.length());
  }


  protected StrictBounds getParagraphBounds()
  {
    return paragraphBounds;
  }


  public boolean isTextLineOverflow()
  {
    return textLineOverflow;
  }

  public boolean isOverflowX()
  {
    return overflowX;
  }

  public boolean isOverflowY()
  {
    return overflowY;
  }
}
