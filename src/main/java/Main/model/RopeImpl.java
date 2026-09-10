// FR-04 绳索物理实现：最大绳长、当前绳长，随钩子运动动态伸缩
package Main.model;

public class RopeImpl implements Rope {
    private double maxLen;
    private double currentLen;

    public RopeImpl(double maxLen) {
        this.maxLen = maxLen;
        this.currentLen = 0;
    }

    @Override public double getMaxLen() { return maxLen; }
    @Override public double getCurrentLen() { return currentLen; }
    @Override public void setCurrentLen(double len) {
        this.currentLen = Math.max(0, Math.min(len, maxLen));
    }
}
