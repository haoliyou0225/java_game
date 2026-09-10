// FR-04 绳索接口：最大绳长、当前绳长，随钩子运动动态伸缩
package Main.model;

public interface Rope {
    double getMaxLen();
    double getCurrentLen();
    void setCurrentLen(double len);
}
