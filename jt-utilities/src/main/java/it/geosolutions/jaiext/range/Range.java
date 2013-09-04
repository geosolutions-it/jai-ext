package it.geosolutions.jaiext.range;

import java.awt.image.DataBuffer;

/**
 * Abstract class used for checking if a selected value is inside the selected Range. All the methods throw an {@link UnsupportedOperationException}
 * but for every subclass one of these methods is overridden with the correct functionality. These 6 methods are different only for the data type
 * used. In this way it is possible to reach a better performance by using primitive variables than generic. All the subclasses can contain a Range
 * composed by a minimum and a maximum or a single-point Range. For Double and Float data type the NaN data can be used only with a single-point
 * Range.
 */
public abstract class Range {
    
    public enum DataType{
        BYTE(Byte.class,DataBuffer.TYPE_BYTE),
        USHORT(Short.class,DataBuffer.TYPE_USHORT),
        SHORT(Short.class,DataBuffer.TYPE_SHORT),
        INTEGER(Integer.class,DataBuffer.TYPE_INT),
        FLOAT(Float.class,DataBuffer.TYPE_FLOAT),
        DOUBLE(Double.class,DataBuffer.TYPE_DOUBLE);

        private Class<?> classType;
        
        private int dataType;
        
        private DataType(Class<?> classType, int dataType) {
            this.classType = classType;
            this.dataType = dataType;
        }
        
        public Class<?> getClassValue(){
            return classType;
        }
        
        public int getDataType(){
            return dataType;
        }
    }
    
    

    /** Method for checking if a byte value is contained inside the Range */
    public boolean contains(byte value) {
        throw new UnsupportedOperationException("Wrong data type");
    }

    /** Method for checking if a short/ushort value is contained inside the Range */
    public boolean contains(short value) {
        throw new UnsupportedOperationException("Wrong data type");
    }

    /** Method for checking if an integer value is contained inside the Range */
    public boolean contains(int value) {
        throw new UnsupportedOperationException("Wrong data type");
    }

    /** Method for checking if a float value is contained inside the Range */
    public boolean contains(float value) {
        throw new UnsupportedOperationException("Wrong data type");
    }

    /** Method for checking if a double value is contained inside the Range */
    public boolean contains(double value) {
        throw new UnsupportedOperationException("Wrong data type");
    }

    /** Returns the Range data Type */
    public abstract DataType getDataType();

}
