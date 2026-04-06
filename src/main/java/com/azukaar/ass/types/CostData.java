package com.azukaar.ass.types;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CostData {
    @Expose @SerializedName("const")
    private double constValue = 0;

    @Expose @SerializedName("lin")
    private double lin = 1;

    @Expose @SerializedName("pow")
    private double pow = 1;

    public CostData() {}

    public CostData(double constValue, double lin, double pow) {
        this.constValue = constValue;
        this.lin = lin;
        this.pow = pow;
    }

    public int calculateCost(int currentLevel) {
        return Math.max(1, (int) Math.round(constValue + lin * Math.pow(currentLevel, pow)));
    }

    public double getConstValue() { return constValue; }
    public double getLin() { return lin; }
    public double getPow() { return pow; }
}
