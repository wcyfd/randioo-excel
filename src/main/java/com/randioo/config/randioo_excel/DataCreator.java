package com.randioo.config.randioo_excel;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.randioo.config.randioo_excel.po.ClassConfig;
import com.randioo.config.randioo_excel.po.Data;
import com.randioo.config.randioo_excel.po.FieldConfig;
import com.randioo.config.randioo_excel.po.ReplaceLocation;
import com.randioo.config.randioo_excel.util.ExcelUtils;
import com.randioo.config.randioo_excel.util.FileUtils;

/**
 * 数据生成器
 * @author AIM
 *
 */
public class DataCreator {
	public Data getData(ClassConfig node, String excelPath) {
		List<FieldConfig> itemConfigList = node.itemList;
		Sheet sheet = ExcelUtils.sheet(excelPath + FileUtils.fileSplit + node.xlsx, node.page);
		Iterator<Row> rowIt = sheet.rowIterator();
		Map<String, Integer> nameColumnIndexMap = initColumns(rowIt.next());
		Data data = new Data();
		while (rowIt.hasNext()) {
			Row row = rowIt.next();
			String type = itemConfigList.get(0).type;

			// 查看是否有下行
			if (!hasNextLine(type, row.getCell(0))) {
				break;
			}

			for (int i = 0; i < itemConfigList.size(); i++) {
				FieldConfig itemConfig = itemConfigList.get(i);

				Cell cell = this.locationCell(row, nameColumnIndexMap, itemConfig,excelPath);
				pushData(data, itemConfig.type, cell);
			}

		}

		return data;
	}

	public Cell locationCell(Row row, Map<String, Integer> nameColumnIndexMap, FieldConfig item,String excelPath) {
		String columnName = item.name;
		Cell cell = row.getCell(nameColumnIndexMap.get(columnName));
		if (item.replace == null) {
			return cell;
		}
		ReplaceLocation replaceLocation = new ReplaceLocation(item.replace);
		File file = new File(excelPath + FileUtils.fileSplit + replaceLocation.fileName);
		Sheet sheet = ExcelUtils.sheet(file, replaceLocation.page);
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
		case BasicType.INT: {
			int value = cell.getCellType() == Cell.CELL_TYPE_STRING ? Integer.parseInt(cell.getStringCellValue())
					: (int) cell.getNumericCellValue();
			data.putInt(value);
		}

			break;
		case BasicType.STRING: {
			String value = cell.getCellType() == Cell.CELL_TYPE_NUMERIC ? cell.getNumericCellValue() + ""
					: cell.getStringCellValue();
			data.putString(value);
		}

			break;
		case BasicType.SHORT: {
			short value = cell.getCellType() == Cell.CELL_TYPE_STRING ? Short.parseShort(cell.getStringCellValue())
					: (short) cell.getNumericCellValue();
			data.putShort(value);
		}

			break;
		case BasicType.DOUBLE: {
			double value = cell.getCellType() == Cell.CELL_TYPE_NUMERIC ? cell.getNumericCellValue()
					: Double.parseDouble(cell.getStringCellValue());
			data.putDouble(value);
		}
			break;
		case BasicType.BYTE: {
			byte v = Byte.parseByte(cell.getCellType() == Cell.CELL_TYPE_NUMERIC ? cell.getNumericCellValue() + ""
					: cell.getStringCellValue());
			data.putByte(v);
		}

			break;
		}
	}

	/**
	 * 是否有下一行
	 * @param type
	 * @param cell
	 * @return
	 */
	private boolean hasNextLine(String type, Cell cell) {
		if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
			if (type.equals(BasicType.INT) || type.equals(BasicType.SHORT) || type.equals(BasicType.DOUBLE) || type.equals(BasicType.BYTE)) {
				return false;
			}

			if (type.equals(BasicType.STRING)) {
				String str = cell.getStringCellValue().trim();
				if (str == null || str.equals("")) {
					return false;
				}
			}

		}
		return true;

	}

	/**
	 * 初始化列表名称
	 * @param row
	 * @return
	 */
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
}
