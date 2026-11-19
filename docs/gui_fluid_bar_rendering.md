# GUI 液位条平铺渲染

## 使用场景
在太阳能蒸馏机等带有内部流体储罐的 GUI 中，需要把流体材质以**平铺（Tile）**方式渲染到固定大小的液位条区域，避免拉伸导致的变形。

## 实现代码

```java
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 在 GUI 中平铺渲染流体液位条。
 *
 * @param guiGraphics 绘制上下文
 * @param x           液位条左上角在 GUI 中的 x 坐标
 * @param y           液位条左上角在 GUI 中的 y 坐标
 * @param width       液位条总宽度
 * @param height      液位条总高度
 * @param fluid       要渲染的流体及其当前量
 * @param capacity    流体储罐容量
 */
private void renderFluidBar(GuiGraphics guiGraphics, int x, int y, int width, int height,
                            FluidStack fluid, int capacity) {
    if (fluid.isEmpty() || capacity <= 0) return;

    // 根据当前流体占比计算实际填充高度（从下往上）
    int fillHeight = (int) ((float) fluid.getAmount() / capacity * height);
    if (fillHeight <= 0) return;

    // 获取客户端流体扩展，用于读取 still 纹理与着色
    IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluidType());
    TextureAtlasSprite sprite = Minecraft.getInstance()
        .getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)
        .apply(extensions.getStillTexture());

    // 读取流体着色（ARGB）并拆分为 RGBA 分量
    int color = extensions.getTintColor(fluid);
    float a = ((color >> 24) & 0xFF) / 255f;
    float r = ((color >> 16) & 0xFF) / 255f;
    float g = ((color >> 8) & 0xFF) / 255f;
    float b = (color & 0xFF) / 255f;

    // 每个平铺单元的大小，通常为 16x16
    int tileSize = sprite.contents().width();

    // 填充区域顶部 y 坐标（总高度 - 填充高度）
    int fillTop = y + height - fillHeight;

    // 应用流体颜色
    guiGraphics.setColor(r, g, b, a);

    // 使用裁剪限定只绘制在填充区域内
    guiGraphics.enableScissor(x, fillTop, x + width, y + height);

    // 平铺绘制流体精灵
    for (int tileY = fillTop; tileY < y + height; tileY += tileSize) {
        for (int tileX = x; tileX < x + width; tileX += tileSize) {
            guiGraphics.blit(tileX, tileY, 0, tileSize, tileSize, sprite);
        }
    }

    // 恢复裁剪与颜色
    guiGraphics.disableScissor();
    guiGraphics.setColor(1f, 1f, 1f, 1f);
}
```

## 调用示例

```java
// 水槽：横向液位条，宽 53、高 18，位于 (37,43)
renderFluidBar(guiGraphics, x + 37, y + 43, 53, 18,
    new FluidStack(Fluids.WATER, menu.getWaterAmount()),
    mio_icif_solar_distiller.WATER_TANK_CAPACITY);

// 蒸馏水槽：纵向液位条，宽 17、高 43，位于 (115,55)
renderFluidBar(guiGraphics, x + 115, y + 55, 17, 43,
    new FluidStack(mio_icif_fluids.DISTILLEDWATER.get(), menu.getDistilledAmount()),
    mio_icif_solar_distiller.DISTILLED_TANK_CAPACITY);
```

## 关键点

1. **平铺而非拉伸**：每个 tile 使用精灵原始大小（`sprite.contents().width()`），通过循环覆盖整个填充区域，再用 `enableScissor` 裁剪到实际液位高度。
2. **从下往上填充**：`fillTop = y + height - fillHeight`，裁剪框底部固定为 `y + height`，随液位升高向上扩展。
3. **颜色还原**：绘制结束后必须调用 `guiGraphics.setColor(1f, 1f, 1f, 1f)`，否则后续 GUI 元素会被流体颜色污染。
4. **裁剪恢复**：`disableScissor()` 必须在 `setColor` 之前调用，确保裁剪状态不影响后续绘制。
