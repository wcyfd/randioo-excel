package com.randioo.config.randioo_excel.po;

import java.io.File;
import java.io.FileInputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.randioo.config.randioo_excel.Constant;
import com.randioo.config.randioo_excel.Macro;
import com.randioo.config.randioo_excel.language.LanguageParser;
import com.randioo.config.randioo_excel.util.FileUtils;
/**
 * 配置表转换器
 * @author wcy 2016年12月16日
 *
 */
public class ConfigParser {
	private Sheet sheet;
	private ClassConfig node;

	/**
	 * 
	 * @param node 节点
	 * @param excelPath excel文件路径（不带文件名）
	 */
	public ConfigParser(ClassConfig node, String excelPath) {
		this.node = node;
		File f = new File(excelPath + FileUtils.fileSplit + node.xlsx);
		this.sheet = this.getSheet(f, node.page);
	}

	/**
	 * 声明成员变量
	 * @param parser
	 * @param flag
	 * @return
	 * @author wcy 2016年12月16日
	 */
	public String parseDeclare(LanguageParser parser, String flag) {
		List<FieldConfig> itemConfigList = node.itemList;
		StringBuilder sb = new StringBuilder();
		for (FieldConfig config : itemConfigList) {
			String comment = MessageFormat.format(parser.getCommentFormat(),
					config.comment != null ? config.comment : config.name);
			String statement = parser.getDeclareFormat(config).replace(Macro.$CODE, config.code);
			sb.append(comment).append(Macro.$DECLARE_BRACE).append(statement).append(Macro.$DECLARE_BRACE);
		}

		return sb.toString();
	}

	public String parseAssignment(String targetVarName, String dataVarName, LanguageParser parser, String flag) {
		List<FieldConfig> itemConfigList = node.itemList;
		StringBuilder sb = new StringBuilder();
		for (FieldConfig config : itemConfigList) {
			String format = parser.getAssignmentFormat(config);
			String statement = MessageFormat.format(format, targetVarName, config.code, dataVarName);
			sb.append(statement).append(flag);
		}
		
		return sb.toString();
	}
	
	public String parseDataStructure(LanguageParser parser){
		StringBuilder sb = new StringBuilder();
		String format = parser.getDataStructureClass(node.key);
		sb.append(MessageFormat.format(format,node.className));
		return sb.toString();
	}
	
	public String parseDataStructureAddMethod(LanguageParser parser, String po) {
		StringBuilder sb = new StringBuilder();
		String format = parser.getDataStructureAddMethod(node.key);
		sb.append(MessageFormat.format(format, node.key, po));
		return sb.toString();
	}

	public Data getData() {
		List<FieldConfig> itemConfigList = node.itemList;
		Iterator<Row> rowIt = sheet.rowIterator();
		Map<String, Integer> nameColumnIndexMap = initColumns(rowIt.next());
		Data data = new Data();
		while (rowIt.hasNext()) {
			Row row = rowIt.next();
			String type = itemConfigList.get(0).type;
//			String value = row.getCell(0).getStringCellValue();
//			// 查看是否有下行
//			if (value == null || value.equals(""))
//				break;
			
			// 查看是否有下行
			if(!hasNextLine(type, row.getCell(0))){
				break;
			}

			for (int i = 0; i < itemConfigList.size(); i++) {
				FieldConfig itemConfig = itemConfigList.get(i);

				Cell cell = this.locationCell(row, nameColumnIndexMap, itemConfig);
				pushData(data, itemConfig.type, cell);
			}

		}

		return data;
	}

	public Cell locationCell(Row row, Map<String, Integer> nameColumnIndexMap, FieldConfig item) {
		String columnName = item.name;
		Cell cell = row.getCell(nameColumnIndexMap.get(columnName));
		if (item.replace == null) {
			return cell;
		}
		ReplaceLocation replaceLocation = new ReplaceLocation(item.replace);
		File file = new File(Constant.EXCEL_URL + FileUtils.fileSplit + replaceLocation.fileName);
		Sheet sheet = this.getSheet(file, replaceLocation.page);
		Iterator<Row> rowIt = sheet.rowIterator();
		Map<String, Integer> replaceNameColumnIndexMap = this.initColumns(rowIt.next());
		int columnIndex = replaceNameColumnIndexMap.get(replaceLocation.columnName);
		int valueIndex = replaceNameColumnIndexMap.get(replaceLocation.value);

		while (rowIt.hasNext()) {
			Row row1 = rowIt.next();
			String varString = row1.getCell(columnIndex).getStringCellValue();
			if (varString.equals(cell.getStringCellValue())) {
				return row1.getCell(valueIndex);
			}
		}
		return null;
	}

	private void pushData(Data data, String type, Cell cell) {
		switch (type) {
		case "int": {
			int value = cell.getCellType() == Cell.CELL_TYPE_STRING ? Integer.parseInt(cell.getStringCellValue()) : (int) cell
					.getNumericCellValue();
			data.putInt(value);
		}

			break;
		case "string": {
			String value = cell.getCellType() == Cell.CELL_TYPE_NUMERIC ? cell.getNumericCellValue() + "" : cell
					.getStringCellValue();
			data.putString(value);
		}

			break;
		case "short": {
			short value = cell.getCellType() == Cell.CELL_TYPE_STRING ? Short.parseShort(cell.getStringCellValue()) : (short) cell
					.getNumericCellValue();
			data.putShort(value);
		}

			break;
		case "double": {
			double value = cell.getCellType() == Cell.CELL_TYPE_NUMERIC ? cell.getNumericCellValue() : Double
					.parseDouble(cell.getStringCellValue());
			data.putDouble(value);
		}
			break;
		case "byte": {
			byte v = Byte
					.parseByte(cell.getCellType() == Cell.CELL_TYPE_NUMERIC ? cell.getNumericCellValue() + "" : cell
							.getStringCellValue());
			data.putByte(v);
		}

			break;
		}
	}
	
	private boolean hasNextLine(String type, Cell cell) {		
		if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
			if (type.equals("int") || type.equals("short") || type.equals("double") || type.equals("byte")) {
				return false;
			}

			if (type.equals("string")) {
				String str = cell.getStringCellValue().trim();
				if (str == null || str.equals("")) {
					return false;
				}
			}

		}
		return true;

	}

	private static Map<String, Integer> initColumns(Row row) {
		Iterator<Cell> cells = row.cellIterator();
		Map<String, Integer> nameMap = new HashMap<>();
		while (cells.hasNext()) {
			Cell cell = cells.next();
			String str = cell.getStringCellValue();
			if (str == null || str.equals("")) {
				break;
			}

			nameMap.put(str, cell.getColumnIndex());
		}
		return nameMap;
	}

	private Sheet getSheet(File f, String page) {
		try {
			FileInputStream fis = new FileInputStream(f);
			@SuppressWarnings("resource")
			Workbook workbook = new XSSFWorkbook(fis);
			return workbook.getSheet(page);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
