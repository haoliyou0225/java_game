// FR-13 关卡接口：generateSceneItems() 随机生成 Gold/Diamond/Stone/Bomb 混合场景
package Main.model;

import java.util.List;

public interface Level {
    List<Item> generateSceneItems();
    List<Item> getItemPool();
}
