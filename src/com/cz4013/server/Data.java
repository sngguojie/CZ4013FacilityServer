package com.cz4013.server;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by danielseetoh on 3/31/17.
 */
public class Data {
    private String objectReference;
    private String methodId;
    private ArrayList<String> stringList;
    private ArrayList<Integer> intList;

    public Data(){
        this.stringList = new ArrayList<String>();
        this.intList = new ArrayList<Integer>();
    }

    public void addString(String str){
        this.stringList.add(str);
    }

    public void addInt(int i){
        this.intList.add(i);
    }

    public void setObjectReference(String objectReference){
        this.objectReference = objectReference;
    }

    public void setMethodId(String methodId){
        this.methodId = methodId;
    }

    public ArrayList<String> getStringList(){
        return this.stringList;
    }

    public ArrayList<Integer> getIntList(){
        return this.intList;
    }

    public String getObjectReference(){
        return this.objectReference;
    }

    public String getMethodId(){
        return this.methodId;
    }

    public String toString(){
        String result = "ObjectReference: " + this.objectReference + "\n";
        result += "MethodId: " + this.methodId + "\n";
        for (String s : this.stringList){
            result += s + "\n";
        }

        for (int i : this.intList){
            result += i + "\n";
        }

        return result;
    }

    public String stringListToString(){
        return Arrays.toString((this.stringList).toArray());
    }
}
