import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.xssf.binary.XSSFBCommentsTable;
import org.apache.poi.xssf.binary.XSSFBSharedStringsTable;
import org.apache.poi.xssf.binary.XSSFBSheetHandler.SheetContentsHandler;
import org.apache.poi.xssf.binary.XSSFBStylesTable;
import org.apache.poi.xssf.eventusermodel.XSSFBReader;
import org.apache.poi.xssf.extractor.XSSFBEventBasedExcelExtractor;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.xmlbeans.XmlException;
import org.xml.sax.SAXException;

public class XSSFBTextExtractor extends XSSFBEventBasedExcelExtractor {
	public XSSFBTextExtractor(String path) throws XmlException, OpenXML4JException, IOException {
		super(path);
	}
	
	public  String getText() {
        try {
            XSSFBSharedStringsTable strings = new XSSFBSharedStringsTable(getPackage());
            XSSFBReader xssfbReader = new XSSFBReader(getPackage());
            XSSFBStylesTable styles = xssfbReader.getXSSFBStylesTable();
            XSSFBReader.SheetIterator iter = (XSSFBReader.SheetIterator) xssfbReader.getSheetsData();

            StringBuffer text = new StringBuffer();
            SheetTextExtractor sheetExtractor = new SheetTextExtractor();
            
            while (iter.hasNext()) {
                InputStream stream = iter.next();
                if (getIncludeSheetNames()) {
                    text.append(iter.getSheetName());
                    text.append('\n');
                }
                
                XSSFBCommentsTable comments = getIncludeCellComments() ? iter.getXSSFBSheetComments() : null;
                processSheet(sheetExtractor, styles, comments, strings, stream);
                
                if (getIncludeHeadersFooters()) {
                    sheetExtractor.appendHeaderText(text);
                }
                
                sheetExtractor.appendCellText(text);
                
                if (getIncludeHeadersFooters()) {
                    sheetExtractor.appendFooterText(text);
                }
                
                sheetExtractor.reset();
                stream.close();
            }

            return text.toString();
        } catch (IOException e) {
            return null;
        } catch (SAXException se) {
            return null;
        } catch (OpenXML4JException o4je) {
            return null;
        }
    }
	
	protected class SheetTextExtractor implements SheetContentsHandler {
        private final StringBuffer output;
        private boolean firstCellOfRow;
        private final Map<String, String> headerFooterMap;

        protected SheetTextExtractor() {
            this.output = new StringBuffer();
            this.firstCellOfRow = true;
            this.headerFooterMap = includeHeadersFooters ? new HashMap<String, String>() : null;
        }

        @Override
        public  void startRow(int rowNum) {
            firstCellOfRow = true;
        }

        @Override
        public  void endRow(int rowNum) {
            output.append('\n');
        }

        @Override
        public  void cell(String cellRef, String formattedValue, XSSFComment comment) {
            if(firstCellOfRow) {
                firstCellOfRow = false;
            } else {
                output.append('\t');
            }
            
            if (formattedValue != null) {
                checkMaxTextSize(output, formattedValue);
                output.append(formattedValue);
            } else {
            	output.append("__EMPTY__");
            }
            
            if (includeCellComments && comment != null) {
                String commentText = comment.getString().getString().replace('\n', ' ');
                output.append(formattedValue != null ? " Comment by " : "Comment by ");
                checkMaxTextSize(output, commentText);
                if (commentText.startsWith(comment.getAuthor() + ": ")) {
                    output.append(commentText);
                } else {
                    output.append(comment.getAuthor()).append(": ").append(commentText);
                }
            }
        }

        @Override
        public  void headerFooter(String text, boolean isHeader, String tagName) {
            if (headerFooterMap != null) {
                headerFooterMap.put(tagName, text);
            }
        }

        /**
         * Append the text for the named header or footer if found.
         */
        private  void appendHeaderFooterText(StringBuffer buffer, String name) {
            String text = headerFooterMap.get(name);
            if (text != null && text.length() > 0) {
                // this is a naive way of handling the left, center, and right
                // header and footer delimiters, but it seems to be as good as
                // the method used by XSSFExcelExtractor
                text = handleHeaderFooterDelimiter(text, "&L");
                text = handleHeaderFooterDelimiter(text, "&C");
                text = handleHeaderFooterDelimiter(text, "&R");
                buffer.append(text).append('\n');
            }
        }
        /**
         * Remove the delimiter if its found at the beginning of the text,
         * or replace it with a tab if its in the middle.
         */
        private  String handleHeaderFooterDelimiter(String text, String delimiter) {
            int index = text.indexOf(delimiter);
            if (index == 0) {
                text = text.substring(2);
            } else if (index > 0) {
                text = text.substring(0, index) + "\t" + text.substring(index + 2);
            }
            return text;
        }


        /**
         * Append the text for each header type in the same order
         * they are appended in XSSFExcelExtractor.
         * @see XSSFExcelExtractor#getText()
         * @see org.apache.poi.hssf.extractor.ExcelExtractor#_extractHeaderFooter(org.apache.poi.ss.usermodel.HeaderFooter)
         */
         void appendHeaderText(StringBuffer buffer) {
            appendHeaderFooterText(buffer, "firstHeader");
            appendHeaderFooterText(buffer, "oddHeader");
            appendHeaderFooterText(buffer, "evenHeader");
        }

        /**
         * Append the text for each footer type in the same order
         * they are appended in XSSFExcelExtractor.
         * @see XSSFExcelExtractor#getText()
         * @see org.apache.poi.hssf.extractor.ExcelExtractor#_extractHeaderFooter(org.apache.poi.ss.usermodel.HeaderFooter)
         */
         void appendFooterText(StringBuffer buffer) {
            appendHeaderFooterText(buffer, "firstFooter");
            appendHeaderFooterText(buffer, "oddFooter");
            appendHeaderFooterText(buffer, "evenFooter");
        }

        /**
         * Append the cell contents we have collected.
         */
         void appendCellText(StringBuffer buffer) {
            checkMaxTextSize(buffer, output.toString());
            buffer.append(output);
        }

        /**
         * Reset this <code>SheetTextExtractor</code> for the next sheet.
         */
         void reset() {
            output.setLength(0);
            firstCellOfRow = true;
            if (headerFooterMap != null) {
                headerFooterMap.clear();
            }
        }

		@Override
		public void hyperlinkCell(String arg0, String arg1, String arg2, String arg3, XSSFComment arg4) {
			// TODO Auto-generated method stub
		}
    }
}
