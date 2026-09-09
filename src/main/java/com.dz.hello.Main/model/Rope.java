// FR-04 绳索物理：最大绳长、当前绳长，随钩子运动动态伸缩
package com.dz.hello.main.model;

public class Rope {
    private double maxLen;
    private double currentLen;

    public Rope(double maxLen) {
        this.maxLen = maxLen;
        this.currentLen = 0;
    }

    public double getMaxLen() { return maxLen; }
    public double getCurrentLen() { return currentLen; }
    public void setCurrentLen(double len) {
        this.currentLen = Math.max(0, Math.min(len, maxLen));
    }
}
