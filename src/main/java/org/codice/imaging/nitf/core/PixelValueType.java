/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.imaging.nitf.core;

/**
    Pixel value type (PVTYPE).
*/
public enum PixelValueType {

    UNKNOWN (""),
    INTEGER ("INT"),
    BILEVEL ("B"),
    SIGNEDINTEGER ("SI"),
    REAL ("R"),
    COMPLEX ("C");

    private final String textEquivalent;

    PixelValueType(final String abbreviation) {
        this.textEquivalent = abbreviation;
    }

    public static PixelValueType getEnumValue(final String textEquivalent) {
        for (PixelValueType pv : values()) {
            if (textEquivalent.equals(pv.textEquivalent)) {
                return pv;
            }
        }
        return UNKNOWN;
    }

    public String getTextEquivalent() {
        return textEquivalent;
    }
};
