/*
 * Copyright (c) 2014, Codice
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.codice.imaging.cgm;

import java.awt.Graphics2D;
import java.io.IOException;

/**
 *
 */
class ColourSelectionModeElement extends ElementHelpers implements AbstractElement {
    ColourSelectionModeElement() {
        super(CgmIdentifier.COLOUR_SELECTION_MODE);
    }

    enum Mode {
        INDEXED_COLOUR_MODE,
        DIRECT_COLOUR_MODE
    }
    Mode mode = Mode.INDEXED_COLOUR_MODE;

    @Override
    public void readParameters(CgmInputReader inputReader, int parameterListLength) throws IOException {
        int data = inputReader.readEnumValue();
        switch (data) {
            case 0:
                mode = Mode.INDEXED_COLOUR_MODE;
                break;
            case 1:
                mode = Mode.DIRECT_COLOUR_MODE;
                break;
            default:
                System.out.println("Unknown Colour Selection Mode value: " + data);
                break;
        }
    }

    @Override
    public void dumpParameters() {
        System.out.println("\tColour Selection Mode: " + mode);
    }

    @Override
    public void render(Graphics2D g2, CgmGraphicState graphicState) {
        // Colour selection mode always has to be "1" (for direct colour mode) in 
        // BIIF profile BPCGM01.10, so this has no effect
    }

}