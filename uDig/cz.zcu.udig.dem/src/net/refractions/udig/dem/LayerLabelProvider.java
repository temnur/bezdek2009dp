/*
 *    uDig - User Friendly Desktop Internet GIS client
 *    http://udig.refractions.net
 *    (C) 2004, Refractions Research Inc.
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 */
package net.refractions.udig.dem;

import net.refractions.udig.project.internal.Layer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;


/**
 * Provider for adding a Layer Object into the JFace widgets.
 *
 * @author jezekjan
 */
public class LayerLabelProvider extends LabelProvider {
    /**
     * Retuns the glyph of the layer.
     *
     * @param element
     *
     * @return Image
     */
    @Override
    public Image getImage(Object element) {
        Layer layer = (Layer) element;

        return layer.getGlyph().createImage();
    }

    /**
     * Returns Layer name.
     *
     * @param element
     *
     * @return Name of the Layer.
     */
    public String getText(Object element) {
        Layer layer = (Layer) element;

        return layer.getName();
    }
}
