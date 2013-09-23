package com.tilt.algorithm;

import org.opencv.core.Mat;

public class Cells {
	private Mat[][] cells;
	private int rows;
	private int cols;
	
	public Cells(int rows, int cols) {
		// check the argument
		if ((rows < 0) || (cols < 0)) {
			throw new IllegalArgumentException("The size of the cells should >= 0!");
		}
		
		// initial the cells
		this.rows = rows;
		this.cols = cols;
		cells = new Mat[rows][];
		for (int ix = 0; ix < rows; ++ix) {
			cells[ix] = new Mat[cols];
		}
	}
	
	public void putCell(int row, int col, Mat mat) {
		// check the argument
		if ((row >= rows) || (col >= cols) || (row < 0) || (col < 0)) {
			throw new IllegalArgumentException("The position is outside the cells!");
		}
		
		cells[row][col] = mat;
	}
	
	public Mat getCell(int row, int col) {
		// check the argument
		if ((row >= rows) || (col >= cols) || (row < 0) || (col < 0)) {
			throw new IllegalArgumentException("The position is outside the cells!");
		}
		
		return cells[row][col];
	}
	
	public void setValue(int rowOfCell, int colOfCell, int rowOfMat, int colOfMat, double value) {
		// check the argument
		if ((rowOfCell >= rows) || (colOfCell >= cols) || (rowOfCell < 0) || (colOfCell< 0)) {
			throw new IllegalArgumentException("The position is outside the cells!");
		}
		
		if (cells[rowOfCell][colOfCell] == null) { 
			throw new IllegalArgumentException("The cell is null!");
		}
		
		if ((rowOfMat < 0) || (colOfMat < 0) || (rowOfMat >= cells[rowOfCell][colOfCell].rows()) || (colOfMat >= cells[rowOfCell][colOfCell].cols())) {
			throw new IllegalArgumentException("The position is outside the matrix!");
		}
		
		cells[rowOfCell][colOfCell].put(rowOfMat, colOfMat, value);
	}
	
	public double getValue(int rowOfCell, int colOfCell, int rowOfMat, int colOfMat) {
		// check the argument
		if ((rowOfCell >= rows) || (colOfCell >= cols) || (rowOfCell < 0) || (colOfCell< 0)) {
			throw new IllegalArgumentException("The position is outside the cells!");
		}
		
		if (cells[rowOfCell][colOfCell] == null) { 
			throw new IllegalArgumentException("The cell is null!");
		}
		
		if ((rowOfMat < 0) || (colOfMat < 0) || (rowOfMat >= cells[rowOfCell][colOfCell].rows()) || (colOfMat >= cells[rowOfCell][colOfCell].cols())) {
			throw new IllegalArgumentException("The position is outside the matrix!");
		}
		
		return cells[rowOfCell][colOfCell].get(rowOfMat, colOfMat)[0];
	}
	
	public int getRows() {
		return this.rows;
	}
	
	public int getColumns() {
		return this.cols;
	}
}
