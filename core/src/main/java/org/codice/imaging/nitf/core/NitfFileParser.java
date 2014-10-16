/*
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
 */
package org.codice.imaging.nitf.core;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
    Parser for a NITF file.
*/
final class NitfFileParser extends AbstractNitfSegmentParser {

    private long nitfFileLength = -1;
    private int nitfHeaderLength = -1;

    private int numberImageSegments = 0;
    private int numberGraphicSegments = 0;
    private int numberTextSegments = 0;
    private int numberLabelSegments = 0;
    private int numberDataExtensionSegments = 0;
    private int numberReservedExtensionSegments = 0;

    private List<Integer> lish = new ArrayList<Integer>();
    private List<Long> li = new ArrayList<Long>();
    private List<Integer> lssh = new ArrayList<Integer>();
    private List<Integer> ls = new ArrayList<Integer>();
    private List<Integer> llsh = new ArrayList<Integer>();
    private List<Integer> ll = new ArrayList<Integer>();
    private List<Integer> ltsh = new ArrayList<Integer>();
    private List<Integer> lt = new ArrayList<Integer>();
    private List<Integer> ldsh = new ArrayList<Integer>();
    private List<Integer> ld = new ArrayList<Integer>();
    private int userDefinedHeaderDataLength = 0;
    private int extendedHeaderDataLength = 0;

    private Nitf nitf = null;
    private Set<ParseOption> parseOptionSet = null;

    private NitfFileParser(final NitfReader nitfReader, final Set<ParseOption> parseOptions) {
        nitf = new Nitf();
        parseOptionSet = parseOptions;
        reader = nitfReader;
    };

    public static Nitf parse(final NitfReader nitfReader, final Set<ParseOption> parseOptions) throws ParseException {
        NitfFileParser parser = new NitfFileParser(nitfReader, parseOptions);

        parser.readBaseHeaders();
        if (parser.isStreamingMode()) {
            parser.handleStreamingMode();
        }
        parser.readDataSegments();
        return parser.nitf;
    }

    private void readBaseHeaders() throws ParseException {
        readFHDRFVER();
        reader.setFileType(nitf.getFileType());
        readCLEVEL();
        readSTYPE();
        readOSTAID();
        readFDT();
        readFTITLE();
        nitf.setFileSecurityMetadata(new NitfFileSecurityMetadata(reader));
        readENCRYP();
        if ((reader.getFileType() == FileType.NITF_TWO_ONE) || (reader.getFileType() == FileType.NSIF_ONE_ZERO)) {
            readFBKGC();
        }
        readONAME();
        readOPHONE();
        readFL();
        readHL();
        readBaseHeaderImageParts();
        readBaseHeaderGraphicParts();
        readBaseHeaderLabelParts();
        readBaseHeaderTextParts();
        readBaseHeaderDataExtensionSegmentParts();
        readBaseHeaderReservedExtensionParts();
        readBaseHeaderUserDefinedHeaderData();
        readBaseHeaderExtendedHeader();
    }

    private void readBaseHeaderImageParts() throws ParseException {
        readNUMI();
        for (int i = 0; i < numberImageSegments; ++i) {
            readLISH(i);
            readLI(i);
        }
    }

    private void readBaseHeaderGraphicParts() throws ParseException {
        readNUMS();
        for (int i = 0; i < numberGraphicSegments; ++i) {
            readLSSH();
            readLS();
        }
    }

    private void readBaseHeaderLabelParts() throws ParseException {
        readNUMX();
        for (int i = 0; i < numberLabelSegments; ++i) {
            readLLSH();
            readLL();
        }
    }

    private void readBaseHeaderTextParts() throws ParseException {
        readNUMT();
        for (int i = 0; i < numberTextSegments; ++i) {
            readLTSH();
            readLT();
        }
    }

    private void readBaseHeaderDataExtensionSegmentParts() throws ParseException {
        readNUMDES();
        for (int i = 0; i < numberDataExtensionSegments; ++i) {
            readLDSH();
            readLD();
        }
    }

    private void readBaseHeaderReservedExtensionParts() throws ParseException {
        readNUMRES();
        for (int i = 0; i < numberReservedExtensionSegments; ++i) {
            // TODO: find a case that exercises this and implement it
            throw new UnsupportedOperationException("IMPLEMENT RES PARSING");
        }
    }

    private void readBaseHeaderUserDefinedHeaderData() throws ParseException {
        readUDHDL();
        if (userDefinedHeaderDataLength > 0) {
            readUDHOFL();
            readUDHD();
        }
    }

     private void readBaseHeaderExtendedHeader() throws ParseException {
        readXHDL();
        if (extendedHeaderDataLength > 0) {
            readXHDLOFL();
            readXHD();
        }
    }

    private boolean isStreamingMode() {
        return nitfFileLength == NitfConstants.STREAMING_FILE_MODE;
    }

    private void handleStreamingMode() throws ParseException {
        if (reader.canSeek()) {
            readStreamingModeHeader();
        } else {
            throw new ParseException("No support for streaming mode unless input is seekable", 0);
        }
    }

    // This code will probably make more sense if you read MIL-STD-2500C Section 5.8.3.2 and then have
    // MIL-STD-2500C Table A-8(B) open.
    private void readStreamingModeHeader() throws ParseException {
        // Store the current position
        long dataSegmentsOffset = reader.getCurrentOffset();

        seekToSfhDelim2();

        verifySfhDelim2();

        long sfhL2 = reader.readBytesAsLong(NitfConstants.SFH_L2_LENGTH);

        seekToSfhDelim1(sfhL2);

        // verify the lengths match.
        long sfhL1 = reader.readBytesAsLong(NitfConstants.SFH_L1_LENGTH);
        if (sfhL1 != sfhL2) {
            throw new ParseException("Mismatch between SFH_L1 and SFH_L2", (int) reader.getCurrentOffset());
        }

        verifySfhDelim1();

        // Read the replacement header content
        // This assumes that the streaming mode header will contain the full "base" headers from MIL-STD-2500C Table A-1.
        readBaseHeaders();

        // Continue to read the subheaders and associated data
        reader.seekToAbsoluteOffset(dataSegmentsOffset);
    }

    private void seekToSfhDelim2() throws ParseException {
        reader.seekToEndOfFile();
        reader.seekBackwards(NitfConstants.SFH_DELIM2_LENGTH + NitfConstants.SFH_L2_LENGTH);
    }

    private void verifySfhDelim2() throws ParseException {
        byte[] sfhDelim2 = reader.readBytesRaw(NitfConstants.SFH_DELIM2_LENGTH);
        if (!Arrays.equals(NitfConstants.SFH_DELIM2, sfhDelim2)) {
            throw new ParseException("Unexpected SFH_DELIM2 value read from file", (int) reader.getCurrentOffset());
        }
    }

    private void seekToSfhDelim1(final long sfhDrLength) throws ParseException {
        reader.seekBackwards(NitfConstants.SFH_L1_LENGTH + NitfConstants.SFH_DELIM1_LENGTH
                + sfhDrLength + NitfConstants.SFH_DELIM2_LENGTH + NitfConstants.SFH_L2_LENGTH);
    }

    private void verifySfhDelim1() throws ParseException {
        byte[] sfhDelim1 = reader.readBytesRaw(NitfConstants.SFH_DELIM1_LENGTH);
        if (!Arrays.equals(NitfConstants.SFH_DELIM1, sfhDelim1)) {
            throw new ParseException("Unexpected SFH_DELIM1 value read from file", (int) reader.getCurrentOffset());
        }
    }

    private void readDataSegments() throws ParseException {
        readImageSegments();
        if (reader.getFileType() == FileType.NITF_TWO_ZERO) {
            readSymbolSegments();
            readLabelSegments();
        } else {
            readGraphicSegments();
        }
        readTextSegments();
        readDataExtensionSegments();
    }

    private void readFHDRFVER() throws ParseException {
        String fhdrfver = reader.readBytes(NitfConstants.FHDR_LENGTH + NitfConstants.FVER_LENGTH);
        nitf.setFileType(FileType.getEnumValue(fhdrfver));
    }

    private void readCLEVEL() throws ParseException {
        nitf.setComplexityLevel(reader.readBytesAsInteger(NitfConstants.CLEVEL_LENGTH));
        if ((nitf.getComplexityLevel() < NitfConstants.MIN_COMPLEXITY_LEVEL) || (nitf.getComplexityLevel() > NitfConstants.MAX_COMPLEXITY_LEVEL)) {
            throw new ParseException(String.format("CLEVEL out of range: %d", nitf.getComplexityLevel()), (int) reader.getCurrentOffset());
        }
    }

    private void readSTYPE() throws ParseException {
        nitf.setStandardType(reader.readTrimmedBytes(NitfConstants.STYPE_LENGTH));
    }

    private void readOSTAID() throws ParseException {
        nitf.setOriginatingStationId(reader.readTrimmedBytes(NitfConstants.OSTAID_LENGTH));
    }

    private void readFDT() throws ParseException {
        nitf.setFileDateTime(readNitfDateTime());
    }

    private void readFTITLE() throws ParseException {
        nitf.setFileTitle(reader.readTrimmedBytes(NitfConstants.FTITLE_LENGTH));
    }

    private void readFBKGC() throws ParseException {
        nitf.setFileBackgroundColour(readRGBColour());
    }

    private void readONAME() throws ParseException {
        if ((nitf.getFileType() == FileType.NITF_TWO_ONE) || (nitf.getFileType() == FileType.NSIF_ONE_ZERO)) {
            nitf.setOriginatorsName(reader.readTrimmedBytes(NitfConstants.ONAME_LENGTH));
        } else {
            nitf.setOriginatorsName(reader.readTrimmedBytes(NitfConstants.ONAME20_LENGTH));
        }
    }

    private void readOPHONE() throws ParseException {
        nitf.setOriginatorsPhoneNumber(reader.readTrimmedBytes(NitfConstants.OPHONE_LENGTH));
    }

    private void readFL() throws ParseException {
        nitfFileLength = reader.readBytesAsLong(NitfConstants.FL_LENGTH);
    }

    private void readHL() throws ParseException {
        nitfHeaderLength = reader.readBytesAsInteger(NitfConstants.HL_LENGTH);
    }

    private void readNUMI() throws ParseException {
        numberImageSegments = reader.readBytesAsInteger(NitfConstants.NUMI_LENGTH);
    }

    private void readLISH(final int i) throws ParseException {
        if (i < lish.size()) {
            lish.set(i, reader.readBytesAsInteger(NitfConstants.LISH_LENGTH));
        } else {
            lish.add(reader.readBytesAsInteger(NitfConstants.LISH_LENGTH));
        }
    }

    private void readLI(final int i) throws ParseException {
        if (i < li.size()) {
            li.set(i, reader.readBytesAsLong(NitfConstants.LI_LENGTH));
        } else {
            li.add(reader.readBytesAsLong(NitfConstants.LI_LENGTH));
        }
    }

    // The next three methods are also used for NITF 2.0 Symbol segment lengths
    private void readNUMS() throws ParseException {
        numberGraphicSegments = reader.readBytesAsInteger(NitfConstants.NUMS_LENGTH);
    }

    private void readLSSH() throws ParseException {
        lssh.add(reader.readBytesAsInteger(NitfConstants.LSSH_LENGTH));
    }

    private void readLS() throws ParseException {
        ls.add(reader.readBytesAsInteger(NitfConstants.LS_LENGTH));
    }

    private void readNUMX() throws ParseException {
        if (reader.getFileType() == FileType.NITF_TWO_ZERO) {
            numberLabelSegments = reader.readBytesAsInteger(NitfConstants.NUML20_LENGTH);
        } else {
            reader.skip(NitfConstants.NUMX_LENGTH);
        }
    }

    private void readLLSH() throws ParseException {
        llsh.add(reader.readBytesAsInteger(NitfConstants.LLSH_LENGTH));
    }

    private void readLL() throws ParseException {
        ll.add(reader.readBytesAsInteger(NitfConstants.LL_LENGTH));
    }

    private void readNUMT() throws ParseException {
       numberTextSegments = reader.readBytesAsInteger(NitfConstants.NUMT_LENGTH);
    }

    private void readLTSH() throws ParseException {
        ltsh.add(reader.readBytesAsInteger(NitfConstants.LTSH_LENGTH));
    }

    private void readLT() throws ParseException {
        lt.add(reader.readBytesAsInteger(NitfConstants.LT_LENGTH));
    }

    private void readNUMDES() throws ParseException {
        numberDataExtensionSegments = reader.readBytesAsInteger(NitfConstants.NUMDES_LENGTH);
    }

    private void readLDSH() throws ParseException {
        ldsh.add(reader.readBytesAsInteger(NitfConstants.LDSH_LENGTH));
    }

    private void readLD() throws ParseException {
        ld.add(reader.readBytesAsInteger(NitfConstants.LD_LENGTH));
    }

    private void readNUMRES() throws ParseException {
        numberReservedExtensionSegments = reader.readBytesAsInteger(NitfConstants.NUMRES_LENGTH);
    }

    private void readUDHDL() throws ParseException {
        userDefinedHeaderDataLength = reader.readBytesAsInteger(NitfConstants.UDHDL_LENGTH);
    }

    private void readUDHOFL() throws ParseException {
        nitf.setUserDefinedHeaderOverflow(reader.readBytesAsInteger(NitfConstants.UDHOFL_LENGTH));
    }

    private void readUDHD() throws ParseException {
        TreParser treParser = new TreParser();
        TreCollection userDefinedHeaderTREs = treParser.parse(reader, userDefinedHeaderDataLength - NitfConstants.UDHOFL_LENGTH);
        nitf.mergeTREs(userDefinedHeaderTREs);
    }

    private void readXHDL() throws ParseException {
        extendedHeaderDataLength = reader.readBytesAsInteger(NitfConstants.XHDL_LENGTH);
    }

    private void readXHDLOFL() throws ParseException {
        nitf.setExtendedHeaderDataOverflow(reader.readBytesAsInteger(NitfConstants.XHDLOFL_LENGTH));
    }

    private void readXHD() throws ParseException {
        TreParser treParser = new TreParser();
        TreCollection extendedHeaderTres = treParser.parse(reader, extendedHeaderDataLength - NitfConstants.XHDLOFL_LENGTH);
        nitf.mergeTREs(extendedHeaderTres);
    }

    private void readImageSegments() throws ParseException {
        for (int i = 0; i < numberImageSegments; ++i) {
            NitfImageSegment imageSegment = new NitfImageSegment();
            new NitfImageSegmentParser(reader, li.get(i), parseOptionSet, imageSegment);
            nitf.addImageSegment(imageSegment);
        }
    }

    private void readGraphicSegments() throws ParseException {
        for (int i = 0; i < numberGraphicSegments; ++i) {
            NitfGraphicSegment graphicSegment = new NitfGraphicSegment();
            new NitfGraphicSegmentParser(reader, ls.get(i), parseOptionSet, graphicSegment);
            nitf.addGraphicSegment(graphicSegment);
        }
    }

    // We reuse the Graphic Segment length values here, but generate different type
    private void readSymbolSegments() throws ParseException {
        for (int i = 0; i < numberGraphicSegments; ++i) {
            NitfSymbolSegment symbolSegment = new NitfSymbolSegment();
            new NitfSymbolSegmentParser(reader, ls.get(i), parseOptionSet, symbolSegment);
            nitf.addSymbolSegment(symbolSegment);
        }
    }

    private void readLabelSegments() throws ParseException {
        for (int i = 0; i < numberLabelSegments; ++i) {
            NitfLabelSegment labelSegment = new NitfLabelSegment();
            new NitfLabelSegmentParser(reader, ll.get(i), parseOptionSet, labelSegment);
            nitf.addLabelSegment(labelSegment);
        }
    }

    private void readTextSegments() throws ParseException {
        for (int i = 0; i < numberTextSegments; ++i) {
            NitfTextSegment textSegment = new NitfTextSegment();
            new NitfTextSegmentParser(reader, lt.get(i), parseOptionSet, textSegment);
            nitf.addTextSegment(textSegment);
        }
    }

    private void readDataExtensionSegments() throws ParseException {
        for (int i = 0; i < numberDataExtensionSegments; ++i) {
            NitfDataExtensionSegment segment = new NitfDataExtensionSegment();
            new NitfDataExtensionSegmentParser(reader, ld.get(i), segment);
            nitf.addDataExtensionSegment(segment);
        }
    }
}