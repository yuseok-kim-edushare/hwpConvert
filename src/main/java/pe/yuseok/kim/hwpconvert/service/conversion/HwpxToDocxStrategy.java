package pe.yuseok.kim.hwpconvert.service.conversion;

import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.SectionXMLFile;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Para;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Run;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.RunItem;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.T;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.TItem;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.t.NormalText;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Picture;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Table;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.table.CellSpan;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.table.Tc;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.table.Tr;
import kr.dogfoot.hwpxlib.object.content.context_hpf.ManifestItem;
import kr.dogfoot.hwpxlib.object.metainf.FileEntry;
import kr.dogfoot.hwpxlib.object.metainf.RootFile;
import kr.dogfoot.hwpxlib.reader.HWPXReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTVMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.springframework.stereotype.Component;
import pe.yuseok.kim.hwpconvert.model.ConversionResult;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Strategy for converting HWPX files to DOCX format
 */
@Slf4j
@Component
public class HwpxToDocxStrategy implements ConversionStrategy {

    private static final List<String> SUPPORTED_SOURCE_FORMATS = Arrays.asList("hwpx");
    private static final List<String> SUPPORTED_TARGET_FORMATS = Arrays.asList("docx");

    @Override
    public boolean supportsSourceFormat(String sourceFormat) {
        return SUPPORTED_SOURCE_FORMATS.contains(sourceFormat.toLowerCase());
    }

    @Override
    public boolean supportsTargetFormat(String targetFormat) {
        return SUPPORTED_TARGET_FORMATS.contains(targetFormat.toLowerCase());
    }

    @Override
    public ConversionResult convert(File sourceFile, File outputDirectory, String targetFormat) {
        log.info("Converting HWPX file to DOCX: {}", sourceFile.getName());
        ConversionResult result = new ConversionResult();
        result.setSourceFileName(sourceFile.getName());
        result.setSourceFormat("hwpx");
        result.setTargetFormat(targetFormat);

        String baseName = sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf('.'));
        String outputFileName = baseName + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + targetFormat;
        Path outputPath = Paths.get(outputDirectory.getAbsolutePath(), outputFileName);
        result.setConvertedFileName(outputFileName);
        result.setDownloadUrl(outputPath.toString());

        try (XWPFDocument docxDocument = new XWPFDocument()) {
            HWPXFile hwpxFile = HWPXReader.fromFile(sourceFile);
            
            // Process the document structure
            processDocument(hwpxFile, docxDocument);
            
            try (FileOutputStream out = new FileOutputStream(outputPath.toFile())) {
                docxDocument.write(out);
            }

            log.info("Successfully converted HWPX to DOCX: {}", outputFileName);
            result.setSuccess(true);
            result.setCompletionTime(LocalDateTime.now());

        } catch (Exception e) {
            log.error("Error converting HWPX {} to DOCX", sourceFile.getName(), e);
            result.setSuccess(false);
            result.setErrorMessage("Error converting HWPX to DOCX: " + e.getMessage());
            try {
                 outputPath.toFile().delete();
            } catch (SecurityException se) {
                 log.warn("Could not delete partially created file due to security restrictions: {}", outputPath);
            }
        }

        return result;
    }
    
    /**
     * Process the entire HWPX document structure
     */
    private void processDocument(HWPXFile hwpxFile, XWPFDocument docxDocument) {
        // Iterate through sections
        for (SectionXMLFile section : hwpxFile.sectionXMLFileList().items()) {
            processSection(section, docxDocument, hwpxFile);
        }
    }
    
    /**
     * Process a section of the HWPX document
     */
    private void processSection(SectionXMLFile section, XWPFDocument docxDocument, HWPXFile hwpxFile) {
        // Iterate through paragraphs in the section
        for (Para para : section.paras()) {
            processParagraph(para, docxDocument, hwpxFile);
        }
    }
    
    /**
     * Process a paragraph within the document
     */
    private void processParagraph(Para para, XWPFDocument docxDocument, HWPXFile hwpxFile) {
        XWPFParagraph docxParagraph = docxDocument.createParagraph();
        
        // Apply paragraph properties (alignment, indentation, spacing) from para.paraPrIDRef()
        if (para.paraPrIDRef() != null && hwpxFile.headerXMLFile() != null 
                && hwpxFile.headerXMLFile().refList() != null && hwpxFile.headerXMLFile().refList().paraProperties() != null) {
            
            // Find matching ParaPr by ID
            for (kr.dogfoot.hwpxlib.object.content.header_xml.references.ParaPr paraPr : 
                    hwpxFile.headerXMLFile().refList().paraProperties().items()) {
                
                if (para.paraPrIDRef().equals(paraPr.id())) {
                    // Apply alignment
                    if (paraPr.align() != null) {
                        // Set horizontal alignment
                        if (paraPr.align().horizontal() != null) {
                            switch (paraPr.align().horizontal()) {
                                case LEFT:
                                    docxParagraph.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.LEFT);
                                    break;
                                case CENTER:
                                    docxParagraph.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
                                    break;
                                case RIGHT:
                                    docxParagraph.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.RIGHT);
                                    break;
                                case JUSTIFY:
                                    docxParagraph.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.BOTH);
                                    break;
                                case DISTRIBUTE:
                                    docxParagraph.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.DISTRIBUTE);
                                    break;
                                default:
                                    docxParagraph.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.LEFT);
                                    break;
                            }
                        }
                    }
                    
                    // Apply margins/indentation
                    if (paraPr.margin() != null) {
                        // Convert HWP units (1/7200 inch) to OOXML units (1/20 pt)
                        final double HWP_TO_DXA = 635.0 / 7200.0 * 20.0;
                        
                        // Left indent
                        if (paraPr.margin().intent() != null && paraPr.margin().intent().value() != null) {
                            int leftIndent = (int)(paraPr.margin().intent().value() * HWP_TO_DXA);
                            docxParagraph.setIndentationLeft(leftIndent);
                        }
                        
                        // Right indent
                        if (paraPr.margin().right() != null && paraPr.margin().right().value() != null) {
                            int rightIndent = (int)(paraPr.margin().right().value() * HWP_TO_DXA);
                            docxParagraph.setIndentationRight(rightIndent);
                        }
                        
                        // First line indent (or hanging indent)
                        if (paraPr.margin().prev() != null && paraPr.margin().prev().value() != null) {
                            int firstLineIndent = (int)(paraPr.margin().prev().value() * HWP_TO_DXA);
                            docxParagraph.setIndentationFirstLine(firstLineIndent);
                        }
                    }
                    
                    // Apply line spacing
                    if (paraPr.lineSpacing() != null) {
                        if (paraPr.lineSpacing().type() != null) {
                            switch (paraPr.lineSpacing().type()) {
                                case PERCENT:
                                    // Set line spacing as a percentage of normal
                                    if (paraPr.lineSpacing().value() != null) {
                                        double lineSpacing = paraPr.lineSpacing().value() / 100.0;
                                        docxParagraph.setSpacingLineRule(org.apache.poi.xwpf.usermodel.LineSpacingRule.AUTO);
                                        docxParagraph.setSpacingBetween(lineSpacing);
                                    }
                                    break;
                                case FIXED:
                                    // Set fixed line spacing
                                    if (paraPr.lineSpacing().value() != null) {
                                        double lineSpacing = paraPr.lineSpacing().value() / 100.0;
                                        docxParagraph.setSpacingLineRule(org.apache.poi.xwpf.usermodel.LineSpacingRule.EXACT);
                                        docxParagraph.setSpacingBetween(lineSpacing);
                                    }
                                    break;
                                case AT_LEAST:
                                    // Set minimum line spacing
                                    if (paraPr.lineSpacing().value() != null) {
                                        double lineSpacing = paraPr.lineSpacing().value() / 100.0;
                                        docxParagraph.setSpacingLineRule(org.apache.poi.xwpf.usermodel.LineSpacingRule.AT_LEAST);
                                        docxParagraph.setSpacingBetween(lineSpacing);
                                    }
                                    break;
                            }
                        }
                    }
                    
                    // Apply paragraph spacing (before and after)
                    if (paraPr.margin() != null) {
                        final double HWP_TO_DXA = 635.0 / 7200.0 * 20.0;
                        
                        // Spacing before paragraph
                        if (paraPr.margin().prev() != null) {
                            int spacingBefore = (int)(paraPr.margin().prev().value() * HWP_TO_DXA);
                            docxParagraph.setSpacingBefore(spacingBefore);
                        }
                        
                        // Spacing after paragraph
                        if (paraPr.margin().next() != null) {
                            int spacingAfter = (int)(paraPr.margin().next().value() * HWP_TO_DXA);
                            docxParagraph.setSpacingAfter(spacingAfter);
                        }
                    }
                    
                    // Break from the loop once we've found and applied the paragraph properties
                    break;
                }
            }
        }

        // Iterate through runs in the paragraph
        for (Run run : para.runs()) {
            processRun(run, docxParagraph, docxDocument, hwpxFile);
        }
    }
    
    /**
     * Process a run within a paragraph
     */
    private void processRun(Run run, XWPFParagraph docxParagraph, XWPFDocument docxDocument, HWPXFile hwpxFile) {
        // Apply character properties (font, size, bold, italic) from run.charPrIDRef()
        XWPFRun defaultRun = null;
        
        if (run.charPrIDRef() != null && hwpxFile.headerXMLFile() != null 
                && hwpxFile.headerXMLFile().refList() != null && hwpxFile.headerXMLFile().refList().charProperties() != null) {
            
            // Find matching CharPr by ID
            for (kr.dogfoot.hwpxlib.object.content.header_xml.references.CharPr charPr : 
                    hwpxFile.headerXMLFile().refList().charProperties().items()) {
                
                if (run.charPrIDRef().equals(charPr.id())) {
                    // Create a default run with the character properties
                    defaultRun = docxParagraph.createRun();
                    
                    // Apply font family
                    if (charPr.fontRef() != null) {
                        // Try to get the font for the current language or default to hangul
                        String fontName = null;
                        
                        // Priority: 1. Latin (English), 2. Hangul (Korean), 3. Hanja (Chinese), 4. Japanese, 5. Other
                        if (charPr.fontRef().latin() != null) {
                            fontName = charPr.fontRef().latin();
                        } else if (charPr.fontRef().hangul() != null) {
                            fontName = charPr.fontRef().hangul();
                        } else if (charPr.fontRef().hanja() != null) {
                            fontName = charPr.fontRef().hanja();
                        } else if (charPr.fontRef().japanese() != null) {
                            fontName = charPr.fontRef().japanese();
                        } else if (charPr.fontRef().other() != null) {
                            fontName = charPr.fontRef().other();
                        }
                        
                        if (fontName != null) {
                            defaultRun.setFontFamily(fontName);
                        }
                    }
                    
                    // Apply font size
                    if (charPr.height() != null) {
                        // HWP font size is in hwpunit (1/7200 inch), convert to half-points (1/144 inch)
                        int fontSizeHalfPoints = Math.round(charPr.height() * 10.0f / 100.0f);
                        defaultRun.setFontSize(fontSizeHalfPoints);
                    }
                    
                    // Apply text color
                    if (charPr.textColor() != null) {
                        // HWP color format is #RRGGBB
                        String colorStr = charPr.textColor();
                        if (colorStr.startsWith("#") && colorStr.length() == 7) {
                            defaultRun.setColor(colorStr.substring(1));
                        }
                    }
                    
                    // Apply bold
                    if (charPr.bold() != null) {
                        defaultRun.setBold(true);
                    }
                    
                    // Apply italic
                    if (charPr.italic() != null) {
                        defaultRun.setItalic(true);
                    }
                    
                    // Apply underline
                    if (charPr.underline() != null) {
                        defaultRun.setUnderline(org.apache.poi.xwpf.usermodel.UnderlinePatterns.SINGLE);
                    }
                    
                    // Apply strikethrough
                    if (charPr.strikeout() != null) {
                        defaultRun.setStrikeThrough(true);
                    }
                    
                    // Apply superscript
                    if (charPr.supscript() != null) {
                        defaultRun.setSubscript(org.apache.poi.xwpf.usermodel.VerticalAlign.SUPERSCRIPT);
                    }
                    
                    // Apply subscript
                    if (charPr.subscript() != null) {
                        defaultRun.setSubscript(org.apache.poi.xwpf.usermodel.VerticalAlign.SUBSCRIPT);
                    }
                    
                    break;
                }
            }
        }

        for (RunItem item : run.runItems()) {
            if (item instanceof T) {
                processTextComplex((T) item, docxParagraph, defaultRun);
            } else if (item instanceof Table) {
                processTable((Table) item, docxDocument, docxParagraph, hwpxFile);
            } else if (item instanceof Picture) {
                processPicture((Picture) item, docxParagraph, docxDocument, hwpxFile);
            } else if (item instanceof kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Line) {
                processLine((kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Line) item, 
                        docxParagraph, docxDocument, hwpxFile);
            } else if (item instanceof kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Rectangle) {
                processRectangle((kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Rectangle) item, 
                        docxParagraph, docxDocument, hwpxFile);
            } else if (item instanceof kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Ellipse) {
                processEllipse((kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Ellipse) item, 
                        docxParagraph, docxDocument, hwpxFile);
            } else if (item instanceof kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Arc) {
                processArc((kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Arc) item, 
                        docxParagraph, docxDocument, hwpxFile);
            } else if (item instanceof kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Polygon) {
                processPolygon((kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Polygon) item, 
                        docxParagraph, docxDocument, hwpxFile);
            } else if (item instanceof kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Curve) {
                processCurve((kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Curve) item, 
                        docxParagraph, docxDocument, hwpxFile);
            } else if (item instanceof kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Ctrl) {
                processControlCharacter((kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Ctrl) item, 
                        docxParagraph, docxDocument, hwpxFile);
            } else {
                log.warn("Unsupported RunItem type: {}", item.getClass().getName());
            }
        }
    }
    
    /**
     * Process a text element, including complex text with line breaks and tabs
     */
    private void processTextComplex(T textItem, XWPFParagraph docxParagraph, XWPFRun defaultRun) {
        if (textItem.isOnlyText()) {
            String textContent = textItem.onlyText() != null ? textItem.onlyText() : "";
            if (!textContent.isEmpty()) {
                XWPFRun docxRun;
                if (defaultRun != null) {
                    // Clone the properties from defaultRun
                    docxRun = docxParagraph.createRun();
                    // Copy font properties
                    docxRun.setFontFamily(defaultRun.getFontFamily());
                    docxRun.setFontSize(defaultRun.getFontSize());
                    docxRun.setColor(defaultRun.getColor());
                    docxRun.setBold(defaultRun.isBold());
                    docxRun.setItalic(defaultRun.isItalic());
                    docxRun.setUnderline(defaultRun.getUnderline());
                    docxRun.setStrikeThrough(defaultRun.isStrikeThrough());
                } else {
                    docxRun = docxParagraph.createRun();
                }
                docxRun.setText(textContent);
            }
        } else {
            for (TItem tSubItem : textItem.items()) {
                if (tSubItem instanceof NormalText) {
                    String textContent = ((NormalText) tSubItem).text();
                    if (textContent != null && !textContent.isEmpty()) {
                        XWPFRun docxRun;
                        if (defaultRun != null) {
                            // Clone the properties from defaultRun
                            docxRun = docxParagraph.createRun();
                            // Copy font properties
                            docxRun.setFontFamily(defaultRun.getFontFamily());
                            docxRun.setFontSize(defaultRun.getFontSize());
                            docxRun.setColor(defaultRun.getColor());
                            docxRun.setBold(defaultRun.isBold());
                            docxRun.setItalic(defaultRun.isItalic());
                            docxRun.setUnderline(defaultRun.getUnderline());
                            docxRun.setStrikeThrough(defaultRun.isStrikeThrough());
                        } else {
                            docxRun = docxParagraph.createRun();
                        }
                        docxRun.setText(textContent);
                    }
                }
                else if (tSubItem instanceof kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.t.LineBreak) {
                    XWPFRun docxRun = docxParagraph.createRun();
                    docxRun.addBreak();
                }
                else if (tSubItem instanceof kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.t.Tab) {
                    XWPFRun docxRun = docxParagraph.createRun();
                    docxRun.addTab();
                }
            }
        }
    }
    
    /**
     * Process a table element
     */
    private void processTable(Table tableItem, XWPFDocument docxDocument, XWPFParagraph currentPara, HWPXFile hwpxFile) {
        log.debug("Processing table: {}", tableItem.id());

        int numRows = tableItem.countOfTr();
        int numCols = estimateColumnCount(tableItem);

        if (numRows <= 0 || numCols <= 0) {
            log.warn("Skipping table with zero rows or estimated columns: {}", tableItem.id());
            return;
        }

        XWPFTable docxTable = docxDocument.createTable(numRows, numCols);

        // Iterate through rows (tr)
        for (int i = 0; i < numRows; i++) {
            Tr hwpxRow = tableItem.getTr(i);
            XWPFTableRow docxRow = docxTable.getRow(i);

            int currentDocxColIndex = 0;
            // Iterate through cells (tc)
            for (Tc hwpxCell : hwpxRow.tcs()) {
                if (currentDocxColIndex >= numCols) {
                    log.warn("More HWPX cells found than estimated columns for table {}, row {}. Cell ID: {}", 
                             tableItem.id(), i, hwpxCell.name());
                    continue;
                }

                XWPFTableCell docxCell = docxRow.getCell(currentDocxColIndex);
                
                // Apply cell properties
                CTTcPr tcPr = getOrCreateTcPr(docxCell);
                
                // Handle cell merging (colSpan, rowSpan)
                int colSpan = getColSpan(hwpxCell);
                int rowSpan = getRowSpan(hwpxCell);
                
                // Apply column span (horizontal merging)
                if (colSpan > 1) {
                    // set grid span (width in cells)
                    tcPr.addNewGridSpan().setVal(BigInteger.valueOf(colSpan));
                    
                    // POI creates cells in advance, we need to remove the spanned cells
                    for (int j = 1; j < colSpan && currentDocxColIndex + j < numCols; j++) {
                        // Remove the next cell that would be covered by this span
                        try {
                            docxRow.removeCell(currentDocxColIndex + 1);
                        } catch (Exception e) {
                            log.warn("Could not remove spanned cell at column index {} in row {}", 
                                    (currentDocxColIndex + 1), i);
                        }
                    }
                }
                
                // Apply row span (vertical merging)
                if (rowSpan > 1) {
                    // Cell is starting vertical merge
                    CTVMerge vmerge = tcPr.addNewVMerge();
                    vmerge.setVal(STMerge.RESTART);
                    
                    // Apply continuation of vertical merge to cells below
                    for (int j = 1; j < rowSpan && i + j < numRows; j++) {
                        try {
                            XWPFTableRow targetRow = docxTable.getRow(i + j);
                            if (targetRow != null) {
                                // Need to account for previous horizontal spans in this row
                                int targetColIndex = currentDocxColIndex;
                                
                                if (targetColIndex < targetRow.getTableCells().size()) {
                                    XWPFTableCell targetCell = targetRow.getCell(targetColIndex);
                                    CTTcPr targetTcPr = getOrCreateTcPr(targetCell);
                                    
                                    // Mark as continuation of vertical merge
                                    CTVMerge targetVMerge = targetTcPr.addNewVMerge();
                                    targetVMerge.setVal(STMerge.CONTINUE);
                                    
                                    // Apply same colSpan to the continued merged cell
                                    if (colSpan > 1) {
                                        targetTcPr.addNewGridSpan().setVal(BigInteger.valueOf(colSpan));
                                        
                                        // Attempt to remove extra cells
                                        for (int k = 1; k < colSpan && targetColIndex + k < numCols; k++) {
                                            try {
                                                targetRow.removeCell(targetColIndex + 1);
                                            } catch (Exception e) {
                                                log.warn("Could not remove spanned cell at column index {} in row {}", 
                                                        (targetColIndex + k), (i + j));
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Error applying vMerge at row {}: {}", (i + j), e.getMessage());
                        }
                    }
                }

                // Process cell content
                // Clear existing content
                for (int p = docxCell.getParagraphs().size() - 1; p >= 0; p--) {
                    docxCell.removeParagraph(p);
                }

                if (hwpxCell.subList() != null) {
                    for (Para cellPara : hwpxCell.subList().paras()) {
                        XWPFParagraph cellDocxParagraph = docxCell.addParagraph();
                        for (Run cellRun : cellPara.runs()) {
                            processRun(cellRun, cellDocxParagraph, docxDocument, hwpxFile);
                        }
                    }
                    
                    if (hwpxCell.subList().countOfPara() == 0) {
                        if (docxCell.getParagraphs().size() == 0) {
                            docxCell.addParagraph();
                        }
                    }
                } else {
                    if (docxCell.getParagraphs().size() == 0) {
                        docxCell.addParagraph();
                    }
                }

                currentDocxColIndex += colSpan;
            }
        }

        docxDocument.createParagraph();
    }
    
    /**
     * Process a picture element
     */
    private void processPicture(Picture pictureItem, XWPFParagraph docxParagraph, XWPFDocument docxDocument, HWPXFile hwpxFile) {
        String binaryItemIdRef = null;
        if (pictureItem.img() != null) {
            binaryItemIdRef = pictureItem.img().binaryItemIDRef();
        }

        if (binaryItemIdRef == null) {
            log.error("Picture item has no image reference (binaryItemIDRef)");
            XWPFRun errorRun = docxParagraph.createRun();
            errorRun.setText("[Picture Error: Missing Image Reference]");
            return;
        }

        String imageHref = null;
        String imageMimeType = null;
        // Find the manifest item using the binaryItemIDRef to get the actual image file path (href) and mimetype
        if (hwpxFile.contentHPFFile() != null && hwpxFile.contentHPFFile().manifest() != null) {
            for (ManifestItem item : hwpxFile.contentHPFFile().manifest().items()) {
                if (binaryItemIdRef.equals(item.id())) {
                    imageHref = item.href();
                    imageMimeType = item.mediaType();
                    break;
                }
            }
        }

        if (imageHref == null) {
            log.error("Could not find manifest item href for binaryItemIDRef: {}", binaryItemIdRef);
            XWPFRun errorRun = docxParagraph.createRun();
            errorRun.setText("[Picture Error: Cannot find manifest href for ID " + binaryItemIdRef + "]");
            return;
        }

        // Get the image data from the HWPX package using the href
        byte[] imageData = findImageDataByHref(imageHref, hwpxFile);

        if (imageData == null) {
            log.error("Could not load image data for href: {}", imageHref);
            XWPFRun errorRun = docxParagraph.createRun();
            errorRun.setText("[Picture Error: Cannot load data for " + imageHref + "]");
            return;
        }

        // Determine Image Type
        int poiPictureType = determinePoiPictureType(imageHref, imageMimeType);
        if (poiPictureType == -1) {
            log.error("Unsupported or unknown image type for: {} (Mime: {})", imageHref, imageMimeType);
            XWPFRun errorRun = docxParagraph.createRun();
            errorRun.setText("[Picture Error: Unsupported type " + imageHref + "]");
            return;
        }

        // Get image dimensions
        long widthHwp = 0;
        long heightHwp = 0;
        if (pictureItem.sz() != null) {
            widthHwp = pictureItem.sz().width();
            heightHwp = pictureItem.sz().height();
        }

        // Add image to the document
        try (InputStream imageInputStream = new ByteArrayInputStream(imageData)) {
            XWPFRun docxRun = docxParagraph.createRun();
            docxRun.addPicture(
                new ByteArrayInputStream(imageData), 
                poiPictureType, 
                imageHref, 
                Units.toEMU(widthHwp / 72.0), 
                Units.toEMU(heightHwp / 72.0)
            );
        } catch (Exception e) {
            log.error("Error embedding image {}: {}", imageHref, e.getMessage());
            XWPFRun errorRun = docxParagraph.createRun();
            errorRun.setText("[Picture Error: Embedding failed for " + imageHref + "]");
        }
    }
    
    /**
     * Find image data by href in the HWPX file
     */
    private byte[] findImageDataByHref(String imageHref, HWPXFile hwpxFile) {
        byte[] imageData = null;
        
        // Check manifest items first
        if (hwpxFile.contentHPFFile() != null && hwpxFile.contentHPFFile().manifest() != null) {
            for (ManifestItem item : hwpxFile.contentHPFFile().manifest().items()) {
                if (imageHref.equals(item.href()) && item.attachedFile() != null) {
                   imageData = item.attachedFile().data();
                   break;
                }
            }
        }

        // Fallback: Check root file entries
        if (imageData == null && hwpxFile.containerXMLFile() != null && hwpxFile.containerXMLFile().rootFiles() != null) {
            for (RootFile rootFile : hwpxFile.containerXMLFile().rootFiles().items()) {
                if (imageHref.equals(rootFile.fullPath()) && rootFile.attachedFile() != null) {
                    imageData = rootFile.attachedFile().data();
                    break;
                }
            }
        }
        
        return imageData;
    }
    
    /**
     * Determine POI picture type from filename or MIME type
     */
    private int determinePoiPictureType(String filename, String mimeType) {
        String ext = "";
        if (filename != null && filename.contains(".")) {
             ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        }

        // Prioritize mimetype if available
        if (mimeType != null) {
            mimeType = mimeType.toLowerCase();
            if (mimeType.contains("jpeg") || mimeType.contains("jpg")) return XWPFDocument.PICTURE_TYPE_JPEG;
            if (mimeType.contains("png")) return XWPFDocument.PICTURE_TYPE_PNG;
            if (mimeType.contains("gif")) return XWPFDocument.PICTURE_TYPE_GIF;
            if (mimeType.contains("bmp")) return XWPFDocument.PICTURE_TYPE_BMP;
            if (mimeType.contains("emf")) return XWPFDocument.PICTURE_TYPE_EMF;
            if (mimeType.contains("wmf")) return XWPFDocument.PICTURE_TYPE_WMF;
            if (mimeType.contains("pict")) return XWPFDocument.PICTURE_TYPE_PICT;
            if (mimeType.contains("tiff")) return XWPFDocument.PICTURE_TYPE_TIFF;
            if (mimeType.contains("dib")) return XWPFDocument.PICTURE_TYPE_DIB;
            if (mimeType.contains("eps")) return XWPFDocument.PICTURE_TYPE_EPS;
            if (mimeType.contains("wpg")) return XWPFDocument.PICTURE_TYPE_WPG;
        }

        // Fallback to extension if mimetype didn't match or wasn't provided
        switch (ext) {
            case "emf": return XWPFDocument.PICTURE_TYPE_EMF;
            case "wmf": return XWPFDocument.PICTURE_TYPE_WMF;
            case "pict": return XWPFDocument.PICTURE_TYPE_PICT;
            case "jpeg":
            case "jpg": return XWPFDocument.PICTURE_TYPE_JPEG;
            case "png": return XWPFDocument.PICTURE_TYPE_PNG;
            case "dib": return XWPFDocument.PICTURE_TYPE_DIB;
            case "gif": return XWPFDocument.PICTURE_TYPE_GIF;
            case "tiff":
            case "tif": return XWPFDocument.PICTURE_TYPE_TIFF;
            case "eps": return XWPFDocument.PICTURE_TYPE_EPS;
            case "bmp": return XWPFDocument.PICTURE_TYPE_BMP;
            case "wpg": return XWPFDocument.PICTURE_TYPE_WPG;
            default: return -1; // Unknown or unsupported
        }
    }
    
    /**
     * Estimate the number of columns in a table
     */
    private int estimateColumnCount(Table tableItem) {
        int maxCols = 0;
        if (tableItem.countOfTr() > 0) {
            Tr firstRow = tableItem.getTr(0);
            int colsInFirstRow = 0;
             for (Tc hwpxCell : firstRow.tcs()) {
                 colsInFirstRow += getColSpan(hwpxCell);
             }
            maxCols = colsInFirstRow;

            // Check other rows in case of complex merges
             for (int i = 1; i < tableItem.countOfTr(); i++) {
                 Tr row = tableItem.getTr(i);
                 int colsInRow = 0;
                 for (Tc hwpxCell : row.tcs()) {
                    colsInRow += getColSpan(hwpxCell);
                 }
                 if (colsInRow > maxCols) {
                    log.warn("Table {} has inconsistent column count across rows. Using max observed: {}", 
                             tableItem.id(), colsInRow);
                    maxCols = colsInRow;
                 }
             }
        }
        
        // Fallback if no rows
        if (maxCols == 0) {
             log.warn("Could not estimate columns for table: {}. Defaulting to 1.", tableItem.id());
             return 1;
        }
        return maxCols;
    }
    
    /**
     * Get or create CTTcPr for a table cell
     */
    private CTTcPr getOrCreateTcPr(XWPFTableCell cell) {
        if (cell.getCTTc().getTcPr() == null) {
            cell.getCTTc().addNewTcPr();
        }
        return cell.getCTTc().getTcPr();
    }
    
    /**
     * Get column span for a table cell
     */
    private int getColSpan(Tc cell) {
        CellSpan span = cell.cellSpan();
        if (span != null && span.colSpan() != null) {
            return span.colSpan();
        }
        return 1; // Default ColSpan
    }
    
    /**
     * Get row span for a table cell
     */
    private int getRowSpan(Tc cell) {
        CellSpan span = cell.cellSpan();
        if (span != null && span.rowSpan() != null) {
            return span.rowSpan();
        }
        return 1; // Default RowSpan
    }

    /**
     * Process a line drawing object
     */
    private void processLine(kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Line lineItem, 
                             XWPFParagraph docxParagraph, XWPFDocument docxDocument, HWPXFile hwpxFile) {
        // TODO: Implement full line drawing support when needed
        log.debug("Line drawing object {} partially supported", lineItem.id());
        XWPFRun docxRun = docxParagraph.createRun();
        docxRun.setText("[Line Drawing]");
    }

    /**
     * Process a rectangle drawing object
     */
    private void processRectangle(kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Rectangle rectItem, 
                             XWPFParagraph docxParagraph, XWPFDocument docxDocument, HWPXFile hwpxFile) {
        // TODO: Implement full rectangle drawing support when needed
        log.debug("Rectangle drawing object {} partially supported", rectItem.id());
        XWPFRun docxRun = docxParagraph.createRun();
        docxRun.setText("[Rectangle Drawing]");
    }

    /**
     * Process an ellipse drawing object
     */
    private void processEllipse(kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Ellipse ellipseItem, 
                           XWPFParagraph docxParagraph, XWPFDocument docxDocument, HWPXFile hwpxFile) {
        // TODO: Implement full ellipse drawing support when needed
        log.debug("Ellipse drawing object {} partially supported", ellipseItem.id());
        XWPFRun docxRun = docxParagraph.createRun();
        docxRun.setText("[Ellipse Drawing]");
    }

    /**
     * Process an arc drawing object
     */
    private void processArc(kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Arc arcItem, 
                       XWPFParagraph docxParagraph, XWPFDocument docxDocument, HWPXFile hwpxFile) {
        // TODO: Implement full arc drawing support when needed
        log.debug("Arc drawing object {} partially supported", arcItem.id());
        XWPFRun docxRun = docxParagraph.createRun();
        docxRun.setText("[Arc Drawing]");
    }

    /**
     * Process a polygon drawing object
     */
    private void processPolygon(kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Polygon polygonItem, 
                           XWPFParagraph docxParagraph, XWPFDocument docxDocument, HWPXFile hwpxFile) {
        // TODO: Implement full polygon drawing support when needed
        log.debug("Polygon drawing object {} partially supported", polygonItem.id());
        XWPFRun docxRun = docxParagraph.createRun();
        docxRun.setText("[Polygon Drawing]");
    }

    /**
     * Process a curve drawing object
     */
    private void processCurve(kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Curve curveItem, 
                         XWPFParagraph docxParagraph, XWPFDocument docxDocument, HWPXFile hwpxFile) {
        // TODO: Implement full curve drawing support when needed
        log.debug("Curve drawing object {} partially supported", curveItem.id());
        XWPFRun docxRun = docxParagraph.createRun();
        docxRun.setText("[Curve Drawing]");
    }

    /**
     * Process control characters
     */
    private void processControlCharacter(kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Ctrl ctrlItem,
                                    XWPFParagraph docxParagraph, XWPFDocument docxDocument, HWPXFile hwpxFile) {
        // TODO: Implement control character support when needed
        log.debug("Control character {} partially supported", ctrlItem._objectType());
        
        // Special handling for common control characters
        String controlId = ctrlItem._objectType().toString();
        if (controlId != null) {
            if (controlId.contains("char")) {
                // This might be a special character, add a space or placeholder
                XWPFRun docxRun = docxParagraph.createRun();
                docxRun.setText(" ");
            } else if (controlId.contains("bookmark")) {
                // Could be implemented as Word bookmark
                // For now, just add a minimal indicator
                XWPFRun docxRun = docxParagraph.createRun();
                docxRun.setText("[Bookmark]");
            } else if (controlId.contains("header") || controlId.contains("footer")) {
                // Header/footer controls require special handling at document level
                // For now, just add a minimal indicator
                XWPFRun docxRun = docxParagraph.createRun();
                docxRun.setText("[Header/Footer]");
            } else if (controlId.contains("footnote") || controlId.contains("endnote")) {
                // Footnote/endnote controls require special handling
                // For now, just add a minimal indicator
                XWPFRun docxRun = docxParagraph.createRun();
                docxRun.setText("[Footnote/Endnote]");
            } else {
                // Generic control character
                XWPFRun docxRun = docxParagraph.createRun();
                docxRun.setText("[Control]");
            }
        }
    }
} 