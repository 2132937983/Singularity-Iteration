package guideme;
public class GuideBuilder {
    private final net.minecraft.resources.ResourceLocation id;
    GuideBuilder(net.minecraft.resources.ResourceLocation id) { this.id = id; }
    public GuideBuilder folder(String folder) { return this; }
    public <T extends guideme.extensions.Extension> GuideBuilder extension(guideme.extensions.ExtensionPoint<T> point, T extension) { return this; }
    public Guide build() { return () -> id; }
}
