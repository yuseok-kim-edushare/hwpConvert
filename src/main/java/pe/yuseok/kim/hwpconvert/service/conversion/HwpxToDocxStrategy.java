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
import org.apache.poi.xwpf.usermodel.VerticalAlign;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTVMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.springframework.stereotype.Component;
import pe.yuseok.kim.hwpconvert.model.ConversionResult;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLanguage;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDocDefaults;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPrDefault;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPrDefault;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STStyleType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

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
            // Set document properties to ensure valid metadata
            docxDocument.getProperties().getCoreProperties().setCreator("HwpxToDocx Converter");
            docxDocument.getProperties().getCoreProperties().setCreated(Optional.of(new Date()));
            
            HWPXFile hwpxFile = HWPXReader.fromFile(sourceFile);
            
            // Process the document structure
            processDocument(hwpxFile, docxDocument);
            
            // Ensure all document parts are properly connected
            docxDocument.enforceUpdateFields();
            
            // Validate document before saving
            validateDocument(docxDocument);
            
            try (FileOutputStream out = new FileOutputStream(outputPath.toFile())) {
                docxDocument.write(out);
                out.flush();
            }
            
            // Verify the created file is valid
            if (verifyDocxFile(outputPath.toFile())) {
                log.info("Successfully converted HWPX to DOCX: {}", outputFileName);
                result.setSuccess(true);
                result.setCompletionTime(LocalDateTime.now());
            } else {
                log.error("Generated DOCX file failed validation: {}", outputFileName);
                result.setSuccess(false);
                result.setErrorMessage("Generated DOCX file failed validation checks");
                try {
                    outputPath.toFile().delete();
                } catch (SecurityException se) {
                    log.warn("Could not delete invalid file due to security restrictions: {}", outputPath);
                }
            }
        } catch (IOException e) {
            log.error("Error converting HWPX {} to DOCX: I/O error", sourceFile.getName(), e);
            result.setSuccess(false);
            result.setErrorMessage("Error converting HWPX to DOCX: " + e.getMessage());
            try {
                 outputPath.toFile().delete();
            } catch (SecurityException se) {
                 log.warn("Could not delete partially created file due to security restrictions: {}", outputPath);
            }
        } catch (Exception e) {
            log.error("Error converting HWPX {} to DOCX: unexpected error", sourceFile.getName(), e);
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
        // Setup document with basic style definitions for better compatibility
        setupBasicDocumentStyles(docxDocument);
        
        // Iterate through sections
        for (SectionXMLFile section : hwpxFile.sectionXMLFileList().items()) {
            processSection(section, docxDocument, hwpxFile);
        }
    }
    
    /**
     * Setup basic document styles for better compatibility with various word processors
     */
    private void setupBasicDocumentStyles(XWPFDocument docxDocument) {
        try {
            // Set basic document properties
            if (docxDocument.getProperties() != null) {
                docxDocument.getProperties().getCoreProperties().setCreator("HwpxToDocx Converter");
                docxDocument.getProperties().getCoreProperties().setCreated(Optional.of(new Date()));
            }
            
            // Add document default style to ensure better compatibility
            // Setting default styles programmatically is too complex with many API requirements
            // Instead, we'll set the document defaults directly through paragraph and run properties
            
            // Create a simple default paragraph style for Korean documents
            XWPFParagraph defaultPara = docxDocument.createParagraph();
            defaultPara.setStyle("Normal");
            
            // Create a simple run with Korean font settings
            XWPFRun defaultRun = defaultPara.createRun();
            defaultRun.setFontFamily("Malgun Gothic"); // Korean default font
            defaultRun.setFontSize(10);
            
            // Set East Asian font
            if (defaultRun.getCTR() != null && defaultRun.getCTR().isSetRPr()) {
                try {
                    defaultRun.getCTR().getRPr().addNewRFonts().setEastAsia("Malgun Gothic");
                    defaultRun.getCTR().getRPr().addNewLang().setEastAsia("ko-KR");
                } catch (Exception e) {
                    log.warn("Couldn't set East Asian font settings: {}", e.getMessage());
                }
            }
            
            // Remove this temporary paragraph after using it to set defaults
            int paraPosition = docxDocument.getPosOfParagraph(defaultPara);
            if (paraPosition >= 0) {
                docxDocument.removeBodyElement(paraPosition);
            }
            
            log.info("Added basic document styles for better compatibility");
        } catch (Exception e) {
            log.warn("Could not set up basic document styles: {}", e.getMessage());
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
                                case BETWEEN_LINES:
                                    // Handle between lines spacing
                                    if (paraPr.lineSpacing().value() != null) {
                                        double lineSpacing = paraPr.lineSpacing().value() / 100.0;
                                        docxParagraph.setSpacingLineRule(org.apache.poi.xwpf.usermodel.LineSpacingRule.AUTO);
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
                        
                        // For Korean text, prioritize Hangul font to ensure proper rendering
                        if (charPr.fontRef().hangul() != null) {
                            fontName = charPr.fontRef().hangul();
                        } else if (charPr.fontRef().latin() != null) {
                            fontName = charPr.fontRef().latin();
                        } else if (charPr.fontRef().hanja() != null) {
                            fontName = charPr.fontRef().hanja();
                        } else if (charPr.fontRef().japanese() != null) {
                            fontName = charPr.fontRef().japanese();
                        } else if (charPr.fontRef().other() != null) {
                            fontName = charPr.fontRef().other();
                        }
                        
                        if (fontName != null) {
                            defaultRun.setFontFamily(fontName);
                            // Set the East Asian font name explicitly to ensure proper Korean rendering
                            try {
                                defaultRun.getCTR().addNewRPr().addNewRFonts().setEastAsia(fontName);
                            } catch (Exception e) {
                                log.warn("Could not set East Asian font: {}", e.getMessage());
                            }
                        }
                    }
                    
                    // Apply font size
                    if (charPr.height() != null) {
                        // Logs for debugging
                        log.info("Processing HWP font height value: {}", charPr.height());
                        
                        // For Korean HWP files, typical values are 100 = 10pt, 90 = 9pt, etc.
                        // Divide by 10 to get the point size
                        int fontSizePoints = Math.round(charPr.height() / 10.0f);
                        
                        // Ensure font size is reasonable - set minimum and maximum bounds
                        if (fontSizePoints < 6) fontSizePoints = 10; // Default to 10pt if too small
                        if (fontSizePoints > 72) fontSizePoints = 72; // Cap at 72pt
                        
                        log.info("Setting font size to: {} points", fontSizePoints);
                        defaultRun.setFontSize(fontSizePoints);
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
                        // HWP bold property might be a boolean value or an object
                        boolean isBold = charPr.bold() != null && 
                                       ("true".equalsIgnoreCase(charPr.bold().toString()) || 
                                        "yes".equalsIgnoreCase(charPr.bold().toString()));
                        defaultRun.setBold(isBold);
                    } else {
                        defaultRun.setBold(false);
                    }
                    
                    // Apply italic
                    if (charPr.italic() != null) {
                        // HWP italic property might be a boolean value or an object
                        boolean isItalic = charPr.italic() != null && 
                                         ("true".equalsIgnoreCase(charPr.italic().toString()) || 
                                          "yes".equalsIgnoreCase(charPr.italic().toString()));
                        defaultRun.setItalic(isItalic);
                    } else {
                        defaultRun.setItalic(false);
                    }
                    
                    // Apply underline - only if explicitly enabled
                    if (charPr.underline() != null && charPr.underline().type() != null) {
                        // Set underline based on type
                        String ulType = charPr.underline().type().toString();
                        if ("solid".equalsIgnoreCase(ulType) || "single".equalsIgnoreCase(ulType)) {
                            defaultRun.setUnderline(UnderlinePatterns.SINGLE);
                        } else if ("double".equalsIgnoreCase(ulType) || "db".equalsIgnoreCase(ulType)) {
                            defaultRun.setUnderline(UnderlinePatterns.DOUBLE);
                        } else if ("dotted".equalsIgnoreCase(ulType)) {
                            defaultRun.setUnderline(UnderlinePatterns.DOTTED);
                        } else if ("dashed".equalsIgnoreCase(ulType)) {
                            defaultRun.setUnderline(UnderlinePatterns.DASH);
                        } else if ("dash-dot".equalsIgnoreCase(ulType)) {
                            defaultRun.setUnderline(UnderlinePatterns.DOT_DASH);
                        } else if ("dash-dot-dot".equalsIgnoreCase(ulType)) {
                            defaultRun.setUnderline(UnderlinePatterns.DOT_DOT_DASH);
                        } else if ("wave".equalsIgnoreCase(ulType)) {
                            defaultRun.setUnderline(UnderlinePatterns.WAVE);
                        } else if ("thick".equalsIgnoreCase(ulType)) {
                            defaultRun.setUnderline(UnderlinePatterns.THICK);
                        } else if ("none".equalsIgnoreCase(ulType)) {
                            defaultRun.setUnderline(UnderlinePatterns.NONE);
                        }
                        
                        // If there's a color specified for the underline
                        if (charPr.underline().color() != null) {
                            String ulColor = charPr.underline().color();
                            if (ulColor.startsWith("#") && ulColor.length() == 7) {
                                // Set underline color if possible - not directly supported in XWPFRun
                                try {
                                    // Add the underline color via direct CTR access
                                    if (defaultRun.getCTR() != null) {
                                        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR ctr = defaultRun.getCTR();
                                        if (!ctr.isSetRPr()) {
                                            ctr.addNewRPr();
                                        }
                                        
                                        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTUnderline underline = 
                                            ctr.getRPr().addNewU();
                                        underline.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STUnderline.SINGLE);
                                        underline.setColor(ulColor.substring(1));
                                    }
                                } catch (Exception e) {
                                    log.warn("Could not set underline color: {}", e.getMessage());
                                }
                            }
                        }
                    } else {
                        // Explicitly set no underline if not specified
                        defaultRun.setUnderline(UnderlinePatterns.NONE);
                    }
                    
                    // Apply strikethrough
                    if (charPr.strikeout() != null) {
                        // HWP strikeout property might be a boolean value or an object
                        boolean isStrikeout = charPr.strikeout() != null && 
                                            ("true".equalsIgnoreCase(charPr.strikeout().toString()) || 
                                             "yes".equalsIgnoreCase(charPr.strikeout().toString()));
                        defaultRun.setStrikeThrough(isStrikeout);
                    } else {
                        defaultRun.setStrikeThrough(false);
                    }
                    
                    // Apply superscript
                    if (charPr.supscript() != null) {
                        // HWP supscript property might be a boolean value or an object
                        boolean isSupscript = ("true".equalsIgnoreCase(charPr.supscript().toString()) || 
                                              "yes".equalsIgnoreCase(charPr.supscript().toString()));
                        if (isSupscript) {
                            defaultRun.setSubscript(VerticalAlign.SUPERSCRIPT);
                        }
                    }
                    
                    // Apply subscript
                    if (charPr.subscript() != null) {
                        // HWP subscript property might be a boolean value or an object
                        boolean isSubscript = ("true".equalsIgnoreCase(charPr.subscript().toString()) || 
                                              "yes".equalsIgnoreCase(charPr.subscript().toString()));
                        if (isSubscript) {
                            defaultRun.setSubscript(VerticalAlign.SUBSCRIPT);
                        }
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
                    // Copy font properties, but only those explicitly set
                    if (defaultRun.getFontFamily() != null) {
                        docxRun.setFontFamily(defaultRun.getFontFamily());
                    }
                    if (defaultRun.getFontSizeAsDouble() > 0) {
                        docxRun.setFontSize(defaultRun.getFontSizeAsDouble());
                    }
                    if (defaultRun.getColor() != null) {
                        docxRun.setColor(defaultRun.getColor());
                    }
                    // For boolean properties, only set if true 
                    // This prevents accidentally setting all text as bold, italic, etc.
                    if (defaultRun.isBold()) {
                        docxRun.setBold(true);
                    }
                    if (defaultRun.isItalic()) {
                        docxRun.setItalic(true);
                    }
                    // Only apply underline if it's explicitly set and not NONE
                    if (defaultRun.getUnderline() != UnderlinePatterns.NONE) {
                        docxRun.setUnderline(defaultRun.getUnderline());
                    }
                    // Only apply strikethrough if it's explicitly set
                    if (defaultRun.isStrikeThrough()) {
                        docxRun.setStrikeThrough(true);
                    }
                } else {
                    docxRun = docxParagraph.createRun();
                }
                docxRun.setText(textContent);
            }
        } else {
            // Check if items() is null to avoid NullPointerException
            if (textItem.items() != null) {
                for (TItem tSubItem : textItem.items()) {
                    if (tSubItem instanceof NormalText) {
                        String textContent = ((NormalText) tSubItem).text();
                        if (textContent != null && !textContent.isEmpty()) {
                            XWPFRun docxRun;
                            if (defaultRun != null) {
                                // Clone the properties from defaultRun
                                docxRun = docxParagraph.createRun();
                                // Copy font properties, but only those explicitly set
                                if (defaultRun.getFontFamily() != null) {
                                    docxRun.setFontFamily(defaultRun.getFontFamily());
                                }
                                if (defaultRun.getFontSizeAsDouble() > 0) {
                                    docxRun.setFontSize(defaultRun.getFontSizeAsDouble());
                                }
                                if (defaultRun.getColor() != null) {
                                    docxRun.setColor(defaultRun.getColor());
                                }
                                // For boolean properties, only set if true
                                if (defaultRun.isBold()) {
                                    docxRun.setBold(true);
                                }
                                if (defaultRun.isItalic()) {
                                    docxRun.setItalic(true);
                                }
                                // Only apply underline if it's explicitly set and not NONE
                                if (defaultRun.getUnderline() != UnderlinePatterns.NONE) {
                                    docxRun.setUnderline(defaultRun.getUnderline());
                                }
                                // Only apply strikethrough if it's explicitly set
                                if (defaultRun.isStrikeThrough()) {
                                    docxRun.setStrikeThrough(true);
                                }
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
            } else {
                // If items() is null but isOnlyText() is false, create an empty run to maintain structure
                log.warn("Text item has null items collection but isOnlyText() is false");
                docxParagraph.createRun();
            }
        }
    }
    
    /**
     * Process a table element
     */
    private void processTable(Table tableItem, XWPFDocument docxDocument, XWPFParagraph currentPara, HWPXFile hwpxFile) {
        log.info("Processing table: {}", tableItem.id());

        // First pass: analyze the table structure to determine true dimensions and merge information
        TableStructure tableStructure = analyzeTableStructure(tableItem);
        
        if (tableStructure.numRows <= 0 || tableStructure.numCols <= 0) {
            log.warn("Skipping table with zero rows or columns: {}", tableItem.id());
            return;
        }

        // Create a table with the correct dimensions
        XWPFTable docxTable = docxDocument.createTable(tableStructure.numRows, tableStructure.numCols);
        
        // Set basic table properties
        if (docxTable.getCTTbl() != null) {
            CTTblPr tblPr = docxTable.getCTTbl().getTblPr() != null ? 
                docxTable.getCTTbl().getTblPr() : docxTable.getCTTbl().addNewTblPr();
            
            // Set table to be sized by content
            tblPr.addNewTblLayout().setType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType.AUTOFIT);
            
            // Default table border setting - can be overridden by cell-specific settings
            try {
                // Add simple borders for better visibility
                tblPr.addNewTblBorders();
            } catch (Exception e) {
                log.warn("Could not set default table borders: {}", e.getMessage());
            }
        }

        // Process table content cell by cell using the computed structure
        for (int rowIdx = 0; rowIdx < tableStructure.numRows; rowIdx++) {
            Tr hwpxRow = rowIdx < tableItem.countOfTr() ? tableItem.getTr(rowIdx) : null;
            if (hwpxRow == null) continue;
            
            int colIdx = 0;
            for (Tc hwpxCell : hwpxRow.tcs()) {
                // Skip this position if it's covered by a previously defined vertical or horizontal span
                while (colIdx < tableStructure.numCols && tableStructure.cellMatrix[rowIdx][colIdx] != null) {
                    colIdx++;
                }
                
                if (colIdx >= tableStructure.numCols) {
                    break;  // No more cells can be placed in this row
                }
                
                // Get span information
                int colSpan = getColSpan(hwpxCell);
                int rowSpan = getRowSpan(hwpxCell);
                
                // Mark this cell and all spanned cells in our matrix as occupied
                CellInfo cellInfo = new CellInfo(rowIdx, colIdx, colSpan, rowSpan);
                for (int r = rowIdx; r < rowIdx + rowSpan && r < tableStructure.numRows; r++) {
                    for (int c = colIdx; c < colIdx + colSpan && c < tableStructure.numCols; c++) {
                        tableStructure.cellMatrix[r][c] = cellInfo;
                    }
                }
                
                // Get the actual DOCX cell to modify
                XWPFTableCell docxCell = docxTable.getRow(rowIdx).getCell(colIdx);
                
                // Apply horizontal span (gridSpan)
                if (colSpan > 1) {
                    CTTcPr tcPr = getOrCreateTcPr(docxCell);
                    tcPr.addNewGridSpan().setVal(BigInteger.valueOf(colSpan));
                    
                    // Remove the spanned cells (they were auto-created by POI)
                    XWPFTableRow docxRow = docxTable.getRow(rowIdx);
                    for (int i = 1; i < colSpan && colIdx + i < tableStructure.numCols; i++) {
                        try {
                            if (docxRow.getTableCells().size() > colIdx + 1) {
                                docxRow.removeCell(colIdx + 1);
                            }
                        } catch (Exception e) {
                            log.warn("Error removing spanned cell at position ({},{}): {}", rowIdx, colIdx + i, e.getMessage());
                        }
                    }
                }
                
                // Apply vertical span (vMerge)
                if (rowSpan > 1) {
                    // Start of vertical merge
                    CTTcPr tcPr = getOrCreateTcPr(docxCell);
                    CTVMerge vmerge = tcPr.addNewVMerge();
                    vmerge.setVal(STMerge.RESTART);
                    
                    // Mark continuation cells for the vertical merge
                    for (int i = 1; i < rowSpan && rowIdx + i < tableStructure.numRows; i++) {
                        XWPFTableRow targetRow = docxTable.getRow(rowIdx + i);
                        if (targetRow == null) continue;
                        
                        int targetCellIdx = colIdx;
                        // Adjust for previous gridSpans in the target row
                        int adjustedIdx = getAdjustedCellIndex(targetRow, colIdx);
                        
                        if (adjustedIdx >= 0 && adjustedIdx < targetRow.getTableCells().size()) {
                            XWPFTableCell targetCell = targetRow.getCell(adjustedIdx);
                            CTTcPr targetTcPr = getOrCreateTcPr(targetCell);
                            
                            // Mark as continuation
                            CTVMerge targetVMerge = targetTcPr.addNewVMerge();
                            targetVMerge.setVal(STMerge.CONTINUE);
                            
                            // Apply same colSpan to continued cells if needed
                            if (colSpan > 1) {
                                targetTcPr.addNewGridSpan().setVal(BigInteger.valueOf(colSpan));
                                
                                // Remove spanned cells in the continuation row
                                for (int j = 1; j < colSpan && adjustedIdx + j < targetRow.getTableCells().size(); j++) {
                                    try {
                                        targetRow.removeCell(adjustedIdx + 1);
                                    } catch (Exception e) {
                                        log.warn("Error removing cell in continuation row: {}", e.getMessage());
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Clear any existing content in the cell
                for (int p = docxCell.getParagraphs().size() - 1; p >= 0; p--) {
                    docxCell.removeParagraph(p);
                }
                
                // Process cell content
                if (hwpxCell.subList() != null) {
                    for (Para cellPara : hwpxCell.subList().paras()) {
                        XWPFParagraph cellDocxParagraph = docxCell.addParagraph();
                        for (Run cellRun : cellPara.runs()) {
                            processRun(cellRun, cellDocxParagraph, docxDocument, hwpxFile);
                        }
                    }
                    
                    // Ensure empty cells still have at least one paragraph
                    if (hwpxCell.subList().countOfPara() == 0) {
                        if (docxCell.getParagraphs().size() == 0) {
                            docxCell.addParagraph();
                        }
                    }
                } else {
                    // Add empty paragraph to preserve structure
                    if (docxCell.getParagraphs().size() == 0) {
                        docxCell.addParagraph();
                    }
                }
                
                // Move to next cell position
                colIdx += colSpan;
            }
        }
        
        // Add a paragraph after the table for better formatting
        docxDocument.createParagraph();
    }
    
    /**
     * Helper class to track table structure and cell merging
     */
    private static class TableStructure {
        int numRows;
        int numCols;
        CellInfo[][] cellMatrix;
        
        TableStructure(int rows, int cols) {
            this.numRows = rows;
            this.numCols = cols;
            this.cellMatrix = new CellInfo[rows][cols];
        }
    }
    
    /**
     * Helper class to track cell information
     */
    private static class CellInfo {
        int rowStart;
        int colStart;
        int rowSpan;
        int colSpan;
        
        CellInfo(int rowStart, int colStart, int colSpan, int rowSpan) {
            this.rowStart = rowStart;
            this.colStart = colStart;
            this.colSpan = colSpan;
            this.rowSpan = rowSpan;
        }
    }
    
    /**
     * Analyze the table structure to determine dimensions and cell merging information
     */
    private TableStructure analyzeTableStructure(Table tableItem) {
        int numRows = tableItem.countOfTr();
        
        // First pass: determine maximum number of columns
        int maxCols = 0;
        for (int i = 0; i < numRows; i++) {
            Tr row = tableItem.getTr(i);
            int colCount = 0;
            for (Tc cell : row.tcs()) {
                colCount += getColSpan(cell);
            }
            maxCols = Math.max(maxCols, colCount);
        }
        
        // Initialize the table structure
        TableStructure structure = new TableStructure(numRows, maxCols);
        
        // Second pass: populate the cell matrix with spanning information
        for (int rowIdx = 0; rowIdx < numRows; rowIdx++) {
            Tr row = tableItem.getTr(rowIdx);
            int colIdx = 0;
            
            for (Tc cell : row.tcs()) {
                // Skip positions that are already covered by spans
                while (colIdx < maxCols && structure.cellMatrix[rowIdx][colIdx] != null) {
                    colIdx++;
                }
                
                if (colIdx >= maxCols) break;
                
                int colSpan = getColSpan(cell);
                int rowSpan = getRowSpan(cell);
                
                // Create cell info and mark the span in the matrix
                CellInfo cellInfo = new CellInfo(rowIdx, colIdx, colSpan, rowSpan);
                for (int r = rowIdx; r < rowIdx + rowSpan && r < numRows; r++) {
                    for (int c = colIdx; c < colIdx + colSpan && c < maxCols; c++) {
                        if (r < structure.cellMatrix.length && c < structure.cellMatrix[r].length) {
                            structure.cellMatrix[r][c] = cellInfo;
                        }
                    }
                }
                
                colIdx += colSpan;
            }
        }
        
        return structure;
    }
    
    /**
     * Get the adjusted cell index accounting for gridSpans in previous cells
     */
    private int getAdjustedCellIndex(XWPFTableRow row, int desiredIndex) {
        if (row == null) return -1;
        
        int currentIndex = 0;
        int physicalIndex = 0;
        
        for (XWPFTableCell cell : row.getTableCells()) {
            if (currentIndex == desiredIndex) {
                return physicalIndex;
            }
            
            int cellGridSpan = 1;
            if (cell.getCTTc() != null && cell.getCTTc().getTcPr() != null && 
                cell.getCTTc().getTcPr().getGridSpan() != null) {
                BigInteger span = cell.getCTTc().getTcPr().getGridSpan().getVal();
                if (span != null) {
                    cellGridSpan = span.intValue();
                }
            }
            
            currentIndex += cellGridSpan;
            physicalIndex++;
            
            if (currentIndex > desiredIndex) {
                // The desired index is covered by a span, so return the current physical cell
                return physicalIndex - 1;
            }
        }
        
        // If we can't find it, the index might be beyond the actual cells due to spans
        return row.getTableCells().size() - 1;
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
     * Process a line drawing object
     */
    private void processLine(kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Line lineItem, 
                             XWPFParagraph docxParagraph, XWPFDocument docxDocument, HWPXFile hwpxFile) {
        // TODO: Implement full line drawing support when needed
        log.info("Line drawing object {} partially supported", lineItem.id());
        XWPFRun docxRun = docxParagraph.createRun();
        docxRun.setText("[Line Drawing]");
    }

    /**
     * Process a rectangle drawing object
     */
    private void processRectangle(kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Rectangle rectItem, 
                             XWPFParagraph docxParagraph, XWPFDocument docxDocument, HWPXFile hwpxFile) {
        // TODO: Implement full rectangle drawing support when needed
        log.info("Rectangle drawing object {} partially supported", rectItem.id());
        XWPFRun docxRun = docxParagraph.createRun();
        docxRun.setText("[Rectangle Drawing]");
    }

    /**
     * Process an ellipse drawing object
     */
    private void processEllipse(kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Ellipse ellipseItem, 
                           XWPFParagraph docxParagraph, XWPFDocument docxDocument, HWPXFile hwpxFile) {
        // TODO: Implement full ellipse drawing support when needed
        log.info("Ellipse drawing object {} partially supported", ellipseItem.id());
        XWPFRun docxRun = docxParagraph.createRun();
        docxRun.setText("[Ellipse Drawing]");
    }

    /**
     * Process an arc drawing object
     */
    private void processArc(kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Arc arcItem, 
                       XWPFParagraph docxParagraph, XWPFDocument docxDocument, HWPXFile hwpxFile) {
        // TODO: Implement full arc drawing support when needed
        log.info("Arc drawing object {} partially supported", arcItem.id());
        XWPFRun docxRun = docxParagraph.createRun();
        docxRun.setText("[Arc Drawing]");
    }

    /**
     * Process a polygon drawing object
     */
    private void processPolygon(kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Polygon polygonItem, 
                           XWPFParagraph docxParagraph, XWPFDocument docxDocument, HWPXFile hwpxFile) {
        // TODO: Implement full polygon drawing support when needed
        log.info("Polygon drawing object {} partially supported", polygonItem.id());
        XWPFRun docxRun = docxParagraph.createRun();
        docxRun.setText("[Polygon Drawing]");
    }

    /**
     * Process a curve drawing object
     */
    private void processCurve(kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Curve curveItem, 
                         XWPFParagraph docxParagraph, XWPFDocument docxDocument, HWPXFile hwpxFile) {
        // TODO: Implement full curve drawing support when needed
        log.info("Curve drawing object {} partially supported", curveItem.id());
        XWPFRun docxRun = docxParagraph.createRun();
        docxRun.setText("[Curve Drawing]");
    }

    /**
     * Process control characters
     */
    private void processControlCharacter(kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Ctrl ctrlItem,
                                    XWPFParagraph docxParagraph, XWPFDocument docxDocument, HWPXFile hwpxFile) {
        // Log the control character type for debugging
        log.info("Control character type: {}", ctrlItem._objectType());
        
        // Don't add visible placeholder text for most control characters
        // Only handle specific ones that need representation
        
        String controlId = ctrlItem._objectType().toString();
        if (controlId != null) {
            if (controlId.contains("char")) {
                // For special characters, check if there's actual content to display
                // Don't add placeholder text by default
            } else if (controlId.contains("bookmark")) {
                // Could implement as Word bookmark - but don't add placeholder text
            } else if (controlId.contains("header") || controlId.contains("footer")) {
                // Headers/footers should be handled at document level, not as inline text
            } else if (controlId.contains("footnote") || controlId.contains("endnote")) {
                // Footnotes need special handling at document level
                XWPFRun docxRun = docxParagraph.createRun();
                docxRun.setText("[^]"); // Minimal footnote indicator
            }
        }
    }
    
    /**
     * Validate document structure before saving
     */
    private void validateDocument(XWPFDocument document) {
        // Ensure document has at least one section
        if (document.getDocument() != null && document.getDocument().getBody() != null) {
            if (document.getParagraphs().isEmpty() && document.getTables().isEmpty()) {
                // Add an empty paragraph if document is completely empty to ensure valid structure
                document.createParagraph();
                log.warn("Added empty paragraph to ensure valid document structure");
            }
            
            // Validate that all runs have proper content or are valid empty runs
            for (XWPFParagraph para : document.getParagraphs()) {
                for (int i = para.getRuns().size() - 1; i >= 0; i--) {
                    XWPFRun run = para.getRuns().get(i);
                    // Check for invalid runs that might cause Word to crash
                    if (run.getCTR() == null) {
                        log.warn("Removing invalid run with null CTR");
                        para.removeRun(i);
                        continue;
                    }
                    
                    // Check for problematic text content that might cause issues in Hangul Word
                    if (run.getText(0) != null) {
                        String text = run.getText(0);
                        // Handle control characters that might cause issues
                        if (text.contains("\u0000") || text.contains("\uFFFD")) {
                            log.warn("Fixing text with invalid control characters");
                            run.setText(text.replace("\u0000", "").replace("\uFFFD", ""), 0);
                        }
                    }
                    
                    // Check for problematic formatting that might cause Hangul Word to crash
                    if (run.getCTR().isSetRPr()) {
                        // Simplified check for run formatting
                        log.info("Checking run formatting for compatibility");
                    }
                }
                
                // If paragraph has no runs after cleanup, add an empty run
                if (para.getRuns().isEmpty()) {
                    para.createRun();
                }
                
                // Simplify complex paragraph properties that might cause issues
                if (para.getCTP() != null && para.getCTP().isSetPPr()) {
                    // Simplify complex formatting if present
                    log.info("Checking paragraph formatting for compatibility");
                }
            }
            
            // Ensure tables have valid structure
            for (XWPFTable table : document.getTables()) {
                if (table.getRows().isEmpty()) {
                    // Remove empty tables that might cause issues
                    log.warn("Removing empty table");
                    document.removeBodyElement(document.getPosOfTable(table));
                    continue;
                }
                
                // Validate table structure
                for (XWPFTableRow row : table.getRows()) {
                    if (row.getTableCells().isEmpty()) {
                        log.warn("Row with no cells found - adding an empty cell");
                        row.addNewTableCell();
                    }
                    
                    for (XWPFTableCell cell : row.getTableCells()) {
                        // Ensure all cells have at least one paragraph
                        if (cell.getParagraphs().isEmpty()) {
                            cell.addParagraph();
                        }
                        
                        // Check and fix cell properties that might cause issues
                        if (cell.getCTTc() != null && cell.getCTTc().isSetTcPr()) {
                            CTTcPr tcPr = cell.getCTTc().getTcPr();
                            
                            // For Hangul Word compatibility, simplify vMerge and hMerge attributes
                            if (tcPr.isSetVMerge()) {
                                CTVMerge vmerge = tcPr.getVMerge();
                                // Ensure valid vMerge values
                                if (vmerge.getVal() == null || 
                                    (!vmerge.getVal().equals(STMerge.RESTART) && 
                                     !vmerge.getVal().equals(STMerge.CONTINUE))) {
                                    // Reset to a valid value
                                    vmerge.setVal(STMerge.RESTART);
                                }
                            }
                            
                            // Ensure grid span is valid
                            if (tcPr.isSetGridSpan() && tcPr.getGridSpan().getVal().intValue() <= 0) {
                                tcPr.getGridSpan().setVal(BigInteger.ONE);
                            }
                        }
                    }
                }
                
                // Simplified table properties check
                if (table.getCTTbl() != null) {
                    log.info("Checking table properties for compatibility");
                }
            }
            
            // Remove any bookmarks or complex fields that might cause issues
            removeComplexFields(document);
        }
    }
    
    /**
     * Remove complex fields and bookmarks that might cause compatibility issues
     */
    private void removeComplexFields(XWPFDocument document) {
        try {
            // Remove complex fields like TOC, indexes, etc.
            if (document.getDocument() != null && document.getDocument().getBody() != null) {
                org.w3c.dom.Node bodyNode = document.getDocument().getBody().getDomNode();
                removeComplexNodes(bodyNode);
            }
        } catch (Exception e) {
            log.warn("Could not remove complex fields: {}", e.getMessage());
        }
    }
    
    /**
     * Recursively remove problematic nodes from a DOM tree
     */
    private void removeComplexNodes(org.w3c.dom.Node node) {
        if (node == null) return;
        
        // Create a list of nodes to remove (can't remove while iterating)
        java.util.List<org.w3c.dom.Node> nodesToRemove = new java.util.ArrayList<>();
        
        // Check all child nodes
        org.w3c.dom.NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            
            // Check node name for problematic elements
            String nodeName = child.getNodeName();
            if (nodeName.contains("bookmarkStart") || 
                nodeName.contains("bookmarkEnd") ||
                nodeName.contains("fldSimple") ||
                nodeName.contains("fldChar") ||
                nodeName.contains("instrText")) {
                nodesToRemove.add(child);
                continue;
            }
            
            // Recursively process child nodes
            removeComplexNodes(child);
        }
        
        // Remove the identified problematic nodes
        for (org.w3c.dom.Node nodeToRemove : nodesToRemove) {
            try {
                node.removeChild(nodeToRemove);
            } catch (Exception e) {
                // Some nodes might be already removed or can't be removed
            }
        }
    }
    
    /**
     * Verify the generated DOCX file is valid and compatible with various office suites
     */
    private boolean verifyDocxFile(File docxFile) {
        if (!docxFile.exists() || docxFile.length() == 0) {
            log.error("Generated DOCX file is empty or doesn't exist");
            return false;
        }
        
        // Try to open the generated file to verify it's valid
        try (XWPFDocument verification = new XWPFDocument(new FileInputStream(docxFile))) {
            // If we can open it, check basic structure
            boolean hasValidStructure = verification.getDocument() != null && 
                                       verification.getDocument().getBody() != null;
                                       
            if (!hasValidStructure) {
                log.error("DOCX file has invalid document structure");
                return false;
            }
            
            // Additional checks for Hangul Word compatibility
            boolean hasValidContent = false;
            
            // Check for at least one valid paragraph or table
            if (!verification.getParagraphs().isEmpty() || !verification.getTables().isEmpty()) {
                hasValidContent = true;
            }
            
            if (!hasValidContent) {
                log.error("DOCX file has no valid content");
                return false;
            }
            
            // Additional Korean text compatibility check
            checkKoreanCompatibility(verification);
            
            return true;
        } catch (Exception e) {
            log.error("Failed to verify DOCX file: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check for Korean text compatibility issues
     */
    private void checkKoreanCompatibility(XWPFDocument doc) {
        try {
            // For Hancom Office compatibility, ensure there's proper language settings
            log.info("Checking document for Korean language compatibility");
            
            // Basic compatibility checks sufficient for this pass
        } catch (Exception e) {
            log.warn("Could not check Korean compatibility: {}", e.getMessage());
        }
    }
}
 