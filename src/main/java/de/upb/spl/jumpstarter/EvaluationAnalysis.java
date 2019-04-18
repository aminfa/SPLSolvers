package de.upb.spl.jumpstarter;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

public class EvaluationAnalysis {
    private int memory = -1;
    private long time = -1;
    private double hv = -1;
    private double rank = -1;

    public EvaluationAnalysis() {
    }

    public EvaluationAnalysis(int memoryConsumption, long time, double hv) {
        this.memory = memoryConsumption;
        this.time = time;
        this.hv = hv;
    }

    public EvaluationAnalysis(int memory, long time, double hv, double rank) {
        this.memory = memory;
        this.time = time;
        this.hv = hv;
        this.rank = rank;
    }

    public int getMemory() {
        return memory;
    }

    public void setMemory(int memory) {
        this.memory = memory;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public double getHv() {
        return hv;
    }

    public void setHv(double hv) {
        this.hv = hv;
    }

    public double getRank() {
        return rank;
    }

    public void setRank(double rank) {
        this.rank = rank;
    }

    /**
     * Creates the Type Token for:
     * Map<String, List<Map<Integer, EvaluationAnalysis>>>
     * Used by Gson json reader.
     * @return TypeToken for the data file.
     */
    public static Type dataFileToken() {
        return TypeToken.getParameterized(
            HashMap.class,
            String.class,
            TypeToken.getParameterized(
                    ArrayList.class,
                    TypeToken.getParameterized(
                            HashMap.class,
                            Integer.class,
                            EvaluationAnalysis.class
                            ).getType()
                    ).getType()
        ).getType();
    }
}
