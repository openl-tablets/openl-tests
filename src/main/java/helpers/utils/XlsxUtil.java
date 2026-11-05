package helpers.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class XlsxUtil {

    private static final String SHEET_RELATIONSHIP_ID = "id";

    private XlsxUtil() {
    }

    public static List<List<String>> readSheetFromRow(File xlsx, String sheetName, String firstCellOfFirstRow) {
        List<List<String>> rows = readSheet(xlsx, sheetName);
        for (int index = 0; index < rows.size(); index++) {
            List<String> row = rows.get(index);
            if (!row.isEmpty() && firstCellOfFirstRow.equals(row.get(0))) {
                return rows.subList(index, rows.size());
            }
        }
        throw new AssertionError("The sheet '" + sheetName + "' of " + xlsx.getName()
                + " has no row starting with '" + firstCellOfFirstRow + "': " + rows);
    }

    private static List<List<String>> readSheet(File xlsx, String sheetName) {
        try (ZipFile zip = new ZipFile(xlsx)) {
            Document workbook = parse(zip, "xl/workbook.xml");
            String relationshipId = findRelationshipId(workbook, sheetName, xlsx);
            String target = findSheetTarget(parse(zip, "xl/_rels/workbook.xml.rels"), relationshipId);
            List<String> sharedStrings = readSharedStrings(zip);
            return readRows(parse(zip, "xl/" + target.replaceFirst("^/?(xl/)?", "")), sharedStrings);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + xlsx.getName() + ": " + e.getMessage(), e);
        }
    }

    private static String findRelationshipId(Document workbook, String sheetName, File xlsx) {
        NodeList sheets = workbook.getElementsByTagName("sheet");
        List<String> names = new ArrayList<>();
        for (int index = 0; index < sheets.getLength(); index++) {
            Element sheet = (Element) sheets.item(index);
            names.add(sheet.getAttribute("name"));
            if (sheetName.equals(sheet.getAttribute("name"))) {
                for (int attr = 0; attr < sheet.getAttributes().getLength(); attr++) {
                    Node attribute = sheet.getAttributes().item(attr);
                    if (attribute.getNodeName().endsWith(":" + SHEET_RELATIONSHIP_ID)) {
                        return attribute.getNodeValue();
                    }
                }
            }
        }
        throw new AssertionError(xlsx.getName() + " has no sheet '" + sheetName + "', its sheets: " + names);
    }

    private static String findSheetTarget(Document relationships, String relationshipId) {
        NodeList all = relationships.getElementsByTagName("Relationship");
        for (int index = 0; index < all.getLength(); index++) {
            Element relationship = (Element) all.item(index);
            if (relationshipId.equals(relationship.getAttribute("Id"))) {
                return relationship.getAttribute("Target");
            }
        }
        throw new AssertionError("The workbook has no relationship " + relationshipId);
    }

    private static List<String> readSharedStrings(ZipFile zip) throws IOException {
        List<String> strings = new ArrayList<>();
        if (zip.getEntry("xl/sharedStrings.xml") == null) {
            return strings;
        }
        NodeList items = parse(zip, "xl/sharedStrings.xml").getElementsByTagName("si");
        for (int index = 0; index < items.getLength(); index++) {
            strings.add(items.item(index).getTextContent());
        }
        return strings;
    }

    private static List<List<String>> readRows(Document sheet, List<String> sharedStrings) {
        List<List<String>> rows = new ArrayList<>();
        NodeList rowNodes = sheet.getElementsByTagName("row");
        for (int index = 0; index < rowNodes.getLength(); index++) {
            List<String> cells = new ArrayList<>();
            NodeList cellNodes = ((Element) rowNodes.item(index)).getElementsByTagName("c");
            for (int cellIndex = 0; cellIndex < cellNodes.getLength(); cellIndex++) {
                Element cell = (Element) cellNodes.item(cellIndex);
                int column = columnOf(cell.getAttribute("r"));
                while (cells.size() < column) {
                    cells.add("");
                }
                cells.add(valueOf(cell, sharedStrings));
            }
            while (!cells.isEmpty() && cells.get(cells.size() - 1).isEmpty()) {
                cells.remove(cells.size() - 1);
            }
            if (!cells.isEmpty()) {
                rows.add(cells);
            }
        }
        int unusedColumns = rows.stream().mapToInt(XlsxUtil::leadingEmptyCells).min().orElse(0);
        return rows.stream().map(row -> List.copyOf(row.subList(unusedColumns, row.size()))).toList();
    }

    private static int leadingEmptyCells(List<String> row) {
        int count = 0;
        while (count < row.size() && row.get(count).isEmpty()) {
            count++;
        }
        return count;
    }

    private static String valueOf(Element cell, List<String> sharedStrings) {
        String type = cell.getAttribute("t");
        if ("inlineStr".equals(type)) {
            return cell.getTextContent();
        }
        NodeList values = cell.getElementsByTagName("v");
        if (values.getLength() == 0) {
            return "";
        }
        String value = values.item(0).getTextContent();
        return "s".equals(type) ? sharedStrings.get(Integer.parseInt(value)) : value;
    }

    private static int columnOf(String reference) {
        int column = 0;
        for (char symbol : reference.toCharArray()) {
            if (!Character.isLetter(symbol)) {
                break;
            }
            column = column * 26 + (Character.toUpperCase(symbol) - 'A' + 1);
        }
        return column - 1;
    }

    private static Document parse(ZipFile zip, String entryName) throws IOException {
        ZipEntry entry = zip.getEntry(entryName);
        if (entry == null) {
            throw new AssertionError("The workbook has no " + entryName);
        }
        try (InputStream stream = zip.getInputStream(entry)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(stream);
        } catch (Exception e) {
            throw new IOException("Cannot parse " + entryName + ": " + e.getMessage(), e);
        }
    }
}
