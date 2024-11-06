package com.image;

public class UndoAction {
	String shapeType;
    int index; // Used for Rectangle and Circle
    Integer lineKey; // New field for tracking line key, nullable for other shapes

    UndoAction(String shapeType, int index) {
        this.shapeType = shapeType;
        this.index = index;
        this.lineKey = null; // Default to null
    }

    UndoAction(String shapeType, int index, Integer lineKey) {
        this.shapeType = shapeType;
        this.index = index;
        this.lineKey = lineKey;
    }

}
